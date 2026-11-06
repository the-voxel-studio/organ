<?php

declare(strict_types=1);

namespace App\Service;

use App\Entity\Organ;
use App\Entity\OrganRole;
use App\Entity\Project;
use App\Entity\ProjectMember;
use App\Entity\User;
use App\Enum\ProjectGlobalRole;
use Doctrine\ORM\EntityManagerInterface;

/**
 * Construit le contexte "projet courant" injecté dans le prompt de l'assistant IA.
 *
 * La route est fournie par le client : elle n'est donc pas digne de confiance.
 * Ce service applique les mêmes règles d'accès que l'API REST, afin que le modèle
 * ne reçoive jamais une information que l'utilisateur n'aurait pas le droit de voir.
 */
class ChatbotContextBuilder
{
    public function __construct(
        private readonly EntityManagerInterface $entityManager,
        private readonly OrganPermissionService $permissionService
    ) {}

    /**
     * @return array{organs: list<array{uuid: string, title: string, roles: list<array{uuid: string, name: string}>}>, members: list<array{uuid: string, name: string, email: string}>}
     */
    public function buildProjectContext(User $user, string $currentRoute): array
    {
        $context = ['organs' => [], 'members' => []];

        $projectUuid = $this->extractProjectUuid($currentRoute);
        if ($projectUuid === null) {
            return $context;
        }

        $project = $this->entityManager->getRepository(Project::class)->findOneBy([
            'uuid' => $projectUuid,
            'deletedAt' => null,
        ]);
        if (!$project) {
            return $context;
        }

        // 1. L'utilisateur doit être membre du projet (même règle que ProjectMemberController / OrganController)
        $membership = $this->entityManager->getRepository(ProjectMember::class)->findOneBy([
            'project' => $project,
            'user' => $user,
            'deletedAt' => null,
        ]);
        if (!$membership) {
            return $context;
        }

        $isPlainMember = $membership->getGlobalRole() === ProjectGlobalRole::MEMBER;

        $organs = $this->entityManager->getRepository(Organ::class)->findBy([
            'project' => $project,
            'deletedAt' => null,
        ]);

        foreach ($organs as $organ) {
            // 2. Cloisonnement par Organ : même règle que OrganController::index
            if ($isPlainMember && !$this->permissionService->hasPermission($user, $organ, 'ORGAN_VIEW')) {
                continue;
            }

            // 3. Les rôles ne sont listés qu'à ceux qui peuvent les gérer (même règle que OrganRoleController::index)
            $organRoles = [];
            if ($this->permissionService->hasPermission($user, $organ, 'ORGAN_MANAGE_ROLES')) {
                $roles = $this->entityManager->getRepository(OrganRole::class)->findBy([
                    'organ' => $organ,
                    'deletedAt' => null,
                ]);
                foreach ($roles as $role) {
                    $organRoles[] = [
                        'uuid' => $role->getUuid(),
                        'name' => $role->getName(),
                    ];
                }
            }

            $context['organs'][] = [
                'uuid' => $organ->getUuid(),
                'title' => $organ->getTitle(),
                'roles' => $organRoles,
            ];
        }

        // 4. Membres du projet : visibles par tout membre du projet (même règle que ProjectMemberController::index)
        $projectMembers = $this->entityManager->getRepository(ProjectMember::class)->findBy([
            'project' => $project,
            'deletedAt' => null,
        ]);
        foreach ($projectMembers as $pm) {
            $member = $pm->getUser();
            if ($member) {
                $context['members'][] = [
                    'uuid' => $member->getUuid(),
                    'name' => $member->getFirstName() . ' ' . $member->getLastName(),
                    'email' => $member->getEmail(),
                ];
            }
        }

        return $context;
    }

    private function extractProjectUuid(string $currentRoute): ?string
    {
        if (preg_match('/\/project\/([a-fA-F0-9-]{36})/', $currentRoute, $matches)) {
            return $matches[1];
        }
        if (preg_match('/\/organ\/([a-fA-F0-9-]{36})\/([a-fA-F0-9-]{36})/', $currentRoute, $matches)) {
            return $matches[1];
        }

        return null;
    }
}
