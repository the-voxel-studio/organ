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
    use ProjectIdentityTrait;

    #[Route('/projects/new', name: 'app_project_new')]
    public function new(#[Target('api.client')] HttpClientInterface $apiClient, RequestStack $requestStack): Response
    {
        return $this->render('project/create.html.twig', [
            'projects' => $this->getSidebarProjects($apiClient, $requestStack)
        ]);
    }

    #[Route('/trash', name: 'app_project_trash')]
    public function trash(#[Target('api.client')] HttpClientInterface $apiClient, RequestStack $requestStack): Response
    {
        $trashedProjects = [];
        try {
            $request = $requestStack->getCurrentRequest();
            $bearer = $request?->cookies->get('BEARER');

            $response = $apiClient->request('GET', '/api/projects/trash', [
                'headers' => [
                    'Cookie' => 'BEARER=' . $bearer
                ]
            ]);

            if ($response->getStatusCode() === 200) {
                $trashedProjects = $response->toArray();
                foreach ($trashedProjects as &$p) {
                    $identity = $this->extractIdentity($p);
                    $p['color'] = $identity['color'];
                    $p['iconName'] = $identity['iconName'];
                }
            }
        } catch (\Exception $e) {}

        return $this->render('project/trash.html.twig', [
            'trashedProjects' => $trashedProjects,
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

            // Fetch pending invitations
            $invitations = [];
            $responseInv = $apiClient->request('GET', "/api/projects/$uuid/members/invitations", [
                'headers' => [
                    'Cookie' => 'BEARER=' . $bearer
                ]
            ]);
            if ($responseInv->getStatusCode() === 200) {
                $invitations = $responseInv->toArray();
            }

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
                'invitations' => $invitations,
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
}
