<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\User;
use Symfony\Contracts\Cache\CacheInterface;
use Symfony\Contracts\Cache\ItemInterface;

class UserCacheService
{
    private const CACHE_PREFIX = 'user_summary_';
    private const CACHE_TTL = 86400; // 24 hours

    public function __construct(
        private readonly CacheInterface $cache
    ) {}

    /**
     * Get user summary (uuid, firstName, lastName) from cache or DB.
     */
    public function getUserSummary(User $user): array
    {
        return $this->cache->get(self::CACHE_PREFIX . $user->getUuid(), function (ItemInterface $item) use ($user) {
            $item->expiresAfter(self::CACHE_TTL);
            
            return [
                'uuid' => $user->getUuid(),
                'email' => $user->getEmail(),
                'firstName' => $user->getFirstName(),
                'lastName' => $user->getLastName(),
            ];
        });
    }

    /**
     * Invalidate user summary cache.
     */
    public function invalidate(string $uuid): void
    {
        $this->cache->delete(self::CACHE_PREFIX . $uuid);
    }

    /**
     * Update/Refresh user summary cache.
     */
    public function refresh(User $user): void
    {
        $this->invalidate($user->getUuid());
        $this->getUserSummary($user);
    }
}
