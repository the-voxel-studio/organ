<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\Organ;
use App\Entity\Project;
use App\Entity\Task;
use App\Entity\TaskLink;
use App\Entity\User;
use App\Service\OrganPermissionService;
use App\Service\TaskService;
use App\Service\TaskCacheService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/links', name: 'task_links_')]
class TaskLinkController extends AbstractController
{
    public function __construct(
        private readonly TaskService $taskService,
        private readonly OrganPermissionService $permissionService,
        private readonly TaskCacheService $taskCacheService
    ) {}

    #[Route('', name: 'index', methods: ['GET'])]
    public function index(string $projectUuid, string $organUuid, string $taskUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);

        if (!$task) return $this->json(['message' => 'Task not found'], Response::HTTP_NOT_FOUND);

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_VIEW')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $links = $entityManager->getRepository(TaskLink::class)->findBy(['task' => $task, 'deletedAt' => null]);
        
        $data = [];
        foreach ($links as $link) {
            $data[] = [
                'uuid' => $link->getUuid(),
                'url' => $link->getUrl(),
                'description' => $link->getDescription()
            ];
        }

        return $this->json($data);
    }

    #[Route('', name: 'create', methods: ['POST'])]
    public function create(string $projectUuid, string $organUuid, string $taskUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);

        if (!$task) return $this->json(['message' => 'Task not found'], Response::HTTP_NOT_FOUND);

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->taskService->can($user, $task, 'TASK_LINK_MANAGE')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $data = json_decode($request->getContent(), true);
        if (!isset($data['url'])) {
            return $this->json(['message' => 'Missing URL'], Response::HTTP_BAD_REQUEST);
        }

        $link = new TaskLink();
        $link->setTask($task);
        $link->setUrl($data['url']);
        $link->setDescription($data['description'] ?? null);

        $entityManager->persist($link);
        $entityManager->flush();

        // Invalidate cache
        $this->taskCacheService->invalidateSummary($taskUuid);
        $this->taskCacheService->invalidateList($organUuid);

        return $this->json([
            'uuid' => $link->getUuid(),
            'url' => $link->getUrl()
        ], Response::HTTP_CREATED);
    }

    #[Route('/{linkUuid}', name: 'delete', methods: ['DELETE'])]
    public function delete(string $projectUuid, string $organUuid, string $taskUuid, string $linkUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);
        
        $isPermanent = $request->query->getBoolean('permanent', false);
        $criteria = ['uuid' => $linkUuid, 'task' => $task];
        if (!$isPermanent) {
            $criteria['deletedAt'] = null;
        }
        
        $link = $entityManager->getRepository(TaskLink::class)->findOneBy($criteria);

        if (!$link) return $this->json(['message' => 'Link not found'], Response::HTTP_NOT_FOUND);

        /** @var User $user */
        $user = $this->getUser();
        
        $isCreator = ($task->getCreatedBy() === $user);
        $isManager = ($task->getManager() === $user);
        $isOwner = $isCreator || $isManager;

        if ($isPermanent) {
            $permAll = $this->permissionService->hasPermission($user, $organ, 'TASK_LINK_HARD_DELETE_ALL');
            $permOwn = $isOwner && $this->permissionService->hasPermission($user, $organ, 'TASK_LINK_HARD_DELETE_OWN');
            
            if (!$permAll && !$permOwn && !$this->permissionService->hasPermission($user, $organ, 'ORGAN_EDIT')) {
                return $this->json(['message' => 'Access denied for permanent deletion'], Response::HTTP_FORBIDDEN);
            }
            
            $entityManager->remove($link);
        } else {
            if (!$this->taskService->can($user, $task, 'TASK_LINK_MANAGE')) {
                return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
            }
            $link->setDeletedAt(new \DateTime());
        }

        $entityManager->flush();

        // Invalidate cache
        $this->taskCacheService->invalidateSummary($taskUuid);
        $this->taskCacheService->invalidateList($organUuid);

        return $this->json(null, Response::HTTP_NO_CONTENT);
    }

    #[Route('/trash', name: 'trash', methods: ['GET'], priority: 1)]
    public function trash(string $projectUuid, string $organUuid, string $taskUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);

        if (!$task) return $this->json(['message' => 'Task not found'], Response::HTTP_NOT_FOUND);

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_VIEW')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $links = $entityManager->getRepository(TaskLink::class)->createQueryBuilder('l')
            ->where('l.task = :task')
            ->andWhere('l.deletedAt IS NOT NULL')
            ->setParameter('task', $task)
            ->orderBy('l.deletedAt', 'DESC')
            ->getQuery()
            ->getResult();
        
        $data = [];
        foreach ($links as $link) {
            $data[] = [
                'uuid' => $link->getUuid(),
                'url' => $link->getUrl(),
                'description' => $link->getDescription(),
                'deletedAt' => $link->getDeletedAt()->format(\DateTimeInterface::ATOM)
            ];
        }

        return $this->json($data);
    }

    #[Route('/{linkUuid}/restore', name: 'restore', methods: ['POST'])]
    public function restore(string $projectUuid, string $organUuid, string $taskUuid, string $linkUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);
        $link = $entityManager->getRepository(TaskLink::class)->findOneBy(['uuid' => $linkUuid, 'task' => $task]);

        if (!$link) return $this->json(['message' => 'Link not found'], Response::HTTP_NOT_FOUND);

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->taskService->can($user, $task, 'TASK_LINK_MANAGE')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $link->setDeletedAt(null);
        $entityManager->flush();

        // Invalidate cache
        $this->taskCacheService->invalidateSummary($taskUuid);
        $this->taskCacheService->invalidateList($organUuid);

        return $this->json([
            'uuid' => $link->getUuid(),
            'url' => $link->getUrl()
        ]);
    }
}
