<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\Organ;
use App\Entity\Project;
use App\Entity\Task;
use App\Entity\TaskAttachment;
use App\Entity\User;
use App\Entity\ProjectDriveConfig;
use App\Service\OrganPermissionService;
use App\Service\TaskService;
use App\Service\UserCacheService;
use App\Service\GoogleDriveService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/attachments', name: 'task_attachments_')]
class TaskAttachmentController extends AbstractController
{
    public function __construct(
        private readonly TaskService $taskService,
        private readonly OrganPermissionService $permissionService,
        private readonly UserCacheService $userCacheService,
        private readonly GoogleDriveService $googleDriveService
    ) {}

    #[Route('', name: 'index', methods: ['GET'])]
    public function index(string $projectUuid, string $organUuid, string $taskUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);

        if (!$task) return $this->json(['message' => 'Task not found'], Response::HTTP_NOT_FOUND);

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_VIEW')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $attachments = $entityManager->getRepository(TaskAttachment::class)->findBy(['task' => $task, 'deletedAt' => null]);
        
        // --- PROACTIVE CLEANING : Check Google Drive files ---
        $driveFiles = [];
        foreach ($attachments as $att) {
            if (str_starts_with($att->getFilePath(), 'drive://')) {
                $driveFiles[$att->getUuid()] = str_replace('drive://', '', $att->getFilePath());
            }
        }

        if (!empty($driveFiles)) {
            $driveConfig = $entityManager->getRepository(ProjectDriveConfig::class)->findOneBy(['project' => $project, 'isActive' => true]);
            if ($driveConfig) {
                $accessToken = $this->googleDriveService->getAccessToken($driveConfig->getEncryptedRefreshToken());
                if ($accessToken) {
                    $existingIds = $this->googleDriveService->checkFilesExistence($accessToken, array_values($driveFiles));
                    $needsFlush = false;

                    foreach ($driveFiles as $uuid => $driveId) {
                        if (!in_array($driveId, $existingIds, true)) {
                            // File is gone! Hard delete from DB
                            $matches = array_filter($attachments, fn($a) => $a->getUuid() === $uuid);
                            $attToClean = reset($matches);
                            if ($attToClean) {
                                // Clean up associated history first
                                $entityManager->getRepository(\App\Entity\TaskHistory::class)->createQueryBuilder('h')
                                    ->delete()
                                    ->where('h.task = :task')
                                    ->andWhere('h.fieldName = :field')
                                    ->andWhere('h.old_value = :uuid OR h.new_value = :uuid')
                                    ->setParameter('task', $task)
                                    ->setParameter('field', 'attachment')
                                    ->setParameter('uuid', $attToClean->getUuid())
                                    ->getQuery()
                                    ->execute();

                                $entityManager->remove($attToClean);
                                $needsFlush = true;
                            }
                        }
                    }

                    if ($needsFlush) {
                        $entityManager->flush();
                        // Refresh list after cleaning
                        $attachments = $entityManager->getRepository(TaskAttachment::class)->findBy(['task' => $task, 'deletedAt' => null]);
                    }
                }
            }
        }

        $data = [];
        foreach ($attachments as $att) {
            $data[] = [
                'uuid' => $att->getUuid(),
                'fileName' => $att->getFileName(),
                'fileSize' => $att->getFileSize(),
                'fileType' => $att->getFileType(),
                'filePath' => $att->getFilePath(),
                'uploadedBy' => $this->userCacheService->getUserSummary($att->getUploadedBy()),
                'createdAt' => $att->getCreatedAt()->format(\DateTimeInterface::ATOM),
            ];
        }

        return $this->json($data);
    }

    #[Route('/init-upload', name: 'init_upload', methods: ['POST'])]
    public function initUpload(string $projectUuid, string $organUuid, string $taskUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);

        if (!$task) return $this->json(['message' => 'Task not found'], Response::HTTP_NOT_FOUND);

        $data = json_decode($request->getContent(), true);
        $fileSize = $data['fileSize'] ?? 0;
        $mimeType = $data['fileType'] ?? '';
        
        $allowedBinaryTypes = [
            'image/jpeg', 'image/png', 'image/gif', 'image/webp',
            'application/pdf'
        ];

        // Seul le stockage local (DB) a une limite de taille raisonnable (ex: 20Mo)
        $maxLocalSize = 20 * 1024 * 1024;
        $maxSizeMB = 20;
        $isBinaryType = in_array($mimeType, $allowedBinaryTypes, true);

        // Si ce n'est pas un type supporté localement OU si c'est trop gros pour la DB
        if (!$isBinaryType || $fileSize > $maxLocalSize) {
            $driveConfig = $entityManager->getRepository(ProjectDriveConfig::class)->findOneBy(['project' => $project, 'isActive' => true]);
            if ($driveConfig) {
                $accessToken = $this->googleDriveService->getAccessToken($driveConfig->getEncryptedRefreshToken());
                if ($accessToken) {
                    $folderId = $driveConfig->getDriveFolderId();
                    
                    // Fallback reconstruction dossier si besoin
                    if (!$folderId || !$this->googleDriveService->fileExists($accessToken, $folderId)) {
                        $rootFolderId = null;
                        foreach ($this->googleDriveService->listFolders($accessToken) as $f) {
                            if ($f['name'] === "Organ App") { $rootFolderId = $f['id']; break; }
                        }
                        if (!$rootFolderId) {
                            $root = $this->googleDriveService->createFolder($accessToken, "Organ App");
                            $rootFolderId = $root['id'] ?? null;
                        }
                        if ($rootFolderId) {
                            $shortId = substr($project->getUuid(), 0, 8);
                            $name = sprintf("%s (%s)", $project->getTitle(), $shortId);
                            $newFolder = $this->googleDriveService->createFolder($accessToken, $name, $rootFolderId);
                            if ($newFolder) {
                                $folderId = $newFolder['id'];
                                $driveConfig->setDriveFolderId($folderId);
                                $entityManager->flush();
                            }
                        }
                    }

                    if ($folderId) {
                        return $this->json([
                            'action' => 'upload_to_drive',
                            'folderId' => $folderId,
                            'accessToken' => $accessToken
                        ]);
                    }
                }
            }

            // Si on arrive ici, c'est que Drive n'est pas dispo mais que le fichier nécessite le Cloud
            if (!$isBinaryType) {
                return $this->json([
                    'message' => 'error.upload.unsupported_format_no_cloud',
                    'code' => 'UNSUPPORTED_FORMAT_NO_CLOUD'
                ], Response::HTTP_UNSUPPORTED_MEDIA_TYPE);
            }

            if ($fileSize > $maxLocalSize) {
                return $this->json([
                    'message' => 'error.upload.file_too_large_no_cloud',
                    'code' => 'FILE_TOO_LARGE_NO_CLOUD',
                    'limit' => $maxSizeMB
                ], Response::HTTP_REQUEST_ENTITY_TOO_LARGE);
            }
        }

        // Si on arrive ici, c'est un petit fichier Image/PDF -> Upload local classique
        return $this->json(['action' => 'upload_local']);
    }

    #[Route('', name: 'create', methods: ['POST'])]
    public function create(string $projectUuid, string $organUuid, string $taskUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);

        if (!$task) return $this->json(['message' => 'Task not found'], Response::HTTP_NOT_FOUND);

        if ($task->getDeletedAt() !== null) {
            return $this->json(['message' => 'Task is deleted and cannot be modified'], Response::HTTP_FORBIDDEN);
        }

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->taskService->can($user, $task, 'ATTACHMENT_ADD')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $file = $request->files->get('file');
        if (!$file) {
            return $this->json(['message' => 'No file uploaded'], Response::HTTP_BAD_REQUEST);
        }

        $mimeType = $file->getMimeType();
        $allowedBinaryTypes = [
            'image/jpeg', 'image/png', 'image/gif', 'image/webp',
            'application/pdf'
        ];
        
        $isBinaryType = in_array($mimeType, $allowedBinaryTypes, true);

        // If not Image/PDF, check for BYOS (Google Drive)
        if (!$isBinaryType) {
            $driveConfig = $entityManager->getRepository(ProjectDriveConfig::class)->findOneBy(['project' => $project, 'isActive' => true]);
            
            if ($driveConfig) {
                $accessToken = $this->googleDriveService->getAccessToken($driveConfig->getEncryptedRefreshToken());
                
                if ($accessToken) {
                    $folderId = $driveConfig->getDriveFolderId();
                    
                    // --- FALLBACK : Check if project folder exists, if not recreate ---
                    if (!$folderId || !$this->googleDriveService->fileExists($accessToken, $folderId)) {
                        // 1. Find or Create root "Organ App"
                        $rootFolderId = null;
                        foreach ($this->googleDriveService->listFolders($accessToken) as $f) {
                            if ($f['name'] === "Organ App") { $rootFolderId = $f['id']; break; }
                        }
                        if (!$rootFolderId) {
                            $root = $this->googleDriveService->createFolder($accessToken, "Organ App");
                            $rootFolderId = $root['id'] ?? null;
                        }

                        // 2. Create Project Folder
                        if ($rootFolderId) {
                            $shortId = substr($project->getUuid(), 0, 8);
                            $name = sprintf("%s (%s)", $project->getTitle(), $shortId);
                            $newFolder = $this->googleDriveService->createFolder($accessToken, $name, $rootFolderId);
                            if ($newFolder) {
                                $folderId = $newFolder['id'];
                                $driveConfig->setDriveFolderId($folderId);
                                $entityManager->flush();
                            }
                        }
                    }

                    if ($folderId) {
                        return $this->json([
                            'action' => 'upload_to_drive',
                            'folderId' => $folderId,
                            'accessToken' => $accessToken,
                            'fileName' => $file->getClientOriginalName(),
                            'fileSize' => $file->getSize(),
                            'fileType' => $mimeType
                        ], Response::HTTP_ACCEPTED);
                    }
                }
            }

            return $this->json(['message' => 'Format de fichier non supporté (Uniquement PDF et Images sans configuration Cloud)'], Response::HTTP_UNSUPPORTED_MEDIA_TYPE);
        }

        $attachment = new TaskAttachment();
        $attachment->setTask($task);
        $attachment->setUploadedBy($user);
        $attachment->setFileName($file->getClientOriginalName());
        $attachment->setFileSize((string)$file->getSize());
        $attachment->setFileType($mimeType);
        $attachment->setFileContent(fopen($file->getPathname(), 'r'));
        $attachment->setFilePath('db://' . $attachment->getUuid());

        $entityManager->persist($attachment);
        $entityManager->flush();

        return $this->json([
            'uuid' => $attachment->getUuid(),
            'fileName' => $attachment->getFileName()
        ], Response::HTTP_CREATED);
    }

    #[Route('/drive-confirm', name: 'drive_confirm', methods: ['POST'])]
    public function driveConfirm(string $projectUuid, string $organUuid, string $taskUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);

        if (!$task) return $this->json(['message' => 'Task not found'], Response::HTTP_NOT_FOUND);

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->taskService->can($user, $task, 'ATTACHMENT_ADD')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $data = json_decode($request->getContent(), true);
        if (!isset($data['driveId']) || !isset($data['fileName'])) {
            return $this->json(['message' => 'Missing fields (driveId, fileName)'], Response::HTTP_BAD_REQUEST);
        }

        $attachment = new TaskAttachment();
        $attachment->setTask($task);
        $attachment->setUploadedBy($user);
        $attachment->setFileName($data['fileName']);
        $attachment->setFileSize((string)($data['fileSize'] ?? 0));
        $attachment->setFileType($data['fileType'] ?? null);
        $attachment->setFilePath('drive://' . $data['driveId']);
        $attachment->setFileContent(null);

        $entityManager->persist($attachment);
        $entityManager->flush();

        return $this->json([
            'uuid' => $attachment->getUuid(),
            'fileName' => $attachment->getFileName()
        ], Response::HTTP_CREATED);
    }

    #[Route('/{attachmentUuid}/download', name: 'download', methods: ['GET'])]
    public function download(string $projectUuid, string $organUuid, string $taskUuid, string $attachmentUuid, EntityManagerInterface $entityManager): Response
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);
        $attachment = $entityManager->getRepository(TaskAttachment::class)->findOneBy(['uuid' => $attachmentUuid, 'task' => $task, 'deletedAt' => null]);

        if (!$attachment) {
            return $this->json(['message' => 'File not found'], Response::HTTP_NOT_FOUND);
        }

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_VIEW')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        // Handle Google Drive links
        if (str_starts_with($attachment->getFilePath(), 'drive://')) {
            $driveId = str_replace('drive://', '', $attachment->getFilePath());
            
            // --- FALLBACK : Check if file still exists on Drive ---
            $driveConfig = $entityManager->getRepository(ProjectDriveConfig::class)->findOneBy(['project' => $project, 'isActive' => true]);
            if ($driveConfig) {
                $accessToken = $this->googleDriveService->getAccessToken($driveConfig->getEncryptedRefreshToken());
                if ($accessToken) {
                    if (!$this->googleDriveService->fileExists($accessToken, $driveId)) {
                        // File is gone! Clean up history and hard delete from DB
                        $entityManager->getRepository(\App\Entity\TaskHistory::class)->createQueryBuilder('h')
                            ->delete()
                            ->where('h.task = :task')
                            ->andWhere('h.fieldName = :field')
                            ->andWhere('h.old_value = :uuid OR h.new_value = :uuid')
                            ->setParameter('task', $task)
                            ->setParameter('field', 'attachment')
                            ->setParameter('uuid', $attachment->getUuid())
                            ->getQuery()
                            ->execute();

                        $entityManager->remove($attachment);
                        $entityManager->flush();
                        return $this->json(['message' => 'Le fichier a été supprimé du Google Drive. Référence et historique nettoyés.'], Response::HTTP_NOT_FOUND);
                    }
                }
            }

            return $this->redirect('https://drive.google.com/open?id=' . $driveId);
        }

        $content = $attachment->getFileContent();
        if (!$content) {
            return $this->json(['message' => 'Binary content not found'], Response::HTTP_NOT_FOUND);
        }

        $binaryData = is_resource($content) ? stream_get_contents($content) : $content;

        $response = new Response($binaryData);
        $response->headers->set('Content-Type', $attachment->getFileType() ?? 'application/octet-stream');
        $response->headers->set('Content-Disposition', 'inline; filename="' . $attachment->getFileName() . '"');

        return $response;
    }

    #[Route('/{attachmentUuid}', name: 'delete', methods: ['DELETE'])]
    public function delete(string $projectUuid, string $organUuid, string $taskUuid, string $attachmentUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);
        
        $isPermanent = $request->query->getBoolean('permanent', false);
        $criteria = ['uuid' => $attachmentUuid, 'task' => $task];
        if (!$isPermanent) {
            $criteria['deletedAt'] = null;
        }

        $attachment = $entityManager->getRepository(TaskAttachment::class)->findOneBy($criteria);

        if (!$attachment) return $this->json(['message' => 'Attachment not found'], Response::HTTP_NOT_FOUND);

        /** @var User $user */
        $user = $this->getUser();
        
        $isOwner = ($attachment->getUploadedBy() === $user);
        
        if ($isPermanent) {
            $permAll = $this->permissionService->hasPermission($user, $organ, 'ATTACHMENT_HARD_DELETE_ALL');
            $permOwn = $isOwner && $this->permissionService->hasPermission($user, $organ, 'ATTACHMENT_HARD_DELETE_OWN');
            
            if (!$permAll && !$permOwn) {
                return $this->json(['message' => 'Access denied for permanent deletion'], Response::HTTP_FORBIDDEN);
            }
            
            // --- BYOS : Delete from Google Drive if needed ---
            if (str_starts_with($attachment->getFilePath(), 'drive://')) {
                $driveId = str_replace('drive://', '', $attachment->getFilePath());
                $driveConfig = $entityManager->getRepository(ProjectDriveConfig::class)->findOneBy(['project' => $project, 'isActive' => true]);
                if ($driveConfig) {
                    $accessToken = $this->googleDriveService->getAccessToken($driveConfig->getEncryptedRefreshToken());
                    if ($accessToken) {
                        $this->googleDriveService->deleteFile($accessToken, $driveId);
                    }
                }
            }

            $entityManager->remove($attachment);
        } else {
            $permAll = $this->permissionService->hasPermission($user, $organ, 'ATTACHMENT_DELETE_ALL');
            $permOwn = $isOwner && $this->permissionService->hasPermission($user, $organ, 'ATTACHMENT_DELETE_OWN');

            if (!$permAll && !$permOwn) {
                return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
            }
            
            $attachment->setDeletedAt(new \DateTime());
        }

        $entityManager->flush();

        return $this->json(null, Response::HTTP_NO_CONTENT);
    }

    #[Route('/trash', name: 'trash', methods: ['GET'], priority: 1)]
    public function trash(string $projectUuid, string $organUuid, string $taskUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);

        if (!$task) return $this->json(['message' => 'Task not found'], Response::HTTP_NOT_FOUND);

        /** @var User $user */
        $user = $this->getUser();
        if (!$this->permissionService->hasPermission($user, $organ, 'ORGAN_VIEW')) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $attachments = $entityManager->getRepository(TaskAttachment::class)->createQueryBuilder('a')
            ->where('a.task = :task')
            ->andWhere('a.deletedAt IS NOT NULL')
            ->setParameter('task', $task)
            ->getQuery()
            ->getResult();
        
        $data = [];
        foreach ($attachments as $att) {
            $data[] = [
                'uuid' => $att->getUuid(),
                'fileName' => $att->getFileName(),
                'fileSize' => $att->getFileSize(),
                'fileType' => $att->getFileType(),
                'uploadedBy' => $this->userCacheService->getUserSummary($att->getUploadedBy()),
                'deletedAt' => $att->getDeletedAt()->format(\DateTimeInterface::ATOM),
            ];
        }

        return $this->json($data);
    }

    #[Route('/{attachmentUuid}/restore', name: 'restore', methods: ['POST'])]
    public function restore(string $projectUuid, string $organUuid, string $taskUuid, string $attachmentUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);
        $attachment = $entityManager->getRepository(TaskAttachment::class)->findOneBy(['uuid' => $attachmentUuid, 'task' => $task]);

        if (!$attachment) return $this->json(['message' => 'Attachment not found'], Response::HTTP_NOT_FOUND);
        if ($attachment->getDeletedAt() === null) return $this->json(['message' => 'Attachment is not deleted'], Response::HTTP_BAD_REQUEST);

        /** @var User $user */
        $user = $this->getUser();
        
        $isOwner = ($attachment->getUploadedBy() === $user);
        $permAll = $this->permissionService->hasPermission($user, $organ, 'ATTACHMENT_DELETE_ALL');
        $permOwn = $isOwner && $this->permissionService->hasPermission($user, $organ, 'ATTACHMENT_DELETE_OWN');

        if (!$permAll && !$permOwn) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $attachment->setDeletedAt(null);
        $entityManager->flush();

        return $this->json([
            'uuid' => $attachment->getUuid(),
            'fileName' => $attachment->getFileName()
        ]);
    }
}
