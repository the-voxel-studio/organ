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
class OrganController extends AbstractController
{
    use ProjectIdentityTrait;

    #[Route('/projects/{projectUuid}/organs/new', name: 'app_organ_new')]
    public function new(
        string $projectUuid,
        #[Target('api.client')] HttpClientInterface $apiClient,
        RequestStack $requestStack
    ): Response {
        return $this->handleCreateOrEdit($projectUuid, null, $apiClient, $requestStack);
    }

    #[Route('/projects/{projectUuid}/organs/{organUuid}/edit', name: 'app_organ_edit')]
    public function edit(
        string $projectUuid,
        string $organUuid,
        #[Target('api.client')] HttpClientInterface $apiClient,
        RequestStack $requestStack
    ): Response {
        return $this->handleCreateOrEdit($projectUuid, $organUuid, $apiClient, $requestStack);
    }

    private function handleCreateOrEdit(
        string $projectUuid,
        ?string $organUuid,
        HttpClientInterface $apiClient,
        RequestStack $requestStack
    ): Response {
        $projectData = [];
        $organData = null;
        $availablePermissions = [];
        $userPermissions = [];
        try {
            $request = $requestStack->getCurrentRequest();
            $bearer = $request?->cookies->get('BEARER');

            $response = $apiClient->request('GET', "/api/projects/$projectUuid/detailed", [
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

                    // Grant 'ALL' permission to project admins and managers for organ setup
                    $projectRole = $projectData['project']['role'] ?? 'MEMBER';
                    if (in_array($projectRole, ['ADMIN', 'MANAGER'], true)) {
                        $userPermissions[] = 'ALL';
                    }
                }
            } else {
                return $this->redirectToRoute('app_dashboard');
            }

            if ($organUuid) {
                // Fetch User specific permissions for this organ
                $response = $apiClient->request('GET', "/api/projects/$projectUuid/organs/$organUuid/permissions", [
                    'headers' => [
                        'Cookie' => 'BEARER=' . $bearer
                    ]
                ]);
                if ($response->getStatusCode() === 200) {
                    $permsData = $response->toArray();
                    $userPermissions = $permsData['permissions'] ?? [];
                    
                    // Access Control: Must have ORGAN_EDIT or ORGAN_MANAGE_ROLES to be here
                    if (!in_array('ORGAN_EDIT', $userPermissions) && !in_array('ORGAN_MANAGE_ROLES', $userPermissions) && !in_array('ALL', $userPermissions)) {
                        $this->addFlash('error', 'Vous n\'avez pas l\'autorisation de modifier cet organ.');
                        return $this->redirectToRoute('app_organ_show', ['projectUuid' => $projectUuid, 'organUuid' => $organUuid]);
                    }
                }

                // Fetch Organ Data
                $response = $apiClient->request('GET', "/api/projects/$projectUuid/organs/$organUuid", [
                    'headers' => [
                        'Cookie' => 'BEARER=' . $bearer
                    ]
                ]);
                if ($response->getStatusCode() === 200) {
                    $organData = $response->toArray();

                    // Fetch Roles for this organ (includes members and permissions)
                    $response = $apiClient->request('GET', "/api/projects/$projectUuid/organs/$organUuid/roles", [
                        'headers' => [
                            'Cookie' => 'BEARER=' . $bearer
                        ]
                    ]);
                    if ($response->getStatusCode() === 200) {
                        $organData['roles'] = $response->toArray();
                    }
                } else {
                    $this->addFlash('error', 'Vous n\'avez pas accès à cet organ ou il n\'existe pas.');
                    return $this->redirectToRoute('app_project_show', ['uuid' => $projectUuid]);
                }
            }

            // Fetch available permissions
            $response = $apiClient->request('GET', "/api/permissions/available", [
                'headers' => [
                    'Cookie' => 'BEARER=' . $bearer
                ]
            ]);
            if ($response->getStatusCode() === 200) {
                $availablePermissions = $response->toArray();
            }
        } catch (\Exception $e) {
            return $this->redirectToRoute('app_dashboard');
        }

        return $this->render('organ/create.html.twig', [
            'project' => $projectData['project'] ?? [],
            'members' => $projectData['members'] ?? [],
            'organ' => $organData,
            'availablePermissions' => $availablePermissions,
            'userPermissions' => $userPermissions,
            'projects' => $this->getSidebarProjects($apiClient, $requestStack)
        ]);
    }

    #[Route('/projects/{projectUuid}/organs/{organUuid}/trash', name: 'app_organ_trash')]
    public function trash(
        string $projectUuid,
        string $organUuid,
        #[Target('api.client')] HttpClientInterface $apiClient,
        RequestStack $requestStack
    ): Response {
        $projectData = [];
        $organData = [];
        $trashedTasks = [];
        $trashedRoles = [];
        $trashedMembers = [];
        $userPermissions = [];

        try {
            $request = $requestStack->getCurrentRequest();
            $bearer = $request?->cookies->get('BEARER');

            // 1. Fetch User Permissions
            $response = $apiClient->request('GET', "/api/projects/$projectUuid/organs/$organUuid/permissions", [
                'headers' => ['Cookie' => 'BEARER=' . $bearer]
            ]);
            if ($response->getStatusCode() === 200) {
                $userPermissions = $response->toArray()['permissions'] ?? [];
            }

            // Define specific access rights
            $canManageTasks = in_array('TASK_DELETE_OWN', $userPermissions) || in_array('TASK_DELETE_ALL', $userPermissions) || in_array('ORGAN_EDIT', $userPermissions) || in_array('ALL', $userPermissions);
            $canManageRoles = in_array('ORGAN_MANAGE_ROLES', $userPermissions) || in_array('ORGAN_EDIT', $userPermissions) || in_array('ALL', $userPermissions);

            // Access Control: Redirection if no management permissions at all
            if (!$canManageTasks && !$canManageRoles) {
                $this->addFlash('error', 'Vous n\'avez pas l\'autorisation d\'accéder à la corbeille.');
                return $this->redirectToRoute('app_organ_show', ['projectUuid' => $projectUuid, 'organUuid' => $organUuid]);
            }

            // 2. Fetch Project & Organ Data
            $response = $apiClient->request('GET', "/api/projects/$projectUuid/detailed", [
                'headers' => ['Cookie' => 'BEARER=' . $bearer]
            ]);
            if ($response->getStatusCode() === 200) {
                $projectData = $response->toArray();
                if (isset($projectData['project'])) {
                    $identity = $this->extractIdentity($projectData['project']);
                    $projectData['project']['color'] = $identity['color'];
                    $projectData['project']['iconName'] = $identity['iconName'];

                    // Grant 'ALL' permission to project admins and managers
                    $projectRole = $projectData['project']['role'] ?? 'MEMBER';
                    if (in_array($projectRole, ['ADMIN', 'MANAGER'], true)) {
                        $permissions[] = 'ALL';
                    }
                }
            }

            // Fetch Organ Data
            $response = $apiClient->request('GET', "/api/projects/$projectUuid/organs/$organUuid", [
                'headers' => ['Cookie' => 'BEARER=' . $bearer]
            ]);
            if ($response->getStatusCode() === 200) {
                $organData = $response->toArray();
            }

            // 3. Fetch Trashed Items based on granular permissions
            if ($canManageTasks) {
                $response = $apiClient->request('GET', "/api/projects/$projectUuid/organs/$organUuid/tasks/trash", [
                    'headers' => ['Cookie' => 'BEARER=' . $bearer]
                ]);
                if ($response->getStatusCode() === 200) $trashedTasks = $response->toArray();
            }

            if ($canManageRoles) {
                $response = $apiClient->request('GET', "/api/projects/$projectUuid/organs/$organUuid/roles/trash", [
                    'headers' => ['Cookie' => 'BEARER=' . $bearer]
                ]);
                if ($response->getStatusCode() === 200) $trashedRoles = $response->toArray();

                $response = $apiClient->request('GET', "/api/projects/$projectUuid/organs/$organUuid/roles/members/trash", [
                    'headers' => ['Cookie' => 'BEARER=' . $bearer]
                ]);
                if ($response->getStatusCode() === 200) $trashedMembers = $response->toArray();
            }

        } catch (\Exception $e) {
            return $this->redirectToRoute('app_dashboard');
        }

        return $this->render('organ/trash.html.twig', [
            'project' => $projectData['project'] ?? [],
            'organ' => $organData,
            'trashedTasks' => $trashedTasks,
            'trashedRoles' => $trashedRoles,
            'trashedMembers' => $trashedMembers,
            'userPermissions' => $userPermissions,
            'projects' => $this->getSidebarProjects($apiClient, $requestStack)
        ]);
    }

    #[Route('/projects/{projectUuid}/organs/{organUuid}', name: 'app_organ_show')]
    public function show(
        string $projectUuid,
        string $organUuid,
        #[Target('api.client')] HttpClientInterface $apiClient,
        RequestStack $requestStack
    ): Response {
        $projectData = [];
        $organData = [];
        $permissions = [];
        try {
            $request = $requestStack->getCurrentRequest();
            $bearer = $request?->cookies->get('BEARER');

            // Fetch Project Data
            $response = $apiClient->request('GET', "/api/projects/$projectUuid/detailed", [
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
                    
                    // Grant 'ALL' permission to project admins and managers
                    $projectRole = $projectData['project']['role'] ?? 'MEMBER';
                    if (in_array($projectRole, ['ADMIN', 'MANAGER'], true)) {
                        $userPermissions[] = 'ALL';
                    }
                }
            } else {
                return $this->redirectToRoute('app_dashboard');
            }

            // Fetch Organ Data
            $response = $apiClient->request('GET', "/api/projects/$projectUuid/organs/$organUuid", [
                'headers' => [
                    'Cookie' => 'BEARER=' . $bearer
                ]
            ]);

            if ($response->getStatusCode() === 200) {
                $organData = $response->toArray();
            } elseif ($response->getStatusCode() === 403) {
                $this->addFlash('error', 'Vous n\'avez pas l\'autorisation d\'accéder à cet organ.');
                return $this->redirectToRoute('app_project_show', ['uuid' => $projectUuid]);
            } else {
                $this->addFlash('error', 'Organ introuvable.');
                return $this->redirectToRoute('app_project_show', ['uuid' => $projectUuid]);
            }

            // Fetch Permissions
            $response = $apiClient->request('GET', "/api/projects/$projectUuid/organs/$organUuid/permissions", [
                'headers' => [
                    'Cookie' => 'BEARER=' . $bearer
                ]
            ]);

            if ($response->getStatusCode() === 200) {
                $permissionsData = $response->toArray();
                $permissions = $permissionsData['permissions'] ?? [];
            }

        } catch (\Exception $e) {
            return $this->redirectToRoute('app_dashboard');
        }

        return $this->render('organ/show.html.twig', [
            'project' => $projectData['project'] ?? [],
            'organ' => $organData,
            'permissions' => $permissions,
            'projects' => $this->getSidebarProjects($apiClient, $requestStack)
        ]);
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
