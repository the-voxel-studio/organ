<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\Project;
use App\Entity\ProjectMember;
use App\Entity\User;
use App\Enum\ProjectGlobalRole;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Contracts\Cache\CacheInterface;
use Symfony\Contracts\Cache\ItemInterface;

class ProjectMembershipService
{
    private const CACHE_PREFIX = 'project_role_';
    private const CACHE_TTL = 86400; // 24 hours

    public function __construct(
        private readonly EntityManagerInterface $entityManager,
        private readonly CacheInterface $cache
    ) {}

    /**
     * Get user's global role in a project (Cached).
     * Returns null if user is not a member.
     */
    public function getGlobalRole(User $user, Project $project): ?ProjectGlobalRole
    {
        $cacheKey = self::CACHE_PREFIX . $project->getUuid() . '_' . $user->getUuid();

        return $this->cache->get($cacheKey, function (ItemInterface $item) use ($user, $project) {
            $item->expiresAfter(self::CACHE_TTL);

            $membership = $this->entityManager->getRepository(ProjectMember::class)->findOneBy([
                'project' => $project,
                'user' => $user,
                'deletedAt' => null
            ]);

            return $membership?->getGlobalRole();
        });
    }

    /**
     * Invalidate the cached role for a specific member.
     */
    public function invalidate(string $userUuid, string $projectUuid): void
    {
        $this->cache->delete(self::CACHE_PREFIX . $projectUuid . '_' . $userUuid);
    }
}
