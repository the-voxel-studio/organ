<?php

namespace App\Controller;

use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Security\Http\Authentication\AuthenticationUtils;

use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Component\HttpFoundation\RedirectResponse;

class SecurityController extends AbstractController
{
    public function __construct(
        private readonly HttpClientInterface $apiClient
    ) {}

    #[Route('/login', name: 'app_login')]
    public function login(AuthenticationUtils $authenticationUtils): Response
    {
        // On ne redirige QUE si l'utilisateur est réellement authentifié par l'Authenticator
        if ($this->getUser()) {
            return $this->redirectToRoute('app_dashboard');
        }

        // get the login error if there is one
        $error = $authenticationUtils->getLastAuthenticationError();
        // last username entered by the user
        $lastUsername = $authenticationUtils->getLastUsername();

        return $this->render('security/login.html.twig', ['last_username' => $lastUsername, 'error' => $error]);
    }

    #[Route('/logout', name: 'app_logout')]
    public function logout(\Symfony\Component\HttpFoundation\Request $request): Response
    {
        // On tente d'appeler l'API pour invalider le refresh token côté serveur.
        try {
            // IMPORTANT: Pass current cookies to the API so it knows which token to invalidate
            $cookies = $request->headers->get('Cookie');
            $this->apiClient->request('POST', '/api/auth/logout', [
                'headers' => [
                    'Cookie' => $cookies
                ]
            ]);
        } catch (\Exception $e) {
            // Log error if needed
        }

        $response = new RedirectResponse($this->generateUrl('app_home'));
        
        // On nettoie les cookies localement sur le domaine du front
        $response->headers->clearCookie('BEARER', '/');
        $response->headers->clearCookie('refresh_token', '/');

        return $response;
    }
}
