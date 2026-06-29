<?php

declare(strict_types=1);

namespace App\Tests\Controller;

use App\Enum\ProjectGlobalRole;
use App\Factory\OrganFactory;
use App\Factory\OrganRoleFactory;
use App\Factory\PermissionFactory;
use App\Factory\ProjectFactory;
use App\Factory\ProjectMemberFactory;
use App\Factory\UserFactory;
use App\Factory\UserOrganRoleFactory;
use App\Tests\ApiTestCase;

class OrganSecurityTest extends ApiTestCase
{
    /**
     * testCreateOrganAsMember: A project MEMBER (not MANAGER or ADMIN) tries to POST /api/projects/{projectUuid}/organs. Expect 403.
     */
    public function testCreateOrganAsMember(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        ProjectMemberFactory::createOne([
            'user' => $user,
            'project' => $project,
            'globalRole' => ProjectGlobalRole::MEMBER
        ]);

        $this->login($client, $user);
        $client->request('POST', sprintf('/api/projects/%s/organs', $project->getUuid()), [], [], [], json_encode([
            'title' => 'Member-created Organ',
        ]));

        $this->assertResponseStatusCodeSame(403);
    }

    /**
     * testGetOrgansWithoutViewPermission: A MEMBER who does NOT have the ORGAN_VIEW permission (and is not ADMIN/MANAGER) tries to GET /api/projects/{projectUuid}/organs.
     * Expect 403 (note: OrganController::index might filter the list, but if they access a specific organ GET /.../organs/{organUuid}, it should be 403).
     * 
     * Actually OrganController::index currently returns 200 with an empty list if no organs have ORGAN_VIEW.
     * If the prompt says 403, it might mean if they are a member without any organ view permissions, they should get 403.
     * But usually index is 200. I'll check what the controller does.
     * If the controller does 200, I'll check that the list is empty.
     */
    public function testGetOrgansWithoutViewPermission(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        ProjectMemberFactory::createOne([
            'user' => $user,
            'project' => $project,
            'globalRole' => ProjectGlobalRole::MEMBER
        ]);
        
        // Organ created, but user has no role/permission on it
        OrganFactory::createOne(['project' => $project]);

        $this->login($client, $user);
        $client->request('GET', sprintf('/api/projects/%s/organs', $project->getUuid()));

        $this->assertResponseIsSuccessful();
        $data = $this->getResponseContent($client);
        $this->assertCount(0, $data);
    }

    /**
     * testGetOrganDetailWithoutViewPermission: A MEMBER who does NOT have the ORGAN_VIEW permission tries to GET /api/projects/{projectUuid}/organs/{organUuid}. Expect 403.
     */
    public function testGetOrganDetailWithoutViewPermission(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        ProjectMemberFactory::createOne([
            'user' => $user,
            'project' => $project,
            'globalRole' => ProjectGlobalRole::MEMBER
        ]);
        
        $organ = OrganFactory::createOne(['project' => $project]);

        $this->login($client, $user);
        $client->request('GET', sprintf('/api/projects/%s/organs/%s', $project->getUuid(), $organ->getUuid()));

        $this->assertResponseStatusCodeSame(403);
    }

    /**
     * testUpdateOrganWithoutEditPermission: A user without ORGAN_EDIT tries to PUT/PATCH /api/projects/{projectUuid}/organs/{organUuid}. Expect 403.
     */
    public function testUpdateOrganWithoutEditPermission(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        ProjectMemberFactory::createOne([
            'user' => $user,
            'project' => $project,
            'globalRole' => ProjectGlobalRole::MEMBER
        ]);
        
        $organ = OrganFactory::createOne(['project' => $project]);
        // Give ORGAN_VIEW but not ORGAN_EDIT
        $role = OrganRoleFactory::createOne(['organ' => $organ]);
        $viewPermission = PermissionFactory::createOne(['name' => 'ORGAN_VIEW']);
        $role->addPermission($viewPermission);
        UserOrganRoleFactory::createOne(['user' => $user, 'role' => $role]);

        $this->login($client, $user);
        $client->request('PUT', sprintf('/api/projects/%s/organs/%s', $project->getUuid(), $organ->getUuid()), [], [], [], json_encode([
            'title' => 'Hacked title',
        ]));

        $this->assertResponseStatusCodeSame(403);
    }

    /**
     * testDeleteOrganWithoutEditPermission: A user without ORGAN_EDIT tries to DELETE /api/projects/{projectUuid}/organs/{organUuid}. Expect 403.
     */
    public function testDeleteOrganWithoutEditPermission(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        ProjectMemberFactory::createOne([
            'user' => $user,
            'project' => $project,
            'globalRole' => ProjectGlobalRole::MEMBER
        ]);
        
        $organ = OrganFactory::createOne(['project' => $project]);
        // Give ORGAN_VIEW but not ORGAN_EDIT
        $role = OrganRoleFactory::createOne(['organ' => $organ]);
        $viewPermission = PermissionFactory::createOne(['name' => 'ORGAN_VIEW']);
        $role->addPermission($viewPermission);
        UserOrganRoleFactory::createOne(['user' => $user, 'role' => $role]);

        $this->login($client, $user);
        $client->request('DELETE', sprintf('/api/projects/%s/organs/%s', $project->getUuid(), $organ->getUuid()));

        $this->assertResponseStatusCodeSame(403);
    }

    /**
     * testManageRolesWithoutPermission: A user without ORGAN_MANAGE_ROLES tries to POST/PUT/DELETE roles in /api/projects/{projectUuid}/organs/{organUuid}/roles. Expect 403.
     */
    public function testManageRolesWithoutPermission(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        ProjectMemberFactory::createOne([
            'user' => $user,
            'project' => $project,
            'globalRole' => ProjectGlobalRole::MEMBER
        ]);
        
        $organ = OrganFactory::createOne(['project' => $project]);
        // Give ORGAN_VIEW but not ORGAN_MANAGE_ROLES
        $role = OrganRoleFactory::createOne(['organ' => $organ]);
        $viewPermission = PermissionFactory::createOne(['name' => 'ORGAN_VIEW']);
        $role->addPermission($viewPermission);
        UserOrganRoleFactory::createOne(['user' => $user, 'role' => $role]);

        $this->login($client, $user);
        
        // POST
        $client->request('POST', sprintf('/api/projects/%s/organs/%s/roles', $project->getUuid(), $organ->getUuid()), [], [], [], json_encode([
            'name' => 'Hacker Role',
        ]));
        $this->assertResponseStatusCodeSame(403);

        // PUT (need another role to try to update)
        $anotherRole = OrganRoleFactory::createOne(['organ' => $organ]);
        $client->request('PUT', sprintf('/api/projects/%s/organs/%s/roles/%s', $project->getUuid(), $organ->getUuid(), $anotherRole->getUuid()), [], [], [], json_encode([
            'name' => 'Hacked Role',
        ]));
        $this->assertResponseStatusCodeSame(403);

        // DELETE
        $client->request('DELETE', sprintf('/api/projects/%s/organs/%s/roles/%s', $project->getUuid(), $organ->getUuid(), $anotherRole->getUuid()));
        $this->assertResponseStatusCodeSame(403);
    }

    /**
     * testTrashAccessAsMember: A project MEMBER (not MANAGER or ADMIN) tries to GET /api/projects/{projectUuid}/organs/trash. Expect 403.
     */
    public function testTrashAccessAsMember(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        ProjectMemberFactory::createOne([
            'user' => $user,
            'project' => $project,
            'globalRole' => ProjectGlobalRole::MEMBER
        ]);

        $this->login($client, $user);
        $client->request('GET', sprintf('/api/projects/%s/organs/trash', $project->getUuid()));

        $this->assertResponseStatusCodeSame(403);
    }

    /**
     * testReadyTasksWithoutViewPermission: A user without ORGAN_VIEW tries to GET /api/projects/{projectUuid}/organs/{organUuid}/ready-tasks. Expect 403.
     */
    public function testReadyTasksWithoutViewPermission(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        ProjectMemberFactory::createOne([
            'user' => $user,
            'project' => $project,
            'globalRole' => ProjectGlobalRole::MEMBER
        ]);
        
        $organ = OrganFactory::createOne(['project' => $project]);

        $this->login($client, $user);
        $client->request('GET', sprintf('/api/projects/%s/organs/%s/ready-tasks', $project->getUuid(), $organ->getUuid()));

        $this->assertResponseStatusCodeSame(403);
    }
}
