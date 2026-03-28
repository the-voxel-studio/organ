<?php

declare(strict_types=1);

namespace App\EventListener;

use App\Entity\Notification;
use App\Entity\Task;
use App\Entity\TaskAssignee;
use App\Entity\TaskComment;
use App\Entity\TaskAttachment;
use App\Entity\User;
use App\Service\NotificationService;
use Doctrine\Bundle\DoctrineBundle\Attribute\AsDoctrineListener;
use Doctrine\ORM\Event\OnFlushEventArgs;
use Doctrine\ORM\Event\PostFlushEventArgs;
use Doctrine\ORM\Events;
use Symfony\Bundle\SecurityBundle\Security;

#[AsDoctrineListener(event: Events::onFlush)]
#[AsDoctrineListener(event: Events::postFlush)]
class TaskNotificationListener
{
    private array $pendingNotifications = [];

    public function __construct(
        private readonly Security $security,
        private readonly NotificationService $notificationService
    ) {}

    public function onFlush(OnFlushEventArgs $eventArgs): void
    {
        $em = $eventArgs->getObjectManager();
        $uow = $em->getUnitOfWork();
        /** @var User|null $actor */
        $actor = $this->security->getUser();

        // 1. Handle Task updates (Status, Manager)
        foreach ($uow->getScheduledEntityUpdates() as $entity) {
            if ($entity instanceof Task) {
                $changeset = $uow->getEntityChangeSet($entity);
                
                // Status Change
                if (isset($changeset['status'])) {
                    $this->notifyTaskInvolved($entity, 'TASK_STATUS', sprintf('Statut de la tâche "%s" mis à jour : %s', $entity->getTitle(), $entity->getStatus()->value), $actor, $em);
                }

                // New Manager
                if (isset($changeset['manager'])) {
                    $newManager = $changeset['manager'][1];
                    if ($newManager instanceof User && $newManager !== $actor) {
                        $this->queueNotification($newManager, 'TASK_MANAGEMENT', sprintf('Vous avez été nommé responsable de la tâche "%s"', $entity->getTitle()), $entity, $em);
                    }
                }
            }
        }

        // 2. Handle Insertions (Assignees, Comments, Attachments)
        foreach ($uow->getScheduledEntityInsertions() as $entity) {
            if ($entity instanceof TaskAssignee) {
                $user = $entity->getUser();
                if ($user !== $actor) {
                    $this->queueNotification($user, 'TASK_ASSIGNMENT', sprintf('Vous avez été assigné à la tâche "%s"', $entity->getTask()->getTitle()), $entity->getTask(), $em);
                }
            }

            if ($entity instanceof TaskComment) {
                $this->notifyTaskInvolved($entity->getTask(), 'TASK_COMMENT', sprintf('Nouveau commentaire sur la tâche "%s" par %s', $entity->getTask()->getTitle(), $actor?->getFirstName() ?? 'un utilisateur'), $actor, $em);
            }

            if ($entity instanceof TaskAttachment) {
                $this->notifyTaskInvolved($entity->getTask(), 'TASK_ATTACHMENT', sprintf('Nouveau fichier ajouté à la tâche "%s"', $entity->getTask()->getTitle()), $actor, $em);
            }
        }
    }

    /**
     * Notify everyone involved in a task (Manager + Assignees) except the actor.
     */
    private function notifyTaskInvolved(Task $task, string $type, string $message, ?User $actor, $em): void
    {
        // Notify Manager
        if ($task->getManager() && $task->getManager() !== $actor) {
            $this->queueNotification($task->getManager(), $type, $message, $task, $em);
        }

        // Notify Assignees
        // Warning: this fetches currently persisted assignees. For new ones being added in the same flush, 
        // they are handled by the insertion loop in onFlush.
        foreach ($em->getRepository(TaskAssignee::class)->findBy(['task' => $task, 'deletedAt' => null]) as $ta) {
            if ($ta->getUser() !== $actor) {
                $this->queueNotification($ta->getUser(), $type, $message, $task, $em);
            }
        }
    }

    private function queueNotification(User $user, string $type, string $message, Task $task, $em): void
    {
        $notification = $this->notificationService->createNotification($user, $type, $message, $task);
        $em->persist($notification);
        
        $uow = $em->getUnitOfWork();
        $uow->computeChangeSet($em->getClassMetadata(Notification::class), $notification);
        
        $this->pendingNotifications[] = $notification;
    }

    /**
     * Send Mercure updates AFTER the database transaction is committed.
     */
    public function postFlush(PostFlushEventArgs $eventArgs): void
    {
        foreach ($this->pendingNotifications as $notification) {
            $this->notificationService->sendToMercure($notification);
        }
        $this->pendingNotifications = [];
    }
}
