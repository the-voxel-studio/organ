<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\Organ;
use App\Entity\Project;
use App\Entity\Task;
use App\Entity\TaskAssignee;
use App\Entity\User;
use App\Enum\ProjectGlobalRole;
use App\Enum\TaskStatus;
use Doctrine\ORM\EntityManagerInterface;

class TaskService
{
    public function __construct(
        private readonly EntityManagerInterface $entityManager,
        private readonly OrganPermissionService $organPermissionService,
        private readonly ProjectMembershipService $membershipService
    ) {}

    /**
     * Check if user can perform an action on a task.
     */
    public function can(User $user, Task $task, string $action): bool
    {
        $organ = $task->getOrgan();
        $project = $organ->getProject();

        // 1. Project ADMIN has total control (Cached)
        if ($this->isProjectAdmin($user, $project)) return true;

        $isManager = ($task->getManager() === $user);
        $isAssignee = $this->isAssignee($user, $task);
        $isCreator = ($task->getCreatedBy() === $user);

        // 2. Check for Global Organ Permission (ALL)
        if ($this->organPermissionService->hasPermission($user, $organ, $action . '_ALL')) {
            return true;
        }

        // 3. Check for specific Organ Role permission (OWN)
        if (!$this->organPermissionService->hasPermission($user, $organ, $action . '_OWN')) {
            return false;
        }

        // 4. Ownership-based logic
        return match ($action) {
            'TASK_EDIT' => $isManager || $isAssignee || $isCreator,
            'TASK_DELETE' => $isManager || $isCreator,
            'TASK_ASSIGN_OTHERS' => $isManager,
            'TASK_ASSIGN_SELF' => true,
            'TASK_VALIDATE' => $isManager,
            'COMMENT_CREATE', 'ATTACHMENT_ADD', 'TASK_LINK_MANAGE', 'TASK_TAG_MANAGE' => $isManager || $isAssignee,
            default => $isManager || $isAssignee || $isCreator,
        };
    }

    /**
     * Granular field-level permissions.
     */
    public function canEditField(User $user, Task $task, string $fieldName): bool
    {
        if ($this->isProjectAdmin($user, $task->getOrgan()->getProject())) return true;

        $isManager = ($task->getManager() === $user);
        $isAssignee = $this->isAssignee($user, $task);

        if ($this->organPermissionService->hasPermission($user, $task->getOrgan(), 'TASK_EDIT_ALL')) {
            return true;
        }

        if (!$this->organPermissionService->hasPermission($user, $task->getOrgan(), 'TASK_EDIT_OWN')) {
            return false;
        }

        return match ($fieldName) {
            'priority', 'expiresAt', 'startDate', 'manager' => $isManager,
            'estimatedHours' => $isManager || $isAssignee,
            'title', 'description' => $isManager || $isAssignee,
            default => $isManager,
        };
    }

    public function canChangeStatus(User $user, Task $task, TaskStatus $newStatus): bool
    {
        if ($this->isProjectAdmin($user, $task->getOrgan()->getProject())) return true;

        $isManager = ($task->getManager() === $user);
        $isAssignee = $this->isAssignee($user, $task);

        if (!$this->can($user, $task, 'TASK_STATUS_CHANGE')) return false;

        if (in_array($newStatus, [TaskStatus::DONE, TaskStatus::CANCELED], true)) {
            return $isManager;
        }

        return $isManager || $isAssignee;
    }

    public function isProjectAdmin(User $user, Project $project): bool
    {
        return $this->membershipService->getGlobalRole($user, $project) === ProjectGlobalRole::ADMIN;
    }

    public function isAssignee(User $user, Task $task): bool
    {
        $assignee = $this->entityManager->getRepository(TaskAssignee::class)->findOneBy([
            'task' => $task,
            'user' => $user,
            'deletedAt' => null
        ]);
        return $assignee !== null;
    }

    public function getTaskData(Task $task, UserCacheService $userCacheService): array
    {
        $assignees = $this->entityManager->getRepository(TaskAssignee::class)->findBy(['task' => $task, 'deletedAt' => null]);
        $assigneeData = [];
        foreach ($assignees as $as) {
            $assigneeData[] = $userCacheService->getUserSummary($as->getUser());
        }

        return [
            'uuid' => $task->getUuid(),
            'title' => $task->getTitle(),
            'description' => $task->getDescription(),
            'status' => $task->getStatus()->value,
            'priority' => $task->getPriority(),
            'estimatedHours' => $task->getEstimatedHours(),
            'startDate' => $task->getStartDate()?->format(\DateTimeInterface::ATOM),
            'expiresAt' => $task->getExpiresAt()?->format(\DateTimeInterface::ATOM),
            'createdBy' => $task->getCreatedBy() ? $userCacheService->getUserSummary($task->getCreatedBy()) : null,
            'manager' => $task->getManager() ? $userCacheService->getUserSummary($task->getManager()) : null,
            'assignees' => $assigneeData,
            'createdAt' => $task->getCreatedAt()->format(\DateTimeInterface::ATOM),
        ];
    }
}
