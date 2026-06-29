<?php

declare(strict_types=1);

namespace App\Security;

use App\Entity\User;
use Symfony\Component\HttpFoundation\RequestStack;
use Symfony\Component\Security\Core\Authentication\Token\TokenInterface;
use Symfony\Component\Security\Core\Exception\CustomUserMessageAccountStatusException;
use Symfony\Component\Security\Core\User\UserCheckerInterface;
use Symfony\Component\Security\Core\User\UserInterface;

class UserChecker implements UserCheckerInterface
{
    public function __construct(
        private readonly RequestStack $requestStack
    ) {
    }

    public function checkPreAuth(UserInterface $user): void
    {
        if (!$user instanceof User) {
            return;
        }

        $request = $this->requestStack->getCurrentRequest();
        $isLoginRoute = $request && $request->attributes->get('_route') === 'auth_login';

        if ($user->getDeletedAt() !== null && !$isLoginRoute) {
            throw new CustomUserMessageAccountStatusException('Your account is scheduled for deletion. Please log in again to reactivate it.');
        }

        // Only block if we are on the traditional login route (password login)
        if ($isLoginRoute) {
            if ($user->getPassword() === null && $user->getGoogleId() !== null) {
                throw new CustomUserMessageAccountStatusException('This account uses Google Login. Please use the "Sign in with Google" button.');
            }
        }
    }

    public function checkPostAuth(UserInterface $user, ?TokenInterface $token = null): void
    {
        if (!$user instanceof User || !$token) {
            return;
        }

        // Check JWT version if available in payload
        // Note: For LexikJWT, the payload is usually available via getAttributes() or similar
        // but it's cleaner to handle this in a dedicated listener or by extracting it from the request attribute
        
        $request = $this->requestStack->getCurrentRequest();
        if (!$request) {
            return;
        }

        $payload = $request->attributes->get('jwt_payload');
        if ($payload && isset($payload['jwt_version'])) {
            if ($payload['jwt_version'] < $user->getJwtVersion()) {
                throw new CustomUserMessageAccountStatusException('Token has been invalidated. Please refresh your session.');
            }
        }
    }
}
