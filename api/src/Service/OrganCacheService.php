<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\Organ;
use App\Entity\Project;
use Symfony\Contracts\Cache\CacheInterface;
use Symfony\Contracts\Cache\ItemInterface;

class OrganCacheService
{
    private const LIST_CACHE_PREFIX = 'organ_list_';
    private const SUMMARY_CACHE_PREFIX = 'organ_summary_';
    private const CACHE_TTL = 3600; // 1 hour

    private const ROLE_LIST_CACHE_PREFIX = 'organ_role_list_';
    private const MEMBER_LIST_CACHE_PREFIX = 'organ_members_list_';

    public function __construct(
        private readonly CacheInterface $cache
    ) {}

    /**
     * Get organ members list summary for an organ.
     */
    public function getMemberList(Organ $organ, callable $fetcher): array
    {
        return $this->cache->get(self::MEMBER_LIST_CACHE_PREFIX . $organ->getUuid(), function (ItemInterface $item) use ($fetcher) {
            $item->expiresAfter(self::CACHE_TTL);
            return $fetcher();
        });
    }

    /**
     * Invalidate organ members list cache.
     */
    public function invalidateMemberList(string $organUuid): void
    {
        $this->cache->delete(self::MEMBER_LIST_CACHE_PREFIX . $organUuid);
    }

    /**
     * Get organ roles list summary for an organ.
     */
    public function getRoleList(Organ $organ, callable $fetcher): array
    {
        return $this->cache->get(self::ROLE_LIST_CACHE_PREFIX . $organ->getUuid(), function (ItemInterface $item) use ($fetcher) {
            $item->expiresAfter(self::CACHE_TTL);
            return $fetcher();
        });
    }

    /**
     * Invalidate organ roles list cache.
     */
    public function invalidateRoleList(string $organUuid): void
    {
        $this->cache->delete(self::ROLE_LIST_CACHE_PREFIX . $organUuid);
    }

    /**
     * Get organ list summary for a project.
     */
    public function getOrganList(Project $project, callable $fetcher): array
    {
        return $this->cache->get(self::LIST_CACHE_PREFIX . $project->getUuid(), function (ItemInterface $item) use ($fetcher) {
            $item->expiresAfter(self::CACHE_TTL);
            return $fetcher();
        });
    }

    /**
     * Get organ summary from cache or DB.
     */
    public function getOrganSummary(Organ $organ, callable $fetcher): array
    {
        return $this->cache->get(self::SUMMARY_CACHE_PREFIX . $organ->getUuid(), function (ItemInterface $item) use ($fetcher) {
            $item->expiresAfter(self::CACHE_TTL);
            return $fetcher();
        });
    }

    /**
     * Invalidate organ list cache for a project.
     */
    public function invalidateList(string $projectUuid): void
    {
        $this->cache->delete(self::LIST_CACHE_PREFIX . $projectUuid);
    }

    /**
     * Invalidate organ summary cache.
     */
    public function invalidateSummary(string $organUuid): void
    {
        $this->cache->delete(self::SUMMARY_CACHE_PREFIX . $organUuid);
    }
}
