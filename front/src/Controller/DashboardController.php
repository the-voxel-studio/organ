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
class DashboardController extends AbstractController
{
    #[Route('/dashboard', name: 'app_dashboard')]
    public function index(
        #[Target('api.client')] HttpClientInterface $apiClient, 
        RequestStack $requestStack
    ): Response
    {
        $projects = [];
        $tasks = [];
        
        try {
            $request = $requestStack->getCurrentRequest();
            $bearer = $request?->cookies->get('BEARER');

            $response = $apiClient->request('GET', '/api/dashboard', [
                'headers' => [
                    'Cookie' => 'BEARER=' . $bearer
                ]
            ]);
            
            if ($response->getStatusCode() === 200) {
                $data = $response->toArray();
                $projects = $data['projects'] ?? [];
                $tasks = $data['tasks'] ?? [];

                foreach ($projects as &$project) {
                    $identity = $this->extractIdentity($project);
                    $project['color'] = $identity['color'];
                    $project['iconName'] = $identity['iconName'];
                }
            }
        } catch (\Exception $e) {
            // Handle error silently
        }

        return $this->render('dashboard/index.html.twig', [
            'user' => $this->getUser(),
            'projects' => $projects,
            'tasks' => $tasks,
        ]);
    }

    private function extractIdentity(array $project): array
    {
        $default = ['color' => '#FF7EB6', 'iconName' => 'icon_1'];
        if ($project['iconType'] === 'SVG' && isset($project['iconData']) && str_starts_with($project['iconData'], '{')) {
            $data = json_decode($project['iconData'], true);
            return [
                'color' => $data['color'] ?? $default['color'],
                'iconName' => $data['icon'] ?? $default['iconName']
            ];
        }
        return $default;
    }
}
