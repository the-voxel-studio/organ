<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\Project;
use Symfony\Contracts\Cache\CacheInterface;
use Symfony\Contracts\Cache\ItemInterface;

class ProjectDriveCacheService
{
    private const CONFIG_CACHE_PREFIX = 'project_drive_config_';
    private const CACHE_TTL = 3600; // 1 hour

    public function __construct(
        private readonly CacheInterface $cache
    ) {}

    /**
     * Get drive configuration for a project.
     */
    public function getDriveConfig(Project $project, callable $fetcher): array
    {
        return $this->cache->get(self::CONFIG_CACHE_PREFIX . $project->getUuid(), function (ItemInterface $item) use ($fetcher) {
            $item->expiresAfter(self::CACHE_TTL);
            return $fetcher();
        });
    }

    /**
     * Invalidate drive configuration cache for a project.
     */
    public function invalidate(string $projectUuid): void
    {
        $this->cache->delete(self::CONFIG_CACHE_PREFIX . $projectUuid);
    }
}
