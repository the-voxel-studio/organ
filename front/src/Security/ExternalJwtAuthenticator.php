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
        // On s'active si l'un des deux cookies est présent
        return $request->cookies->has('BEARER') || $request->cookies->has('refresh_token');
    }

    public function authenticate(Request $request): Passport
    {
        $accessToken = $request->cookies->get('BEARER') ?: null;
        $refreshToken = $request->cookies->get('refresh_token') ?: null;

        $user = null;

        // 1. Tenter l'access token s'il existe
        if ($accessToken) {
            try {
                $user = $this->userProvider->loadUserByToken($accessToken);
            } catch (ExpiredTokenException $e) {
                // Access token expiré (401), on passe au refresh
                $accessToken = null;
            } catch (\Exception $e) {
                // Erreur API (500, etc.), on échoue sans forcément tout casser
                throw new AuthenticationException('API temporarily unavailable');
            }
        }

        // 2. Refresh si nécessaire
        if (!$user && $refreshToken) {
            try {
                $response = $this->apiClient->request('POST', '/api/auth/refresh', [
                    'headers' => [
                        'Accept' => 'application/json',
                        'Cookie' => 'refresh_token=' . $refreshToken,
                    ],
                    'json' => ['refresh_token' => $refreshToken],
                ]);

                $statusCode = $response->getStatusCode();
                if ($statusCode === 200 || $statusCode === 204) {
                    // Extraction des nouveaux jetons
                    $setCookies = $response->getHeaders(false)['set-cookie'] ?? [];
                    foreach ($setCookies as $cookieString) {
                        if (preg_match('/(BEARER|bearer)=([^;]+)/i', $cookieString, $matches)) {
                            $accessToken = $matches[2];
                            $request->attributes->set('_new_bearer_cookie', $cookieString);
                            // On injecte immédiatement pour les appels suivants de la requête
                            $request->cookies->set('BEARER', $accessToken);
                        }
                        if (preg_match('/refresh_token=([^;]+)/i', $cookieString, $matches)) {
                            $request->attributes->set('_new_refresh_cookie', $cookieString);
                            $request->cookies->set('refresh_token', $matches[1]);
                        }
                    }

                    // Fallback JSON pour 200
                    if ($statusCode === 200) {
                        $data = $response->toArray(false);
                        if (!$accessToken && isset($data['token'])) {
                            $accessToken = $data['token'];
                            $request->attributes->set('_new_bearer_cookie', 'BEARER=' . $accessToken);
                            $request->cookies->set('BEARER', $accessToken);
                        }
                    }

                    if ($accessToken) {
                        try {
                            $user = $this->userProvider->loadUserByToken($accessToken);
                        } catch (\Exception $e) {
                            $user = null;
                        }
                    }
                }
            } catch (\Exception $e) {
                // Le refresh a échoué (réseau ou 401 sur le refresh)
            }
        }

        // 3. Échec final : Aucun utilisateur trouvé après tentative d'access token et de refresh
        if (!$user) {
            // Signal pour onAuthenticationFailure de supprimer physiquement les cookies chez le client
            $request->attributes->set('_auth_should_clear_cookies', true);
            throw new AuthenticationException('Session expired');
        }

        return new SelfValidatingPassport(
            new UserBadge($user->getUserIdentifier(), fn() => $user)
        );
    }

    public function onAuthenticationSuccess(Request $request, TokenInterface $token, string $firewallName): ?Response
    {
        return null;
    }

    public function onAuthenticationFailure(Request $request, AuthenticationException $exception): ?Response
    {
        $shouldClear = $request->attributes->get('_auth_should_clear_cookies');

        // Si on est déjà sur le login, on arrête la redirection mais on force le nettoyage
        if ($request->attributes->get('_route') === 'app_login') {
            if ($shouldClear) {
                $response = new RedirectResponse($this->urlGenerator->generate('app_login'));
                $response->headers->clearCookie('BEARER');
                $response->headers->clearCookie('refresh_token');
                return $response;
            }
            return null;
        }

        $response = new RedirectResponse($this->urlGenerator->generate('app_login'));
        if ($shouldClear) {
            $response->headers->clearCookie('BEARER');
            $response->headers->clearCookie('refresh_token');
        }

        return $response;
    }
}
