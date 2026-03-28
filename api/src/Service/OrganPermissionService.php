<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\Organ;
use App\Entity\OrganRole;
use App\Entity\ProjectMember;
use App\Entity\User;
use App\Entity\UserOrganRole;
use App\Enum\ProjectGlobalRole;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Contracts\Cache\CacheInterface;
use Symfony\Contracts\Cache\ItemInterface;

class OrganPermissionService
{
    private const ROLE_CACHE_PREFIX = 'role_definition_';
    private const USER_ROLES_CACHE_PREFIX = 'user_organ_roles_';
    private const CACHE_TTL = 86400; // 24 hours

    public function __construct(
        private readonly EntityManagerInterface $entityManager,
        private readonly CacheInterface $cache
    ) {}

    /**
     * Check if a user has a specific permission in an organ.
     */
    public function hasPermission(User $user, Organ $organ, string $permissionName): bool
    {
        // 1. Project ADMIN has all permissions (Master key)
        $projectMember = $this->entityManager->getRepository(ProjectMember::class)->findOneBy([
            'project' => $organ->getProject(),
            'user' => $user,
            'deletedAt' => null
        ]);

        if ($projectMember?->getGlobalRole() === ProjectGlobalRole::ADMIN) {
            return true;
        }

        // 2. Aggregate permissions from all user's roles in this organ
        $userRoles = $this->getUserRolesInOrgan($user, $organ);
        
        foreach ($userRoles as $role) {
            $perms = $this->getRolePermissions($role);
            if (in_array($permissionName, $perms, true)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get shared role definition (permissions list) from cache.
     */
    public function getRolePermissions(OrganRole $role): array
    {
        return $this->cache->get(self::ROLE_CACHE_PREFIX . $role->getUuid(), function (ItemInterface $item) use ($role) {
            $item->expiresAfter(self::CACHE_TTL);
            
            $permissions = [];
            foreach ($role->getPermissions() as $rolePerm) {
                $permissions[] = $rolePerm->getPermission()->getName();
            }
            
            return $permissions;
        });
    }

    /**
     * Get user's active roles in an organ (cached per user/organ).
     */
    private function getUserRolesInOrgan(User $user, Organ $organ): array
    {
        $cacheKey = self::USER_ROLES_CACHE_PREFIX . $organ->getUuid() . '_' . $user->getUuid();

        return $this->cache->get($cacheKey, function (ItemInterface $item) use ($user, $organ) {
            $item->expiresAfter(self::CACHE_TTL);
            
            $userOrganRoles = $this->entityManager->getRepository(UserOrganRole::class)->findBy([
                'user' => $user,
                'deletedAt' => null
            ]);

            $roles = [];
            foreach ($userOrganRoles as $uor) {
                $role = $uor->getRole();
                if ($role && $role->getOrgan() === $organ && $role->getDeletedAt() === null) {
                    $roles[] = $role;
                }
            }
            return $roles;
        });
    }

    /**
     * Invalidate a specific role definition (when its permissions change).
     */
    public function invalidateRoleDefinition(string $roleUuid): void
    {
        $this->cache->delete(self::ROLE_CACHE_PREFIX . $roleUuid);
    }

    /**
     * Invalidate user's roles list (when they are assigned/removed from a role).
     */
    public function invalidateUserRoles(string $userUuid, string $organUuid): void
    {
        $this->cache->delete(self::USER_ROLES_CACHE_PREFIX . $organUuid . '_' . $userUuid);
    }
}
