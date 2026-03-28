<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\User;
use App\Entity\UserSession;
use App\Service\UserCacheService;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Validator\Validator\ValidatorInterface;

#[Route('/users', name: 'users_')]
class UserController extends AbstractController
{
    public function __construct(
        private readonly UserCacheService $userCacheService
    ) {}

    #[Route('/me', name: 'me', methods: ['GET'])]
    public function me(): JsonResponse
    {
        /** @var User|null $user */
        $user = $this->getUser();

        if (!$user) {
            return $this->json(['message' => 'Not authenticated'], Response::HTTP_UNAUTHORIZED);
        }

        // Pre-warm cache for current user if needed
        $this->userCacheService->getUserSummary($user);

        return $this->json([
            'id' => $user->getId(),
            'uuid' => $user->getUuid(),
            'email' => $user->getEmail(),
            'firstName' => $user->getFirstName(),
            'lastName' => $user->getLastName(),
            'isVerified' => $user->isVerified(),
            'createdAt' => $user->getCreatedAt()->format(\DateTimeInterface::ATOM),
            'authWithGoogle' => (bool) $user->getGoogleId(),
        ]);
    }

    #[Route('/me', name: 'update_me', methods: ['PUT'])]
    public function updateMe(
        Request $request,
        EntityManagerInterface $entityManager,
        ValidatorInterface $validator
    ): JsonResponse {
        /** @var User|null $user */
        $user = $this->getUser();

        if (!$user) {
            return $this->json(['message' => 'Not authenticated'], Response::HTTP_UNAUTHORIZED);
        }

        try {
            $data = $request->toArray();
        } catch (\Exception $e) {
            return $this->json(['message' => 'Invalid JSON'], Response::HTTP_BAD_REQUEST);
        }

        if (empty($data)) {
            return $this->json(['message' => 'No data provided'], Response::HTTP_BAD_REQUEST);
        }

        $hasChanged = false;
        $emailChanged = false;
        $profileChanged = false;

        if (isset($data['firstName'])) {
            $user->setFirstName($data['firstName']);
            $hasChanged = true;
            $profileChanged = true;
        }

        if (isset($data['lastName'])) {
            $user->setLastName($data['lastName']);
            $hasChanged = true;
            $profileChanged = true;
        }

        if (isset($data['email']) && $data['email'] !== $user->getEmail()) {
            $user->setEmail($data['email']);
            $hasChanged = true;
            $emailChanged = true;
        }

        if (!$hasChanged) {
            return $this->json(['message' => 'No data to update'], Response::HTTP_BAD_REQUEST);
        }

        // Validate
        $errors = $validator->validate($user);
        if (count($errors) > 0) {
            return $this->json($errors, Response::HTTP_BAD_REQUEST);
        }

        // Handle email change sessions
        if ($emailChanged) {
            $sessions = $entityManager->getRepository(UserSession::class)->findBy(['user' => $user]);
            foreach ($sessions as $session) {
                $session->setUsername($user->getEmail());
            }
        }

        $entityManager->flush();

        // Refresh cache if profile changed
        if ($profileChanged) {
            $this->userCacheService->refresh($user);
        }

        return $this->json([
            'message' => 'Profile updated successfully',
            'refresh' => $emailChanged,
            'user' => [
                'firstName' => $user->getFirstName(),
                'lastName' => $user->getLastName(),
                'email' => $user->getEmail(),
            ]
        ]);
    }

    #[Route('/me', name: 'delete_me', methods: ['DELETE'])]
    public function deleteMe(EntityManagerInterface $entityManager): JsonResponse
    {
        /** @var User|null $user */
        $user = $this->getUser();

        if (!$user) {
            return $this->json(['message' => 'Not authenticated'], Response::HTTP_UNAUTHORIZED);
        }

        $uuid = $user->getUuid();

        // Soft delete user
        $user->setDeletedAt(new \DateTime());

        // Invalidate sessions
        $sessions = $entityManager->getRepository(UserSession::class)->findBy(['user' => $user]);
        foreach ($sessions as $session) {
            $entityManager->remove($session);
        }

        $entityManager->flush();

        // Invalidate cache
        $this->userCacheService->invalidate($uuid);

        return $this->json([
            'message' => 'Account scheduled for deletion. All sessions have been invalidated.',
            'deletedAt' => $user->getDeletedAt()->format(\DateTimeInterface::ATOM)
        ]);
    }
}
