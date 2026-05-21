<?php

namespace App\EventListener;

use App\Entity\Task;
use App\Entity\TaskAssignee;
use App\Entity\TaskAttachment;
use App\Entity\TaskComment;
use App\Entity\User;
use App\Service\AuditLogService;
use Doctrine\Bundle\DoctrineBundle\Attribute\AsEventListener;
use Doctrine\ORM\Event\OnFlushEventArgs;
use Doctrine\ORM\Events;
use Symfony\Bundle\SecurityBundle\Security;

class TaskAuditListener
{
    public function __construct(
        private AuditLogService $auditLogService,
        private Security $security
    ) {}

    public function onFlush(OnFlushEventArgs $args): void
    {
        $em = $args->getObjectManager();
        $uow = $em->getUnitOfWork();
        $user = $this->security->getUser();
        if (!$user instanceof User) {
            $user = null;
        }

        // 1. Creations
        foreach ($uow->getScheduledEntityInsertions() as $entity) {
            if ($entity instanceof Task) {
                $this->auditLogService->log($entity, $user, 'CREATE', null, null, [
                    'title' => $entity->getTitle(),
                    'status' => $entity->getStatus()->value
                ]);
            } elseif ($entity instanceof TaskComment) {
                $this->auditLogService->log($entity->getTask(), $user, 'COMMENT_ADD', 'comment', null, [
                    'content' => $entity->getContent()
                ]);
            } elseif ($entity instanceof TaskAttachment) {
                $this->auditLogService->log($entity->getTask(), $user, 'ATTACHMENT_ADD', 'attachment', null, [
                    'fileName' => $entity->getFileName(),
                    'uuid' => $entity->getUuid()
                ]);
            } elseif ($entity instanceof TaskAssignee) {
                $this->auditLogService->log($entity->getTask(), $user, 'ASSIGNEE_ADD', 'assignee', null, [
                    'userName' => $entity->getUser()->getFirstName() . ' ' . $entity->getUser()->getLastName(),
                    'userUuid' => $entity->getUser()->getUuid()
                ]);
            }
        }

        // 2. Updates
        foreach ($uow->getScheduledEntityUpdates() as $entity) {
            if ($entity instanceof Task) {
                $changeSet = $uow->getEntityChangeSet($entity);
                foreach ($changeSet as $field => $values) {
                    [$oldValue, $newValue] = $values;

                    // Skip some fields if necessary
                    if (in_array($field, ['updatedAt'])) continue;

                    $actionType = 'UPDATE';
                    if ($field === 'status') {
                        $actionType = 'STATUS_CHANGE';
                        $oldValue = $oldValue?->value ?? $oldValue;
                        $newValue = $newValue?->value ?? $newValue;
                    }

                    // Format DateTime values
                    if ($oldValue instanceof \DateTimeInterface) $oldValue = $oldValue->format(\DateTimeInterface::ATOM);
                    if ($newValue instanceof \DateTimeInterface) $newValue = $newValue->format(\DateTimeInterface::ATOM);

                    $this->auditLogService->log($entity, $user, $actionType, $field, [$field => $oldValue], [$field => $newValue]);
                }
            }
        }

        // 3. Deletions (Soft or Hard)
        foreach ($uow->getScheduledEntityDeletions() as $entity) {
            if ($entity instanceof Task) {
                $this->auditLogService->log($entity, $user, 'HARD_DELETE', null, ['title' => $entity->getTitle()]);
            } elseif ($entity instanceof TaskAssignee) {
                $this->auditLogService->log($entity->getTask(), $user, 'ASSIGNEE_REMOVE', 'assignee', [
                    'userName' => $entity->getUser()->getFirstName() . ' ' . $entity->getUser()->getLastName(),
                    'userUuid' => $entity->getUser()->getUuid()
                ]);
            }
            // Note: Soft delete for Task is handled in Update (deletedAt field change)
        }
    }
}
