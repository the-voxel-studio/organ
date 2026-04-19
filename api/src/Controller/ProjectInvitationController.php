<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\ProjectInvitation;
use App\Entity\ProjectMember;
use App\Entity\User;
use App\Service\ProjectMembershipService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/invitations', name: 'invitations_')]
class ProjectInvitationController extends AbstractController
{
    #[Route('', name: 'index', methods: ['GET'])]
    public function index(EntityManagerInterface $entityManager): JsonResponse
    {
        /** @var User $user */
        $user = $this->getUser();
        if (!$user) return $this->json(['message' => 'Not authenticated'], Response::HTTP_UNAUTHORIZED);

        $invitations = $entityManager->getRepository(ProjectInvitation::class)->findBy([
            'email' => $user->getEmail(),
            'acceptedAt' => null
        ]);

        $data = [];
        foreach ($invitations as $inv) {
            if ($inv->getExpiresAt() > new \DateTime()) {
                $data[] = [
                    'uuid' => $inv->getUuid(),
                    'projectName' => $inv->getProject()->getTitle(),
                    'invitedBy' => $inv->getInvitedBy()?->getFirstName() ?? 'Un utilisateur',
                    'role' => $inv->getRole()->value,
                    'createdAt' => $inv->getCreatedAt()->format(\DateTimeInterface::ATOM),
                    'expiresAt' => $inv->getExpiresAt()->format(\DateTimeInterface::ATOM),
                ];
            }
        }

        return $this->json($data);
    }

    #[Route('/{uuid}/accept', name: 'accept', methods: ['POST'])]
    public function accept(
        string $uuid, 
        EntityManagerInterface $entityManager, 
        ProjectMembershipService $membershipService
    ): JsonResponse {
        /** @var User $user */
        $user = $this->getUser();
        $invitation = $entityManager->getRepository(ProjectInvitation::class)->findOneBy([
            'uuid' => $uuid,
            'email' => $user->getEmail(),
            'acceptedAt' => null
        ]);

        if (!$invitation || $invitation->getExpiresAt() < new \DateTime()) {
            return $this->json(['message' => 'Invitation not found or expired'], Response::HTTP_NOT_FOUND);
        }

        // Check if user was already in the project (possibly deleted)
        $existingMember = $entityManager->getRepository(ProjectMember::class)->findOneBy([
            'project' => $invitation->getProject(),
            'user' => $user
        ]);

        if ($existingMember) {
            // Restore presence
            $existingMember->setDeletedAt(null);
            $existingMember->setGlobalRole($invitation->getRole());
        } else {
            // Create new Project Member
            $member = new ProjectMember();
            $member->setProject($invitation->getProject());
            $member->setUser($user);
            $member->setGlobalRole($invitation->getRole());
            $entityManager->persist($member);
        }

        // Mark invitation as accepted
        $invitation->setAcceptedAt(new \DateTime());
        
        $entityManager->flush();

        // Invalidate cache for this project/user
        $membershipService->invalidate($user->getUuid(), $invitation->getProject()->getUuid());

        return $this->json(['message' => 'Joined project successfully']);
    }

    #[Route('/{uuid}/refuse', name: 'refuse', methods: ['POST'])]
    public function refuse(string $uuid, EntityManagerInterface $entityManager): JsonResponse
    {
        /** @var User $user */
        $user = $this->getUser();
        $invitation = $entityManager->getRepository(ProjectInvitation::class)->findOneBy([
            'uuid' => $uuid,
            'email' => $user->getEmail(),
            'acceptedAt' => null
        ]);

        if ($invitation) {
            $entityManager->remove($invitation);
            $entityManager->flush();
        }

        return $this->json(['message' => 'Invitation refused']);
    }
}
