<?php

namespace App\EventListener;

use Symfony\Component\EventDispatcher\Attribute\AsEventListener;
use Symfony\Component\HttpKernel\Event\ResponseEvent;

final class AuthCookieListener
{
    #[AsEventListener(event: 'kernel.response')]
    public function onKernelResponse(ResponseEvent $event): void
    {
        $request = $event->getRequest();
        $response = $event->getResponse();

        $bearerStr = $request->attributes->get('_new_bearer_cookie');
        $refreshStr = $request->attributes->get('_new_refresh_cookie');

        if ($bearerStr) {
            $this->addCookieToResponse($response, $bearerStr, 'BEARER');
        }

        if ($refreshStr) {
            $this->addCookieToResponse($response, $refreshStr, 'refresh_token');
        }
    }

    private function addCookieToResponse(\Symfony\Component\HttpFoundation\Response $response, string $cookieStr, string $name): void
    {
        // On extrait la valeur du cookie (ex: BEARER=abc; path=/; ...)
        // Utilisation de /i pour l'insensibilité à la casse
        if (preg_match('/' . $name . '=([^;]+)/i', $cookieStr, $matches)) {
            $value = $matches[1];
            
            // On crée un nouveau cookie propre au domaine du frontend
            $cookie = \Symfony\Component\HttpFoundation\Cookie::create($name)
                ->withValue($value)
                ->withPath('/')
                ->withHttpOnly(true)
                ->withSameSite('lax');

            // On ajoute une durée de vie cohérente (ex: 30 min pour BEARER, 30 jours pour refresh)
            if ($name === 'BEARER') {
                $cookie = $cookie->withExpires(time() + 1800);
            } else {
                $cookie = $cookie->withExpires(time() + 2592000);
            }

            $response->headers->setCookie($cookie);
        }
    }
}
