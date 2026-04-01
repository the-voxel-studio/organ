<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\User;
use App\Entity\Project;
use App\Entity\ProjectMember;
use App\Enum\IconType;
use App\Enum\ProjectGlobalRole;
use App\Enum\ProjectStatus;
use App\Service\ProjectCacheService;
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
        private readonly ProjectCacheService $projectCacheService
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
                // Fetch summary from cache or DB
                $projects[] = $this->getProjectSummary($project);
            }
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
        
        // Detailed view adds description, createdAt and role
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

        // Optional: pre-warm cache
        $this->getProjectSummary($project);

        return $this->json([
            'uuid' => $project->getUuid(),
            'title' => $project->getTitle(),
            'role' => ProjectGlobalRole::ADMIN->value,
        ], Response::HTTP_CREATED);
    }

    #[Route('/trash', name: 'trash', methods: ['GET'])]
    public function trash(EntityManagerInterface $entityManager): JsonResponse
    {
        /** @var User|null $user */
        $user = $this->getUser();

        if (!$user) {
            return $this->json(['message' => 'Not authenticated'], Response::HTTP_UNAUTHORIZED);
        }

        // We find projects where the user is an ADMIN but the project is deleted
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
            // Re-warm
            $this->getProjectSummary($project);
        }

        return $this->json([
            'uuid' => $project->getUuid(),
            'title' => $project->getTitle(),
            'status' => $project->getStatus()->value,
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

        // To restore, we need an ADMIN membership, but since the project is deleted, the membership might be deleted too
        $membership = $entityManager->getRepository(ProjectMember::class)->findOneBy(['user' => $user, 'project' => $project]);

        if (!$membership || $membership->getGlobalRole() !== ProjectGlobalRole::ADMIN) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $project->setDeletedAt(null);
        
        // Restore ADMIN membership if it was deleted during project deletion
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
    public function delete(string $uuid, EntityManagerInterface $entityManager): JsonResponse
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

        if (!$membership || $membership->getGlobalRole() !== ProjectGlobalRole::ADMIN) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        // Soft delete project
        $project->setDeletedAt(new \DateTime());
        
        // Soft delete memberships
        $projectMembers = $entityManager->getRepository(ProjectMember::class)->findBy(['project' => $project]);
        foreach ($projectMembers as $pm) {
            if ($pm->getDeletedAt() === null) {
                $pm->setDeletedAt(new \DateTime());
            }
        }

        $entityManager->flush();

        // Invalidate cache
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

        /* 
        // VERSION SYMFONY PROPRE (ORM) :
        $organs = $project->getOrgans();
        $stats = [];
        foreach ($organs as $organ) {
            foreach ($organ->getTasks() as $task) {
                $stats[$organ->getTitle()][$task->getStatus()->value] = ($stats[$organ->getTitle()][$task->getStatus()->value] ?? 0) + 1;
            }
        }
        */

        // VERSION SQL PURE (SI40 PRE-REQUIS) :
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

    /**
     * Get or set the project summary in cache.
     */
    private function getProjectSummary(Project $project): array
    {
        return $this->projectCacheService->getProjectSummary($project, function () use ($project) {
            return [
                'uuid' => $project->getUuid(),
                'title' => $project->getTitle(),
                'status' => $project->getStatus()->value,
                'iconType' => $project->getIconType()->value,
                'iconData' => $project->getIconData(),
            ];
        });
    }
}
