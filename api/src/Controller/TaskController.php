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
use App\Service\TaskCacheService;
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
        private readonly UserCacheService $userCacheService,
        private readonly TaskCacheService $taskCacheService
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

        $fetcher = function () use ($entityManager, $organ) {
            $tasks = $entityManager->getRepository(Task::class)->findBy(['organ' => $organ, 'deletedAt' => null]);
            
            $data = [];
            foreach ($tasks as $task) {
                $data[] = $this->taskService->getTaskData($task, $this->userCacheService);
            }
            return $data;
        };

        return $this->json($this->taskCacheService->getTaskList($organ, $fetcher));
    }

    #[Route('/trash', name: 'trash', methods: ['GET'])]
    public function trash(string $projectUuid, string $organUuid, EntityManagerInterface $entityManager): JsonResponse
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

        // On récupère uniquement les tâches supprimées
        $queryBuilder = $entityManager->getRepository(Task::class)->createQueryBuilder('t')
            ->where('t.organ = :organ')
            ->andWhere('t.deletedAt IS NOT NULL')
            ->setParameter('organ', $organ);
        
        $tasks = $queryBuilder->getQuery()->getResult();
        
        $data = [];
        foreach ($tasks as $task) {
            $taskData = $this->taskService->getTaskData($task, $this->userCacheService);
            $taskData['deletedAt'] = $task->getDeletedAt()->format(\DateTimeInterface::ATOM);
            $data[] = $taskData;
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

        // Invalidate list cache
        $this->taskCacheService->invalidateList($organUuid);

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

        $summary = $this->taskCacheService->getTaskSummary($task, function () use ($task) {
            return $this->taskService->getTaskData($task, $this->userCacheService);
        });

        return $this->json($summary);
    }

    #[Route('/{taskUuid}', name: 'update', methods: ['PUT', 'PATCH'])]
    public function update(string $projectUuid, string $organUuid, string $taskUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ]);

        if (!$task) {
            return $this->json(['message' => 'Task not found'], Response::HTTP_NOT_FOUND);
        }

        if ($task->getDeletedAt() !== null) {
            return $this->json(['message' => 'Task is deleted and cannot be updated'], Response::HTTP_FORBIDDEN);
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

        // Invalidate cache
        $this->taskCacheService->invalidateSummary($taskUuid);
        $this->taskCacheService->invalidateList($organUuid);

        return $this->json($this->taskService->getTaskData($task, $this->userCacheService));
    }

    #[Route('/{taskUuid}', name: 'delete', methods: ['DELETE'])]
    public function delete(string $projectUuid, string $organUuid, string $taskUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);

        if (!$task) {
            return $this->json(['message' => 'Task not found'], Response::HTTP_NOT_FOUND);
        }

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->taskService->can($user, $task, 'TASK_DELETE')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $task->setDeletedAt(new \DateTime());
        $entityManager->flush();

        // Invalidate cache
        $this->taskCacheService->invalidateSummary($taskUuid);
        $this->taskCacheService->invalidateList($organUuid);

        return $this->json(['message' => 'Task deleted successfully']);
    }

    #[Route('/{taskUuid}/restore', name: 'restore', methods: ['POST'])]
    public function restore(string $projectUuid, string $organUuid, string $taskUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ]);

        if (!$task) {
            return $this->json(['message' => 'Task not found'], Response::HTTP_NOT_FOUND);
        }

        if ($task->getDeletedAt() === null) {
            return $this->json(['message' => 'Task is not deleted'], Response::HTTP_BAD_REQUEST);
        }

        /** @var User $user */
        $user = $this->getUser();
        // We use TASK_DELETE permission for restoring as well, or we could have a TASK_RESTORE
        if (!$this->taskService->can($user, $task, 'TASK_DELETE')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $task->setDeletedAt(null);
        $task->setUpdatedAt(new \DateTime());
        $entityManager->flush();

        // Invalidate cache
        $this->taskCacheService->invalidateSummary($taskUuid);
        $this->taskCacheService->invalidateList($organUuid);

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

    #[Route('/{taskUuid}/timeline', name: 'timeline', methods: ['GET'])]
    public function timeline(string $projectUuid, string $organUuid, string $taskUuid, EntityManagerInterface $entityManager): JsonResponse
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

        /*
        // VERSION SYMFONY PROPRE (ORM) :
        $comments = $task->getComments();
        $history = $task->getTaskHistories();
        $attachments = $task->getTaskAttachments();
        // fusionner et trier en PHP...
        */

        // VERSION SQL PURE (SI40 PRE-REQUIS : UNION + JOIN par UUID) :
        $conn = $entityManager->getConnection();
        $sql = "
            SELECT 'COMMENT' as type, tc.content as detail, tc.created_at 
            FROM task_comments tc
            JOIN tasks t ON tc.task_id = t.id
            WHERE t.uuid = :taskUuid AND tc.deleted_at IS NULL
            
            UNION ALL
            
            SELECT 'HISTORY' as type, CONCAT(th.action_type, ' ', COALESCE(th.field_name, '')) as detail, th.created_at 
            FROM task_history th
            JOIN tasks t ON th.task_id = t.id
            WHERE t.uuid = :taskUuid
            
            UNION ALL
            
            SELECT 'ATTACHMENT' as type, ta.file_name as detail, ta.created_at 
            FROM task_attachments ta
            JOIN tasks t ON ta.task_id = t.id
            WHERE t.uuid = :taskUuid AND ta.deleted_at IS NULL
            
            ORDER BY created_at DESC
        ";

        $resultSet = $conn->executeQuery($sql, ['taskUuid' => $taskUuid]);
        return $this->json($resultSet->fetchAllAssociative());
    }

    #[Route('/{taskUuid}/assignees', name: 'add_assignee', methods: ['POST'])]
    public function addAssignee(string $projectUuid, string $organUuid, string $taskUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ]);

        if (!$task) return $this->json(['message' => 'Task not found'], Response::HTTP_NOT_FOUND);

        if ($task->getDeletedAt() !== null) {
            return $this->json(['message' => 'Task is deleted and cannot be updated'], Response::HTTP_FORBIDDEN);
        }

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

        // Invalidate cache
        $this->taskCacheService->invalidateSummary($taskUuid);
        $this->taskCacheService->invalidateList($organUuid);

        return $this->json(['message' => 'User assigned successfully']);
    }
}
