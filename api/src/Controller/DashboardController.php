<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\Project;
use App\Entity\ProjectMember;
use App\Entity\User;
use App\Service\ProjectCacheService;
use App\Service\TaskService;
use App\Service\UserCacheService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/dashboard', name: 'dashboard_')]
class DashboardController extends AbstractController
{
    public function __construct(
        private readonly ProjectCacheService $projectCacheService,
        private readonly TaskService $taskService,
        private readonly UserCacheService $userCacheService
    ) {}

    #[Route('', name: 'index', methods: ['GET'])]
    public function index(EntityManagerInterface $entityManager): JsonResponse
    {
        /** @var User|null $user */
        $user = $this->getUser();

        if (!$user) {
            return $this->json(['message' => 'Not authenticated'], Response::HTTP_UNAUTHORIZED);
        }

        // 1. Fetch Projects summaries
        $memberships = $entityManager->getRepository(ProjectMember::class)->findBy(['user' => $user, 'deletedAt' => null]);
        $projects = [];
        foreach ($memberships as $membership) {
            $project = $membership->getProject();
            if ($project && $project->getDeletedAt() === null) {
                $projects[] = $this->projectCacheService->getProjectSummary($project, function () use ($project) {
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

        // 2. Fetch Priority Tasks
        // Logic: priority DESC, expiresAt ASC, estimatedHours DESC
        // We use SQL for performance and to handle the cross-project joins easily
        $conn = $entityManager->getConnection();
        $sql = "
            SELECT t.uuid
            FROM tasks t
            JOIN organs o ON t.organ_id = o.id
            JOIN projects p ON o.project_id = p.id
            JOIN project_members pm ON p.id = pm.project_id
            LEFT JOIN task_assignees ta ON t.id = ta.task_id
            WHERE (t.manager_id = :userId OR ta.user_id = :userId OR t.created_by = :userId)
            AND t.deleted_at IS NULL
            AND o.deleted_at IS NULL
            AND t.status NOT IN ('DONE', 'CANCELED')
            AND p.deleted_at IS NULL
            AND pm.user_id = :userId
            AND pm.deleted_at IS NULL
            GROUP BY t.id
            ORDER BY 
                t.priority DESC, 
                (t.expires_at IS NULL) ASC,
                t.expires_at ASC, 
                t.created_at ASC
            LIMIT 4
        ";

        $taskUuids = $conn->executeQuery($sql, ['userId' => $user->getId()])->fetchFirstColumn();
        
        $tasks = [];
        foreach ($taskUuids as $uuid) {
            $task = $entityManager->getRepository(\App\Entity\Task::class)->findOneBy(['uuid' => $uuid]);
            if ($task) {
                $taskData = $this->taskService->getTaskData($task, $this->userCacheService);
                $taskData['projectName'] = $task->getOrgan()->getProject()->getTitle();
                $taskData['projectUuid'] = $task->getOrgan()->getProject()->getUuid();
                $taskData['organName'] = $task->getOrgan()->getTitle();
                $taskData['organUuid'] = $task->getOrgan()->getUuid();
                $tasks[] = $taskData;
            }
        }

        return $this->json([
            'projects' => $projects,
            'tasks' => $tasks
        ]);
    }
}
