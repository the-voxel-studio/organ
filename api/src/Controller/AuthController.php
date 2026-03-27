<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\User;
use App\Entity\UserSession;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\PasswordHasher\Hasher\UserPasswordHasherInterface;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Validator\Validator\ValidatorInterface;

#[Route('/auth', name: 'auth_')]
class AuthController extends AbstractController
{
    #[Route('/register', name: 'register', methods: ['POST'])]
    public function register(
        Request $request,
        UserPasswordHasherInterface $passwordHasher,
        EntityManagerInterface $entityManager,
        ValidatorInterface $validator
    ): JsonResponse {
        $data = $request->toArray();

        // Check if user already exists
        $existingUser = $entityManager->getRepository(User::class)->findOneBy(['email' => $data['email'] ?? '']);
        if ($existingUser) {
            return new JsonResponse(['error' => 'User already exists'], 400);
        }

        $user = new User();
        
        // Simple sanitization
        $email = filter_var($data['email'] ?? '', FILTER_SANITIZE_EMAIL);
        $firstName = strip_tags($data['firstName'] ?? '');
        $lastName = strip_tags($data['lastName'] ?? '');
        $plainPassword = $data['password'] ?? '';

        $user->setEmail($email);
        $user->setFirstName($firstName);
        $user->setLastName($lastName);

        // Validation
        $errors = $validator->validate($user);
        if (count($errors) > 0) {
            return $this->json($errors, 400);
        }

        if (empty($plainPassword) || strlen($plainPassword) < 8) {
            return new JsonResponse(['error' => 'Password must be at least 8 characters'], 400);
        }

        // Hashing
        $hashedPassword = $passwordHasher->hashPassword($user, $plainPassword);
        $user->setPassword($hashedPassword);

        $entityManager->persist($user);
        $entityManager->flush();

        return new JsonResponse([
            'status' => 'User registered successfully',
            'user' => [
                'uuid' => $user->getUuid(),
                'email' => $user->getEmail()
            ]
        ], 201);
    }

    #[Route('/login', name: 'login', methods: ['POST'])]
    public function login(): void
    {
        // Handled by LexikJWT (json_login)
    }

    #[Route('/refresh', name: 'refresh', methods: ['POST'])]
    public function refresh(): void
    {
        // Handled by GesdinetJWT (refresh_jwt)
    }

    #[Route('/logout', name: 'logout', methods: ['POST'])]
    public function logout(Request $request, EntityManagerInterface $entityManager): JsonResponse
    {
        $refreshToken = $request->cookies->get('refresh_token');

        if ($refreshToken) {
            $session = $entityManager->getRepository(UserSession::class)->findOneBy(['refreshToken' => $refreshToken]);
            if ($session) {
                $entityManager->remove($session);
                $entityManager->flush();
            }
        }

        $response = new JsonResponse(['message' => 'Logged out successfully']);
        
        // Clear cookies
        $response->headers->clearCookie('BEARER', '/');
        $response->headers->clearCookie('refresh_token', '/api/auth/refresh');

        return $response;
    }
}
