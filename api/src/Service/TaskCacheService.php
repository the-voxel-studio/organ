<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\Organ;
use App\Entity\Task;
use App\Entity\TaskAssignee;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Contracts\Cache\CacheInterface;
use Symfony\Contracts\Cache\ItemInterface;

class TaskCacheService
{
    private const LIST_CACHE_PREFIX = 'task_list_';
    private const SUMMARY_CACHE_PREFIX = 'task_summary_';
    private const CACHE_TTL = 300; // 5 minutes - Tasks change often, so short TTL

    public function __construct(
        private readonly CacheInterface $cache,
        private readonly EntityManagerInterface $entityManager
    ) {}

    /**
     * Get task list for an organ.
     */
    public function getTaskList(Organ $organ, callable $fetcher): array
    {
        return $this->cache->get(self::LIST_CACHE_PREFIX . $organ->getUuid(), function (ItemInterface $item) use ($fetcher) {
            $item->expiresAfter(self::CACHE_TTL);
            return $fetcher();
        });
    }

    /**
     * Get task summary (details) from cache or DB.
     */
    public function getTaskSummary(Task $task, callable $fetcher): array
    {
        return $this->cache->get(self::SUMMARY_CACHE_PREFIX . $task->getUuid(), function (ItemInterface $item) use ($fetcher) {
            $item->expiresAfter(self::CACHE_TTL);
            return $fetcher();
        });
    }

    /**
     * Invalidate task list cache for an organ.
     */
    public function invalidateList(string $organUuid): void
    {
        $this->cache->delete(self::LIST_CACHE_PREFIX . $organUuid);
    }

    /**
     * Invalidate task summary cache.
     */
    public function invalidateSummary(string $taskUuid): void
    {
        $this->cache->delete(self::SUMMARY_CACHE_PREFIX . $taskUuid);
    }

    /**
     * Invalidate all tasks associated with a user in an organ.
     */
    public function invalidateUserTasksInOrgan(string $userUuid, string $organUuid): void
    {
        // Find tasks where user is manager
        $tasksAsManager = $this->entityManager->getRepository(Task::class)->createQueryBuilder('t')
            ->join('t.organ', 'o')
            ->join('t.manager', 'u')
            ->where('o.uuid = :organUuid')
            ->andWhere('u.uuid = :userUuid')
            ->setParameter('organUuid', $organUuid)
            ->setParameter('userUuid', $userUuid)
            ->getQuery()
            ->getResult();

        foreach ($tasksAsManager as $task) {
            $this->invalidateSummary($task->getUuid());
        }

        // Find tasks where user is assignee
        $assignees = $this->entityManager->getRepository(TaskAssignee::class)->createQueryBuilder('ta')
            ->join('ta.task', 't')
            ->join('t.organ', 'o')
            ->join('ta.user', 'u')
            ->where('o.uuid = :organUuid')
            ->andWhere('u.uuid = :userUuid')
            ->setParameter('organUuid', $organUuid)
            ->setParameter('userUuid', $userUuid)
            ->getQuery()
            ->getResult();

        foreach ($assignees as $assignee) {
            $this->invalidateSummary($assignee->getTask()->getUuid());
        }

        // Also invalidate the organ task list as assignments changed
        $this->invalidateList($organUuid);
    }
}
