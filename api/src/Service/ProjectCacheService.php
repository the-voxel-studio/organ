<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\Project;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Contracts\Cache\CacheInterface;
use Symfony\Contracts\Cache\ItemInterface;

class ProjectCacheService
{
    private const SUMMARY_CACHE_PREFIX = 'project_summary_';
    private const CACHE_TTL = 3600; // 1 hour

    public function __construct(
        private readonly CacheInterface $cache,
        private readonly EntityManagerInterface $entityManager
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
     * Get project summary by UUID (useful for NoSQL joins).
     */
    public function getProjectSummaryByUuid(string $uuid): ?array
    {
        return $this->cache->get(self::SUMMARY_CACHE_PREFIX . $uuid, function (ItemInterface $item) use ($uuid) {
            $item->expiresAfter(self::CACHE_TTL);
            
            $project = $this->entityManager->getRepository(Project::class)->findOneBy(['uuid' => $uuid, 'deletedAt' => null]);
            if (!$project) {
                return null;
            }

            return [
                'uuid' => $project->getUuid(),
                'title' => $project->getTitle(),
                'color' => $project->getColor(),
            ];
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
