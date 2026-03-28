<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\User;
use App\Entity\Project;
use App\Entity\ProjectMember;
use App\Enum\IconType;
use App\Enum\ProjectGlobalRole;
use App\Enum\ProjectStatus;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Validator\Validator\ValidatorInterface;
use Symfony\Contracts\Cache\CacheInterface;
use Symfony\Contracts\Cache\ItemInterface;

#[Route('/projects', name: 'projects_')]
class ProjectController extends AbstractController
{
    private const CACHE_PREFIX = 'project_summary_';
    private const CACHE_TTL = 3600; // 1 hour

    #[Route('', name: 'index', methods: ['GET'])]
    public function index(EntityManagerInterface $entityManager, CacheInterface $cache): JsonResponse
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
                $projects[] = $this->getProjectSummary($project, $cache);
            }
        }

        return $this->json($projects);
    }

    #[Route('/{uuid}', name: 'show', methods: ['GET'])]
    public function show(string $uuid, EntityManagerInterface $entityManager, CacheInterface $cache): JsonResponse
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

        $summary = $this->getProjectSummary($project, $cache);
        
        // Detailed view adds description, createdAt and role
        return $this->json(array_merge($summary, [
            'description' => $project->getDescription(),
            'createdAt' => $project->getCreatedAt()->format(\DateTimeInterface::ATOM),
            'role' => $membership->getGlobalRole()->value,
        ]));
    }

    #[Route('', name: 'create', methods: ['POST'])]
    public function create(Request $request, EntityManagerInterface $entityManager, ValidatorInterface $validator, CacheInterface $cache): JsonResponse
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
        $this->getProjectSummary($project, $cache);

        return $this->json([
            'uuid' => $project->getUuid(),
            'title' => $project->getTitle(),
            'role' => ProjectGlobalRole::ADMIN->value,
        ], Response::HTTP_CREATED);
    }

    #[Route('/{uuid}', name: 'update', methods: ['PUT', 'PATCH'])]
    public function update(string $uuid, Request $request, EntityManagerInterface $entityManager, ValidatorInterface $validator, CacheInterface $cache): JsonResponse
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
            $cache->delete(self::CACHE_PREFIX . $project->getUuid());
            // Re-warm
            $this->getProjectSummary($project, $cache);
        }

        return $this->json([
            'uuid' => $project->getUuid(),
            'title' => $project->getTitle(),
            'status' => $project->getStatus()->value,
        ]);
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
    public function delete(string $uuid, EntityManagerInterface $entityManager, CacheInterface $cache): JsonResponse
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
        $cache->delete(self::CACHE_PREFIX . $uuid);

        return $this->json(null, Response::HTTP_NO_CONTENT);
    }

    /**
     * Get or set the project summary in cache.
     */
    private function getProjectSummary(Project $project, CacheInterface $cache): array
    {
        return $cache->get(self::CACHE_PREFIX . $project->getUuid(), function (ItemInterface $item) use ($project) {
            $item->expiresAfter(self::CACHE_TTL);
            
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
