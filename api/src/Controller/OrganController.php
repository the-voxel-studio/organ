<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\Organ;
use App\Entity\Project;
use App\Entity\ProjectMember;
use App\Entity\User;
use App\Enum\IconType;
use App\Enum\ProjectGlobalRole;
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
        private readonly OrganPermissionService $permissionService
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

        $organs = $entityManager->getRepository(Organ::class)->findBy(['project' => $project, 'deletedAt' => null]);
        
        $data = [];
        foreach ($organs as $organ) {
            // Optional: only show organs where user has ORGAN_VIEW or is Project ADMIN/MANAGER
            if ($projectMember->getGlobalRole() !== ProjectGlobalRole::MEMBER || $this->permissionService->hasPermission($user, $organ, 'ORGAN_VIEW')) {
                $data[] = [
                    'uuid' => $organ->getUuid(),
                    'title' => $organ->getTitle(),
                    'description' => $organ->getDescription(),
                    'iconType' => $organ->getIconType()->value,
                    'iconData' => $organ->getIconData(),
                    'highlightColor' => $organ->getHighlightColor(),
                ];
            }
        }

        return $this->json($data);
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

        return $this->json([
            'uuid' => $organ->getUuid(),
            'title' => $organ->getTitle()
        ], Response::HTTP_CREATED);
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

        return $this->json([
            'uuid' => $organ->getUuid(),
            'title' => $organ->getTitle(),
            'description' => $organ->getDescription(),
            'iconType' => $organ->getIconType()->value,
            'iconData' => $organ->getIconData(),
            'highlightColor' => $organ->getHighlightColor(),
            'createdAt' => $organ->getCreatedAt()->format(\DateTimeInterface::ATOM),
        ]);
    }

    #[Route('/{organUuid}', name: 'update', methods: ['PUT', 'PATCH'])]
    public function update(string $projectUuid, string $organUuid, Request $request, EntityManagerInterface $entityManager, ValidatorInterface $validator): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);

        if (!$organ) {
            return $this->json(['message' => 'Organ not found'], Response::HTTP_NOT_FOUND);
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

        return $this->json(['message' => 'Organ updated']);
    }

    #[Route('/{organUuid}', name: 'delete', methods: ['DELETE'])]
    public function delete(string $projectUuid, string $organUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);

        if (!$organ) {
            return $this->json(['message' => 'Organ not found'], Response::HTTP_NOT_FOUND);
        }

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_EDIT')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $organ->setDeletedAt(new \DateTime());
        $entityManager->flush();

        return $this->json(null, Response::HTTP_NO_CONTENT);
    }
}
