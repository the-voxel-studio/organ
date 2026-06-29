<?php

declare(strict_types=1);

namespace App\Tests\Controller;

use App\Enum\IconType;
use App\Enum\ProjectGlobalRole;
use App\Factory\OrganFactory;
use App\Factory\ProjectFactory;
use App\Factory\ProjectMemberFactory;
use App\Factory\UserFactory;
use App\Tests\ApiTestCase;

class OrganControllerTest extends ApiTestCase
{
    public function testGetOrgansIndexSuccess(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        ProjectMemberFactory::createOne([
            'user' => $user, 
            'project' => $project,
            'globalRole' => ProjectGlobalRole::MANAGER
        ]);
        
        // Créer 2 organes pour ce projet
        OrganFactory::createMany(2, ['project' => $project]);

        $this->login($client, $user);
        $client->request('GET', sprintf('/api/projects/%s/organs', $project->getUuid()));

        $this->assertResponseIsSuccessful();
        $data = $this->getResponseContent($client);
        
        $this->assertCount(2, $data);
        $this->assertArrayHasKey('uuid', $data[0]);
    }

    public function testCreateOrganSuccess(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        ProjectMemberFactory::createOne([
            'user' => $user, 
            'project' => $project,
            'globalRole' => ProjectGlobalRole::ADMIN
        ]);

        $this->login($client, $user);
        $client->request('POST', sprintf('/api/projects/%s/organs', $project->getUuid()), [], [], [], json_encode([
            'title' => 'New Organ',
            'iconType' => IconType::EMOJI->value,
            'iconData' => '🎻',
            'highlightColor' => '#FF0000'
        ]));

        $this->assertResponseStatusCodeSame(201);
        $data = $this->getResponseContent($client);
        
        $this->assertEquals('New Organ', $data['title']);
    }

    public function testUpdateOrganSuccess(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        ProjectMemberFactory::createOne([
            'user' => $user, 
            'project' => $project,
            'globalRole' => ProjectGlobalRole::ADMIN
        ]);
        
        $organ = OrganFactory::createOne(['project' => $project, 'title' => 'Old Organ']);

        $this->login($client, $user);
        $client->request('PUT', sprintf('/api/projects/%s/organs/%s', $project->getUuid(), $organ->getUuid()), [], [], [], json_encode([
            'title' => 'Updated Organ'
        ]));

        $this->assertResponseIsSuccessful();
        $data = $this->getResponseContent($client);
        
        $this->assertEquals('Organ updated', $data['message']);
    }

    public function testDeleteOrganSuccess(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        ProjectMemberFactory::createOne([
            'user' => $user, 
            'project' => $project,
            'globalRole' => ProjectGlobalRole::ADMIN
        ]);
        
        $organ = OrganFactory::createOne(['project' => $project]);

        $this->login($client, $user);
        $client->request('DELETE', sprintf('/api/projects/%s/organs/%s', $project->getUuid(), $organ->getUuid()));

        $this->assertResponseStatusCodeSame(204);
    }

    public function testGetOrgansForbiddenForNonMember(): void
    {
        $client = static::createClient();
        $userA = UserFactory::createOne();
        $userB = UserFactory::createOne();
        $projectOfA = ProjectFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $userA, 'project' => $projectOfA]);

        $this->login($client, $userB);
        $client->request('GET', sprintf('/api/projects/%s/organs', $projectOfA->getUuid()));

        $this->assertResponseStatusCodeSame(403);
    }

    public function testCheckPermissionSqlSuccess(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        ProjectMemberFactory::createOne([
            'user' => $user, 
            'project' => $project,
            'globalRole' => ProjectGlobalRole::ADMIN
        ]);
        $organ = OrganFactory::createOne(['project' => $project]);

        $this->login($client, $user);
        
        // ADMIN should have any permission
        $client->request('GET', sprintf('/api/projects/%s/organs/%s/check-permission/SOME_PERM', $project->getUuid(), $organ->getUuid()));
        
        $this->assertResponseIsSuccessful();
        $data = $this->getResponseContent($client);
        $this->assertTrue($data['allowed']);

        // Test with a specific role
        $userMember = UserFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $userMember, 'project' => $project, 'globalRole' => ProjectGlobalRole::MEMBER]);
        
        $role = \App\Factory\OrganRoleFactory::createOne(['organ' => $organ, 'name' => 'Tester']);
        $permission = \App\Entity\Permission::class; // Assuming entity exists
        // We need to find or create a permission entity
        // I'll assume the permissions are already inserted in init.sql
        $em = static::getContainer()->get('doctrine')->getManager();
        $permEntity = $em->getRepository(\App\Entity\Permission::class)->findOneBy(['name' => 'TASK_CREATE']);
        if (!$permEntity) {
            $permEntity = new \App\Entity\Permission();
            $permEntity->setName('TASK_CREATE');
            $em->persist($permEntity);
        }
        $role->addPermission($permEntity);
        $em->flush();

        \App\Factory\UserOrganRoleFactory::createOne(['user' => $userMember, 'role' => $role]);

        $this->login($client, $userMember);
        $client->request('GET', sprintf('/api/projects/%s/organs/%s/check-permission/TASK_CREATE', $project->getUuid(), $organ->getUuid()));
        
        $this->assertResponseIsSuccessful();
        $this->assertTrue($this->getResponseContent($client)['allowed']);

        // Wrong permission
        $client->request('GET', sprintf('/api/projects/%s/organs/%s/check-permission/UNKNOWN', $project->getUuid(), $organ->getUuid()));
        $this->assertResponseIsSuccessful();
        $this->assertFalse($this->getResponseContent($client)['allowed']);
    }

    public function testGetReadyTasksSqlSuccess(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        ProjectMemberFactory::createOne([
            'user' => $user, 
            'project' => $project,
            'globalRole' => ProjectGlobalRole::ADMIN
        ]);
        $organ = OrganFactory::createOne(['project' => $project]);

        // Task A is ready (no dependencies)
        $taskA = \App\Factory\TaskFactory::createOne(['organ' => $organ, 'status' => \App\Enum\TaskStatus::TODO, 'title' => 'Ready Task']);
        
        // Task B depends on Task C (not DONE) -> not ready
        $taskB = \App\Factory\TaskFactory::createOne(['organ' => $organ, 'status' => \App\Enum\TaskStatus::TODO, 'title' => 'Blocked Task']);
        $taskC = \App\Factory\TaskFactory::createOne(['organ' => $organ, 'status' => \App\Enum\TaskStatus::IN_PROGRESS]);
        \App\Factory\TaskDependencyFactory::createOne(['task' => $taskB, 'dependsOnTask' => $taskC]);

        // Task D depends on Task E (DONE) -> ready
        $taskD = \App\Factory\TaskFactory::createOne(['organ' => $organ, 'status' => \App\Enum\TaskStatus::TODO, 'title' => 'Ready Dependency Task']);
        $taskE = \App\Factory\TaskFactory::createOne(['organ' => $organ, 'status' => \App\Enum\TaskStatus::DONE]);
        \App\Factory\TaskDependencyFactory::createOne(['task' => $taskD, 'dependsOnTask' => $taskE]);

        $this->login($client, $user);
        $client->request('GET', sprintf('/api/projects/%s/organs/%s/ready-tasks', $project->getUuid(), $organ->getUuid()));

        $this->assertResponseIsSuccessful();
        $data = $this->getResponseContent($client);
        
        $this->assertCount(2, $data);
        $titles = array_column($data, 'title');
        $this->assertContains('Ready Task', $titles);
        $this->assertContains('Ready Dependency Task', $titles);
        $this->assertNotContains('Blocked Task', $titles);
    }
}
