<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\Organ;
use App\Entity\Project;
use App\Entity\Tag;
use App\Entity\Task;
use App\Entity\TaskTag;
use App\Entity\User;
use App\Service\TaskService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/tags', name: 'task_tags_')]
class TaskTagController extends AbstractController
{
    public function __construct(
        private readonly TaskService $taskService
    ) {}

    #[Route('', name: 'add', methods: ['POST'])]
    public function add(string $projectUuid, string $organUuid, string $taskUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);

        if (!$task) return $this->json(['message' => 'Task not found'], Response::HTTP_NOT_FOUND);

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->taskService->can($user, $task, 'TASK_TAG_MANAGE')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $data = json_decode($request->getContent(), true);
        if (!isset($data['tagUuid'])) return $this->json(['message' => 'tagUuid required'], Response::HTTP_BAD_REQUEST);

        $tag = $entityManager->getRepository(Tag::class)->findOneBy(['uuid' => $data['tagUuid'], 'project' => $project, 'deletedAt' => null]);
        if (!$tag) return $this->json(['message' => 'Tag not found in this project'], Response::HTTP_NOT_FOUND);

        $existing = $entityManager->getRepository(TaskTag::class)->findOneBy(['task' => $task, 'tag' => $tag]);
        if ($existing && $existing->getDeletedAt() === null) {
            return $this->json(['message' => 'Tag already added to task'], Response::HTTP_CONFLICT);
        }

        if ($existing) {
            $existing->setDeletedAt(null);
        } else {
            $taskTag = new TaskTag();
            $taskTag->setTask($task);
            $taskTag->setTag($tag);
            $entityManager->persist($taskTag);
        }

        $entityManager->flush();

        return $this->json(['message' => 'Tag added successfully']);
    }

    #[Route('/{tagUuid}', name: 'remove', methods: ['DELETE'])]
    public function remove(string $projectUuid, string $organUuid, string $taskUuid, string $tagUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);
        $tag = $entityManager->getRepository(Tag::class)->findOneBy(['uuid' => $tagUuid, 'project' => $project]);

        if (!$task || !$tag) return $this->json(['message' => 'Resource not found'], Response::HTTP_NOT_FOUND);

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->taskService->can($user, $task, 'TASK_TAG_MANAGE')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $taskTag = $entityManager->getRepository(TaskTag::class)->findOneBy(['task' => $task, 'tag' => $tag, 'deletedAt' => null]);
        if ($taskTag) {
            $taskTag->setDeletedAt(new \DateTime());
            $entityManager->flush();
        }

        return $this->json(null, Response::HTTP_NO_CONTENT);
    }
}
