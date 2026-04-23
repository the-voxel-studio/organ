<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\Project;
use App\Entity\ProjectMember;
use App\Entity\Tag;
use App\Entity\User;
use App\Enum\ProjectGlobalRole;
use App\Service\TagCacheService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/projects/{projectUuid}/tags', name: 'tags_')]
class TagController extends AbstractController
{
    public function __construct(
        private readonly TagCacheService $tagCacheService
    ) {}

    #[Route('', name: 'index', methods: ['GET'])]
    public function index(string $projectUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        if (!$project) return $this->json(['message' => 'Project not found'], Response::HTTP_NOT_FOUND);

        /** @var User $user */
        $user = $this->getUser();
        $membership = $entityManager->getRepository(ProjectMember::class)->findOneBy(['project' => $project, 'user' => $user, 'deletedAt' => null]);

        if (!$membership) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $fetcher = function () use ($entityManager, $project) {
            $tags = $entityManager->getRepository(Tag::class)->findBy(['project' => $project, 'deletedAt' => null]);
            
            $data = [];
            foreach ($tags as $tag) {
                $data[] = [
                    'uuid' => $tag->getUuid(),
                    'name' => $tag->getName(),
                    'color' => $tag->getColor(),
                ];
            }
            return $data;
        };

        return $this->json($this->tagCacheService->getTagList($project, $fetcher));
    }

    #[Route('', name: 'create', methods: ['POST'])]
    public function create(string $projectUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        if (!$project) return $this->json(['message' => 'Project not found'], Response::HTTP_NOT_FOUND);

        /** @var User $user */
        $user = $this->getUser();
        $membership = $entityManager->getRepository(ProjectMember::class)->findOneBy(['project' => $project, 'user' => $user, 'deletedAt' => null]);

        if (!$membership || !in_array($membership->getGlobalRole(), [ProjectGlobalRole::ADMIN, ProjectGlobalRole::MANAGER], true)) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $data = json_decode($request->getContent(), true);
        if (!isset($data['name'])) return $this->json(['message' => 'Name required'], Response::HTTP_BAD_REQUEST);

        $tag = new Tag();
        $tag->setProject($project);
        $tag->setName($data['name']);
        $tag->setColor($data['color'] ?? '#808080');

        $entityManager->persist($tag);
        $entityManager->flush();

        // Invalidate cache
        $this->tagCacheService->invalidateList($projectUuid);

        return $this->json([
            'uuid' => $tag->getUuid(),
            'name' => $tag->getName(),
            'color' => $tag->getColor(),
        ], Response::HTTP_CREATED);
    }

    #[Route('/trash', name: 'trash', methods: ['GET'])]
    public function trash(string $projectUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        if (!$project) return $this->json(['message' => 'Project not found'], Response::HTTP_NOT_FOUND);

        /** @var User $user */
        $user = $this->getUser();
        $membership = $entityManager->getRepository(ProjectMember::class)->findOneBy(['project' => $project, 'user' => $user, 'deletedAt' => null]);

        if (!$membership || !in_array($membership->getGlobalRole(), [ProjectGlobalRole::ADMIN, ProjectGlobalRole::MANAGER], true)) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $tags = $entityManager->getRepository(Tag::class)->findBy(['project' => $project]);
        
        $data = [];
        foreach ($tags as $tag) {
            if ($tag->getDeletedAt() !== null) {
                $data[] = [
                    'uuid' => $tag->getUuid(),
                    'name' => $tag->getName(),
                    'color' => $tag->getColor(),
                    'deletedAt' => $tag->getDeletedAt()->format(\DateTimeInterface::ATOM),
                ];
            }
        }

        return $this->json($data);
    }

    #[Route('/{tagUuid}', name: 'update', methods: ['PUT', 'PATCH'])]
    public function update(string $projectUuid, string $tagUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        if (!$project) return $this->json(['message' => 'Project not found'], Response::HTTP_NOT_FOUND);

        $tag = $entityManager->getRepository(Tag::class)->findOneBy(['uuid' => $tagUuid, 'project' => $project]);
        if (!$tag) return $this->json(['message' => 'Tag not found'], Response::HTTP_NOT_FOUND);

        if ($tag->getDeletedAt() !== null) {
            return $this->json(['message' => 'Tag is deleted and cannot be updated'], Response::HTTP_FORBIDDEN);
        }

        /** @var User $user */
        $user = $this->getUser();
        $membership = $entityManager->getRepository(ProjectMember::class)->findOneBy(['project' => $project, 'user' => $user, 'deletedAt' => null]);

        if (!$membership || !in_array($membership->getGlobalRole(), [ProjectGlobalRole::ADMIN, ProjectGlobalRole::MANAGER], true)) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $data = json_decode($request->getContent(), true);
        if (isset($data['name'])) $tag->setName($data['name']);
        if (isset($data['color'])) $tag->setColor($data['color']);

        $entityManager->flush();

        // Invalidate cache
        $this->tagCacheService->invalidateList($projectUuid);

        return $this->json(['message' => 'Tag updated']);
    }

    #[Route('/{tagUuid}/restore', name: 'restore', methods: ['POST'])]
    public function restore(string $projectUuid, string $tagUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        if (!$project) return $this->json(['message' => 'Project not found'], Response::HTTP_NOT_FOUND);

        $tag = $entityManager->getRepository(Tag::class)->findOneBy(['uuid' => $tagUuid, 'project' => $project]);
        if (!$tag) return $this->json(['message' => 'Tag not found'], Response::HTTP_NOT_FOUND);

        if ($tag->getDeletedAt() === null) {
            return $this->json(['message' => 'Tag is not deleted'], Response::HTTP_BAD_REQUEST);
        }

        /** @var User $user */
        $user = $this->getUser();
        $membership = $entityManager->getRepository(ProjectMember::class)->findOneBy(['project' => $project, 'user' => $user, 'deletedAt' => null]);

        if (!$membership || !in_array($membership->getGlobalRole(), [ProjectGlobalRole::ADMIN, ProjectGlobalRole::MANAGER], true)) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $tag->setDeletedAt(null);
        $entityManager->flush();

        // Invalidate cache
        $this->tagCacheService->invalidateList($projectUuid);

        return $this->json(['message' => 'Tag restored']);
    }

    #[Route('/{tagUuid}', name: 'delete', methods: ['DELETE'])]
    public function delete(string $projectUuid, string $tagUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        if (!$project) return $this->json(['message' => 'Project not found'], Response::HTTP_NOT_FOUND);

        $isPermanent = $request->query->getBoolean('permanent', false);
        $criteria = ['uuid' => $tagUuid, 'project' => $project];
        if (!$isPermanent) {
            $criteria['deletedAt'] = null;
        }
        
        $tag = $entityManager->getRepository(Tag::class)->findOneBy($criteria);
        if (!$tag) return $this->json(['message' => 'Tag not found'], Response::HTTP_NOT_FOUND);

        /** @var User $user */
        $user = $this->getUser();
        $membership = $entityManager->getRepository(ProjectMember::class)->findOneBy(['project' => $project, 'user' => $user, 'deletedAt' => null]);

        if (!$membership || !in_array($membership->getGlobalRole(), [ProjectGlobalRole::ADMIN, ProjectGlobalRole::MANAGER], true)) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        if ($isPermanent) {
            $entityManager->remove($tag);
        } else {
            $tag->setDeletedAt(new \DateTime());
        }
        
        $entityManager->flush();

        // Invalidate cache
        $this->tagCacheService->invalidateList($projectUuid);

        return $this->json(null, Response::HTTP_NO_CONTENT);
    }
}
