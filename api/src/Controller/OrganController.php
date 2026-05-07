<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\Organ;
use App\Entity\OrganLink;
use App\Entity\Project;
use App\Entity\ProjectMember;
use App\Entity\User;
use App\Enum\IconType;
use App\Enum\ProjectGlobalRole;
use App\Service\OrganCacheService;
use App\Service\OrganPermissionService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Validator\Validator\ValidatorInterface;

#[Route('/projects/{projectUuid}/organs', name: 'organs_')]
class OrganController extends AbstractController
{
    public function __construct(
        private readonly OrganPermissionService $permissionService,
        private readonly OrganCacheService $organCacheService
    ) {}

    #[Route('', name: 'index', methods: ['GET'])]
    public function index(string $projectUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        if (!$project) {
            return $this->json(['message' => 'Project not found'], Response::HTTP_NOT_FOUND);
        }

        /** @var User $user */
        $user = $this->getUser();
        $projectMember = $entityManager->getRepository(ProjectMember::class)->findOneBy([
            'project' => $project,
            'user' => $user,
            'deletedAt' => null
        ]);

        if (!$projectMember) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $fetcher = function () use ($entityManager, $project) {
            $organs = $entityManager->getRepository(Organ::class)->findBy(['project' => $project, 'deletedAt' => null]);
            
            $data = [];
            foreach ($organs as $organ) {
                $data[] = [
                    'uuid' => $organ->getUuid(),
                    'title' => $organ->getTitle(),
                    'description' => $organ->getDescription(),
                    'iconType' => $organ->getIconType()->value,
                    'iconData' => $organ->getIconData(),
                    'highlightColor' => $organ->getHighlightColor(),
                ];
            }
            return $data;
        };

        // Cache the full list (project-wide)
        $fullList = $this->organCacheService->getOrganList($project, $fetcher);

        // Filter the list based on user permissions
        $filteredData = [];
        foreach ($fullList as $organData) {
            // We need to re-fetch the organ entity or use a proxy to check permissions if we don't want to rely on the cache alone
            // But for performance, we can assume the entity is needed for hasPermission
            $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organData['uuid']]);
            if ($organ && ($projectMember->getGlobalRole() !== ProjectGlobalRole::MEMBER || $this->permissionService->hasPermission($user, $organ, 'ORGAN_VIEW'))) {
                $filteredData[] = $organData;
            }
        }

        return $this->json($filteredData);
    }

    #[Route('', name: 'create', methods: ['POST'])]
    public function create(string $projectUuid, Request $request, EntityManagerInterface $entityManager, ValidatorInterface $validator): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        if (!$project) {
            return $this->json(['message' => 'Project not found'], Response::HTTP_NOT_FOUND);
        }

        /** @var User $user */
        $user = $this->getUser();
        $projectMember = $entityManager->getRepository(ProjectMember::class)->findOneBy(['project' => $project, 'user' => $user, 'deletedAt' => null]);

        // Only Project ADMIN or MANAGER can create an organ
        if (!$projectMember || !in_array($projectMember->getGlobalRole(), [ProjectGlobalRole::ADMIN, ProjectGlobalRole::MANAGER], true)) {
            return $this->json(['message' => 'Insufficient permissions'], Response::HTTP_FORBIDDEN);
        }

        $data = json_decode($request->getContent(), true);
        if (!$data || !isset($data['title'])) {
            return $this->json(['message' => 'Missing required fields'], Response::HTTP_BAD_REQUEST);
        }

        $organ = new Organ();
        $organ->setProject($project);
        $organ->setTitle($data['title']);
        $organ->setDescription($data['description'] ?? null);
        $organ->setHighlightColor($data['highlightColor'] ?? '#000000');

        if (isset($data['iconType'])) {
            $iconType = IconType::tryFrom($data['iconType']);
            if ($iconType) {
                $organ->setIconType($iconType);
            }
        }
        $organ->setIconData($data['iconData'] ?? null);

        $errors = $validator->validate($organ);
        if (count($errors) > 0) {
            return $this->json($errors, Response::HTTP_BAD_REQUEST);
        }

        $entityManager->persist($organ);
        $entityManager->flush();

        // Invalidate list cache
        $this->organCacheService->invalidateList($projectUuid);

        return $this->json([
            'uuid' => $organ->getUuid(),
            'title' => $organ->getTitle()
        ], Response::HTTP_CREATED);
    }

    #[Route('/trash', name: 'trash', methods: ['GET'])]
    public function trash(string $projectUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        if (!$project) {
            return $this->json(['message' => 'Project not found'], Response::HTTP_NOT_FOUND);
        }

        /** @var User $user */
        $user = $this->getUser();
        $projectMember = $entityManager->getRepository(ProjectMember::class)->findOneBy([
            'project' => $project,
            'user' => $user,
            'deletedAt' => null
        ]);

        if (!$projectMember) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        // We show trashed organs if user is ADMIN or MANAGER of the project
        if (!in_array($projectMember->getGlobalRole(), [ProjectGlobalRole::ADMIN, ProjectGlobalRole::MANAGER], true)) {
            return $this->json(['message' => 'Insufficient permissions'], Response::HTTP_FORBIDDEN);
        }

        $organs = $entityManager->getRepository(Organ::class)->createQueryBuilder('o')
            ->where('o.project = :project')
            ->andWhere('o.deletedAt IS NOT NULL')
            ->setParameter('project', $project)
            ->getQuery()
            ->getResult();
        
        $data = [];
        foreach ($organs as $organ) {
            $data[] = [
                'uuid' => $organ->getUuid(),
                'title' => $organ->getTitle(),
                'description' => $organ->getDescription(),
                'deletedAt' => $organ->getDeletedAt()->format(\DateTimeInterface::ATOM),
            ];
        }

        return $this->json($data);
    }

    #[Route('/{organUuid}', name: 'show', methods: ['GET'])]
    public function show(string $projectUuid, string $organUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);

        if (!$organ) {
            return $this->json(['message' => 'Organ not found'], Response::HTTP_NOT_FOUND);
        }

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_VIEW')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $summary = $this->organCacheService->getOrganSummary($organ, function () use ($organ, $entityManager) {
            $links = $entityManager->getRepository(OrganLink::class)->findBy(['organ' => $organ, 'deletedAt' => null]);
            $linkData = [];
            foreach ($links as $link) {
                $linkData[] = [
                    'uuid' => $link->getUuid(),
                    'url' => $link->getUrl(),
                    'description' => $link->getDescription(),
                ];
            }

            return [
                'uuid' => $organ->getUuid(),
                'title' => $organ->getTitle(),
                'description' => $organ->getDescription(),
                'iconType' => $organ->getIconType()->value,
                'iconData' => $organ->getIconData(),
                'highlightColor' => $organ->getHighlightColor(),
                'createdAt' => $organ->getCreatedAt()->format(\DateTimeInterface::ATOM),
                'links' => $linkData,
            ];
        });

        return $this->json($summary);
    }

    #[Route('/{organUuid}', name: 'update', methods: ['PUT', 'PATCH'])]
    public function update(string $projectUuid, string $organUuid, Request $request, EntityManagerInterface $entityManager, ValidatorInterface $validator): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project]);

        if (!$organ) {
            return $this->json(['message' => 'Organ not found'], Response::HTTP_NOT_FOUND);
        }

        if ($organ->getDeletedAt() !== null) {
            return $this->json(['message' => 'Organ is deleted and cannot be updated'], Response::HTTP_FORBIDDEN);
        }

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_EDIT')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $data = json_decode($request->getContent(), true);
        if (isset($data['title'])) {
            $organ->setTitle($data['title']);
        }
        if (isset($data['description'])) {
            $organ->setDescription($data['description']);
        }
        if (isset($data['highlightColor'])) {
            $organ->setHighlightColor($data['highlightColor']);
        }
        if (isset($data['iconType'])) {
            $iconType = IconType::tryFrom($data['iconType']);
            if ($iconType) {
                $organ->setIconType($iconType);
            }
        }
        if (array_key_exists('iconData', $data)) {
            $organ->setIconData($data['iconData']);
        }

        $errors = $validator->validate($organ);
        if (count($errors) > 0) {
            return $this->json($errors, Response::HTTP_BAD_REQUEST);
        }

        $entityManager->flush();

        // Invalidate cache
        $this->organCacheService->invalidateSummary($organUuid);
        $this->organCacheService->invalidateList($projectUuid);

        return $this->json(['message' => 'Organ updated']);
    }

    #[Route('/{organUuid}/restore', name: 'restore', methods: ['POST'])]
    public function restore(string $projectUuid, string $organUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project]);

        if (!$organ) {
            return $this->json(['message' => 'Organ not found'], Response::HTTP_NOT_FOUND);
        }

        if ($organ->getDeletedAt() === null) {
            return $this->json(['message' => 'Organ is not deleted'], Response::HTTP_BAD_REQUEST);
        }

        /** @var User $user */
        $user = $this->getUser();
        
        // Only Project ADMIN or MANAGER can restore an organ
        $projectMember = $entityManager->getRepository(ProjectMember::class)->findOneBy([
            'project' => $project,
            'user' => $user,
            'deletedAt' => null
        ]);

        if (!$projectMember || !in_array($projectMember->getGlobalRole(), [ProjectGlobalRole::ADMIN, ProjectGlobalRole::MANAGER], true)) {
            return $this->json(['message' => 'Insufficient permissions'], Response::HTTP_FORBIDDEN);
        }

        $organ->setDeletedAt(null);
        $entityManager->flush();

        // Invalidate list cache
        $this->organCacheService->invalidateList($projectUuid);

        return $this->json([
            'uuid' => $organ->getUuid(),
            'title' => $organ->getTitle()
        ]);
    }

    #[Route('/{organUuid}/permissions', name: 'permissions', methods: ['GET'])]
    public function permissions(string $projectUuid, string $organUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);

        if (!$organ) return $this->json(['message' => 'Organ not found'], Response::HTTP_NOT_FOUND);

        /** @var User $user */
        $user = $this->getUser();
        
        return $this->json([
            'permissions' => $this->permissionService->getOrganPermissions($user, $organ),
        ]);
    }

    #[Route('/{organUuid}/members', name: 'members', methods: ['GET'])]
    public function members(string $projectUuid, string $organUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);

        if (!$organ) return $this->json(['message' => 'Organ not found'], Response::HTTP_NOT_FOUND);

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_VIEW')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $fetcher = function () use ($entityManager, $organ) {
            $uors = $entityManager->getRepository(\App\Entity\UserOrganRole::class)->createQueryBuilder('uor')
                ->join('uor.role', 'r')
                ->where('r.organ = :organ')
                ->andWhere('uor.deletedAt IS NULL')
                ->andWhere('r.deletedAt IS NULL')
                ->setParameter('organ', $organ)
                ->getQuery()
                ->getResult();
            
            $users = [];
            foreach ($uors as $uor) {
                $u = $uor->getUser();
                if (!isset($users[$u->getUuid()])) {
                    $users[$u->getUuid()] = [
                        'uuid' => $u->getUuid(),
                        'firstName' => $u->getFirstName(),
                        'lastName' => $u->getLastName(),
                        'email' => $u->getEmail(),
                    ];
                }
            }

            return array_values($users);
        };

        return $this->json($this->organCacheService->getMemberList($organ, $fetcher));
    }

    #[Route('/{organUuid}/check-permission/{permissionName}', name: 'check_permission', methods: ['GET'])]
    public function checkPermission(string $projectUuid, string $organUuid, string $permissionName, EntityManagerInterface $entityManager): JsonResponse
    {
        /** @var User $user */
        $user = $this->getUser();

        /*
        // VERSION SYMFONY PROPRE (SERVICE) :
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid]);
        $hasPermission = $this->permissionService->hasPermission($user, $organ, $permissionName);
        */

        // VERSION SQL PURE (SI40 PRE-REQUIS : EXISTS + JOINTURES MULTIPLES PAR UUID) :
        $conn = $entityManager->getConnection();
        $sql = "
            SELECT EXISTS (
                -- Check Project ADMIN (via UUIDs)
                SELECT 1 
                FROM project_members pm
                JOIN projects pr ON pm.project_id = pr.id
                JOIN users u ON pm.user_id = u.id
                WHERE u.uuid = :userUuid 
                  AND pr.uuid = :projectUuid 
                  AND pm.global_role = 'ADMIN' 
                  AND pm.deleted_at IS NULL

                UNION

                -- Check Project MANAGER for ORGAN_VIEW
                SELECT 1 
                FROM project_members pm
                JOIN projects pr ON pm.project_id = pr.id
                JOIN users u ON pm.user_id = u.id
                WHERE u.uuid = :userUuid 
                  AND pr.uuid = :projectUuid 
                  AND pm.global_role = 'MANAGER' 
                  AND :permName = 'ORGAN_VIEW'
                  AND pm.deleted_at IS NULL
                
                UNION
                
                -- Check Organ Specific Permission (via UUIDs)
                SELECT 1 
                FROM user_organ_roles uor
                JOIN users u ON uor.user_id = u.id
                JOIN organ_roles orole ON uor.role_id = orole.id
                JOIN organs o ON orole.organ_id = o.id
                JOIN organ_role_permissions orp ON orole.id = orp.role_id
                JOIN permissions p ON orp.permission_id = p.id
                WHERE u.uuid = :userUuid 
                  AND o.uuid = :organUuid 
                  AND p.name = :permName
                  AND uor.deleted_at IS NULL
                  AND orole.deleted_at IS NULL
                  AND o.deleted_at IS NULL
            ) as allowed
        ";

        $result = $conn->executeQuery($sql, [
            'userUuid' => $user->getUuid(),
            'projectUuid' => $projectUuid,
            'organUuid' => $organUuid,
            'permName' => $permissionName
        ])->fetchOne();

        return $this->json(['allowed' => (bool)$result]);
    }

    #[Route('/{organUuid}/ready-tasks', name: 'ready_tasks', methods: ['GET'])]
    public function readyTasks(string $projectUuid, string $organUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);

        if (!$organ) {
            return $this->json(['message' => 'Organ not found'], Response::HTTP_NOT_FOUND);
        }

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_VIEW')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        // VERSION SQL PURE (SI40 PRE-REQUIS : NOT EXISTS + AUTO-JOINTURE + JOIN UUID) :
        $conn = $entityManager->getConnection();
        $sql = "
            SELECT t.uuid, t.title, t.priority
            FROM tasks t
            JOIN organs o ON t.organ_id = o.id
            WHERE o.uuid = :organUuid 
              AND t.status = 'TODO'
              AND t.deleted_at IS NULL
              AND NOT EXISTS (
                  SELECT 1 
                  FROM task_dependencies td
                  JOIN tasks dep ON td.depends_on_task_id = dep.id
                  WHERE td.task_id = t.id 
                    AND dep.status != 'DONE'
                    AND dep.deleted_at IS NULL
              )
        ";

        $resultSet = $conn->executeQuery($sql, ['organUuid' => $organUuid]);
        return $this->json($resultSet->fetchAllAssociative());
    }

    #[Route('/{organUuid}', name: 'delete', methods: ['DELETE'])]
    public function delete(string $projectUuid, string $organUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        
        $isPermanent = $request->query->getBoolean('permanent', false);
        $criteria = ['uuid' => $organUuid, 'project' => $project];
        if (!$isPermanent) {
            $criteria['deletedAt'] = null;
        }

        $organ = $entityManager->getRepository(Organ::class)->findOneBy($criteria);

        if (!$organ) {
            return $this->json(['message' => 'Organ not found'], Response::HTTP_NOT_FOUND);
        }

        /** @var User $user */
        $user = $this->getUser();
        
        if ($isPermanent) {
            // Need ADMIN or MANAGER global role for permanent deletion
            $membership = $entityManager->getRepository(ProjectMember::class)->findOneBy(['user' => $user, 'project' => $project, 'deletedAt' => null]);
            $isAuthorized = $membership && in_array($membership->getGlobalRole(), [ProjectGlobalRole::ADMIN, ProjectGlobalRole::MANAGER], true);
            
            if (!$isAuthorized && !$this->permissionService->hasPermission($user, $organ, 'ORGAN_HARD_DELETE')) {
                return $this->json(['message' => 'Insufficient permissions for permanent deletion'], Response::HTTP_FORBIDDEN);
            }
            
            $entityManager->remove($organ);
        } else {
            if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_EDIT')) {
                return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
            }
            $organ->setDeletedAt(new \DateTime());
        }

        $entityManager->flush();

        // Invalidate cache
        $this->organCacheService->invalidateSummary($organUuid);
        $this->organCacheService->invalidateList($projectUuid);

        return $this->json(null, Response::HTTP_NO_CONTENT);
    }
}
