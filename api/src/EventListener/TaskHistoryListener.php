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

        // 2. Handle Task updates
        foreach ($uow->getScheduledEntityUpdates() as $entity) {
            if ($entity instanceof Task) {
                $changeset = $uow->getEntityChangeSet($entity);
                foreach ($changeset as $field => $values) {
                    // Skip technical fields
                    if (in_array($field, ['updatedAt', 'id', 'uuid'], true)) continue;

                    $old = $this->formatValue($values[0]);
                    $new = $this->formatValue($values[1]);

                    $action = ($field === 'deletedAt' && $values[1] !== null) ? 'DELETE' : 'UPDATE';
                    
                    $this->createHistory($entity, $action, $field, $old, $new, $currentUser, $em);
                }
            }
            if ($entity instanceof TaskAssignee && isset($uow->getEntityChangeSet($entity)['deletedAt'])) {
                $cs = $uow->getEntityChangeSet($entity);
                if ($cs['deletedAt'][1] !== null) {
                    $this->createHistory($entity->getTask(), 'ASSIGNEE_REMOVE', 'assignee', $entity->getUser()->getUuid(), null, $currentUser, $em);
                }
            }
        }

        // 3. Handle TaskAssignee removals (Hard delete fallback)
        foreach ($uow->getScheduledEntityDeletions() as $entity) {
            if ($entity instanceof TaskAssignee) {
                $this->createHistory($entity->getTask(), 'ASSIGNEE_REMOVE', 'assignee', $entity->getUser()->getUuid(), null, $currentUser, $em);
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
