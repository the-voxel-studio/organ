<?php

declare(strict_types=1);

namespace App\Tests\Service;

use App\Enum\ProjectGlobalRole;
use App\Factory\OrganFactory;
use App\Factory\OrganRoleFactory;
use App\Factory\PermissionFactory;
use App\Factory\ProjectFactory;
use App\Factory\ProjectMemberFactory;
use App\Factory\UserFactory;
use App\Factory\UserOrganRoleFactory;
use App\Service\ChatbotContextBuilder;
use App\Tests\ApiTestCase;

/**
 * Le contexte injecté dans le prompt de l'assistant IA doit respecter les mêmes
 * règles d'accès que l'API REST : la route est fournie par le client et ne doit
 * pas permettre de lire les données d'un projet ou d'un Organ inaccessible.
 */
class ChatbotContextBuilderTest extends ApiTestCase
{
    private function builder(): ChatbotContextBuilder
    {
        return static::getContainer()->get(ChatbotContextBuilder::class);
    }

    public function testNonMemberGetsNoContextForForeignProjectRoute(): void
    {
        $outsider = UserFactory::createOne();
        $owner = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $owner, 'project' => $project, 'globalRole' => ProjectGlobalRole::ADMIN]);
        OrganFactory::createOne(['project' => $project]);

        $context = $this->builder()->buildProjectContext($outsider, '/project/' . $project->getUuid());

        $this->assertSame([], $context['organs']);
        $this->assertSame([], $context['members']);
    }

    public function testNonMemberGetsNoContextForForeignOrganRoute(): void
    {
        $outsider = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        $organ = OrganFactory::createOne(['project' => $project]);

        $context = $this->builder()->buildProjectContext(
            $outsider,
            sprintf('/organ/%s/%s', $project->getUuid(), $organ->getUuid())
        );

        $this->assertSame([], $context['organs']);
        $this->assertSame([], $context['members']);
    }

    public function testPlainMemberOnlySeesOrgansWithViewPermissionAndNoRoles(): void
    {
        $member = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $member, 'project' => $project, 'globalRole' => ProjectGlobalRole::MEMBER]);

        $visibleOrgan = OrganFactory::createOne(['project' => $project, 'title' => 'Visible']);
        $hiddenOrgan = OrganFactory::createOne(['project' => $project, 'title' => 'Hidden']);

        $role = OrganRoleFactory::createOne([
            'organ' => $visibleOrgan,
            'permissions' => [PermissionFactory::createOne(['name' => 'ORGAN_VIEW'])],
        ]);
        UserOrganRoleFactory::createOne(['user' => $member, 'role' => $role]);
        OrganRoleFactory::createOne(['organ' => $hiddenOrgan]);

        $context = $this->builder()->buildProjectContext($member, '/project/' . $project->getUuid());

        $this->assertCount(1, $context['organs']);
        $this->assertSame($visibleOrgan->getUuid(), $context['organs'][0]['uuid']);
        // Sans ORGAN_MANAGE_ROLES, les rôles ne sont pas exposés
        $this->assertSame([], $context['organs'][0]['roles']);
        $this->assertCount(1, $context['members']);
    }

    public function testManagerSeesAllOrgansAndTheirRoles(): void
    {
        $manager = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $manager, 'project' => $project, 'globalRole' => ProjectGlobalRole::MANAGER]);

        $organA = OrganFactory::createOne(['project' => $project]);
        OrganFactory::createOne(['project' => $project]);
        OrganRoleFactory::createOne(['organ' => $organA]);

        $context = $this->builder()->buildProjectContext($manager, '/project/' . $project->getUuid());

        $this->assertCount(2, $context['organs']);
        $rolesByOrgan = array_column($context['organs'], 'roles', 'uuid');
        $this->assertCount(1, $rolesByOrgan[$organA->getUuid()]);
    }

    public function testRouteWithoutProjectGivesEmptyContext(): void
    {
        $user = UserFactory::createOne();

        $context = $this->builder()->buildProjectContext($user, '/dashboard');

        $this->assertSame(['organs' => [], 'members' => []], $context);
    }
}
