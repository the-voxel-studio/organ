<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\Organ;
use App\Entity\OrganLink;
use App\Entity\Project;
use App\Entity\User;
use App\Service\OrganPermissionService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/projects/{projectUuid}/organs/{organUuid}/links', name: 'organ_links_')]
class OrganLinkController extends AbstractController
{
    public function __construct(
        private readonly OrganPermissionService $permissionService
    ) {}

    #[Route('', name: 'index', methods: ['GET'])]
    public function index(string $projectUuid, string $organUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);

        if (!$organ) return $this->json(['message' => 'Organ not found'], Response::HTTP_NOT_FOUND);

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_VIEW')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $links = $entityManager->getRepository(OrganLink::class)->findBy(['organ' => $organ, 'deletedAt' => null]);
        
        $data = [];
        foreach ($links as $link) {
            $data[] = [
                'uuid' => $link->getUuid(),
                'url' => $link->getUrl(),
                'description' => $link->getDescription(),
            ];
        }

        return $this->json($data);
    }

    #[Route('/trash', name: 'trash', methods: ['GET'], priority: 1)]
    public function trash(string $projectUuid, string $organUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);

        if (!$organ) return $this->json(['message' => 'Organ not found'], Response::HTTP_NOT_FOUND);

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_LINK_MANAGE')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $links = $entityManager->getRepository(OrganLink::class)->createQueryBuilder('l')
            ->where('l.organ = :organ')
            ->andWhere('l.deletedAt IS NOT NULL')
            ->setParameter('organ', $organ)
            ->getQuery()
            ->getResult();
        
        $data = [];
        foreach ($links as $link) {
            $data[] = [
                'uuid' => $link->getUuid(),
                'url' => $link->getUrl(),
                'description' => $link->getDescription(),
                'deletedAt' => $link->getDeletedAt()?->format(\DateTimeInterface::ATOM),
            ];
        }

        return $this->json($data);
    }

    #[Route('', name: 'create', methods: ['POST'])]
    public function create(string $projectUuid, string $organUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);

        if (!$organ) return $this->json(['message' => 'Organ not found'], Response::HTTP_NOT_FOUND);

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_LINK_MANAGE')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $data = json_decode($request->getContent(), true);
        if (!$data || !isset($data['url'])) {
            return $this->json(['message' => 'URL is required'], Response::HTTP_BAD_REQUEST);
        }

        $link = new OrganLink();
        $link->setOrgan($organ);
        $link->setUrl($data['url']);
        $link->setDescription($data['description'] ?? null);

        $entityManager->persist($link);
        $entityManager->flush();

        return $this->json([
            'uuid' => $link->getUuid(),
            'url' => $link->getUrl(),
            'description' => $link->getDescription(),
        ], Response::HTTP_CREATED);
    }

    #[Route('/{linkUuid}', name: 'update', methods: ['PUT', 'PATCH'])]
    public function update(string $projectUuid, string $organUuid, string $linkUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $link = $entityManager->getRepository(OrganLink::class)->findOneBy(['uuid' => $linkUuid, 'organ' => $organ, 'deletedAt' => null]);

        if (!$link) return $this->json(['message' => 'Link not found'], Response::HTTP_NOT_FOUND);

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_LINK_MANAGE')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $data = json_decode($request->getContent(), true);
        if (!$data) return $this->json(['message' => 'Invalid JSON'], Response::HTTP_BAD_REQUEST);

        if (isset($data['url'])) {
            $link->setUrl($data['url']);
        }
        if (array_key_exists('description', $data)) {
            $link->setDescription($data['description']);
        }

        $entityManager->flush();

        return $this->json([
            'uuid' => $link->getUuid(),
            'url' => $link->getUrl(),
            'description' => $link->getDescription(),
        ]);
    }

    #[Route('/{linkUuid}', name: 'delete', methods: ['DELETE'])]
    public function delete(string $projectUuid, string $organUuid, string $linkUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $link = $entityManager->getRepository(OrganLink::class)->findOneBy(['uuid' => $linkUuid, 'organ' => $organ, 'deletedAt' => null]);

        if (!$link) return $this->json(['message' => 'Link not found'], Response::HTTP_NOT_FOUND);

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_LINK_MANAGE')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $link->setDeletedAt(new \DateTime());
        $entityManager->flush();

        return $this->json(null, Response::HTTP_NO_CONTENT);
    }

    #[Route('/{linkUuid}/restore', name: 'restore', methods: ['POST'])]
    public function restore(string $projectUuid, string $organUuid, string $linkUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $link = $entityManager->getRepository(OrganLink::class)->findOneBy(['uuid' => $linkUuid, 'organ' => $organ]);

        if (!$link) return $this->json(['message' => 'Link not found'], Response::HTTP_NOT_FOUND);

        if ($link->getDeletedAt() === null) {
            return $this->json(['message' => 'Link is not deleted'], Response::HTTP_BAD_REQUEST);
        }

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_LINK_MANAGE')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $link->setDeletedAt(null);
        $entityManager->flush();

        return $this->json([
            'uuid' => $link->getUuid(),
            'url' => $link->getUrl(),
            'description' => $link->getDescription(),
        ]);
    }
}
