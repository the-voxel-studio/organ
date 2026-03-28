<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\Project;
use App\Entity\ProjectDriveConfig;
use App\Entity\ProjectMember;
use App\Entity\User;
use App\Enum\ProjectGlobalRole;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/projects/{projectUuid}/drive-config', name: 'project_drive_config_')]
class ProjectDriveConfigController extends AbstractController
{
    #[Route('', name: 'show', methods: ['GET'])]
    public function show(string $projectUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $this->checkAccess($project, $entityManager, [ProjectGlobalRole::ADMIN, ProjectGlobalRole::MANAGER]);

        $config = $entityManager->getRepository(ProjectDriveConfig::class)->findOneBy(['project' => $project]);

        if (!$config) {
            return $this->json(['message' => 'No Drive configuration found'], Response::HTTP_NOT_FOUND);
        }

        return $this->json([
            'uuid' => $config->getUuid(),
            'driveFolderId' => $config->getDriveFolderId(),
            'isActive' => $config->isActive(),
            // We do NOT return the encrypted refresh token for security reasons
        ]);
    }

    #[Route('', name: 'update', methods: ['PUT', 'POST'])]
    public function update(string $projectUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        // Only ADMIN can configure Drive
        $this->checkAccess($project, $entityManager, [ProjectGlobalRole::ADMIN]);

        $data = json_decode($request->getContent(), true);
        if (!isset($data['driveFolderId']) || !isset($data['refreshToken'])) {
            return $this->json(['message' => 'Missing required fields (driveFolderId, refreshToken)'], Response::HTTP_BAD_REQUEST);
        }

        $config = $entityManager->getRepository(ProjectDriveConfig::class)->findOneBy(['project' => $project]);

        if (!$config) {
            $config = new ProjectDriveConfig();
            $config->setProject($project);
            $entityManager->persist($config);
        }

        $config->setDriveFolderId($data['driveFolderId']);
        $config->setEncryptedRefreshToken($data['refreshToken']); // In a real app, encrypt this!
        
        if (isset($data['isActive'])) {
            $config->setIsActive((bool) $data['isActive']);
        }

        $entityManager->flush();

        return $this->json([
            'uuid' => $config->getUuid(),
            'message' => 'Drive configuration saved successfully'
        ]);
    }

    #[Route('', name: 'delete', methods: ['DELETE'])]
    public function delete(string $projectUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $this->checkAccess($project, $entityManager, [ProjectGlobalRole::ADMIN]);

        $config = $entityManager->getRepository(ProjectDriveConfig::class)->findOneBy(['project' => $project]);

        if ($config) {
            $entityManager->remove($config);
            $entityManager->flush();
        }

        return $this->json(null, Response::HTTP_NO_CONTENT);
    }

    private function checkAccess(?Project $project, EntityManagerInterface $entityManager, array $allowedRoles = null): void
    {
        if (!$project) {
            throw $this->createNotFoundException('Project not found');
        }

        /** @var User|null $user */
        $user = $this->getUser();
        if (!$user) {
            throw $this->createAccessDeniedException('Not authenticated');
        }

        $membership = $entityManager->getRepository(ProjectMember::class)->findOneBy(['project' => $project, 'user' => $user, 'deletedAt' => null]);
        
        if (!$membership) {
            throw $this->createAccessDeniedException('Access denied');
        }

        if ($allowedRoles !== null && !in_array($membership->getGlobalRole(), $allowedRoles, true)) {
            throw $this->createAccessDeniedException('Insufficient permissions');
        }
    }
}
