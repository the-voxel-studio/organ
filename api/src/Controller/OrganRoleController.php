<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\Organ;
use App\Entity\OrganRole;
use App\Entity\Permission;
use App\Entity\Project;
use App\Entity\User;
use App\Entity\UserOrganRole;
use App\Enum\IconType;
use App\Service\OrganCacheService;
use App\Service\OrganPermissionService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Validator\Validator\ValidatorInterface;

#[Route('/projects/{projectUuid}/organs/{organUuid}/roles', name: 'organ_roles_')]
class OrganRoleController extends AbstractController
{
    public function __construct(
        private readonly OrganPermissionService $permissionService,
        private readonly OrganCacheService $organCacheService
    ) {}

    #[Route('', name: 'index', methods: ['GET'])]
    public function index(string $projectUuid, string $organUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);

        if (!$organ) {
            return $this->json(['message' => 'Organ not found'], Response::HTTP_NOT_FOUND);
        }

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_MANAGE_ROLES')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $fetcher = function () use ($entityManager, $organ) {
            $roles = $entityManager->getRepository(OrganRole::class)->findBy(['organ' => $organ, 'deletedAt' => null]);
            
            $data = [];
            foreach ($roles as $role) {
                $data[] = [
                    'uuid' => $role->getUuid(),
                    'name' => $role->getName(),
                    'iconType' => $role->getIconType()->value,
                    'iconData' => $role->getIconData(),
                    'permissions' => $this->permissionService->getRolePermissions($role),
                ];
            }
            return $data;
        };

        return $this->json($this->organCacheService->getRoleList($organ, $fetcher));
    }

    #[Route('/trash', name: 'trash', methods: ['GET'])]
    public function trash(string $projectUuid, string $organUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);

        if (!$organ) {
            return $this->json(['message' => 'Organ not found'], Response::HTTP_NOT_FOUND);
        }

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_MANAGE_ROLES')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $roles = $entityManager->getRepository(OrganRole::class)->createQueryBuilder('r')
            ->where('r.organ = :organ')
            ->andWhere('r.deletedAt IS NOT NULL')
            ->setParameter('organ', $organ)
            ->getQuery()
            ->getResult();
        
        $data = [];
        foreach ($roles as $role) {
            $data[] = [
                'uuid' => $role->getUuid(),
                'name' => $role->getName(),
                'iconType' => $role->getIconType()->value,
                'iconData' => $role->getIconData(),
                'deletedAt' => $role->getDeletedAt()->format(\DateTimeInterface::ATOM),
            ];
        }

        return $this->json($data);
    }

    #[Route('', name: 'create', methods: ['POST'])]
    public function create(string $projectUuid, string $organUuid, Request $request, EntityManagerInterface $entityManager, ValidatorInterface $validator): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);

        if (!$organ) {
            return $this->json(['message' => 'Organ not found'], Response::HTTP_NOT_FOUND);
        }

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_MANAGE_ROLES')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $data = json_decode($request->getContent(), true);
        if (!isset($data['name'])) {
            return $this->json(['message' => 'Role name is required'], Response::HTTP_BAD_REQUEST);
        }

        $role = new OrganRole();
        $role->setOrgan($organ);
        $role->setName($data['name']);

        if (isset($data['iconType'])) {
            $iconType = IconType::tryFrom($data['iconType']);
            if ($iconType) {
                $role->setIconType($iconType);
            }
        }
        $role->setIconData($data['iconData'] ?? null);
        
        $errors = $validator->validate($role);
        if (count($errors) > 0) {
            return $this->json($errors, Response::HTTP_BAD_REQUEST);
        }

        $entityManager->persist($role);

        if (isset($data['permissions']) && is_array($data['permissions'])) {
            foreach ($data['permissions'] as $permName) {
                $permission = $entityManager->getRepository(Permission::class)->findOneBy(['name' => $permName]);
                if ($permission) {
                    $role->addPermission($permission);
                }
            }
        }

        $entityManager->flush();

        // Invalidate role list cache
        $this->organCacheService->invalidateRoleList($organUuid);

        return $this->json([
            'uuid' => $role->getUuid(),
            'name' => $role->getName()
        ], Response::HTTP_CREATED);
    }

    #[Route('/{roleUuid}', name: 'update', methods: ['PUT', 'PATCH'])]
    public function update(string $projectUuid, string $organUuid, string $roleUuid, Request $request, EntityManagerInterface $entityManager, ValidatorInterface $validator): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $role = $entityManager->getRepository(OrganRole::class)->findOneBy(['uuid' => $roleUuid, 'organ' => $organ, 'deletedAt' => null]);

        if (!$role) {
            return $this->json(['message' => 'Role not found'], Response::HTTP_NOT_FOUND);
        }

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_MANAGE_ROLES')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $data = json_decode($request->getContent(), true);
        if (!$data) {
            return $this->json(['message' => 'Invalid JSON'], Response::HTTP_BAD_REQUEST);
        }

        if (isset($data['name'])) {
            $role->setName($data['name']);
        }

        if (isset($data['iconType'])) {
            $iconType = IconType::tryFrom($data['iconType']);
            if ($iconType) {
                $role->setIconType($iconType);
            }
        }
        if (array_key_exists('iconData', $data)) {
            $role->setIconData($data['iconData']);
        }

        if (isset($data['permissions']) && is_array($data['permissions'])) {
            // Clear existing permissions
            foreach ($role->getPermissions() as $permission) {
                $role->removePermission($permission);
            }
            // Add new ones
            foreach ($data['permissions'] as $permName) {
                $permission = $entityManager->getRepository(Permission::class)->findOneBy(['name' => $permName]);
                if ($permission) {
                    $role->addPermission($permission);
                }
            }
        }

        $errors = $validator->validate($role);
        if (count($errors) > 0) {
            return $this->json($errors, Response::HTTP_BAD_REQUEST);
        }

        $entityManager->flush();

        // Invalidate role definition
        $this->permissionService->invalidateRoleDefinition($role->getUuid());
        // Invalidate role list cache
        $this->organCacheService->invalidateRoleList($organUuid);

        return $this->json([
            'uuid' => $role->getUuid(),
            'name' => $role->getName(),
            'iconType' => $role->getIconType()->value,
            'iconData' => $role->getIconData(),
        ]);
    }

    #[Route('/{roleUuid}/assign', name: 'assign', methods: ['POST'])]
    public function assign(string $projectUuid, string $organUuid, string $roleUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $role = $entityManager->getRepository(OrganRole::class)->findOneBy(['uuid' => $roleUuid, 'organ' => $organ, 'deletedAt' => null]);

        if (!$role) {
            return $this->json(['message' => 'Role not found'], Response::HTTP_NOT_FOUND);
        }

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_MANAGE_ROLES')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $data = json_decode($request->getContent(), true);
        if (!isset($data['userUuid'])) {
            return $this->json(['message' => 'User UUID is required'], Response::HTTP_BAD_REQUEST);
        }

        $targetUser = $entityManager->getRepository(User::class)->findOneBy(['uuid' => $data['userUuid'], 'deletedAt' => null]);
        if (!$targetUser) {
            return $this->json(['message' => 'User not found'], Response::HTTP_NOT_FOUND);
        }

        $uor = $entityManager->getRepository(UserOrganRole::class)->findOneBy(['user' => $targetUser, 'role' => $role]);
        if ($uor) {
            $uor->setDeletedAt(null);
        } else {
            $uor = new UserOrganRole();
            $uor->setUser($targetUser);
            $uor->setRole($role);
            $entityManager->persist($uor);
        }

        $entityManager->flush();

        // Invalidate user roles list for this organ
        $this->permissionService->invalidateUserRoles($targetUser->getUuid(), $organ->getUuid());
        // Also invalidate role list as it might contain permissions summary
        $this->organCacheService->invalidateRoleList($organUuid);

        return $this->json(['message' => 'Role assigned successfully']);
    }

    #[Route('/{roleUuid}', name: 'delete', methods: ['DELETE'])]
    public function delete(string $projectUuid, string $organUuid, string $roleUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $role = $entityManager->getRepository(OrganRole::class)->findOneBy(['uuid' => $roleUuid, 'organ' => $organ, 'deletedAt' => null]);

        if (!$role) {
            return $this->json(['message' => 'Role not found'], Response::HTTP_NOT_FOUND);
        }

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_MANAGE_ROLES')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $role->setDeletedAt(new \DateTime());
        $entityManager->flush();

        // Invalidate role definition
        $this->permissionService->invalidateRoleDefinition($role->getUuid());
        // Invalidate role list cache
        $this->organCacheService->invalidateRoleList($organUuid);

        return $this->json(null, Response::HTTP_NO_CONTENT);
    }

    #[Route('/{roleUuid}/restore', name: 'restore', methods: ['POST'])]
    public function restore(string $projectUuid, string $organUuid, string $roleUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $role = $entityManager->getRepository(OrganRole::class)->findOneBy(['uuid' => $roleUuid, 'organ' => $organ]);

        if (!$role) {
            return $this->json(['message' => 'Role not found'], Response::HTTP_NOT_FOUND);
        }

        if ($role->getDeletedAt() === null) {
            return $this->json(['message' => 'Role is not deleted'], Response::HTTP_BAD_REQUEST);
        }

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_MANAGE_ROLES')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $role->setDeletedAt(null);
        $entityManager->flush();

        // Invalidate role definition
        $this->permissionService->invalidateRoleDefinition($role->getUuid());
        // Invalidate role list cache
        $this->organCacheService->invalidateRoleList($organUuid);

        return $this->json([
            'uuid' => $role->getUuid(),
            'name' => $role->getName()
        ]);
    }
}
