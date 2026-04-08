<?php

namespace App\Security;

use Symfony\Component\HttpFoundation\RedirectResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Generator\UrlGeneratorInterface;
use Symfony\Component\Security\Core\Authentication\Token\TokenInterface;
use Symfony\Component\Security\Core\Exception\AuthenticationException;
use Symfony\Component\Security\Http\Authenticator\AbstractAuthenticator;
use Symfony\Component\Security\Http\Authenticator\Passport\Badge\UserBadge;
use Symfony\Component\Security\Http\Authenticator\Passport\Passport;
use Symfony\Component\Security\Http\Authenticator\Passport\SelfValidatingPassport;
use Symfony\Component\Security\Http\EntryPoint\AuthenticationEntryPointInterface;
use Symfony\Contracts\HttpClient\HttpClientInterface;

class ExternalJwtAuthenticator extends AbstractAuthenticator implements AuthenticationEntryPointInterface
{
    public function __construct(
        private readonly UrlGeneratorInterface $urlGenerator,
        private readonly ApiUserProvider $userProvider,
        private readonly HttpClientInterface $apiClient,
    ) {
    }

    public function start(Request $request, AuthenticationException $authException = null): Response
    {
        return new RedirectResponse($this->urlGenerator->generate('app_login'));
    }

    public function supports(Request $request): ?bool
    {
        // On supporte toutes les requêtes, on vérifiera si le cookie est là
        // Mais on pourrait limiter à certaines routes si besoin.
        return true;
    }

    public function authenticate(Request $request): Passport
    {
        $accessToken = $request->cookies->get('BEARER');
        $refreshToken = $request->cookies->get('refresh_token');

        if (!$accessToken && !$refreshToken) {
            throw new AuthenticationException('No authentication tokens found.');
        }

        if (!$accessToken && $refreshToken) {
            // Tentative de refresh
            try {
                // IMPORTANT: Send the refresh_token cookie to the API
                $response = $this->apiClient->request('POST', '/api/auth/refresh', [
                    'headers' => [
                        'Cookie' => 'refresh_token=' . $refreshToken
                    ]
                ]);

                if (200 === $response->getStatusCode()) {
                    // Extract new access token from Set-Cookie header
                    $setCookies = $response->getHeaders()['set-cookie'] ?? [];
                    foreach ($setCookies as $cookie) {
                        if (str_starts_with($cookie, 'BEARER=')) {
                            preg_match('/BEARER=([^;]+)/', $cookie, $matches);
                            $accessToken = $matches[1] ?? null;
                        }
                    }
                }
            } catch (\Exception $e) {
                throw new AuthenticationException('Token refresh failed.');
            }
        }

        if (!$accessToken) {
            throw new AuthenticationException('Access token expired and refresh failed.');
        }

        return new SelfValidatingPassport(
            new UserBadge($accessToken, function (string $token) {
                return $this->userProvider->loadUserByToken($token);
            })
        );
    }

    public function onAuthenticationSuccess(Request $request, TokenInterface $token, string $firewallName): ?Response
    {
        // On laisse continuer la requête
        return null;
    }

    public function onAuthenticationFailure(Request $request, AuthenticationException $exception): ?Response
    {
        // Si c'est une page publique, on laisse passer sans authentification (le contrôleur fera le job)
        // Mais si on est dans l'access_control, Symfony redirigera vers start()
        return null;
    }
}
