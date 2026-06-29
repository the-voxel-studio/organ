<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\Project;
use App\Entity\ProjectMember;
use App\Entity\ProjectInvitation;
use App\Entity\User;
use App\Enum\ProjectGlobalRole;
use App\Service\ProjectMembershipService;
use App\Service\UserCacheService;
use App\Service\NotificationService;
use App\Entity\Organ;
use App\Entity\UserOrganRole;
use App\Entity\Task;
use App\Entity\TaskAssignee;
use App\Service\OrganPermissionService;
use App\Service\OrganCacheService;
use App\Service\TaskCacheService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/projects/{projectUuid}/members', name: 'project_members_')]
class ProjectMemberController extends AbstractController
{
    public function __construct(
        private readonly UserCacheService $userCacheService,
        private readonly ProjectMembershipService $membershipService,
        private readonly NotificationService $notificationService,
        private readonly OrganPermissionService $permissionService,
        private readonly OrganCacheService $organCacheService,
        private readonly TaskCacheService $taskCacheService
    ) {}

    #[Route('', name: 'index', methods: ['GET'])]
    public function index(string $projectUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        if (!$project) return $this->json(['message' => 'Project not found'], Response::HTTP_NOT_FOUND);

        $this->checkAccess($project, $entityManager);

        $fetcher = function () use ($entityManager, $project) {
            $memberships = $entityManager->getRepository(ProjectMember::class)->findBy(['project' => $project, 'deletedAt' => null]);
            
            $data = [];
            foreach ($memberships as $membership) {
                $user = $membership->getUser();
                if ($user) {
                    $data[] = [
                        'uuid' => $membership->getUuid(),
                        'user' => $this->userCacheService->getUserSummary($user),
                        'role' => $membership->getGlobalRole()->value,
                    ];
                }
            }
            return $data;
        };

        return $this->json($this->membershipService->getMemberList($project, $fetcher));
    }

    #[Route('/trash', name: 'trash', methods: ['GET'])]
    public function trash(string $projectUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        if (!$project) return $this->json(['message' => 'Project not found'], Response::HTTP_NOT_FOUND);

        $this->checkAccess($project, $entityManager, [ProjectGlobalRole::ADMIN, ProjectGlobalRole::MANAGER]);

        $memberships = $entityManager->getRepository(ProjectMember::class)->createQueryBuilder('pm')
            ->where('pm.project = :project')
            ->andWhere('pm.deletedAt IS NOT NULL')
            ->setParameter('project', $project)
            ->getQuery()
            ->getResult();
        
        $data = [];
        foreach ($memberships as $membership) {
            $user = $membership->getUser();
            if ($user) {
                $data[] = [
                    'uuid' => $membership->getUuid(),
                    'user' => $this->userCacheService->getUserSummary($user),
                    'role' => $membership->getGlobalRole()->value,
                    'deletedAt' => $membership->getDeletedAt()->format(\DateTimeInterface::ATOM),
                ];
            }
        }

        return $this->json($data);
    }

    #[Route('/invitations', name: 'invitations', methods: ['GET'])]
    public function invitations(string $projectUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        if (!$project) return $this->json(['message' => 'Project not found'], Response::HTTP_NOT_FOUND);

        $this->checkAccess($project, $entityManager, [ProjectGlobalRole::ADMIN, ProjectGlobalRole::MANAGER]);

        $invitations = $entityManager->getRepository(ProjectInvitation::class)->findBy([
            'project' => $project,
            'acceptedAt' => null
        ]);

        $data = [];
        foreach ($invitations as $inv) {
            if ($inv->getExpiresAt() > new \DateTime()) {
                $data[] = [
                    'uuid' => $inv->getUuid(),
                    'email' => $inv->getEmail(),
                    'role' => $inv->getRole()->value,
                    'createdAt' => $inv->getCreatedAt()->format(\DateTimeInterface::ATOM),
                    'expiresAt' => $inv->getExpiresAt()->format(\DateTimeInterface::ATOM),
                ];
            }
        }

        return $this->json($data);
    }

    #[Route('/invite', name: 'invite', methods: ['POST'])]
    public function invite(string $projectUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $currentUserMember = $this->checkAccess($project, $entityManager, [ProjectGlobalRole::ADMIN, ProjectGlobalRole::MANAGER]);

        $data = json_decode($request->getContent(), true);
        if (!isset($data['email'])) {
            return $this->json(['message' => 'Email is required'], Response::HTTP_BAD_REQUEST);
        }

        $email = $data['email'];
        $role = isset($data['role']) ? ProjectGlobalRole::tryFrom($data['role']) : ProjectGlobalRole::MEMBER;

        if ($currentUserMember->getGlobalRole() === ProjectGlobalRole::MANAGER && $role !== ProjectGlobalRole::MEMBER) {
            return $this->json(['message' => 'Managers can only invite members with MEMBER role'], Response::HTTP_FORBIDDEN);
        }

        // Check if already member
        $existingUser = $entityManager->getRepository(User::class)->findOneBy(['email' => $email, 'deletedAt' => null]);
        if ($existingUser) {
            $existingMember = $entityManager->getRepository(ProjectMember::class)->findOneBy(['project' => $project, 'user' => $existingUser, 'deletedAt' => null]);
            if ($existingMember) {
                return $this->json(['message' => 'User is already a member'], Response::HTTP_CONFLICT);
            }
        }

        // Check if already invited (active invitation)
        $existingInv = $entityManager->getRepository(ProjectInvitation::class)->findOneBy([
            'project' => $project,
            'email' => $email,
            'acceptedAt' => null
        ]);

        if ($existingInv && $existingInv->getExpiresAt() > new \DateTime()) {
            return $this->json(['message' => 'User is already invited'], Response::HTTP_CONFLICT);
        }

        $invitation = new ProjectInvitation();
        $invitation->setProject($project);
        $invitation->setEmail($email);
        $invitation->setRole($role);
        $invitation->setInvitedBy($this->getUser());

        $entityManager->persist($invitation);
        $entityManager->flush();

        // Notify user if they already exist on the platform
        if ($existingUser) {
            $this->notificationService->notify(
                $existingUser, 
                'PROJECT_INVITATION', 
                sprintf('Vous avez été invité à rejoindre le projet "%s"', $project->getTitle())
            );
        }

        // TODO: Send real email via Mailer service here

        return $this->json([
            'uuid' => $invitation->getUuid(),
            'message' => 'Invitation sent successfully'
        ], Response::HTTP_CREATED);
    }

    #[Route('/{memberUuid}', name: 'update', methods: ['PUT', 'PATCH'])]
    public function update(string $projectUuid, string $memberUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $currentUserMember = $this->checkAccess($project, $entityManager, [ProjectGlobalRole::ADMIN, ProjectGlobalRole::MANAGER]);

        $targetMember = $entityManager->getRepository(ProjectMember::class)->findOneBy(['uuid' => $memberUuid, 'project' => $project]);
        if (!$targetMember) return $this->json(['message' => 'Member not found'], Response::HTTP_NOT_FOUND);

        if ($targetMember->getDeletedAt() !== null) {
            return $this->json(['message' => 'Member is deleted and cannot be updated'], Response::HTTP_FORBIDDEN);
        }

        $data = json_decode($request->getContent(), true);
        if (isset($data['role'])) {
            $newRole = ProjectGlobalRole::tryFrom($data['role']);
            if (!$newRole) return $this->json(['message' => 'Invalid role'], Response::HTTP_BAD_REQUEST);

            if ($targetMember->getGlobalRole() === $newRole) {
                return $this->json(['message' => 'Member updated']);
            }

            if ($currentUserMember->getGlobalRole() === ProjectGlobalRole::MANAGER && $targetMember->getGlobalRole() !== ProjectGlobalRole::MEMBER) {
                return $this->json(['message' => 'Managers can only manage users with MEMBER role'], Response::HTTP_FORBIDDEN);
            }

            if ($newRole === ProjectGlobalRole::ADMIN) {
                if ($currentUserMember->getGlobalRole() !== ProjectGlobalRole::ADMIN) return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
                $currentUserMember->setGlobalRole(ProjectGlobalRole::MANAGER);
                $targetMember->setGlobalRole(ProjectGlobalRole::ADMIN);
                $this->membershipService->invalidate($currentUserMember->getUser()->getUuid(), $project->getUuid());
                $this->membershipService->invalidate($targetMember->getUser()->getUuid(), $project->getUuid());
            } else {
                if ($currentUserMember->getGlobalRole() === ProjectGlobalRole::MANAGER && $newRole !== ProjectGlobalRole::MEMBER) return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
                $targetMember->setGlobalRole($newRole);
                $this->membershipService->invalidate($targetMember->getUser()->getUuid(), $project->getUuid());
            }
        }

        $entityManager->flush();
        return $this->json(['message' => 'Member updated']);
    }

    #[Route('/{memberUuid}', name: 'remove', methods: ['DELETE'])]
    public function remove(string $projectUuid, string $memberUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $currentUserMember = $this->checkAccess($project, $entityManager);

        $isPermanent = $request->query->getBoolean('permanent', false);
        $criteria = ['uuid' => $memberUuid, 'project' => $project];
        if (!$isPermanent) {
            $criteria['deletedAt'] = null;
        }

        $targetMember = $entityManager->getRepository(ProjectMember::class)->findOneBy($criteria);
        if (!$targetMember) return $this->json(['message' => 'Member not found'], Response::HTTP_NOT_FOUND);

        $isSelf = ($targetMember->getUser() === $this->getUser());
        if (!$isSelf) {
            if ($currentUserMember->getGlobalRole() === ProjectGlobalRole::MEMBER) return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
            if ($currentUserMember->getGlobalRole() === ProjectGlobalRole::MANAGER && $targetMember->getGlobalRole() !== ProjectGlobalRole::MEMBER) return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        if ($targetMember->getGlobalRole() === ProjectGlobalRole::ADMIN) return $this->json(['message' => 'Cannot remove ADMIN'], Response::HTTP_BAD_REQUEST);

        $targetUserUuid = $targetMember->getUser()->getUuid();

        if ($isPermanent) {
            if ($currentUserMember->getGlobalRole() !== ProjectGlobalRole::ADMIN) {
                return $this->json(['message' => 'Only project admins can perform permanent deletion'], Response::HTTP_FORBIDDEN);
            }
            $entityManager->remove($targetMember);
        } else {
            $targetMember->setDeletedAt(new \DateTime());
        }

        // Clean up user access, roles, and assignments in all project Organs
        $targetUser = $targetMember->getUser();
        $organs = $entityManager->getRepository(Organ::class)->findBy(['project' => $project]);

        foreach ($organs as $organ) {
            // 1. Remove/Soft-delete UserOrganRole relationships
            $uorQuery = $entityManager->getRepository(UserOrganRole::class)->createQueryBuilder('uor')
                ->join('uor.role', 'r')
                ->where('uor.user = :user')
                ->andWhere('r.organ = :organ')
                ->setParameter('user', $targetUser)
                ->setParameter('organ', $organ);
            if (!$isPermanent) {
                $uorQuery->andWhere('uor.deletedAt IS NULL');
            }
            $uors = $uorQuery->getQuery()->getResult();

            foreach ($uors as $uor) {
                if ($isPermanent) {
                    $entityManager->remove($uor);
                } else {
                    $uor->setDeletedAt(new \DateTime());
                }
            }

            // 2. Remove/Soft-delete TaskAssignee relationships
            $taQuery = $entityManager->getRepository(TaskAssignee::class)->createQueryBuilder('ta')
                ->join('ta.task', 't')
                ->where('ta.user = :user')
                ->andWhere('t.organ = :organ')
                ->setParameter('user', $targetUser)
                ->setParameter('organ', $organ);
            if (!$isPermanent) {
                $taQuery->andWhere('ta.deletedAt IS NULL');
            }
            $assignees = $taQuery->getQuery()->getResult();

            foreach ($assignees as $ta) {
                if ($isPermanent) {
                    $entityManager->remove($ta);
                } else {
                    $ta->setDeletedAt(new \DateTime());
                }
            }

            // 3. Unset this user as manager of tasks in this organ
            $managedTasks = $entityManager->getRepository(Task::class)->findBy([
                'organ' => $organ,
                'manager' => $targetUser
            ]);

            foreach ($managedTasks as $task) {
                $task->setManager(null);
            }

            // 4. Invalidate caches for this user and organ
            $this->permissionService->invalidateUserRoles($targetUser->getUuid(), $organ->getUuid());
            $this->organCacheService->invalidateMemberList($organ->getUuid());
            $this->taskCacheService->invalidateUserTasksInOrgan($targetUser->getUuid(), $organ->getUuid());
        }

        $entityManager->flush();
        $this->membershipService->invalidate($targetUserUuid, $project->getUuid());

        return $this->json(null, Response::HTTP_NO_CONTENT);
    }

    #[Route('/{memberUuid}/restore', name: 'restore', methods: ['POST'])]
    public function restore(string $projectUuid, string $memberUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        if (!$project) return $this->json(['message' => 'Project not found'], Response::HTTP_NOT_FOUND);

        $currentUserMember = $this->checkAccess($project, $entityManager, [ProjectGlobalRole::ADMIN, ProjectGlobalRole::MANAGER]);

        $targetMember = $entityManager->getRepository(ProjectMember::class)->findOneBy(['uuid' => $memberUuid, 'project' => $project]);
        if (!$targetMember) return $this->json(['message' => 'Member not found'], Response::HTTP_NOT_FOUND);

        if ($targetMember->getDeletedAt() === null) {
            return $this->json(['message' => 'Member is not deleted'], Response::HTTP_BAD_REQUEST);
        }

        if ($currentUserMember->getGlobalRole() === ProjectGlobalRole::MANAGER && $targetMember->getGlobalRole() !== ProjectGlobalRole::MEMBER) {
            return $this->json(['message' => 'Managers can only restore users with MEMBER role'], Response::HTTP_FORBIDDEN);
        }

        $deletedTime = $targetMember->getDeletedAt();
        $targetUser = $targetMember->getUser();

        $targetMember->setDeletedAt(null);

        if ($deletedTime) {
            $organs = $entityManager->getRepository(Organ::class)->findBy(['project' => $project]);

            foreach ($organs as $organ) {
                // Restore UserOrganRole relationships deleted at the same time
                $uors = $entityManager->getRepository(UserOrganRole::class)->createQueryBuilder('uor')
                    ->join('uor.role', 'r')
                    ->where('uor.user = :user')
                    ->andWhere('r.organ = :organ')
                    ->andWhere('uor.deletedAt IS NOT NULL')
                    ->setParameter('user', $targetUser)
                    ->setParameter('organ', $organ)
                    ->getQuery()
                    ->getResult();

                foreach ($uors as $uor) {
                    $diff = abs($uor->getDeletedAt()->getTimestamp() - $deletedTime->getTimestamp());
                    if ($diff <= 5) {
                        $uor->setDeletedAt(null);
                    }
                }

                // Restore TaskAssignee relationships deleted at the same time
                $assignees = $entityManager->getRepository(TaskAssignee::class)->createQueryBuilder('ta')
                    ->join('ta.task', 't')
                    ->where('ta.user = :user')
                    ->andWhere('t.organ = :organ')
                    ->andWhere('ta.deletedAt IS NOT NULL')
                    ->setParameter('user', $targetUser)
                    ->setParameter('organ', $organ)
                    ->getQuery()
                    ->getResult();

                foreach ($assignees as $ta) {
                    $diff = abs($ta->getDeletedAt()->getTimestamp() - $deletedTime->getTimestamp());
                    if ($diff <= 5) {
                        $ta->setDeletedAt(null);
                    }
                }

                // Invalidate caches for this user and organ
                $this->permissionService->invalidateUserRoles($targetUser->getUuid(), $organ->getUuid());
                $this->organCacheService->invalidateMemberList($organ->getUuid());
                $this->taskCacheService->invalidateUserTasksInOrgan($targetUser->getUuid(), $organ->getUuid());
            }
        }

        $entityManager->flush();
        $this->membershipService->invalidate($targetUser->getUuid(), $project->getUuid());

        return $this->json(['message' => 'Member restored successfully']);
    }

    private function checkAccess(?Project $project, EntityManagerInterface $entityManager, ?array $allowedRoles = null): ProjectMember
    {
        if (!$project) throw $this->createNotFoundException('Project not found');
        $user = $this->getUser();
        if (!$user instanceof User) throw $this->createAccessDeniedException();

        $membership = $entityManager->getRepository(ProjectMember::class)->findOneBy(['project' => $project, 'user' => $user, 'deletedAt' => null]);
        if (!$membership) throw $this->createAccessDeniedException();
        
        if ($allowedRoles !== null) {
            $roleValue = $membership->getGlobalRole();
            if ($roleValue instanceof \BackedEnum) $roleValue = $roleValue->value;
            
            $allowedValues = array_map(fn($r) => $r instanceof \BackedEnum ? $r->value : $r, $allowedRoles);
            
            if (!in_array($roleValue, $allowedValues, true)) {
                throw $this->createAccessDeniedException('Insufficient permissions');
            }
        }

        return $membership;
    }
}
