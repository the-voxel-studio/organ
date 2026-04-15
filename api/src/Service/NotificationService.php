<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\Notification;
use App\Entity\Task;
use App\Entity\User;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\Mercure\HubInterface;
use Symfony\Component\Mercure\Update;
use Symfony\Component\DependencyInjection\Attribute\Autowire;

use Symfony\Contracts\Cache\CacheInterface;
use Symfony\Contracts\Cache\ItemInterface;

class NotificationService
{
    private const LIST_CACHE_PREFIX = 'user_notifications_';
    private const CACHE_TTL = 3600; // 1 hour

    public function __construct(
        private readonly EntityManagerInterface $entityManager,
        private readonly HubInterface $hub,
        private readonly CacheInterface $cache,
        #[Autowire('%notification_base_url%')]
        private readonly string $baseUrl
    ) {}

    /**
     * Get user notifications list (Cached).
     */
    public function getNotificationList(User $user, callable $fetcher): array
    {
        return $this->cache->get(self::LIST_CACHE_PREFIX . $user->getUuid(), function (ItemInterface $item) use ($fetcher) {
            $item->expiresAfter(self::CACHE_TTL);
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
     * Create and send a notification.
     */
    public function notify(User $user, string $type, string $message, ?Task $task = null): void
    {
        $notification = $this->createNotification($user, $type, $message, $task);
        
        $this->entityManager->persist($notification);
        $this->entityManager->flush();

        $this->sendToMercure($notification);
        $this->invalidate($user->getUuid());
    }

    /**
     * Internal: prepare notification object without flushing.
     */
    public function createNotification(User $user, string $type, string $message, ?Task $task = null): Notification
    {
        $notification = new Notification();
        $notification->setUser($user);
        $notification->setType($type);
        $notification->setMessage($message);
        $notification->setTask($task);

        return $notification;
    }

    /**
     * Internal: send to Mercure hub.
     */
    public function sendToMercure(Notification $notification): void
    {
        $user = $notification->getUser();
        $topic = sprintf('%s/users/%s/notifications', rtrim($this->baseUrl, '/'), $user->getUuid());
        
        $update = new Update(
            $topic,
            json_encode([
                'uuid' => $notification->getUuid(),
                'type' => $notification->getType(),
                'message' => $notification->getMessage(),
                'taskUuid' => $notification->getTask()?->getUuid(),
                'createdAt' => $notification->getCreatedAt()->format(\DateTimeInterface::ATOM),
            ]),
            true
        );

        $this->hub->publish($update);
    }
}
