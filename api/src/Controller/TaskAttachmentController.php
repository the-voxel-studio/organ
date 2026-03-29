<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\Organ;
use App\Entity\Project;
use App\Entity\Task;
use App\Entity\TaskAttachment;
use App\Entity\User;
use App\Service\OrganPermissionService;
use App\Service\TaskService;
use App\Service\UserCacheService;
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
        private readonly UserCacheService $userCacheService
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

        $data = json_decode($request->getContent(), true);
        if (!$data || !isset($data['fileName']) || !isset($data['filePath'])) {
            return $this->json(['message' => 'Missing fields (fileName, filePath, fileSize)'], Response::HTTP_BAD_REQUEST);
        }

        $attachment = new TaskAttachment();
        $attachment->setTask($task);
        $attachment->setUploadedBy($user);
        $attachment->setFileName($data['fileName']);
        $attachment->setFilePath($data['filePath']);
        $attachment->setFileSize($data['fileSize'] ?? 0);
        $attachment->setFileType($data['fileType'] ?? null);

        $entityManager->persist($attachment);
        $entityManager->flush();

        return $this->json([
            'uuid' => $attachment->getUuid(),
            'fileName' => $attachment->getFileName()
        ], Response::HTTP_CREATED);
    }

    #[Route('/{attachmentUuid}', name: 'delete', methods: ['DELETE'])]
    public function delete(string $projectUuid, string $organUuid, string $taskUuid, string $attachmentUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $organ = $entityManager->getRepository(Organ::class)->findOneBy(['uuid' => $organUuid, 'project' => $project, 'deletedAt' => null]);
        $task = $entityManager->getRepository(Task::class)->findOneBy(['uuid' => $taskUuid, 'organ' => $organ, 'deletedAt' => null]);
        $attachment = $entityManager->getRepository(TaskAttachment::class)->findOneBy(['uuid' => $attachmentUuid, 'task' => $task, 'deletedAt' => null]);

        if (!$attachment) return $this->json(['message' => 'Attachment not found'], Response::HTTP_NOT_FOUND);

        /** @var User $user */
        $user = $this->getUser();
        
        $isOwner = ($attachment->getUploadedBy() === $user);
        $permAll = $this->permissionService->hasPermission($user, $organ, 'ATTACHMENT_DELETE_ALL');
        $permOwn = $isOwner && $this->permissionService->hasPermission($user, $organ, 'ATTACHMENT_DELETE_OWN');

        if (!$permAll && !$permOwn) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $attachment->setDeletedAt(new \DateTime());
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
