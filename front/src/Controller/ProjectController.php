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
class ProjectController extends AbstractController
{
    #[Route('/projects/new', name: 'app_project_new')]
    public function new(#[Target('api.client')] HttpClientInterface $apiClient, RequestStack $requestStack): Response
    {
        return $this->render('project/create.html.twig', [
            'projects' => $this->getSidebarProjects($apiClient, $requestStack)
        ]);
    }

    #[Route('/projects/{uuid}', name: 'app_project_show')]
    public function show(
        string $uuid,
        #[Target('api.client')] HttpClientInterface $apiClient,
        RequestStack $requestStack
    ): Response {
        $projectData = [];
        try {
            $request = $requestStack->getCurrentRequest();
            $bearer = $request?->cookies->get('BEARER');

            $response = $apiClient->request('GET', "/api/projects/$uuid/detailed", [
                'headers' => [
                    'Cookie' => 'BEARER=' . $bearer
                ]
            ]);

            if ($response->getStatusCode() === 200) {
                $projectData = $response->toArray();
                if (isset($projectData['project'])) {
                    $identity = $this->extractIdentity($projectData['project']);
                    $projectData['project']['color'] = $identity['color'];
                    $projectData['project']['iconName'] = $identity['iconName'];
                }
            } else {
                throw new \Exception('Project not found');
            }
        } catch (\Exception $e) {
            return $this->redirectToRoute('app_dashboard');
        }

        return $this->render('project/show.html.twig', [
            'data' => $projectData,
            'projects' => $this->getSidebarProjects($apiClient, $requestStack)
        ]);
    }

    #[Route('/projects/{uuid}/settings', name: 'app_project_edit')]
    public function edit(
        string $uuid,
        #[Target('api.client')] HttpClientInterface $apiClient,
        RequestStack $requestStack
    ): Response {
        try {
            $request = $requestStack->getCurrentRequest();
            $bearer = $request?->cookies->get('BEARER');

            $response = $apiClient->request('GET', "/api/projects/$uuid/detailed", [
                'headers' => [
                    'Cookie' => 'BEARER=' . $bearer
                ]
            ]);

            if ($response->getStatusCode() !== 200) {
                throw new \Exception('Project not found');
            }

            $projectData = $response->toArray();

            // SECURITY CHECK: Only ADMIN can edit project settings
            if (($projectData['project']['role'] ?? '') !== 'ADMIN') {
                return $this->redirectToRoute('app_project_show', ['uuid' => $uuid]);
            }
            
            if (isset($projectData['project'])) {
                $identity = $this->extractIdentity($projectData['project']);
                $projectData['project']['color'] = $identity['color'];
                $projectData['project']['iconName'] = $identity['iconName'];
            }

            return $this->render('project/create.html.twig', [
                'project' => $projectData['project'],
                'members' => $projectData['members'] ?? [],
                'projects' => $this->getSidebarProjects($apiClient, $requestStack)
            ]);
        } catch (\Exception $e) {
            return $this->redirectToRoute('app_dashboard');
        }
    }

    private function getSidebarProjects(HttpClientInterface $apiClient, RequestStack $requestStack): array
    {
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
                foreach ($projects as &$p) {
                    $identity = $this->extractIdentity($p);
                    $p['color'] = $identity['color'];
                    $p['iconName'] = $identity['iconName'];
                }
                return $projects;
            }
        } catch (\Exception $e) {}
        return [];
    }

    private function extractIdentity(array $project): array
    {
        $default = ['color' => '#FF7EB6', 'iconName' => 'icon_1'];
        if (isset($project['iconType']) && $project['iconType'] === 'SVG' && isset($project['iconData']) && str_starts_with($project['iconData'], '{')) {
            $data = json_decode($project['iconData'], true);
            return [
                'color' => $data['color'] ?? $default['color'],
                'iconName' => $data['icon'] ?? $default['iconName']
            ];
        }
        return $default;
    }
}
