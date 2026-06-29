<?php

declare(strict_types=1);

namespace App\Tests\Controller;

use App\Enum\IconType;
use App\Enum\ProjectGlobalRole;
use App\Factory\OrganFactory;
use App\Factory\OrganRoleFactory;
use App\Factory\PermissionFactory;
use App\Factory\ProjectFactory;
use App\Factory\ProjectMemberFactory;
use App\Factory\UserFactory;
use App\Tests\ApiTestCase;

class OrganRoleControllerTest extends ApiTestCase
{
    public function testCreateOrganRoleWithIcons(): void
    {
        $client = static::createClient();
        
        $project = ProjectFactory::createOne();
        $organ = OrganFactory::createOne(['project' => $project]);
        
        $admin = UserFactory::createOne();
        ProjectMemberFactory::createOne([
            'user' => $admin, 
            'project' => $project, 
            'globalRole' => ProjectGlobalRole::ADMIN
        ]);

        $this->login($client, $admin);

        $client->request('POST', sprintf('/api/projects/%s/organs/%s/roles', $project->getUuid(), $organ->getUuid()), [], [], [], json_encode([
            'name' => 'Iconic Role',
            'iconType' => 'SVG',
            'iconData' => '<svg>...</svg>'
        ]));

        $this->assertResponseStatusCodeSame(201);
        
        // 2. Index check
        $client->request('GET', sprintf('/api/projects/%s/organs/%s/roles', $project->getUuid(), $organ->getUuid()));
        $this->assertResponseIsSuccessful();
        $data = json_decode($client->getResponse()->getContent(), true);
        
        $this->assertCount(1, $data);
        $this->assertEquals('Iconic Role', $data[0]['name']);
        $this->assertEquals('SVG', $data[0]['iconType']);
        $this->assertEquals('<svg>...</svg>', $data[0]['iconData']);
    }

    public function testUpdateOrganRoleIconsAndPermissions(): void
    {
        $client = static::createClient();
        
        $project = ProjectFactory::createOne();
        $organ = OrganFactory::createOne(['project' => $project]);
        $role = OrganRoleFactory::createOne(['organ' => $organ, 'name' => 'Old Name']);
        
        $admin = UserFactory::createOne();
        ProjectMemberFactory::createOne([
            'user' => $admin, 
            'project' => $project, 
            'globalRole' => ProjectGlobalRole::ADMIN
        ]);

        $permission = PermissionFactory::createOne(['name' => 'TASK_CREATE']);

        $this->login($client, $admin);

        $client->request('PATCH', sprintf('/api/projects/%s/organs/%s/roles/%s', $project->getUuid(), $organ->getUuid(), $role->getUuid()), [], [], [], json_encode([
            'name' => 'New Name',
            'iconType' => 'EMOJI',
            'iconData' => '🚀',
            'permissions' => ['TASK_CREATE']
        ]));

        $this->assertResponseIsSuccessful();
        $data = json_decode($client->getResponse()->getContent(), true);
        
        $this->assertEquals('New Name', $data['name']);
        $this->assertEquals('EMOJI', $data['iconType']);
        $this->assertEquals('🚀', $data['iconData']);

        // Check index again to verify cache invalidation
        $client->request('GET', sprintf('/api/projects/%s/organs/%s/roles', $project->getUuid(), $organ->getUuid()));
        $this->assertResponseIsSuccessful();
        $data = json_decode($client->getResponse()->getContent(), true);
        
        $this->assertEquals('New Name', $data[0]['name']);
        $this->assertEquals('EMOJI', $data[0]['iconType']);
        $this->assertEquals('🚀', $data[0]['iconData']);
        $this->assertContains('TASK_CREATE', $data[0]['permissions']);
    }
}
