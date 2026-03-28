<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\Organ;
use App\Entity\OrganRole;
use App\Entity\OrganRolePermission;
use App\Entity\Permission;
use App\Entity\Project;
use App\Entity\User;
use App\Entity\UserOrganRole;
use App\Service\OrganPermissionService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/projects/{projectUuid}/organs/{organUuid}/roles', name: 'organ_roles_')]
class OrganRoleController extends AbstractController
{
    public function __construct(
        private readonly OrganPermissionService $permissionService
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

        $roles = $entityManager->getRepository(OrganRole::class)->findBy(['organ' => $organ, 'deletedAt' => null]);
        
        $data = [];
        foreach ($roles as $role) {
            $data[] = [
                'uuid' => $role->getUuid(),
                'name' => $role->getName(),
                'permissions' => $this->permissionService->getRolePermissions($role),
            ];
        }

        return $this->json($data);
    }

    #[Route('', name: 'create', methods: ['POST'])]
    public function create(string $projectUuid, string $organUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
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
        
        $entityManager->persist($role);

        if (isset($data['permissions']) && is_array($data['permissions'])) {
            foreach ($data['permissions'] as $permName) {
                $permission = $entityManager->getRepository(Permission::class)->findOneBy(['name' => $permName]);
                if ($permission) {
                    $rp = new OrganRolePermission();
                    $rp->setRole($role);
                    $rp->setPermission($permission);
                    $entityManager->persist($rp);
                }
            }
        }

        $entityManager->flush();

        return $this->json([
            'uuid' => $role->getUuid(),
            'name' => $role->getName()
        ], Response::HTTP_CREATED);
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

        return $this->json(null, Response::HTTP_NO_CONTENT);
    }
}
