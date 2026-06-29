<?php

declare(strict_types=1);

namespace App\Tests\Controller;

use App\Entity\Permission;
use App\Enum\ProjectGlobalRole;
use App\Factory\OrganFactory;
use App\Factory\OrganRoleFactory;
use App\Factory\PermissionFactory;
use App\Factory\ProjectFactory;
use App\Factory\ProjectMemberFactory;
use App\Factory\UserFactory;
use App\Tests\ApiTestCase;

class CacheInvalidationTest extends ApiTestCase
{
    public function testPermissionUpdateIsImmediate(): void
    {
        $client = static::createClient();
        $project = ProjectFactory::createOne();
        $organ = OrganFactory::createOne(['project' => $project]);
        $user = UserFactory::createOne();
        
        ProjectMemberFactory::createOne(['user' => $user, 'project' => $project, 'globalRole' => ProjectGlobalRole::MEMBER]);
        
        // 1. Initially user has no roles and cannot see organ
        $this->login($client, $user);
        $client->request('GET', sprintf('/api/projects/%s/organs/%s', $project->getUuid(), $organ->getUuid()));
        $this->assertResponseStatusCodeSame(403);

        // 2. Admin assigns a role with ORGAN_VIEW to the user via API
        $admin = UserFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $admin, 'project' => $project, 'globalRole' => ProjectGlobalRole::ADMIN]);
        
        $permView = PermissionFactory::createOne(['name' => 'ORGAN_VIEW']);
        $role = OrganRoleFactory::createOne(['organ' => $organ, 'name' => 'Viewer']);
        $role->addPermission($permView);
        $em = static::getContainer()->get('doctrine')->getManager();
        $em->persist($role);
        $em->flush();

        $this->login($client, $admin);
        $client->request('POST', sprintf('/api/projects/%s/organs/%s/roles/%s/assign', $project->getUuid(), $organ->getUuid(), $role->getUuid()), [], [], [], json_encode([
            'userUuid' => $user->getUuid()
        ]));
        $this->assertResponseIsSuccessful();

        // 3. User should now have access immediately (Cache must be invalidated)
        $this->login($client, $user);
        $client->request('GET', sprintf('/api/projects/%s/organs/%s', $project->getUuid(), $organ->getUuid()));
        $this->assertResponseIsSuccessful();
    }
}
