<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\Project;
use App\Entity\ProjectMember;
use App\Entity\User;
use App\Enum\ProjectGlobalRole;
use App\Service\UserCacheService;
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
        private readonly UserCacheService $userCacheService
    ) {}

    #[Route('', name: 'index', methods: ['GET'])]
    public function index(string $projectUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        if (!$project) {
            return $this->json(['message' => 'Project not found'], Response::HTTP_NOT_FOUND);
        }

        $this->checkAccess($project, $entityManager);

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

        return $this->json($data);
    }

    #[Route('', name: 'add', methods: ['POST'])]
    public function add(string $projectUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $currentUserMember = $this->checkAccess($project, $entityManager, [ProjectGlobalRole::ADMIN, ProjectGlobalRole::MANAGER]);

        $data = json_decode($request->getContent(), true);
        if (!isset($data['userUuid'])) {
            return $this->json(['message' => 'User UUID is required'], Response::HTTP_BAD_REQUEST);
        }

        $userToAdd = $entityManager->getRepository(User::class)->findOneBy(['uuid' => $data['userUuid'], 'deletedAt' => null]);
        if (!$userToAdd) {
            return $this->json(['message' => 'User not found'], Response::HTTP_NOT_FOUND);
        }

        $roleRequest = isset($data['role']) ? ProjectGlobalRole::tryFrom($data['role']) : ProjectGlobalRole::MEMBER;
        
        // Pyramidal Rule: Only ADMIN can add a MANAGER. MANAGER can only add MEMBER.
        if ($currentUserMember->getGlobalRole() === ProjectGlobalRole::MANAGER && $roleRequest !== ProjectGlobalRole::MEMBER) {
            return $this->json(['message' => 'Managers can only add members with MEMBER role'], Response::HTTP_FORBIDDEN);
        }

        // Only one ADMIN rule: cannot add someone as ADMIN directly if one exists (must use update/transfer)
        if ($roleRequest === ProjectGlobalRole::ADMIN) {
            return $this->json(['message' => 'Cannot add a new ADMIN. Use role transfer instead.'], Response::HTTP_BAD_REQUEST);
        }

        $existingMember = $entityManager->getRepository(ProjectMember::class)->findOneBy(['project' => $project, 'user' => $userToAdd]);
        if ($existingMember && $existingMember->getDeletedAt() === null) {
            return $this->json(['message' => 'User is already a member'], Response::HTTP_CONFLICT);
        }

        if ($existingMember) {
            $existingMember->setDeletedAt(null);
            $existingMember->setGlobalRole($roleRequest);
            $member = $existingMember;
        } else {
            $member = new ProjectMember();
            $member->setProject($project);
            $member->setUser($userToAdd);
            $member->setGlobalRole($roleRequest);
            $entityManager->persist($member);
        }

        $entityManager->flush();

        return $this->json([
            'uuid' => $member->getUuid(),
            'user' => $this->userCacheService->getUserSummary($userToAdd),
            'role' => $member->getGlobalRole()->value,
        ], Response::HTTP_CREATED);
    }

    #[Route('/{memberUuid}', name: 'update', methods: ['PUT', 'PATCH'])]
    public function update(string $projectUuid, string $memberUuid, Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        $currentUserMember = $this->checkAccess($project, $entityManager, [ProjectGlobalRole::ADMIN, ProjectGlobalRole::MANAGER]);

        $targetMember = $entityManager->getRepository(ProjectMember::class)->findOneBy(['uuid' => $memberUuid, 'project' => $project, 'deletedAt' => null]);
        if (!$targetMember) {
            return $this->json(['message' => 'Member not found'], Response::HTTP_NOT_FOUND);
        }

        // Pyramidal Rule: MANAGER cannot update other MANAGERS or the ADMIN
        if ($currentUserMember->getGlobalRole() === ProjectGlobalRole::MANAGER) {
            if ($targetMember->getGlobalRole() !== ProjectGlobalRole::MEMBER) {
                return $this->json(['message' => 'Managers can only manage users with MEMBER role'], Response::HTTP_FORBIDDEN);
            }
        }

        $data = json_decode($request->getContent(), true);
        if (isset($data['role'])) {
            $newRole = ProjectGlobalRole::tryFrom($data['role']);
            if (!$newRole) {
                return $this->json(['message' => 'Invalid role'], Response::HTTP_BAD_REQUEST);
            }

            // Transfer of Ownership (Single ADMIN rule)
            if ($newRole === ProjectGlobalRole::ADMIN) {
                if ($currentUserMember->getGlobalRole() !== ProjectGlobalRole::ADMIN) {
                    return $this->json(['message' => 'Only the current ADMIN can transfer the administrator role'], Response::HTTP_FORBIDDEN);
                }
                // Current ADMIN becomes MANAGER
                $currentUserMember->setGlobalRole(ProjectGlobalRole::MANAGER);
                $targetMember->setGlobalRole(ProjectGlobalRole::ADMIN);
            } else {
                // Pyramidal Rule: MANAGER can only promote to MANAGER if they are ADMIN
                if ($currentUserMember->getGlobalRole() === ProjectGlobalRole::MANAGER && $newRole !== ProjectGlobalRole::MEMBER) {
                    return $this->json(['message' => 'Managers cannot promote members to MANAGER'], Response::HTTP_FORBIDDEN);
                }
                $targetMember->setGlobalRole($newRole);
            }
        }

        $entityManager->flush();

        return $this->json(['message' => 'Member updated', 'newRole' => $targetMember->getGlobalRole()->value]);
    }

    #[Route('/{memberUuid}', name: 'remove', methods: ['DELETE'])]
    public function remove(string $projectUuid, string $memberUuid, EntityManagerInterface $entityManager): JsonResponse
    {
        $project = $entityManager->getRepository(Project::class)->findOneBy(['uuid' => $projectUuid, 'deletedAt' => null]);
        if (!$project) {
            return $this->json(['message' => 'Project not found'], Response::HTTP_NOT_FOUND);
        }

        /** @var User $currentUser */
        $currentUser = $this->getUser();
        $currentUserMember = $entityManager->getRepository(ProjectMember::class)->findOneBy(['project' => $project, 'user' => $currentUser, 'deletedAt' => null]);

        if (!$currentUserMember) {
            return $this->json(['message' => 'Access denied'], Response::HTTP_FORBIDDEN);
        }

        $targetMember = $entityManager->getRepository(ProjectMember::class)->findOneBy(['uuid' => $memberUuid, 'project' => $project, 'deletedAt' => null]);
        if (!$targetMember) {
            return $this->json(['message' => 'Member not found'], Response::HTTP_NOT_FOUND);
        }

        $isSelf = ($targetMember->getUser() === $currentUser);

        // Pyramidal Rule: MANAGER can only remove MEMBERS
        if (!$isSelf) {
            if ($currentUserMember->getGlobalRole() === ProjectGlobalRole::MEMBER) {
                return $this->json(['message' => 'Insufficient permissions'], Response::HTTP_FORBIDDEN);
            }
            if ($currentUserMember->getGlobalRole() === ProjectGlobalRole::MANAGER && $targetMember->getGlobalRole() !== ProjectGlobalRole::MEMBER) {
                return $this->json(['message' => 'Managers can only remove users with MEMBER role'], Response::HTTP_FORBIDDEN);
            }
        }

        // Single ADMIN Safety: cannot leave or be removed if last admin (must transfer first)
        if ($targetMember->getGlobalRole() === ProjectGlobalRole::ADMIN) {
            return $this->json(['message' => 'The ADMIN cannot leave the project. Transfer the role to someone else first.'], Response::HTTP_BAD_REQUEST);
        }

        $targetMember->setDeletedAt(new \DateTime());
        $entityManager->flush();

        return $this->json(null, Response::HTTP_NO_CONTENT);
    }

    private function checkAccess(?Project $project, EntityManagerInterface $entityManager, array $allowedRoles = null): ProjectMember
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

        return $membership;
    }
}
