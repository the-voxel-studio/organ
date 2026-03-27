<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\User;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\PasswordHasher\Hasher\UserPasswordHasherInterface;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Validator\Validator\ValidatorInterface;
use Symfony\Component\Validator\Constraints as Assert;

#[Route('/auth', name: 'auth_')]
class UserController extends AbstractController
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
}
