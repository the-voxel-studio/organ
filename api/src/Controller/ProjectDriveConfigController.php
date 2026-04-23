<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\Project;
use App\Entity\ProjectDriveConfig;
use App\Entity\ProjectMember;
use App\Entity\User;
use App\Enum\ProjectGlobalRole;
use App\Service\ProjectDriveCacheService;
use App\Service\GoogleDriveService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/projects/{projectUuid}/drive-config', name: 'project_drive_config_')]
class ProjectDriveConfigController extends AbstractController
{
    public function __construct(
        private readonly ProjectDriveCacheService $driveCacheService,
        private readonly GoogleDriveService $googleDriveService
    ) {}

    #[Route('', name: 'show', methods: ['GET'])]
    public function show(string $projectUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $this->checkAccess($project, $entityManager, [ProjectGlobalRole::ADMIN, ProjectGlobalRole::MANAGER]);

        $fetcher = function () use ($entityManager, $project) {
            $config = $entityManager->getRepository(ProjectDriveConfig::class)->findOneBy(['project' => $project]);

            if (!$config) {
                return ['message' => 'No Drive configuration found', 'status' => Response::HTTP_NOT_FOUND];
            }

            return [
                'uuid' => $config->getUuid(),
                'driveFolderId' => $config->getDriveFolderId(),
                'isActive' => $config->isActive(),
            ];
        };

        $result = $this->driveCacheService->getDriveConfig($project, $fetcher);

        if (isset($result['status']) && $result['status'] === Response::HTTP_NOT_FOUND) {
            return $this->json(['message' => $result['message']], Response::HTTP_NOT_FOUND);
        }

        return $this->json($result);
    }

    #[Route('/connect-google', name: 'connect_google', methods: ['POST'])]
    public function connectGoogle(string $projectUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $this->checkAccess($project, $entityManager, [ProjectGlobalRole::ADMIN]);

        $data = json_decode($request->getContent(), true);
        if (!isset($data['authCode'])) {
            return $this->json(['message' => 'Missing authCode'], Response::HTTP_BAD_REQUEST);
        }

        $tokens = $this->googleDriveService->exchangeCode($data['authCode']);
        if (!$tokens || !isset($tokens['refresh_token'])) {
            return $this->json(['message' => 'Failed to exchange authorization code for refresh token. Ensure you requested offline access.'], Response::HTTP_BAD_GATEWAY);
        }

        $config = $entityManager->getRepository(ProjectDriveConfig::class)->findOneBy(['project' => $project]);
        if (!$config) {
            $config = new ProjectDriveConfig();
            $config->setProject($project);
            $entityManager->persist($config);
        }

        $config->setEncryptedRefreshToken($tokens['refresh_token']); // In a real app, encrypt this!
        $config->setIsActive(true);

        $entityManager->flush();
        $this->driveCacheService->invalidate($projectUuid);

        return $this->json(['message' => 'Google Account linked successfully']);
    }

    #[Route('/create-folder', name: 'create_folder', methods: ['POST'])]
    public function createFolder(string $projectUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $this->checkAccess($project, $entityManager, [ProjectGlobalRole::ADMIN]);

        $config = $entityManager->getRepository(ProjectDriveConfig::class)->findOneBy(['project' => $project]);
        if (!$config || !$config->getEncryptedRefreshToken()) {
            return $this->json(['message' => 'No linked Google account found'], Response::HTTP_NOT_FOUND);
        }

        $accessToken = $this->googleDriveService->getAccessToken($config->getEncryptedRefreshToken());
        if (!$accessToken) return $this->json(['message' => 'Failed to get access token'], Response::HTTP_UNAUTHORIZED);

        // 1. Find or Create root "Organ App" folder
        $rootFolderName = "Organ App";
        $rootFolderId = null;
        $existingFolders = $this->googleDriveService->listFolders($accessToken);
        foreach ($existingFolders as $f) {
            if ($f['name'] === $rootFolderName) {
                $rootFolderId = $f['id'];
                break;
            }
        }

        if (!$rootFolderId) {
            $rootFolder = $this->googleDriveService->createFolder($accessToken, $rootFolderName);
            if (!$rootFolder) return $this->json(['message' => 'Failed to create root Organ folder'], Response::HTTP_BAD_GATEWAY);
            $rootFolderId = $rootFolder['id'];
        }

        // 2. Create Unique Project Folder: "Project Name (short-uuid)"
        $shortId = substr($project->getUuid(), 0, 8);
        $projectFolderName = sprintf("%s (%s)", $project->getTitle(), $shortId);
        
        $projectFolder = $this->googleDriveService->createFolder($accessToken, $projectFolderName, $rootFolderId);
        if (!$projectFolder) return $this->json(['message' => 'Failed to create project folder'], Response::HTTP_BAD_GATEWAY);

        // 3. Save the folder ID automatically
        $config->setDriveFolderId($projectFolder['id']);
        $entityManager->flush();
        $this->driveCacheService->invalidate($projectUuid);

        return $this->json([
            'message' => 'Storage provisioned successfully',
            'driveFolderId' => $projectFolder['id'],
            'folderName' => $projectFolderName
        ]);
    }

    #[Route('/list-folders', name: 'list_folders', methods: ['GET'])]
    public function listFolders(string $projectUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $this->checkAccess($project, $entityManager, [ProjectGlobalRole::ADMIN]);

        $config = $entityManager->getRepository(ProjectDriveConfig::class)->findOneBy(['project' => $project]);
        if (!$config || !$config->getEncryptedRefreshToken()) {
            return $this->json(['message' => 'No linked Google account found'], Response::HTTP_NOT_FOUND);
        }

        $accessToken = $this->googleDriveService->getAccessToken($config->getEncryptedRefreshToken());
        if (!$accessToken) return $this->json(['message' => 'Failed to get access token'], Response::HTTP_UNAUTHORIZED);

        $folders = $this->googleDriveService->listFolders($accessToken);
        return $this->json($folders);
    }

    #[Route('/select-folder', name: 'select_folder', methods: ['POST'])]
    public function selectFolder(string $projectUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $this->checkAccess($project, $entityManager, [ProjectGlobalRole::ADMIN]);

        $config = $entityManager->getRepository(ProjectDriveConfig::class)->findOneBy(['project' => $project]);
        if (!$config) return $this->json(['message' => 'No linked Google account found'], Response::HTTP_NOT_FOUND);

        $data = json_decode($request->getContent(), true);
        if (!isset($data['driveFolderId'])) {
            return $this->json(['message' => 'Missing driveFolderId'], Response::HTTP_BAD_REQUEST);
        }

        $config->setDriveFolderId($data['driveFolderId']);
        $entityManager->flush();
        $this->driveCacheService->invalidate($projectUuid);

        return $this->json([
            'message' => 'Folder selected successfully',
            'driveFolderId' => $config->getDriveFolderId()
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

        $this->driveCacheService->invalidate($projectUuid);

        return $this->json(null, Response::HTTP_NO_CONTENT);
    }

    private function checkAccess(?Project $project, EntityManagerInterface $entityManager, ?array $allowedRoles = null): void
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

        if ($allowedRoles !== null) {
            $roleValue = $membership->getGlobalRole();
            if ($roleValue instanceof \BackedEnum) $roleValue = $roleValue->value;
            
            $allowedValues = array_map(fn($r) => $r instanceof \BackedEnum ? $r->value : $r, $allowedRoles);
            
            if (!in_array($roleValue, $allowedValues, true)) {
                throw $this->createAccessDeniedException('Insufficient permissions');
            }
        }
    }
}
