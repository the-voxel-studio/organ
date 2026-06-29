<?php

declare(strict_types=1);

namespace App\EventListener;

use App\Entity\User;
use Lexik\Bundle\JWTAuthenticationBundle\Event\JWTDecodedEvent;
use Symfony\Component\Security\Core\User\UserProviderInterface;

class JWTDecodedListener
{
    public function __construct(
        private \Symfony\Component\HttpFoundation\RequestStack $requestStack
    ) {}

    public function onJWTDecoded(JWTDecodedEvent $event): void
    {
        $payload = $event->getPayload();
        $request = $this->requestStack->getCurrentRequest();

        if ($request) {
            $request->attributes->set('jwt_payload', $payload);
        }
    }
}
