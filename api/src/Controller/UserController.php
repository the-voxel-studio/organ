<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\User;
use App\Entity\UserSession;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Validator\Validator\ValidatorInterface;

#[Route('/users', name: 'users_')]
class UserController extends AbstractController
{
    #[Route('/me', name: 'me', methods: ['GET'])]
    public function me(): JsonResponse
    {
        /** @var User|null $user */
        $user = $this->getUser();

        if (!$user) {
            return $this->json(['message' => 'Not authenticated'], 401);
        }

        return $this->json([
            'id' => $user->getId(),
            'uuid' => $user->getUuid(),
            'email' => $user->getEmail(),
            'firstName' => $user->getFirstName(),
            'lastName' => $user->getLastName(),
            'isVerified' => $user->isVerified(),
            'createdAt' => $user->getCreatedAt(),
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
            return $this->json(['message' => 'Not authenticated'], 401);
        }

        try {
            $data = $request->toArray();
        } catch (\Exception $e) {
            return $this->json(['message' => 'Invalid JSON'], 400);
        }

        if (empty($data)) {
            return $this->json(['message' => 'No data provided'], 400);
        }

        $hasChanged = false;
        $emailChanged = false;

        if (isset($data['firstName'])) {
            $user->setFirstName($data['firstName']);
            $hasChanged = true;
        }

        if (isset($data['lastName'])) {
            $user->setLastName($data['lastName']);
            $hasChanged = true;
        }

        if (isset($data['email']) && $data['email'] !== $user->getEmail()) {
            $user->setEmail($data['email']);
            $hasChanged = true;
            $emailChanged = true;
        }

        if (!$hasChanged) {
            return $this->json(['message' => 'No data to update'], 400);
        }

        // Validate the entity with new values
        $errors = $validator->validate($user);
        if (count($errors) > 0) {
            $errorMessages = [];
            foreach ($errors as $error) {
                $errorMessages[$error->getPropertyPath()] = $error->getMessage();
            }
            return $this->json(['message' => 'Validation failed', 'errors' => $errorMessages], 400);
        }

        // If email changed, we MUST update all associated refresh tokens (UserSession)
        if ($emailChanged) {
            $sessions = $entityManager->getRepository(UserSession::class)->findBy(['user' => $user]);
            foreach ($sessions as $session) {
                $session->setUsername($user->getEmail());
            }
        }

        $entityManager->flush();

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
            return $this->json(['message' => 'Not authenticated'], 401);
        }

        // 1. Soft delete the user
        $user->setDeletedAt(new \DateTime());

        // 2. IMPORTANT: Invalidate all refresh tokens immediately
        // This ensures the user cannot use /auth/refresh to bypass the block
        $sessions = $entityManager->getRepository(UserSession::class)->findBy(['user' => $user]);
        foreach ($sessions as $session) {
            $entityManager->remove($session);
        }

        $entityManager->flush();

        return $this->json([
            'message' => 'Account scheduled for deletion. All sessions have been invalidated. Log in again to reactivate.',
            'deletedAt' => $user->getDeletedAt()
        ]);
    }
}
