<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\User;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Contracts\Cache\CacheInterface;
use Symfony\Contracts\Cache\ItemInterface;

class UserCacheService
{
    private const CACHE_PREFIX = 'user_summary_';
    private const CACHE_TTL = 86400; // 24 hours

    public function __construct(
        private readonly CacheInterface $cache,
        private readonly EntityManagerInterface $entityManager
    ) {}

    /**
     * Get user summary from user entity.
     */
    public function getUserSummary(User $user): array
    {
        return $this->cache->get(self::CACHE_PREFIX . $user->getUuid(), function (ItemInterface $item) use ($user) {
            $item->expiresAfter(self::CACHE_TTL);
            return $this->formatUserSummary($user);
        });
    }

    /**
     * Get user summary by UUID (useful for NoSQL joins).
     */
    public function getUserSummaryByUuid(string $uuid): ?array
    {
        return $this->cache->get(self::CACHE_PREFIX . $uuid, function (ItemInterface $item) use ($uuid) {
            $item->expiresAfter(self::CACHE_TTL);
            
            $user = $this->entityManager->getRepository(User::class)->findOneBy(['uuid' => $uuid]);
            if (!$user) {
                return null;
            }

            return $this->formatUserSummary($user);
        });
    }

    private function formatUserSummary(User $user): array
    {
        return [
            'uuid' => $user->getUuid(),
            'email' => $user->getEmail(),
            'firstName' => $user->getFirstName(),
            'lastName' => $user->getLastName(),
        ];
    }

    public function invalidate(string $uuid): void
    {
        $this->cache->delete(self::CACHE_PREFIX . $uuid);
    }

    public function refresh(User $user): void
    {
        $this->invalidate($user->getUuid());
        $this->getUserSummary($user);
    }
}
