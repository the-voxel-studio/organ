<?php

namespace App\Controller;

use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;
use Symfony\Contracts\HttpClient\HttpClientInterface;
use Symfony\Component\HttpFoundation\RequestStack;
use Symfony\Component\DependencyInjection\Attribute\Target;

#[IsGranted('ROLE_USER')]
class SettingsController extends AbstractController
{
    use ProjectIdentityTrait;

    #[Route('/settings', name: 'app_settings')]
    public function index(
        #[Target('api.client')] HttpClientInterface $apiClient, 
        RequestStack $requestStack
    ): Response
    {
        $userData = [];
        $projects = [];
        
        try {
            $request = $requestStack->getCurrentRequest();
            $bearer = $request?->cookies->get('BEARER');

            $userResponse = $apiClient->request('GET', '/api/users/me', [
                'headers' => [
                    'Cookie' => 'BEARER=' . $bearer
                ]
            ]);

            if ($userResponse->getStatusCode() === 200) {
                $userData = $userResponse->toArray();
            }

            $projectsResponse = $apiClient->request('GET', '/api/projects', [
                'headers' => [
                    'Cookie' => 'BEARER=' . $bearer
                ]
            ]);

            if ($projectsResponse->getStatusCode() === 200) {
                $projects = $projectsResponse->toArray();
                foreach ($projects as &$project) {
                    $identity = $this->extractIdentity($project);
                    $project['color'] = $identity['color'];
                    $project['iconName'] = $identity['iconName'];
                }
            }

        } catch (\Exception $e) {
            // Log error or handle silently
        }

        if (empty($userData)) {
            /** @var \App\Security\User $user */
            $user = $this->getUser();
            $userData = [
                'firstName' => $user->getFirstName(),
                'lastName' => $user->getLastName(),
                'email' => $user->getEmail(),
                'authWithGoogle' => false,
            ];
        }

        return $this->render('settings/index.html.twig', [
            'userData' => $userData,
            'projects' => $projects,
        ]);
    }

}
