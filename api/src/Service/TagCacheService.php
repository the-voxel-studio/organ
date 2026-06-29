<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\Project;
use Symfony\Contracts\Cache\CacheInterface;
use Symfony\Contracts\Cache\ItemInterface;

class TagCacheService
{
    private const LIST_CACHE_PREFIX = 'tag_list_';
    private const CACHE_TTL = 3600; // 1 hour

    public function __construct(
        private readonly CacheInterface $cache
    ) {}

    /**
     * Get tag list for a project.
     */
    public function getTagList(Project $project, callable $fetcher): array
    {
        return $this->cache->get(self::LIST_CACHE_PREFIX . $project->getUuid(), function (ItemInterface $item) use ($fetcher) {
            $item->expiresAfter(self::CACHE_TTL);
            return $fetcher();
        });
    }

    /**
     * Invalidate tag list cache for a project.
     */
    public function invalidateList(string $projectUuid): void
    {
        $this->cache->delete(self::LIST_CACHE_PREFIX . $projectUuid);
    }
}
