<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\Organ;
use App\Entity\Task;
use Symfony\Contracts\Cache\CacheInterface;
use Symfony\Contracts\Cache\ItemInterface;

class TaskCacheService
{
    private const LIST_CACHE_PREFIX = 'task_list_';
    private const SUMMARY_CACHE_PREFIX = 'task_summary_';
    private const CACHE_TTL = 300; // 5 minutes - Tasks change often, so short TTL

    public function __construct(
        private readonly CacheInterface $cache
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
}
