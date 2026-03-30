<?php

declare(strict_types=1);

namespace App\Tests\Controller;

use App\Enum\ProjectGlobalRole;
use App\Factory\OrganFactory;
use App\Factory\PermissionFactory;
use App\Factory\ProjectFactory;
use App\Factory\ProjectMemberFactory;
use App\Factory\UserFactory;
use App\Tests\ApiTestCase;

class RoleManagementPermissionTest extends ApiTestCase
{
    public function testUserWithoutPermissionCannotManageRoles(): void
    {
        $client = static::createClient();
        
        $project = ProjectFactory::createOne();
        $organ = OrganFactory::createOne(['project' => $project]);
        
        $user = UserFactory::createOne();
        ProjectMemberFactory::createOne([
            'user' => $user, 
            'project' => $project, 
            'globalRole' => ProjectGlobalRole::MEMBER
        ]);

        $this->login($client, $user);
        
        // 1. Try to create a role
        $client->request('POST', sprintf('/api/projects/%s/organs/%s/roles', $project->getUuid(), $organ->getUuid()), [], [], [], json_encode([
            'name' => 'Should fail'
        ]));
        $this->assertResponseStatusCodeSame(403);
    }

    public function testProjectManagerCannotManageRolesByDefault(): void
    {
        $client = static::createClient();
        
        $project = ProjectFactory::createOne();
        $organ = OrganFactory::createOne(['project' => $project]);
        
        $manager = UserFactory::createOne();
        ProjectMemberFactory::createOne([
            'user' => $manager, 
            'project' => $project, 
            'globalRole' => ProjectGlobalRole::MANAGER
        ]);

        $this->login($client, $manager);
        
        // Even if he can VIEW the organ, he should not be able to manage roles
        $client->request('POST', sprintf('/api/projects/%s/organs/%s/roles', $project->getUuid(), $organ->getUuid()), [], [], [], json_encode([
            'name' => 'Manager attempt'
        ]));
        $this->assertResponseStatusCodeSame(403);
    }
}
