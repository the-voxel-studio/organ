<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\User;
use App\Entity\Project;
use App\Entity\ProjectMember;
use App\Entity\Organ;
use App\Entity\Notification;
use App\Entity\ProjectDriveConfig;
use App\Enum\IconType;
use App\Enum\ProjectGlobalRole;
use App\Enum\ProjectStatus;
use App\Service\ProjectCacheService;
use App\Service\UserCacheService;
use App\Service\GoogleDriveService;
use App\Service\ProjectDriveCacheService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Validator\Validator\ValidatorInterface;

#[Route('/projects', name: 'projects_')]
class ProjectController extends AbstractController
{
    public function __construct(
        private readonly ProjectCacheService $projectCacheService,
        private readonly UserCacheService $userCacheService,
        private readonly GoogleDriveService $googleDriveService,
        private readonly ProjectDriveCacheService $driveCacheService
    ) {}

    #[Route('', name: 'index', methods: ['GET'])]
    public function index(EntityManagerInterface $entityManager): JsonResponse
    {
        /** @var User|null $user */
        $user = $this->getUser();

        if (!$user) {
            return $this->json(['message' => 'Not authenticated'], Response::HTTP_UNAUTHORIZED);
        }

        $memberships = $entityManager->getRepository(ProjectMember::class)->findBy(['user' => $user, 'deletedAt' => null]);
        
        $projects = [];
        foreach ($memberships as $membership) {
            $project = $membership->getProject();
            if ($project && $project->getDeletedAt() === null) {
                $projects[] = $this->getProjectSummary($project);
            }
        }

        return $this->json($projects);
    }

    #[Route('/trash', name: 'trash', methods: ['GET'])]
    public function trash(EntityManagerInterface $entityManager): JsonResponse
    {
        /** @var User|null $user */
        $user = $this->getUser();

        if (!$user) {
            return $this->json(['message' => 'Not authenticated'], Response::HTTP_UNAUTHORIZED);
        }

        // On cherche les membres (actifs ou supprimés) liés à un projet supprimé, avec le rôle ADMIN
        $queryBuilder = $entityManager->getRepository(ProjectMember::class)->createQueryBuilder('pm')
            ->join('pm.project', 'p')
            ->where('pm.user = :user')
            ->andWhere('p.deletedAt IS NOT NULL')
            ->andWhere('pm.globalRole = :role')
            ->setParameter('user', $user)
            ->setParameter('role', ProjectGlobalRole::ADMIN);
        
        $memberships = $queryBuilder->getQuery()->getResult();
        
        $projects = [];
        foreach ($memberships as $membership) {
            $project = $membership->getProject();
            $projects[] = array_merge($this->getProjectSummary($project), [
                'deletedAt' => $project->getDeletedAt()->format(\DateTimeInterface::ATOM)
            ]);
        }

        return $this->json($projects);
    }

    #[Route('/{uuid}', name: 'show', methods: ['GET'])]
    public function show(string $uuid, EntityManagerInterface $entityManager): JsonResponse
    {
        /** @var User|null $user */
        $user = $this->getUser();

        if (!$user) {
            return $this->json(['message' => 'Not authenticated'], Response::HTTP_UNAUTHORIZED);
        }

        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $uuid, 'deletedAt' => null]);

        if (!$project) {
            return $this->json(['message' => 'Project not found'], Response::HTTP_NOT_FOUND);
        }

        $membership = $entityManager->getRepository(ProjectMember::class)->findOneBy(['user' => $user, 'project' => $project, 'deletedAt' => null]);

        if (!$membership) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $summary = $this->getProjectSummary($project);
        
        return $this->json(array_merge($summary, [
            'description' => $project->getDescription(),
            'createdAt' => $project->getCreatedAt()->format(\DateTimeInterface::ATOM),
            'role' => $membership->getGlobalRole()->value,
        ]));
    }

    #[Route('', name: 'create', methods: ['POST'])]
    public function create(Request $request, EntityManagerInterface $entityManager, ValidatorInterface $validator): JsonResponse
    {
        /** @var User|null $user */
        $user = $this->getUser();

        if (!$user) {
            return $this->json(['message' => 'Not authenticated'], Response::HTTP_UNAUTHORIZED);
        }

        $data = json_decode($request->getContent(), true);

        if (!$data || !isset($data['title'])) {
            return $this->json(['message' => 'Missing required fields'], Response::HTTP_BAD_REQUEST);
        }

        $project = new Project();
        $project->setTitle($data['title']);
        $project->setDescription($data['description'] ?? null);
        $project->setColor($data['color'] ?? '#FF7EB6');
        
        if (isset($data['status'])) {
            $status = ProjectStatus::tryFrom($data['status']);
            if ($status) {
                $project->setStatus($status);
            }
        }

        if (isset($data['iconType'])) {
            $iconType = IconType::tryFrom($data['iconType']);
            if ($iconType) {
                $project->setIconType($iconType);
            }
        }

        $project->setIconData($data['iconData'] ?? null);

        $errors = $validator->validate($project);
        if (count($errors) > 0) {
            return $this->json($errors, Response::HTTP_BAD_REQUEST);
        }

        $entityManager->persist($project);

        $member = new ProjectMember();
        $member->setProject($project);
        $member->setUser($user);
        $member->setGlobalRole(ProjectGlobalRole::ADMIN);

        $entityManager->persist($member);
        $entityManager->flush();

        $this->getProjectSummary($project);

        return $this->json([
            'uuid' => $project->getUuid(),
            'title' => $project->getTitle(),
            'role' => ProjectGlobalRole::ADMIN->value,
        ], Response::HTTP_CREATED);
    }

    #[Route('/{uuid}/detailed', name: 'detailed', methods: ['GET'])]
    public function detailed(
        string $uuid, 
        EntityManagerInterface $entityManager
    ): JsonResponse {
        /** @var User|null $user */
        $user = $this->getUser();
        if (!$user) return $this->json(['message' => 'Not authenticated'], Response::HTTP_UNAUTHORIZED);

        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $uuid, 'deletedAt' => null]);
        if (!$project) return $this->json(['message' => 'Project not found'], Response::HTTP_NOT_FOUND);

        $membership = $entityManager->getRepository(ProjectMember::class)->findOneBy(['user' => $user, 'project' => $project, 'deletedAt' => null]);
        if (!$membership) return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);

        // 1. Project Info
        $summary = $this->getProjectSummary($project);

        // 2. Organs
        $organs = $entityManager->getRepository(Organ::class)->findBy(['project' => $project, 'deletedAt' => null]);
        $organsData = [];
        foreach ($organs as $organ) {
            // Count active tasks (not DONE and not CANCELED)
            $activeTasksCount = $entityManager->getRepository(\App\Entity\Task::class)->createQueryBuilder('t')
                ->select('count(t.id)')
                ->where('t.organ = :organ')
                ->andWhere('t.deletedAt IS NULL')
                ->andWhere('t.status NOT IN (:excludedStatuses)')
                ->setParameter('organ', $organ)
                ->setParameter('excludedStatuses', [\App\Enum\TaskStatus::DONE->value, \App\Enum\TaskStatus::CANCELED->value])
                ->getQuery()
                ->getSingleScalarResult();

            $organsData[] = [
                'uuid' => $organ->getUuid(),
                'title' => $organ->getTitle(),
                'description' => $organ->getDescription(),
                'iconType' => $organ->getIconType()->value,
                'iconData' => $organ->getIconData(),
                'highlightColor' => $organ->getHighlightColor(),
                'activeTasksCount' => (int) $activeTasksCount,
            ];
        }

        // 3. Members
        $memberships = $entityManager->getRepository(ProjectMember::class)->findBy(['project' => $project, 'deletedAt' => null]);
        $membersData = [];
        $adminInfo = null;
        foreach ($memberships as $ms) {
            $userSummary = $this->userCacheService->getUserSummary($ms->getUser());
            $membersData[] = [
                'uuid' => $ms->getUuid(),
                'user' => $userSummary,
                'globalRole' => $ms->getGlobalRole()->value,
            ];

            if ($ms->getGlobalRole() === ProjectGlobalRole::ADMIN && $adminInfo === null) {
                $adminInfo = [
                    'firstName' => $userSummary['firstName'],
                    'lastName' => $userSummary['lastName'],
                    'email' => $ms->getUser()->getEmail()
                ];
            }
        }

        // 4. Unified Project Activity Feed (TaskHistory, Comments, Attachments)
        $conn = $entityManager->getConnection();
        $sql = "
            SELECT 
                'COMMENT' as type, 
                tc.content as detail, 
                tc.created_at, 
                NULL as field_name, 
                NULL as action_type, 
                u.first_name as user_first_name,
                u.last_name as user_last_name,
                t.title as task_title,
                t.uuid as task_uuid,
                o.title as organ_title
            FROM task_comments tc
            JOIN tasks t ON tc.task_id = t.id
            JOIN organs o ON t.organ_id = o.id
            JOIN users u ON tc.user_id = u.id
            WHERE o.project_id = :projectId AND tc.deleted_at IS NULL

            UNION ALL

            SELECT 
                'HISTORY' as type, 
                NULL as detail, 
                th.created_at, 
                th.field_name, 
                th.action_type, 
                u.first_name as user_first_name,
                u.last_name as user_last_name,
                t.title as task_title,
                t.uuid as task_uuid,
                o.title as organ_title
            FROM task_history th
            JOIN tasks t ON th.task_id = t.id
            JOIN organs o ON t.organ_id = o.id
            LEFT JOIN users u ON th.user_id = u.id
            WHERE o.project_id = :projectId AND th.action_type NOT IN ('DELETE', 'ASSIGNEE_REMOVE')

            UNION ALL

            SELECT 
                'ATTACHMENT' as type, 
                ta.file_name as detail, 
                ta.created_at, 
                NULL as field_name, 
                NULL as action_type, 
                u.first_name as user_first_name,
                u.last_name as user_last_name,
                t.title as task_title,
                t.uuid as task_uuid,
                o.title as organ_title
            FROM task_attachments ta
            JOIN tasks t ON ta.task_id = t.id
            JOIN organs o ON t.organ_id = o.id
            LEFT JOIN users u ON ta.uploaded_by = u.id
            WHERE o.project_id = :projectId AND ta.deleted_at IS NULL

            ORDER BY created_at DESC
            LIMIT 10
        ";

        $resultSet = $conn->executeQuery($sql, ['projectId' => $project->getId()]);
        $activities = $resultSet->fetchAllAssociative();

        return $this->json([
            'project' => array_merge($summary, [
                'description' => $project->getDescription(),
                'role' => $membership->getGlobalRole()->value,
            ]),
            'admin' => $adminInfo,
            'organs' => $organsData,
            'members' => $membersData,
            'activities' => $activities,
            'securityLogs' => [] // Optional: Keep or remove
        ]);
    }

    #[Route('/{uuid}', name: 'update', methods: ['PUT', 'PATCH'])]
    public function update(string $uuid, Request $request, EntityManagerInterface $entityManager, ValidatorInterface $validator): JsonResponse
    {
        /** @var User|null $user */
        $user = $this->getUser();

        if (!$user) {
            return $this->json(['message' => 'Not authenticated'], Response::HTTP_UNAUTHORIZED);
        }

        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $uuid]);

        if (!$project) {
            return $this->json(['message' => 'Project not found'], Response::HTTP_NOT_FOUND);
        }

        if ($project->getDeletedAt() !== null) {
            return $this->json(['message' => 'Project is deleted and cannot be updated'], Response::HTTP_FORBIDDEN);
        }

        $membership = $entityManager->getRepository(ProjectMember::class)->findOneBy(['user' => $user, 'project' => $project, 'deletedAt' => null]);

        if (!$membership || $membership->getGlobalRole() !== ProjectGlobalRole::ADMIN) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $data = json_decode($request->getContent(), true);
        if (!$data) {
            return $this->json(['message' => 'Invalid JSON'], Response::HTTP_BAD_REQUEST);
        }

        $needsInvalidation = false;
        if (isset($data['title'])) {
            $project->setTitle($data['title']);
            $needsInvalidation = true;
        }
        if (isset($data['description'])) {
            $project->setDescription($data['description']);
        }
        if (isset($data['status'])) {
            $status = ProjectStatus::tryFrom($data['status']);
            if ($status) {
                $project->setStatus($status);
                $needsInvalidation = true;
            }
        }
        if (isset($data['color'])) {
            $project->setColor($data['color']);
            $needsInvalidation = true;
        }
        if (isset($data['iconType'])) {
            $iconType = IconType::tryFrom($data['iconType']);
            if ($iconType) {
                $project->setIconType($iconType);
                $needsInvalidation = true;
            }
        }
        if (array_key_exists('iconData', $data)) {
            $project->setIconData($data['iconData']);
            $needsInvalidation = true;
        }

        $errors = $validator->validate($project);
        if (count($errors) > 0) {
            return $this->json($errors, Response::HTTP_BAD_REQUEST);
        }

        $entityManager->flush();

        if ($needsInvalidation) {
            $this->projectCacheService->invalidate($project->getUuid());
            $this->getProjectSummary($project);
        }

        return $this->json([
            'uuid' => $project->getUuid(),
            'title' => $project->getTitle(),
            'status' => $project->getStatus()->value,
            'color' => $project->getColor(),
        ]);
    }

    #[Route('/{uuid}/restore', name: 'restore', methods: ['POST'])]
    public function restore(string $uuid, EntityManagerInterface $entityManager): JsonResponse
    {
        /** @var User|null $user */
        $user = $this->getUser();

        if (!$user) {
            return $this->json(['message' => 'Not authenticated'], Response::HTTP_UNAUTHORIZED);
        }

        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $uuid]);

        if (!$project) {
            return $this->json(['message' => 'Project not found'], Response::HTTP_NOT_FOUND);
        }

        if ($project->getDeletedAt() === null) {
            return $this->json(['message' => 'Project is not deleted'], Response::HTTP_BAD_REQUEST);
        }

        $membership = $entityManager->getRepository(ProjectMember::class)->findOneBy(['user' => $user, 'project' => $project]);

        if (!$membership || $membership->getGlobalRole() !== ProjectGlobalRole::ADMIN) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $project->setDeletedAt(null);
        
        if ($membership->getDeletedAt() !== null) {
            $membership->setDeletedAt(null);
        }

        $entityManager->flush();
        $this->projectCacheService->invalidate($uuid);

        return $this->json($this->getProjectSummary($project));
    }

    #[Route('/{uuid}/permissions', name: 'permissions', methods: ['GET'])]
    public function permissions(string $uuid, EntityManagerInterface $entityManager): JsonResponse
    {
        /** @var User|null $user */
        $user = $this->getUser();
        if (!$user) return $this->json(['message' => 'Not authenticated'], Response::HTTP_UNAUTHORIZED);

        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $uuid, 'deletedAt' => null]);
        if (!$project) return $this->json(['message' => 'Project not found'], Response::HTTP_NOT_FOUND);

        $membership = $entityManager->getRepository(ProjectMember::class)->findOneBy(['user' => $user, 'project' => $project, 'deletedAt' => null]);
        if (!$membership) return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);

        return $this->json([
            'role' => $membership->getGlobalRole()->value,
        ]);
    }

    #[Route('/{uuid}', name: 'delete', methods: ['DELETE'])]
    public function delete(string $uuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        /** @var User|null $user */
        $user = $this->getUser();

        if (!$user) {
            return $this->json(['message' => 'Not authenticated'], Response::HTTP_UNAUTHORIZED);
        }

        $isPermanent = $request->query->getBoolean('permanent', false);

        $criteria = ['uuid' => $uuid];
        if (!$isPermanent) {
            $criteria['deletedAt'] = null;
        }

        $project = $entityManager->getRepository(Project::class)->findOneBy($criteria);

        if (!$project) {
            return $this->json(['message' => 'Project not found'], Response::HTTP_NOT_FOUND);
        }

        // Fix: Allow finding the member even if soft-deleted during a permanent delete
        $memberCriteria = ['user' => $user, 'project' => $project];
        if (!$isPermanent) {
            $memberCriteria['deletedAt'] = null;
        }
        $membership = $entityManager->getRepository(ProjectMember::class)->findOneBy($memberCriteria);

        if (!$membership || $membership->getGlobalRole() !== ProjectGlobalRole::ADMIN) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        if ($isPermanent) {
            // Cleanup Google Drive folder if configured
            $driveConfig = $entityManager->getRepository(ProjectDriveConfig::class)->findOneBy(['project' => $project]);
            if ($driveConfig && $driveConfig->getDriveFolderId() && $driveConfig->getEncryptedRefreshToken()) {
                $accessToken = $this->googleDriveService->getAccessToken($driveConfig->getEncryptedRefreshToken());
                if ($accessToken) {
                    $this->googleDriveService->deleteFile($accessToken, $driveConfig->getDriveFolderId());
                }
            }
            
            $entityManager->remove($project);
            $this->driveCacheService->invalidate($uuid);
        } else {
            $project->setDeletedAt(new \DateTime());
            
            $projectMembers = $entityManager->getRepository(ProjectMember::class)->findBy(['project' => $project]);
            foreach ($projectMembers as $pm) {
                if ($pm->getDeletedAt() === null) {
                    $pm->setDeletedAt(new \DateTime());
                }
            }
        }

        $entityManager->flush();

        $this->projectCacheService->invalidate($uuid);

        return $this->json(null, Response::HTTP_NO_CONTENT);
    }

    #[Route('/{uuid}/stats', name: 'stats', methods: ['GET'])]
    public function stats(string $uuid, EntityManagerInterface $entityManager): JsonResponse
    {
        /** @var User|null $user */
        $user = $this->getUser();
        if (!$user) return $this->json(['message' => 'Not authenticated'], Response::HTTP_UNAUTHORIZED);

        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $uuid, 'deletedAt' => null]);
        if (!$project) return $this->json(['message' => 'Project not found'], Response::HTTP_NOT_FOUND);

        $conn = $entityManager->getConnection();
        $sql = '
            SELECT 
                o.title as organ_name,
                t.status,
                COUNT(t.id) as task_count,
                SUM(t.estimated_hours) as total_hours
            FROM projects p
            JOIN organs o ON p.id = o.project_id
            LEFT JOIN tasks t ON o.id = t.organ_id AND t.deleted_at IS NULL
            WHERE p.uuid = :uuid
            GROUP BY o.id, t.status
            ORDER BY o.title, t.status
        ';
        
        $resultSet = $conn->executeQuery($sql, ['uuid' => $uuid]);
        return $this->json($resultSet->fetchAllAssociative());
    }

    private function getProjectSummary(Project $project): array
    {
        return $this->projectCacheService->getProjectSummary($project, function () use ($project) {
            return [
                'uuid' => $project->getUuid(),
                'title' => $project->getTitle(),
                'description' => $project->getDescription(),
                'status' => $project->getStatus()->value,
                'color' => $project->getColor(),
                'iconType' => $project->getIconType()->value,
                'iconData' => $project->getIconData(),
                'createdAt' => $project->getCreatedAt()->format(\DateTimeInterface::ATOM),
            ];
        });
    }
}
