<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\Organ;
use App\Entity\Project;
use App\Entity\Tag;
use App\Entity\Task;
use App\Entity\TaskAssignee;
use App\Entity\TaskDependency;
use App\Entity\TaskLink;
use App\Entity\TaskTag;
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

        // Check for TASK_DELETE permissions
        $canDeleteAll = $this->permissionService->hasPermission($user, $organ, 'TASK_DELETE_ALL') || 
                        $this->permissionService->hasPermission($user, $organ, 'ORGAN_EDIT') || 
                        $this->permissionService->hasPermission($user, $organ, 'ALL');
        
        $canDeleteOwn = $this->permissionService->hasPermission($user, $organ, 'TASK_DELETE_OWN');

        if (!$canDeleteAll && !$canDeleteOwn) {
            return $this->json([]); // User cannot see trash if they can't delete anything
        }

        // On récupère uniquement les tâches supprimées
        $queryBuilder = $entityManager->getRepository(Task::class)->createQueryBuilder('t')
            ->where('t.organ = :organ')
            ->andWhere('t.deletedAt IS NOT NULL')
            ->setParameter('organ', $organ);

        if (!$canDeleteAll) {
            // Filter by ownership if they only have TASK_DELETE_OWN
            $queryBuilder->andWhere('t.createdBy = :user OR t.manager = :user')
                ->setParameter('user', $user);
        }
        
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
        $task->setPriority((int)($data['priority'] ?? 1));
        
        if (isset($data['status'])) {
            $status = TaskStatus::tryFrom($data['status']);
            if ($status) $task->setStatus($status);
        }

        if (isset($data['statusMessage'])) {
            $task->setStatusMessage(empty($data['statusMessage']) ? null : $data['statusMessage']);
        }

        if (isset($data['estimatedHours'])) {
            $task->setEstimatedHours(empty($data['estimatedHours']) ? null : (string)$data['estimatedHours']);
        }

        if (!empty($data['startDate'])) {
            $task->setStartDate(new \DateTime($data['startDate']));
        }

        if (!empty($data['expiresAt'])) {
            $task->setExpiresAt(new \DateTime($data['expiresAt']));
        }
        
        if (!empty($data['managerUuid'])) {
            $manager = $entityManager->getRepository(User::class)->findOneBy(['uuid' => $data['managerUuid']]);
            if ($manager) $task->setManager($manager);
        }

        $errors = $validator->validate($task);
        if (count($errors) > 0) {
            return $this->json($errors, Response::HTTP_BAD_REQUEST);
        }

        $entityManager->persist($task);

        // Handle Assignees during creation
        if (!empty($data['assigneeUuids']) && is_array($data['assigneeUuids'])) {
            foreach ($data['assigneeUuids'] as $userUuid) {
                $assigneeUser = $entityManager->getRepository(User::class)->findOneBy(['uuid' => $userUuid, 'deletedAt' => null]);
                if ($assigneeUser) {
                    $assignee = new TaskAssignee();
                    $assignee->setTask($task);
                    $assignee->setUser($assigneeUser);
                    $entityManager->persist($assignee);
                }
            }
        }

        // Handle Tags during creation
        if (!empty($data['tagUuids']) && is_array($data['tagUuids'])) {
            foreach ($data['tagUuids'] as $tagUuid) {
                $tag = $entityManager->getRepository(\App\Entity\Tag::class)->findOneBy(['uuid' => $tagUuid, 'project' => $project, 'deletedAt' => null]);
                if ($tag) {
                    $taskTag = new \App\Entity\TaskTag();
                    $taskTag->setTask($task);
                    $taskTag->setTag($tag);
                    $entityManager->persist($taskTag);
                }
            }
        }

        // Handle Dependencies during creation
        if (!empty($data['dependencyUuids']) && is_array($data['dependencyUuids'])) {
            foreach ($data['dependencyUuids'] as $depUuid) {
                $depTask = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $depUuid, 'organ' => $organ, 'deletedAt' => null]);
                if ($depTask && $depTask !== $task) {
                    $dep = new TaskDependency();
                    $dep->setTask($task);
                    $dep->setDependsOnTask($depTask);
                    $entityManager->persist($dep);
                }
            }
        }

        // Handle Links during creation
        if (!empty($data['linkData']) && is_array($data['linkData'])) {
            foreach ($data['linkData'] as $linkItem) {
                if (!empty($linkItem['url'])) {
                    $link = new TaskLink();
                    $link->setTask($task);
                    $link->setUrl($linkItem['url']);
                    $link->setDescription($linkItem['description'] ?? null);
                    $entityManager->persist($link);
                }
            }
        }

        $entityManager->flush();

        // Invalidate list cache
        $this->taskCacheService->invalidateList($organUuid);

        return $this->json($this->taskService->getTaskData($task, $this->userCacheService), Response::HTTP_CREATED);
    }

    #[Route('/{taskUuid}', name: 'show', methods: ['GET'])]
    public function show(string $projectUuid, string $organUuid, string $taskUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        
        $showTrashed = $request->query->getBoolean('trashed', false);
        $criteria = ['uuid' => $taskUuid, 'organ' => $organ];
        if (!$showTrashed) {
            $criteria['deletedAt'] = null;
        }
        
        $task = $entityManager->getRepository(Task::class)->findOneBy($criteria);

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
    public function update(string $projectUuid, string $organUuid, string $taskUuid, Request $request, EntityManagerInterface $entityManager, ValidatorInterface $validator): JsonResponse
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

        if (isset($data['statusMessage'])) {
            if (!$this->taskService->can($user, $task, 'TASK_STATUS_CHANGE')) {
                return $this->json(['message' => 'Permission denied to change status message'], Response::HTTP_FORBIDDEN);
            }
            $task->setStatusMessage(empty($data['statusMessage']) ? null : $data['statusMessage']);
        }

        if (isset($data['priority'])) {
            if (!$this->taskService->canEditField($user, $task, 'priority')) {
                return $this->json(['message' => 'Permission denied to change priority'], Response::HTTP_FORBIDDEN);
            }
            $task->setPriority((int)$data['priority']);
        }

        if (array_key_exists('startDate', $data) || array_key_exists('expiresAt', $data)) {
            if (!$this->taskService->canEditField($user, $task, 'expiresAt')) {
                return $this->json(['message' => 'Permission denied to manage dates'], Response::HTTP_FORBIDDEN);
            }
            if (array_key_exists('startDate', $data)) {
                $task->setStartDate(empty($data['startDate']) ? null : new \DateTime($data['startDate']));
            }
            if (array_key_exists('expiresAt', $data)) {
                $task->setExpiresAt(empty($data['expiresAt']) ? null : new \DateTime($data['expiresAt']));
            }
        }

        if (isset($data['estimatedHours'])) {
            if (!$this->taskService->canEditField($user, $task, 'estimatedHours')) {
                return $this->json(['message' => 'Permission denied to manage estimates'], Response::HTTP_FORBIDDEN);
            }
            $task->setEstimatedHours(empty($data['estimatedHours']) ? null : (string)$data['estimatedHours']);
        }

        if (array_key_exists('managerUuid', $data)) {
            if (!$this->taskService->canEditField($user, $task, 'manager')) {
                return $this->json(['message' => 'Permission denied to change manager'], Response::HTTP_FORBIDDEN);
            }
            if (empty($data['managerUuid'])) {
                $task->setManager(null);
            } else {
                $manager = $entityManager->getRepository(User::class)->findOneBy(['uuid' => $data['managerUuid']]);
                if ($manager) $task->setManager($manager);
            }
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
            $task->setDescription(empty($data['description']) ? null : $data['description']);
        }

        $errors = $validator->validate($task);
        if (count($errors) > 0) {
            return $this->json($errors, Response::HTTP_BAD_REQUEST);
        }

        $task->setUpdatedAt(new \DateTime());
        $entityManager->flush();

        // Invalidate cache
        $this->taskCacheService->invalidateSummary($taskUuid);
        $this->taskCacheService->invalidateList($organUuid);

        return $this->json($this->taskService->getTaskData($task, $this->userCacheService));
    }

    #[Route('/{taskUuid}', name: 'delete', methods: ['DELETE'])]
    public function delete(string $projectUuid, string $organUuid, string $taskUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        
        $isPermanent = $request->query->getBoolean('permanent', false);
        $criteria = ['uuid' => $taskUuid, 'organ' => $organ];
        if (!$isPermanent) {
            $criteria['deletedAt'] = null;
        }

        $task = $entityManager->getRepository(Task::class)->findOneBy($criteria);

        if (!$task) {
            return $this->json(['message' => 'Task not found'], Response::HTTP_NOT_FOUND);
        }

        /** @var User $user */
        $user = $this->getUser();
        
        $action = $isPermanent ? 'TASK_HARD_DELETE' : 'TASK_DELETE';
        if (!$this->taskService->can($user, $task, $action)) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        if ($isPermanent) {
            $entityManager->remove($task);
        } else {
            $task->setDeletedAt(new \DateTime());
        }
        
        $entityManager->flush();

        // Invalidate cache
        $this->taskCacheService->invalidateSummary($taskUuid);
        $this->taskCacheService->invalidateList($organUuid);

        return $this->json(['message' => $isPermanent ? 'Task permanently deleted' : 'Task deleted successfully']);
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
        
        // Add granular field edit permissions
        $fields = ['priority', 'expiresAt', 'estimatedHours', 'title', 'description', 'manager'];
        $editableFields = [];
        foreach ($fields as $field) {
            if ($this->taskService->canEditField($user, $task, $field)) {
                $editableFields[] = $field;
            }
        }

        return $this->json([
            'permissions' => $this->permissionService->getOrganPermissions($user, $organ),
            'editableFields' => $editableFields,
            'isProjectAdmin' => $this->taskService->isProjectAdmin($user, $project),
            'taskOwnership' => [
                'isManager' => ($task->getManager() === $user),
                'isAssignee' => $this->taskService->isAssignee($user, $task),
                'isCreator' => ($task->getCreatedBy() === $user),
            ]
        ]);
    }
    
    #[Route('/{taskUuid}/timeline', name: 'timeline', methods: ['GET'])]
    public function timeline(string $projectUuid, string $organUuid, string $taskUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
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

        $offset = $request->query->getInt('offset', 0);
        $limit = $request->query->getInt('limit', 10);

        // VERSION SQL PURE (SI40 PRE-REQUIS : UNION + JOIN par UUID) :
        $conn = $entityManager->getConnection();
        $sql = "
            SELECT 
                'COMMENT' as type, 
                tc.content as detail, 
                tc.created_at, 
                NULL as field_name, 
                NULL as action_type, 
                NULL as old_value, 
                NULL as new_value,
                u.first_name,
                u.last_name,
                NULL as target_first_name,
                NULL as target_last_name
            FROM task_comments tc
            JOIN tasks t ON tc.task_id = t.id
            JOIN users u ON tc.user_id = u.id
            WHERE t.uuid = :taskUuid AND tc.deleted_at IS NULL

            UNION ALL

            SELECT 
                'HISTORY' as type, 
                NULL as detail, 
                th.created_at, 
                th.field_name, 
                th.action_type, 
                th.old_value, 
                th.new_value,
                u.first_name,
                u.last_name,
                tu.first_name as target_first_name,
                tu.last_name as target_last_name
            FROM task_history th
            JOIN tasks t ON th.task_id = t.id
            LEFT JOIN users u ON th.user_id = u.id
            LEFT JOIN users tu ON (
                (th.action_type = 'ASSIGNEE_ADD' AND th.new_value = tu.uuid) OR
                (th.action_type = 'ASSIGNEE_REMOVE' AND th.old_value = tu.uuid)
            )
            WHERE t.uuid = :taskUuid AND th.action_type NOT IN ('DELETE', 'ASSIGNEE_REMOVE')

            UNION ALL

            SELECT 
                'ATTACHMENT' as type, 
                ta.file_name as detail, 
                ta.created_at, 
                NULL as field_name, 
                NULL as action_type, 
                NULL as old_value, 
                NULL as new_value,
                u.first_name,
                u.last_name,
                NULL as target_first_name,
                NULL as target_last_name
            FROM task_attachments ta
            JOIN tasks t ON ta.task_id = t.id
            LEFT JOIN users u ON ta.uploaded_by = u.id
            WHERE t.uuid = :taskUuid AND ta.deleted_at IS NULL

            ORDER BY created_at DESC
            LIMIT :limit OFFSET :offset
        ";

        $resultSet = $conn->executeQuery($sql, [
            'taskUuid' => $taskUuid,
            'limit' => $limit,
            'offset' => $offset
        ], [
            'limit' => 'integer',
            'offset' => 'integer'
        ]);

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

    #[Route('/{taskUuid}/assignees/{userUuid}', name: 'remove_assignee', methods: ['DELETE'])]
    public function removeAssignee(string $projectUuid, string $organUuid, string $taskUuid, string $userUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ]);

        if (!$task) return $this->json(['message' => 'Task not found'], Response::HTTP_NOT_FOUND);

        /** @var User $currentUser */
        $currentUser = $this->getUser();
        $targetUser = $entityManager->getRepository(User::class)->findOneBy(['uuid' => $userUuid]);

        if (!$targetUser) return $this->json(['message' => 'User not found'], Response::HTTP_NOT_FOUND);

        $isSelf = ($targetUser === $currentUser);
        $action = $isSelf ? 'TASK_ASSIGN_SELF' : 'TASK_ASSIGN_OTHERS';

        if (!$this->taskService->can($currentUser, $task, $action)) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $assignee = $entityManager->getRepository(TaskAssignee::class)->findOneBy(['task' => $task, 'user' => $targetUser, 'deletedAt' => null]);
        if ($assignee) {
            $entityManager->remove($assignee);
            $entityManager->flush();
            
            // Invalidate cache
            $this->taskCacheService->invalidateSummary($taskUuid);
            $this->taskCacheService->invalidateList($organUuid);
        }

        return $this->json(null, Response::HTTP_NO_CONTENT);
    }
}
