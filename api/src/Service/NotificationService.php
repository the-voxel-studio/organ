<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\Notification;
use App\Entity\Task;
use App\Entity\User;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\Mercure\HubInterface;
use Symfony\Component\Mercure\Update;
use Symfony\Contracts\Cache\CacheInterface;
use Symfony\Contracts\Cache\ItemInterface;

class NotificationService
{
    private const LIST_CACHE_PREFIX = 'user_notifications_';
    private const LIST_CACHE_TTL = 86400; // 24h

    public function __construct(
        private readonly EntityManagerInterface $entityManager,
        private readonly HubInterface $hub,
        private readonly CacheInterface $cache,
        private readonly string $baseUrl
    ) {}

    /**
     * Get user notifications list (Cached).
     */
    public function getNotificationList(User $user, callable $fetcher): array
    {
        return $this->cache->get(self::LIST_CACHE_PREFIX . $user->getUuid(), function (ItemInterface $item) use ($fetcher) {
            $item->expiresAfter(self::LIST_CACHE_TTL);
            return $fetcher();
        });
    }

    /**
     * Invalidate user notifications list cache.
     */
    public function invalidate(string $userUuid): void
    {
        $this->cache->delete(self::LIST_CACHE_PREFIX . $userUuid);
    }

    /**
     * Create a notification but do not flush.
     */
    public function createNotification(User $user, string $type, string $message, ?Task $task = null): Notification
    {
        $notification = new Notification();
        $notification->setUser($user);
        $notification->setType($type);
        $notification->setMessage($message);
        $notification->setTask($task);

        $this->entityManager->persist($notification);
        
        return $notification;
    }

    /**
     * Immediate notification (Persist + Flush + Mercure + Invalidate Cache).
     */
    public function notify(User $user, string $type, string $message, ?Task $task = null): void
    {
        $notification = $this->createNotification($user, $type, $message, $task);
        $this->entityManager->flush();

        // Invalidate cache
        $this->invalidate($user->getUuid());

        // Send to Mercure
        $this->sendToMercure($notification);
    }

    /**
     * Send notification to Mercure Hub.
     */
    public function sendToMercure(Notification $notification): void
    {
        $user = $notification->getUser();
        if (!$user) return;

        $topic = sprintf('%s/users/%s/notifications', rtrim($this->baseUrl, '/'), $user->getUuid());
        
        $update = new Update(
            $topic,
            json_encode($this->getNotificationData($notification)),
            true // private update
        );

        $this->hub->publish($update);
    }

    /**
     * Format notification for JSON response.
     */
    public function getNotificationData(Notification $notification): array
    {
        return [
            'uuid' => $notification->getUuid(),
            'type' => $notification->getType(),
            'message' => $notification->getMessage(),
            'isRead' => $notification->isRead(),
            'createdAt' => $notification->getCreatedAt()->format(\DateTimeInterface::ATOM),
            'task' => $notification->getTask() ? [
                'uuid' => $notification->getTask()->getUuid(),
                'title' => $notification->getTask()->getTitle(),
            ] : null,
        ];
    }
}
