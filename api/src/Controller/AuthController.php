<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\User;
use App\Entity\UserSession;
use App\Service\GoogleAuthService;
use Doctrine\ORM\EntityManagerInterface;
use Lexik\Bundle\JWTAuthenticationBundle\Security\Authenticator\Token\JWTPostAuthenticationToken;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\PasswordHasher\Hasher\UserPasswordHasherInterface;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Security\Http\Authentication\AuthenticationSuccessHandlerInterface;
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

    #[Route('/login/google', name: 'login_google', methods: ['POST'])]
    public function googleLogin(
        Request $request,
        GoogleAuthService $googleAuthService,
        EntityManagerInterface $entityManager,
        AuthenticationSuccessHandlerInterface $authenticationSuccessHandler,
        \Lexik\Bundle\JWTAuthenticationBundle\Encoder\JWTEncoderInterface $jwtEncoder
    ): \Symfony\Component\HttpFoundation\Response {
        $data = $request->toArray();
        $idToken = $data['idToken'] ?? $data['token'] ?? null;

        if (!$idToken) {
            return new JsonResponse(['error' => 'Token is required'], 400);
        }

        $googleUser = $googleAuthService->verifyToken($idToken);
        if (!$googleUser) {
            return new JsonResponse(['error' => 'Invalid Google token'], 401);
        }

        $user = $entityManager->getRepository(User::class)->findOneBy(['email' => $googleUser['email']]);

        if (!$user) {
            $user = new User();
            $user->setEmail($googleUser['email']);
            $user->setFirstName($googleUser['firstName']);
            $user->setLastName($googleUser['lastName']);
            $user->setGoogleId($googleUser['googleId']);
            $user->setIsVerified(true);
            
            $entityManager->persist($user);
            $entityManager->flush();
        } elseif (!$user->getGoogleId()) {
            $user->setGoogleId($googleUser['googleId']);
            $entityManager->flush();
        }

        // Generate a real JWT string for the token constructor
        $jwt = $jwtEncoder->encode(['username' => $user->getUserIdentifier()]);

        // Generate JWT and Refresh Token using Lexik's success handler
        $token = new JWTPostAuthenticationToken($user, 'auth', $user->getRoles(), $jwt);
        
        return $authenticationSuccessHandler->onAuthenticationSuccess($request, $token);
    }
}
