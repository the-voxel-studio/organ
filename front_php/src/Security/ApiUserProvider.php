<?php

namespace App\Security;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Component\Security\Core\Exception\UnsupportedUserException;
use Symfony\Component\Security\Core\User\UserInterface;
use Symfony\Component\Security\Core\User\UserProviderInterface;
use Symfony\Component\HttpFoundation\RequestStack;

class ApiUserProvider implements UserProviderInterface
{
    public function __construct(
        private readonly HttpClientInterface $apiClient,
        private readonly RequestStack $requestStack,
    ) {
    }

    public function loadUserByIdentifier(string $identifier): UserInterface
    {
        // On ne l'utilise pas ici, car on charge l'utilisateur via le token dans l'Authenticator
        throw new \LogicException('Use loadUserByToken instead');
    }

    public function loadUserByToken(string $token): UserInterface
    {
        try {
            $response = $this->apiClient->request('GET', '/api/users/me', [
                'headers' => [
                    'Authorization' => sprintf('Bearer %s', $token),
                ],
            ]);

            if (200 !== $response->getStatusCode()) {
                if (401 === $response->getStatusCode()) {
                    throw new ExpiredTokenException('Token expired');
                }
                $content = $response->getContent(false);
                throw new \Exception('Invalid token (API returned ' . $response->getStatusCode() . '): ' . $content);
            }

            $data = $response->toArray();

            return new User(
                $data['uuid'],
                $data['email'],
                $data['roles'] ?? [],
                $data['firstName'] ?? null,
                $data['lastName'] ?? null,
            );
        } catch (ExpiredTokenException $e) {
            throw $e;
        } catch (\Exception $e) {
            throw new \Exception('Could not load user from API: ' . $e->getMessage());
        }
    }

    public function refreshUser(UserInterface $user): UserInterface
    {
        if (!$user instanceof User) {
            throw new UnsupportedUserException(sprintf('Invalid user class "%s".', $user::class));
        }

        // Since we are stateless, we could just return the user or re-fetch it
        return $user;
    }

    public function supportsClass(string $class): bool
    {
        return User::class === $class || is_subclass_of($class, User::class);
    }
}
