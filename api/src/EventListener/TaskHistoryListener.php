<?php

declare(strict_types=1);

namespace App\EventListener;

use App\Entity\Task;
use App\Entity\TaskAssignee;
use App\Entity\TaskHistory;
use App\Entity\User;
use Doctrine\Bundle\DoctrineBundle\Attribute\AsDoctrineListener;
use Doctrine\ORM\Event\OnFlushEventArgs;
use Doctrine\ORM\Events;
use Symfony\Bundle\SecurityBundle\Security;

#[AsDoctrineListener(event: Events::onFlush)]
class TaskHistoryListener
{
    public function __construct(
        private readonly Security $security
    ) {}

    public function onFlush(OnFlushEventArgs $eventArgs): void
    {
        $em = $eventArgs->getObjectManager();
        $uow = $em->getUnitOfWork();
        /** @var User|null $currentUser */
        $currentUser = $this->security->getUser();

        // 1. Handle Task insertions (Creation)
        foreach ($uow->getScheduledEntityInsertions() as $entity) {
            if ($entity instanceof Task) {
                $this->createHistory($entity, 'CREATE', null, null, null, $currentUser, $em);
            }
            if ($entity instanceof TaskAssignee) {
                $this->createHistory($entity->getTask(), 'ASSIGNEE_ADD', 'assignee', null, $entity->getUser()->getUuid(), $currentUser, $em);
            }
        }

        // 2. Handle updates (including restoration)
        foreach ($uow->getScheduledEntityUpdates() as $entity) {
            $task = null;
            $fieldName = null;

            if ($entity instanceof Task) {
                $task = $entity;
                $fieldName = null;
            } elseif ($entity instanceof \App\Entity\TaskLink) {
                $task = $entity->getTask();
                $fieldName = 'link';
            } elseif ($entity instanceof \App\Entity\TaskAttachment) {
                $task = $entity->getTask();
                $fieldName = 'attachment';
            }

            if ($task) {
                $changeset = $uow->getEntityChangeSet($entity);
                
                // Handle Restoration specifically
                if (isset($changeset['deletedAt'])) {
                    $oldDeletedAt = $changeset['deletedAt'][0];
                    $newDeletedAt = $changeset['deletedAt'][1];

                    // Transition from deleted to active = RESTORE
                    if ($oldDeletedAt !== null && $newDeletedAt === null) {
                        $this->createHistory($task, 'RESTORE', $fieldName, null, null, $currentUser, $em);
                    }
                }

                // Handle other fields for Task only
                if ($entity instanceof Task) {
                    foreach ($changeset as $field => $values) {
                        if (in_array($field, ['updatedAt', 'id', 'uuid', 'deletedAt'], true)) continue;
                        $old = $this->formatValue($values[0]);
                        $new = $this->formatValue($values[1]);
                        $this->createHistory($task, 'UPDATE', $field, $old, $new, $currentUser, $em);
                    }
                }
            }
        }
    }

    private function createHistory(Task $task, string $action, ?string $field, ?string $old, ?string $new, ?User $user, $em): void
    {
        $history = new TaskHistory();
        $history->setTask($task);
        $history->setUser($user);
        $history->setActionType($action);
        $history->setFieldName($field);
        $history->setOldValue($old);
        $history->setNewValue($new);

        $em->persist($history);
        $uow = $em->getUnitOfWork();
        $uow->computeChangeSet($em->getClassMetadata(TaskHistory::class), $history);
    }

    private function formatValue(mixed $value): ?string
    {
        if ($value === null) return null;
        if ($value instanceof \DateTimeInterface) return $value->format(\DateTimeInterface::ATOM);
        if ($value instanceof \UnitEnum) return $value instanceof \BackedEnum ? (string)$value->value : $value->name;
        if ($value instanceof User) return $value->getUuid();
        if (is_bool($value)) return $value ? 'true' : 'false';
        if (is_array($value)) return json_encode($value);
        
        return (string)$value;
    }
}
