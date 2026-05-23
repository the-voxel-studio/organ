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
use App\Service\AuditLogService;
use App\Service\ProjectCacheService;
use App\Service\UserCacheService;
use App\Service\OrganCacheService;
use App\Service\TaskService;
use App\Service\GoogleDriveService;
use App\Service\ProjectDriveCacheService;
use Doctrine\ORM\EntityManagerInterface;
use Doctrine\ODM\MongoDB\DocumentManager;
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
        private readonly OrganCacheService $organCacheService,
        private readonly TaskService $taskService,
        private readonly GoogleDriveService $googleDriveService,
        private readonly ProjectDriveCacheService $driveCacheService,
        private readonly DocumentManager $dm,
        private readonly AuditLogService $auditLogService
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

        // Log consultation
        $this->auditLogService->logConsultation($user, $project);

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

        // 4. Unified Project Activity Feed (AuditLog, Comments, Attachments)
        // a. Fetch Comments & Attachments from SQL
        $conn = $entityManager->getConnection();
        $sqlSql = "
            (SELECT 
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
            WHERE o.project_id = :projectId AND tc.deleted_at IS NULL)

            UNION ALL

            (SELECT 
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
            WHERE o.project_id = :projectId AND ta.deleted_at IS NULL)

            ORDER BY created_at DESC
            LIMIT 50
        ";
        $sqlItems = $conn->executeQuery($sqlSql, ['projectId' => $project->getId()])->fetchAllAssociative();

        // b. Fetch Audit Logs from MongoDB
        $mongoLogs = $this->dm->getRepository(\App\Document\AuditLog::class)->createQueryBuilder()
            ->field('projectUuid')->equals($project->getUuid())
            ->field('actionType')->notEqual('CONSULTATION')
            ->sort('createdAt', 'desc')
            ->limit(50)
            ->getQuery()
            ->execute();

        $activities = [];

        foreach ($sqlItems as $item) {
            $activities[] = [
                'type' => $item['type'],
                'detail' => $item['detail'],
                'created_at' => (new \DateTime($item['created_at']))->format(\DateTimeInterface::ATOM),
                'field_name' => $item['field_name'],
                'action_type' => $item['action_type'],
                'user_first_name' => $item['user_first_name'],
                'user_last_name' => $item['user_last_name'],
                'task_title' => $item['task_title'],
                'task_uuid' => $item['task_uuid'],
                'organ_title' => $item['organ_title']
            ];
        }

        foreach ($mongoLogs as $log) {
            $uSum = $log->getUserUuid() ? $this->userCacheService->getUserSummaryByUuid($log->getUserUuid()) : null;
            $oSum = $log->getOrganUuid() ? $this->organCacheService->getOrganSummaryByUuid($log->getOrganUuid()) : null;
            $tSum = $log->getTaskUuid() ? $this->taskService->getTaskSummaryByUuid($log->getTaskUuid()) : null;

            $activities[] = [
                'type' => 'HISTORY',
                'action_type' => $log->getActionType(),
                'field_name' => $log->getFieldName(),
                'old_value' => $log->getOldValues(),
                'new_value' => $log->getNewValues(),
                'created_at' => $log->getCreatedAt() ? $log->getCreatedAt()->format(\DateTimeInterface::ATOM) : null,
                'user_first_name' => $uSum['firstName'] ?? 'System',
                'user_last_name' => $uSum['lastName'] ?? '',
                'task_title' => $tSum['title'] ?? ($log->getTaskUuid() ? 'Deleted Task' : null),
                'task_uuid' => $log->getTaskUuid(),
                'organ_title' => $oSum['title'] ?? ($log->getOrganUuid() ? 'Deleted Organ' : 'System Update')
            ];
        }

        // c. Sort and limit
        usort($activities, fn($a, $b) => strcmp($b['created_at'], $a['created_at']));
        $activities = array_slice($activities, 0, 10);

        return $this->json([
            'project' => array_merge($summary, [
                'description' => $project->getDescription(),
                'role' => $membership->getGlobalRole()->value,
            ]),
            'admin' => $adminInfo,
            'organs' => $organsData,
            'members' => $membersData,
            'activities' => $activities,
            'securityLogs' => [] 
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
    public function stats(
        string $uuid, 
        Request $request,
        EntityManagerInterface $entityManager
    ): JsonResponse {
        /** @var User|null $user */
        $user = $this->getUser();
        if (!$user) return $this->json(['message' => 'Not authenticated'], Response::HTTP_UNAUTHORIZED);

        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $uuid, 'deletedAt' => null]);
        if (!$project) return $this->json(['message' => 'Project not found'], Response::HTTP_NOT_FOUND);

        $membership = $entityManager->getRepository(ProjectMember::class)->findOneBy(['user' => $user, 'project' => $project, 'deletedAt' => null]);
        if (!$membership) return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);

        $days = $request->query->getInt('days', 7);
        $start = (new \DateTime())->modify("-{$days} days")->setTime(0, 0, 0);

        // Fetch daily stats from MongoDB
        $stats = $this->dm->getRepository(\App\Document\DailyStat::class)->createQueryBuilder()
            ->field('projectUuid')->equals($project->getUuid())
            ->field('organUuid')->equals(null) // Global project stats
            ->field('date')->gte($start)
            ->sort('date', 'asc')
            ->getQuery()
            ->execute();

        $data = [];
        foreach ($stats as $stat) {
            $data[] = [
                'date' => $stat->getDate()->format('Y-m-d'),
                'tasksCreated' => $stat->getTasksCreated(),
                'tasksCompleted' => $stat->getTasksCompleted(),
                'tasksCanceled' => $stat->getTasksCanceled(),
                'commentsAdded' => $stat->getCommentsAdded(),
                'attachmentsAdded' => $stat->getAttachmentsAdded(),
                'membersActive' => $stat->getMembersActive(),
                'consultations' => $stat->getExtra()['consultations'] ?? 0,
                'statusChanges' => $stat->getStatusChanges(),
            ];
        }

        // Add current SQL stats for real-time overview
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
        ';
        $currentStats = $conn->executeQuery($sql, ['uuid' => $uuid])->fetchAllAssociative();

        return $this->json([
            'history' => $data,
            'current' => $currentStats
        ]);
    }

    #[Route('/{uuid}/audit', name: 'audit', methods: ['GET'])]
    public function audit(
        string $uuid,
        Request $request,
        EntityManagerInterface $entityManager
    ): JsonResponse {
        /** @var User|null $user */
        $user = $this->getUser();
        if (!$user) return $this->json(['message' => 'Not authenticated'], Response::HTTP_UNAUTHORIZED);

        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $uuid, 'deletedAt' => null]);
        if (!$project) return $this->json(['message' => 'Project not found'], Response::HTTP_NOT_FOUND);

        $membership = $entityManager->getRepository(ProjectMember::class)->findOneBy(['user' => $user, 'project' => $project, 'deletedAt' => null]);
        
        // Only ADMIN or MANAGER can see detailed audit
        if (!$membership || !in_array($membership->getGlobalRole(), [ProjectGlobalRole::ADMIN, ProjectGlobalRole::MANAGER], true)) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $limit = $request->query->getInt('limit', 100);
        $offset = $request->query->getInt('offset', 0);
        $dateStr = $request->query->get('date'); // Expected YYYY-MM-DD
        
        $qb = $this->dm->getRepository(\App\Document\AuditLog::class)->createQueryBuilder()
            ->field('projectUuid')->equals($project->getUuid())
            ->sort('createdAt', 'desc')
            ->limit($limit)
            ->skip($offset);

        if ($dateStr) {
            try {
                $start = new \DateTime($dateStr . ' 00:00:00');
                $end = new \DateTime($dateStr . ' 23:59:59');
                $qb->field('createdAt')->range($start, $end);
            } catch (\Exception $e) {
                return $this->json(['message' => 'Invalid date format. Use YYYY-MM-DD.'], Response::HTTP_BAD_REQUEST);
            }
        }

        $logs = $qb->getQuery()->execute();
        
        $data = [];
        foreach ($logs as $log) {
            $uSum = $log->getUserUuid() ? $this->userCacheService->getUserSummaryByUuid($log->getUserUuid()) : null;
            $oSum = $log->getOrganUuid() ? $this->organCacheService->getOrganSummaryByUuid($log->getOrganUuid()) : null;
            $tSum = $log->getTaskUuid() ? $this->taskService->getTaskSummaryByUuid($log->getTaskUuid()) : null;

            $data[] = [
                'id' => $log->getId(),
                'actionType' => $log->getActionType(),
                'fieldName' => $log->getFieldName(),
                'oldValues' => $log->getOldValues(),
                'newValues' => $log->getNewValues(),
                'context' => $log->getContext(),
                'createdAt' => $log->getCreatedAt()->format(\DateTimeInterface::ATOM),
                'user' => $uSum,
                'taskUuid' => $log->getTaskUuid(),
                'taskTitle' => $tSum['title'] ?? null,
                'organUuid' => $log->getOrganUuid(),
                'organTitle' => $oSum['title'] ?? null,
            ];
        }

        return $this->json($data);
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
