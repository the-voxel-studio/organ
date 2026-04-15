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

        if ($user->getDeletedAt() !== null) {
            throw new CustomUserMessageAccountStatusException('Your account is scheduled for deletion. Please log in again to reactivate it.');
        }

        // Only block if we are on the traditional login route (password login)
        $request = $this->requestStack->getCurrentRequest();
        if ($request && $request->attributes->get('_route') === 'auth_login') {
            if ($user->getPassword() === null && $user->getGoogleId() !== null) {
                throw new CustomUserMessageAccountStatusException('This account uses Google Login. Please use the "Sign in with Google" button.');
            }
        }
    }

    public function checkPostAuth(UserInterface $user, ?TokenInterface $token = null): void
    {
    }
}
