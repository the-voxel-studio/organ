<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\Organ;
use App\Entity\Project;
use App\Entity\Task;
use App\Entity\TaskAssignee;
use App\Entity\TaskAttachment;
use App\Entity\TaskComment;
use App\Entity\TaskDependency;
use App\Entity\TaskLink;
use App\Entity\TaskTag;
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

        // 2. Exact permission check (for non-variant permissions like COMMENT_CREATE)
        if ($this->organPermissionService->hasPermission($user, $organ, $action)) {
            return true;
        }

        // 3. Check for Global Organ Permission (ALL)
        if ($this->organPermissionService->hasPermission($user, $organ, $action . '_ALL')) {
            return true;
        }

        // 4. Check for specific Organ Role permission (OWN)
        if (!$this->organPermissionService->hasPermission($user, $organ, $action . '_OWN')) {
            return false;
        }

        // 5. Ownership-based logic for _OWN variant
        return match ($action) {
            'TASK_EDIT' => $isManager || $isAssignee || $isCreator,
            'TASK_DELETE', 'TASK_HARD_DELETE' => $isManager || $isCreator,
            'TASK_STATUS_CHANGE', 'TASK_ESTIMATE_MANAGE' => $isManager || $isAssignee,
            'TASK_PRIORITY_CHANGE', 'TASK_DATES_MANAGE' => $isManager,
            'TASK_VALIDATE' => $isManager,
            'TASK_LINK_MANAGE', 'TASK_TAG_MANAGE', 'TASK_DEPENDENCY_MANAGE' => $isManager || $isAssignee,
            'TASK_LINK_HARD_DELETE' => $isManager,
            // For sub-resources, returning true here allows the controller to enforce the sub-resource level ownership.
            'COMMENT_DELETE', 'COMMENT_HARD_DELETE', 'COMMENT_EDIT',
            'ATTACHMENT_DELETE', 'ATTACHMENT_HARD_DELETE' => true,
            default => false,
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

        $tags = $this->entityManager->getRepository(TaskTag::class)->findBy(['task' => $task, 'deletedAt' => null]);
        $tagData = [];
        foreach ($tags as $tt) {
            $t = $tt->getTag();
            if ($t->getDeletedAt() !== null) {
                continue;
            }
            $tagData[] = [
                'uuid' => $t->getUuid(),
                'name' => $t->getName(),
                'color' => $t->getColor(),
            ];
        }

        $links = $this->entityManager->getRepository(TaskLink::class)->findBy(['task' => $task, 'deletedAt' => null]);
        $linkData = [];
        foreach ($links as $link) {
            $linkData[] = [
                'uuid' => $link->getUuid(),
                'url' => $link->getUrl(),
                'description' => $link->getDescription(),
            ];
        }

        $commentCount = $this->entityManager->getRepository(TaskComment::class)->count(['task' => $task, 'deletedAt' => null]);
        $attachmentCount = $this->entityManager->getRepository(TaskAttachment::class)->count(['task' => $task, 'deletedAt' => null]);
        $dependencyCount = $this->entityManager->getRepository(TaskDependency::class)->count(['task' => $task, 'deletedAt' => null]);

        return [
            'uuid' => $task->getUuid(),
            'title' => $task->getTitle(),
            'description' => $task->getDescription(),
            'status' => $task->getStatus()->value,
            'statusMessage' => $task->getStatusMessage(),
            'priority' => $task->getPriority(),
            'estimatedHours' => $task->getEstimatedHours(),
            'startDate' => $task->getStartDate()?->format(\DateTimeInterface::ATOM),
            'expiresAt' => $task->getExpiresAt()?->format(\DateTimeInterface::ATOM),
            'createdBy' => $task->getCreatedBy() ? $userCacheService->getUserSummary($task->getCreatedBy()) : null,
            'manager' => $task->getManager() ? $userCacheService->getUserSummary($task->getManager()) : null,
            'assignees' => $assigneeData,
            'tags' => $tagData,
            'links' => $linkData,
            'commentCount' => $commentCount,
            'attachmentCount' => $attachmentCount,
            'dependencyCount' => $dependencyCount,
            'createdAt' => $task->getCreatedAt()->format(\DateTimeInterface::ATOM),
        ];
    }
}
