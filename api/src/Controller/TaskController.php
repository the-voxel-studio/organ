<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\Organ;
use App\Entity\Project;
use App\Entity\Task;
use App\Entity\TaskAssignee;
use App\Entity\User;
use App\Enum\TaskStatus;
use App\Service\OrganPermissionService;
use App\Service\TaskService;
use App\Service\UserCacheService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Validator\Validator\ValidatorInterface;

#[Route('/projects/{projectUuid}/organs/{organUuid}/tasks', name: 'tasks_')]
class TaskController extends AbstractController
{
    public function __construct(
        private readonly TaskService $taskService,
        private readonly OrganPermissionService $permissionService,
        private readonly UserCacheService $userCacheService
    ) {}

    #[Route('', name: 'index', methods: ['GET'])]
    public function index(string $projectUuid, string $organUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);

        if (!$organ) {
            return $this->json(['message' => 'Organ not found'], Response::HTTP_NOT_FOUND);
        }

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_VIEW')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $tasks = $entityManager->getRepository(Task::class)->findBy(['organ' => $organ, 'deletedAt' => null]);
        
        $data = [];
        foreach ($tasks as $task) {
            $data[] = $this->taskService->getTaskData($task, $this->userCacheService);
        }

        return $this->json($data);
    }

    #[Route('', name: 'create', methods: ['POST'])]
    public function create(string $projectUuid, string $organUuid, Request $request, EntityManagerInterface $entityManager, ValidatorInterface $validator): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);

        if (!$organ) {
            return $this->json(['message' => 'Organ not found'], Response::HTTP_NOT_FOUND);
        }

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'TASK_CREATE')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $data = json_decode($request->getContent(), true);
        if (!$data || !isset($data['title'])) {
            return $this->json(['message' => 'Missing required fields'], Response::HTTP_BAD_REQUEST);
        }

        $task = new Task();
        $task->setOrgan($organ);
        $task->setCreatedBy($user);
        $task->setTitle($data['title']);
        $task->setDescription($data['description'] ?? null);
        $task->setPriority($data['priority'] ?? 1);
        
        if (isset($data['managerUuid'])) {
            $manager = $entityManager->getRepository(User::class)->findOneBy(['uuid' => $data['managerUuid']]);
            if ($manager) $task->setManager($manager);
        }

        $errors = $validator->validate($task);
        if (count($errors) > 0) {
            return $this->json($errors, Response::HTTP_BAD_REQUEST);
        }

        $entityManager->persist($task);
        $entityManager->flush();

        return $this->json($this->taskService->getTaskData($task, $this->userCacheService), Response::HTTP_CREATED);
    }

    #[Route('/{taskUuid}', name: 'show', methods: ['GET'])]
    public function show(string $projectUuid, string $organUuid, string $taskUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);

        if (!$task) {
            return $this->json(['message' => 'Task not found'], Response::HTTP_NOT_FOUND);
        }

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_VIEW')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        return $this->json($this->taskService->getTaskData($task, $this->userCacheService));
    }

    #[Route('/{taskUuid}', name: 'update', methods: ['PUT', 'PATCH'])]
    public function update(string $projectUuid, string $organUuid, string $taskUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);

        if (!$task) {
            return $this->json(['message' => 'Task not found'], Response::HTTP_NOT_FOUND);
        }

        /** @var User $user */
        $user = $this->getUser();
        $data = json_decode($request->getContent(), true);

        // 1. Check Edit Rights
        if (!$this->taskService->can($user, $task, 'TASK_EDIT')) {
            return $this->json(['message' => 'Access denied to edit this task'], Response::HTTP_FORBIDDEN);
        }

        // 2. Specialized permissions for specific fields
        if (isset($data['status'])) {
            $status = TaskStatus::tryFrom($data['status']);
            if (!$status || !$this->taskService->canChangeStatus($user, $task, $status)) {
                return $this->json(['message' => 'Permission denied to change to this status or invalid status'], Response::HTTP_FORBIDDEN);
            }
            $task->setStatus($status);
        }

        if (isset($data['priority'])) {
            if (!$this->taskService->canEditField($user, $task, 'priority')) {
                return $this->json(['message' => 'Permission denied to change priority'], Response::HTTP_FORBIDDEN);
            }
            $task->setPriority((int)$data['priority']);
        }

        if (isset($data['expiresAt']) || isset($data['startDate'])) {
            if (!$this->taskService->canEditField($user, $task, 'expiresAt')) {
                return $this->json(['message' => 'Permission denied to manage dates'], Response::HTTP_FORBIDDEN);
            }
            if (isset($data['startDate'])) $task->setStartDate(new \DateTime($data['startDate']));
            if (isset($data['expiresAt'])) $task->setExpiresAt(new \DateTime($data['expiresAt']));
        }

        if (isset($data['estimatedHours'])) {
            if (!$this->taskService->canEditField($user, $task, 'estimatedHours')) {
                return $this->json(['message' => 'Permission denied to manage estimates'], Response::HTTP_FORBIDDEN);
            }
            $task->setEstimatedHours((string)$data['estimatedHours']);
        }

        if (isset($data['title'])) {
            if (!$this->taskService->canEditField($user, $task, 'title')) {
                return $this->json(['message' => 'Permission denied to change title'], Response::HTTP_FORBIDDEN);
            }
            $task->setTitle($data['title']);
        }

        if (isset($data['description'])) {
            if (!$this->taskService->canEditField($user, $task, 'description')) {
                return $this->json(['message' => 'Permission denied to change description'], Response::HTTP_FORBIDDEN);
            }
            $task->setDescription($data['description']);
        }

        $task->setUpdatedAt(new \DateTime());
        $entityManager->flush();

        return $this->json($this->taskService->getTaskData($task, $this->userCacheService));
    }

    #[Route('/{taskUuid}/permissions', name: 'permissions', methods: ['GET'])]
    public function permissions(string $projectUuid, string $organUuid, string $taskUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);

        if (!$task) return $this->json(['message' => 'Task not found'], Response::HTTP_NOT_FOUND);

        /** @var User $user */
        $user = $this->getUser();
        
        $actions = [];
        $checkActions = [
            'TASK_EDIT' => 'edit',
            'TASK_DELETE' => 'delete',
            'TASK_STATUS_CHANGE' => 'change_status',
            'TASK_PRIORITY_CHANGE' => 'change_priority',
            'TASK_DATES_MANAGE' => 'manage_dates',
            'TASK_ESTIMATE_MANAGE' => 'manage_estimates',
            'TASK_ASSIGN_OTHERS' => 'assign_others',
            'TASK_ASSIGN_SELF' => 'assign_self',
            'TASK_VALIDATE' => 'validate',
            'COMMENT_CREATE' => 'add_comment',
            'ATTACHMENT_ADD' => 'add_attachment',
            'TASK_LINK_MANAGE' => 'manage_links',
            'TASK_TAG_MANAGE' => 'manage_tags',
            'TASK_DEPENDENCY_MANAGE' => 'manage_dependencies',
        ];

        foreach ($checkActions as $perm => $action) {
            if ($this->taskService->can($user, $task, $perm)) {
                $actions[] = $action;
            }
        }

        // Add granular field edit permissions
        $fields = ['priority', 'expiresAt', 'estimatedHours', 'title', 'description', 'manager'];
        $editableFields = [];
        foreach ($fields as $field) {
            if ($this->taskService->canEditField($user, $task, $field)) {
                $editableFields[] = $field;
            }
        }

        return $this->json([
            'actions' => $actions,
            'editableFields' => $editableFields,
            'isManager' => ($task->getManager() === $user),
            'isAssignee' => $this->taskService->isAssignee($user, $task),
        ]);
    }

    #[Route('/{taskUuid}/assignees', name: 'add_assignee', methods: ['POST'])]
    public function addAssignee(string $projectUuid, string $organUuid, string $taskUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);

        if (!$task) return $this->json(['message' => 'Task not found'], Response::HTTP_NOT_FOUND);

        /** @var User $currentUser */
        $currentUser = $this->getUser();
        $data = json_decode($request->getContent(), true);
        
        if (!isset($data['userUuid'])) return $this->json(['message' => 'userUuid required'], Response::HTTP_BAD_REQUEST);

        $targetUser = $entityManager->getRepository(User::class)->findOneBy(['uuid' => $data['userUuid'], 'deletedAt' => null]);
        if (!$targetUser) return $this->json(['message' => 'User not found'], Response::HTTP_NOT_FOUND);

        $isSelf = ($targetUser === $currentUser);
        $action = $isSelf ? 'TASK_ASSIGN_SELF' : 'TASK_ASSIGN_OTHERS';

        if (!$this->taskService->can($currentUser, $task, $action)) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $existing = $entityManager->getRepository(TaskAssignee::class)->findOneBy(['task' => $task, 'user' => $targetUser]);
        if ($existing && $existing->getDeletedAt() === null) {
            return $this->json(['message' => 'User already assigned'], Response::HTTP_CONFLICT);
        }

        if ($existing) {
            $existing->setDeletedAt(null);
        } else {
            $assignee = new TaskAssignee();
            $assignee->setTask($task);
            $assignee->setUser($targetUser);
            $entityManager->persist($assignee);
        }

        $entityManager->flush();

        return $this->json(['message' => 'User assigned successfully']);
    }
}
