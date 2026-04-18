<?php

namespace App\Controller;

use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;
use Symfony\Contracts\HttpClient\HttpClientInterface;

#[IsGranted('ROLE_USER')]
class SettingsController extends AbstractController
{
    #[Route('/settings', name: 'app_settings')]
    public function index(HttpClientInterface $apiClient, \Symfony\Component\HttpFoundation\RequestStack $requestStack): Response
    {
        $userData = [];
        try {
            $request = $requestStack->getCurrentRequest();
            $bearer = $request?->cookies->get('BEARER');

            $response = $apiClient->request('GET', '/api/users/me', [
                'headers' => [
                    'Cookie' => 'BEARER=' . $bearer
                ]
            ]);

            if ($response->getStatusCode() === 200) {
                $userData = $response->toArray();
            }
        } catch (\Exception $e) {
            // Log error or handle silently
        }

        if (empty($userData)) {
            // Fallback to app.user data if API call fails
            /** @var \App\Security\User $user */
            $user = $this->getUser();
            $userData = [
                'firstName' => $user->getFirstName(),
                'lastName' => $user->getLastName(),
                'email' => $user->getEmail(),
                'authWithGoogle' => false, // Default fallback
            ];
        }

        return $this->render('settings/index.html.twig', [
            'userData' => $userData,
        ]);
    }

}
