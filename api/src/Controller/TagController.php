<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\Project;
use App\Entity\ProjectMember;
use App\Entity\Tag;
use App\Entity\User;
use App\Enum\ProjectGlobalRole;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/projects/{projectUuid}/tags', name: 'tags_')]
class TagController extends AbstractController
{
    #[Route('', name: 'index', methods: ['GET'])]
    public function index(string $projectUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        if (!$project) return $this->json(['message' => 'Project not found'], Response::HTTP_NOT_FOUND);

        $tags = $entityManager->getRepository(Tag::class)->findBy(['project' => $project, 'deletedAt' => null]);
        
        $data = [];
        foreach ($tags as $tag) {
            $data[] = [
                'uuid' => $tag->getUuid(),
                'name' => $tag->getName(),
                'color' => $tag->getColor(),
            ];
        }

        return $this->json($data);
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

        return $this->json([
            'uuid' => $tag->getUuid(),
            'name' => $tag->getName(),
            'color' => $tag->getColor(),
        ], Response::HTTP_CREATED);
    }
}
