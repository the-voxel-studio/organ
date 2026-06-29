<?php

declare(strict_types=1);

namespace App\EventListener;

use App\Entity\User;
use Doctrine\ORM\EntityManagerInterface;
use Lexik\Bundle\JWTAuthenticationBundle\Event\AuthenticationSuccessEvent;
use Symfony\Component\EventDispatcher\Attribute\AsEventListener;

final class UserRestoreListener
{
    public function __construct(
        private EntityManagerInterface $entityManager
    ) {
    }

    #[AsEventListener(event: 'lexik_jwt_authentication.on_authentication_success')]
    public function onAuthenticationSuccess(AuthenticationSuccessEvent $event): void
    {
        $user = $event->getUser();

        if (!$user instanceof User) {
            return;
        }

        // If a soft-deleted user successfully logs in, restore them
        if ($user->getDeletedAt() !== null) {
            $user->restore();
            $this->entityManager->flush();
        }
    }
}
