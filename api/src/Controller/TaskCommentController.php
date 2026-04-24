<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\Organ;
use App\Entity\Project;
use App\Entity\Task;
use App\Entity\TaskComment;
use App\Entity\User;
use App\Service\OrganPermissionService;
use App\Service\TaskCacheService;
use App\Service\TaskService;
use App\Service\UserCacheService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/comments', name: 'task_comments_')]
class TaskCommentController extends AbstractController
{
    public function __construct(
        private readonly TaskService $taskService,
        private readonly OrganPermissionService $permissionService,
        private readonly UserCacheService $userCacheService,
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

        $comments = $entityManager->getRepository(TaskComment::class)->findBy(['task' => $task, 'deletedAt' => null], ['createdAt' => 'ASC']);
        
        $data = [];
        foreach ($comments as $comment) {
            $data[] = [
                'uuid' => $comment->getUuid(),
                'content' => $comment->getContent(),
                'user' => $this->userCacheService->getUserSummary($comment->getUser()),
                'createdAt' => $comment->getCreatedAt()->format(\DateTimeInterface::ATOM),
                'updatedAt' => $comment->getUpdatedAt()->format(\DateTimeInterface::ATOM),
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

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_VIEW')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $comments = $entityManager->getRepository(TaskComment::class)->createQueryBuilder('c')
            ->where('c.task = :task')
            ->andWhere('c.deletedAt IS NOT NULL')
            ->setParameter('task', $task)
            ->getQuery()
            ->getResult();
        
        $data = [];
        foreach ($comments as $comment) {
            $data[] = [
                'uuid' => $comment->getUuid(),
                'content' => $comment->getContent(),
                'user' => $this->userCacheService->getUserSummary($comment->getUser()),
                'deletedAt' => $comment->getDeletedAt()->format(\DateTimeInterface::ATOM),
            ];
        }

        return $this->json($data);
    }

    #[Route('', name: 'create', methods: ['POST'])]
    public function create(string $projectUuid, string $organUuid, string $taskUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ]);

        if (!$task) return $this->json(['message' => 'Task not found'], Response::HTTP_NOT_FOUND);

        if ($task->getDeletedAt() !== null) {
            return $this->json(['message' => 'Task is deleted and cannot be updated'], Response::HTTP_FORBIDDEN);
        }

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->taskService->can($user, $task, 'COMMENT_CREATE')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $data = json_decode($request->getContent(), true);
        if (!$data || !isset($data['content'])) {
            return $this->json(['message' => 'Content is required'], Response::HTTP_BAD_REQUEST);
        }

        $comment = new TaskComment();
        $comment->setTask($task);
        $comment->setUser($user);
        $comment->setContent($data['content']);

        $entityManager->persist($comment);
        $entityManager->flush();

        $this->taskCacheService->invalidateSummary($taskUuid);
        $this->taskCacheService->invalidateList($organUuid);

        return $this->json([
            'uuid' => $comment->getUuid(),
            'content' => $comment->getContent(),
            'user' => $this->userCacheService->getUserSummary($user),
            'createdAt' => $comment->getCreatedAt()->format(\DateTimeInterface::ATOM),
        ], Response::HTTP_CREATED);
    }

    #[Route('/{commentUuid}', name: 'update', methods: ['PUT', 'PATCH'])]
    public function update(string $projectUuid, string $organUuid, string $taskUuid, string $commentUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);
        $comment = $entityManager->getRepository(TaskComment::class)->findOneBy(['uuid' => $commentUuid, 'task' => $task]);

        if (!$comment) return $this->json(['message' => 'Comment not found'], Response::HTTP_NOT_FOUND);

        if ($comment->getDeletedAt() !== null) {
            return $this->json(['message' => 'Comment is deleted and cannot be updated'], Response::HTTP_FORBIDDEN);
        }

        /** @var User $user */
        $user = $this->getUser();
        
        // Custom permission check for edit
        $isOwner = ($comment->getUser() === $user);
        $permAll = $this->permissionService->hasPermission($user, $organ, 'COMMENT_EDIT_ALL');
        $permOwn = $isOwner && $this->permissionService->hasPermission($user, $organ, 'COMMENT_EDIT_OWN');

        if (!$permAll && !$permOwn) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $data = json_decode($request->getContent(), true);
        if (isset($data['content'])) {
            $comment->setContent($data['content']);
            $comment->setUpdatedAt(new \DateTime());
        }

        $entityManager->flush();

        $this->taskCacheService->invalidateSummary($taskUuid);
        $this->taskCacheService->invalidateList($organUuid);

        return $this->json(['message' => 'Comment updated']);
    }

    #[Route('/{commentUuid}/restore', name: 'restore', methods: ['POST'])]
    public function restore(string $projectUuid, string $organUuid, string $taskUuid, string $commentUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);
        $comment = $entityManager->getRepository(TaskComment::class)->findOneBy(['uuid' => $commentUuid, 'task' => $task]);

        if (!$comment) return $this->json(['message' => 'Comment not found'], Response::HTTP_NOT_FOUND);

        if ($comment->getDeletedAt() === null) {
            return $this->json(['message' => 'Comment is not deleted'], Response::HTTP_BAD_REQUEST);
        }

        /** @var User $user */
        $user = $this->getUser();
        
        // Use DELETE permission for restore
        $isOwner = ($comment->getUser() === $user);
        $permAll = $this->permissionService->hasPermission($user, $organ, 'COMMENT_DELETE_ALL');
        $permOwn = $isOwner && $this->permissionService->hasPermission($user, $organ, 'COMMENT_DELETE_OWN');

        if (!$permAll && !$permOwn) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $comment->setDeletedAt(null);
        $entityManager->flush();

        $this->taskCacheService->invalidateSummary($taskUuid);
        $this->taskCacheService->invalidateList($organUuid);

        return $this->json(['message' => 'Comment restored']);
    }

    #[Route('/{commentUuid}', name: 'delete', methods: ['DELETE'])]
    public function delete(string $projectUuid, string $organUuid, string $taskUuid, string $commentUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);
        
        $isPermanent = $request->query->getBoolean('permanent', false);
        
        // If permanent, we can delete even if already soft-deleted
        $criteria = ['uuid' => $commentUuid, 'task' => $task];
        if (!$isPermanent) {
            $criteria['deletedAt'] = null;
        }
        
        $comment = $entityManager->getRepository(TaskComment::class)->findOneBy($criteria);

        if (!$comment) return $this->json(['message' => 'Comment not found'], Response::HTTP_NOT_FOUND);

        /** @var User $user */
        $user = $this->getUser();
        
        $isOwner = ($comment->getUser() === $user);
        
        if ($isPermanent) {
            $permAll = $this->permissionService->hasPermission($user, $organ, 'COMMENT_HARD_DELETE_ALL');
            $permOwn = $isOwner && $this->permissionService->hasPermission($user, $organ, 'COMMENT_HARD_DELETE_OWN');
            
            if (!$permAll && !$permOwn) {
                return $this->json(['message' => 'Access denied for permanent deletion'], Response::HTTP_FORBIDDEN);
            }
            
            $entityManager->remove($comment);
        } else {
            $permAll = $this->permissionService->hasPermission($user, $organ, 'COMMENT_DELETE_ALL');
            $permOwn = $isOwner && $this->permissionService->hasPermission($user, $organ, 'COMMENT_DELETE_OWN');

            if (!$permAll && !$permOwn) {
                return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
            }
            
            $comment->setDeletedAt(new \DateTime());
        }
        
        $entityManager->flush();

        $this->taskCacheService->invalidateSummary($taskUuid);
        $this->taskCacheService->invalidateList($organUuid);

        return $this->json(null, Response::HTTP_NO_CONTENT);
    }
}
