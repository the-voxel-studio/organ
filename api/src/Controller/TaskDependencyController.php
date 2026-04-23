<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\Organ;
use App\Entity\Project;
use App\Entity\Task;
use App\Entity\TaskDependency;
use App\Entity\User;
use App\Service\TaskService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/dependencies', name: 'task_dependencies_')]
class TaskDependencyController extends AbstractController
{
    public function __construct(
        private readonly TaskService $taskService
    ) {}

    #[Route('', name: 'index', methods: ['GET'])]
    public function index(string $projectUuid, string $organUuid, string $taskUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);

        if (!$task) return $this->json(['message' => 'Task not found'], Response::HTTP_NOT_FOUND);

        $dependencies = $entityManager->getRepository(TaskDependency::class)->findBy(['task' => $task, 'deletedAt' => null]);
        
        $data = [];
        foreach ($dependencies as $dep) {
            $depOn = $dep->getDependsOnTask();
            $data[] = [
                'dependsOnTaskUuid' => $depOn->getUuid(),
                'title' => $depOn->getTitle(),
                'status' => $depOn->getStatus()->value,
            ];
        }

        return $this->json($data);
    }

    #[Route('/trash', name: 'trash', methods: ['GET'])]
    public function trash(string $projectUuid, string $organUuid, string $taskUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);

        if (!$task) return $this->json(['message' => 'Task not found'], Response::HTTP_NOT_FOUND);

        $dependencies = $entityManager->getRepository(TaskDependency::class)->createQueryBuilder('d')
            ->where('d.task = :task')
            ->andWhere('d.deletedAt IS NOT NULL')
            ->setParameter('task', $task)
            ->getQuery()
            ->getResult();
        
        $data = [];
        foreach ($dependencies as $dep) {
            $depOn = $dep->getDependsOnTask();
            $data[] = [
                'dependsOnTaskUuid' => $depOn->getUuid(),
                'title' => $depOn->getTitle(),
                'status' => $depOn->getStatus()->value,
                'deletedAt' => $dep->getDeletedAt()->format(\DateTimeInterface::ATOM),
            ];
        }

        return $this->json($data);
    }

    #[Route('/{targetTaskUuid}/restore', name: 'restore', methods: ['POST'])]
    public function restore(string $projectUuid, string $organUuid, string $taskUuid, string $targetTaskUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);
        
        $targetTask = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $targetTaskUuid, 'organ' => $organ]);
        if (!$targetTask) return $this->json(['message' => 'Target task not found'], Response::HTTP_NOT_FOUND);

        $dep = $entityManager->getRepository(TaskDependency::class)->findOneBy(['task' => $task, 'dependsOnTask' => $targetTask]);

        if (!$dep) return $this->json(['message' => 'Dependency not found'], Response::HTTP_NOT_FOUND);

        if ($dep->getDeletedAt() === null) {
            return $this->json(['message' => 'Dependency is not deleted'], Response::HTTP_BAD_REQUEST);
        }

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->taskService->can($user, $task, 'TASK_DEPENDENCY_MANAGE')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $dep->setDeletedAt(null);
        $entityManager->flush();

        return $this->json(['message' => 'Dependency restored successfully']);
    }

    #[Route('', name: 'add', methods: ['POST'])]
    public function add(string $projectUuid, string $organUuid, string $taskUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ]);

        if (!$task) return $this->json(['message' => 'Task not found'], Response::HTTP_NOT_FOUND);

        if ($task->getDeletedAt() !== null) {
            return $this->json(['message' => 'Task is deleted and cannot be modified'], Response::HTTP_FORBIDDEN);
        }

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->taskService->can($user, $task, 'TASK_DEPENDENCY_MANAGE')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $data = json_decode($request->getContent(), true);
        if (!isset($data['dependsOnTaskUuid'])) return $this->json(['message' => 'dependsOnTaskUuid required'], Response::HTTP_BAD_REQUEST);

        $dependsOnTask = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $data['dependsOnTaskUuid'], 'organ' => $organ, 'deletedAt' => null]);
        if (!$dependsOnTask) return $this->json(['message' => 'Target task not found in the same organ'], Response::HTTP_NOT_FOUND);

        if ($task === $dependsOnTask) return $this->json(['message' => 'A task cannot depend on itself'], Response::HTTP_BAD_REQUEST);

        $existing = $entityManager->getRepository(TaskDependency::class)->findOneBy(['task' => $task, 'dependsOnTask' => $dependsOnTask]);
        if ($existing && $existing->getDeletedAt() === null) {
            return $this->json(['message' => 'Dependency already exists'], Response::HTTP_CONFLICT);
        }

        if ($existing) {
            $existing->setDeletedAt(null);
        } else {
            $dep = new TaskDependency();
            $dep->setTask($task);
            $dep->setDependsOnTask($dependsOnTask);
            $entityManager->persist($dep);
        }

        $entityManager->flush();

        return $this->json(['message' => 'Dependency added successfully']);
    }

    #[Route('/{targetTaskUuid}', name: 'remove', methods: ['DELETE'])]
    public function remove(string $projectUuid, string $organUuid, string $taskUuid, string $targetTaskUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);

        $isPermanent = $request->query->getBoolean('permanent', false);
        
        $targetTask = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $targetTaskUuid, 'organ' => $organ]);
        if (!$targetTask) return $this->json(['message' => 'Target task not found'], Response::HTTP_NOT_FOUND);

        $criteria = ['task' => $task, 'dependsOnTask' => $targetTask];
        if (!$isPermanent) {
            $criteria['deletedAt'] = null;
        }

        $dep = $entityManager->getRepository(TaskDependency::class)->findOneBy($criteria);

        if (!$dep) return $this->json(['message' => 'Dependency not found'], Response::HTTP_NOT_FOUND);

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->taskService->can($user, $task, 'TASK_DEPENDENCY_MANAGE')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        if ($isPermanent) {
            $entityManager->remove($dep);
        } else {
            $dep->setDeletedAt(new \DateTime());
        }
        
        $entityManager->flush();

        return $this->json(null, Response::HTTP_NO_CONTENT);
    }
}
