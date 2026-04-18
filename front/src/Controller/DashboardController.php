<?php

namespace App\Controller;

use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Security\Http\Attribute\IsGranted;

#[IsGranted('ROLE_USER')]
class DashboardController extends AbstractController
{
    #[Route('/dashboard', name: 'app_dashboard')]
    public function index(\Symfony\Contracts\HttpClient\HttpClientInterface $apiClient, \Symfony\Component\HttpFoundation\RequestStack $requestStack): Response
    {
        $projects = [];
        try {
            $request = $requestStack->getCurrentRequest();
            $bearer = $request?->cookies->get('BEARER');

            $response = $apiClient->request('GET', '/api/projects', [
                'headers' => [
                    'Cookie' => 'BEARER=' . $bearer
                ]
            ]);
            
            if ($response->getStatusCode() === 200) {
                $projects = $response->toArray();
            }
        } catch (\Exception $e) {
            // Log error or handle silently
        }

        return $this->render('dashboard/index.html.twig', [
            'user' => $this->getUser(),
            'projects' => $projects,
        ]);
    }
}
