<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\Project;
use Symfony\Contracts\Cache\CacheInterface;
use Symfony\Contracts\Cache\ItemInterface;

class ProjectCacheService
{
    private const SUMMARY_CACHE_PREFIX = 'project_summary_';
    private const CACHE_TTL = 3600; // 1 hour

    public function __construct(
        private readonly CacheInterface $cache
    ) {}

    /**
     * Get project summary from cache or DB.
     */
    public function getProjectSummary(Project $project, callable $fetcher): array
    {
        return $this->cache->get(self::SUMMARY_CACHE_PREFIX . $project->getUuid(), function (ItemInterface $item) use ($fetcher) {
            $item->expiresAfter(self::CACHE_TTL);
            return $fetcher();
        });
    }

    /**
     * Invalidate project summary cache.
     */
    public function invalidate(string $uuid): void
    {
        $this->cache->delete(self::SUMMARY_CACHE_PREFIX . $uuid);
    }
}
