<?php

declare(strict_types=1);

namespace App\Tests\Controller;

use App\Enum\ProjectGlobalRole;
use App\Factory\OrganFactory;
use App\Factory\ProjectFactory;
use App\Factory\ProjectMemberFactory;
use App\Factory\TaskFactory;
use App\Factory\UserFactory;
use App\Tests\ApiTestCase;

class MassAssignmentSecurityTest extends ApiTestCase
{
    /**
     * testUpdateProjectUuid: Attempt to change project UUID via PUT.
     */
    public function testUpdateProjectUuid(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $user, 'project' => $project, 'globalRole' => ProjectGlobalRole::ADMIN]);

        $this->login($client, $user);
        $originalUuid = $project->getUuid();
        $newUuid = '70857321-4d32-4e5a-8b8b-8b8b8b8b8b8b';

        $client->request('PUT', sprintf('/api/projects/%s', $originalUuid), [], [], [], json_encode([
            'uuid' => $newUuid,
            'title' => 'Changed Title'
        ]));

        $this->assertResponseIsSuccessful();
        $data = $this->getResponseContent($client);
        
        // Ensure UUID has NOT changed in the response
        $this->assertEquals($originalUuid, $data['uuid']);

        // Ensure UUID has NOT changed in the database
        $em = static::getContainer()->get('doctrine')->getManager();
        $em->refresh($project);
        $this->assertEquals($originalUuid, $project->getUuid());
    }

    /**
     * testUpdateOrganUuid: Attempt to change organ UUID via PUT.
     */
    public function testUpdateOrganUuid(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        $organ = OrganFactory::createOne(['project' => $project]);
        ProjectMemberFactory::createOne(['user' => $user, 'project' => $project, 'globalRole' => ProjectGlobalRole::ADMIN]);

        $this->login($client, $user);
        $originalUuid = $organ->getUuid();
        $newUuid = '80857321-4d32-4e5a-8b8b-8b8b8b8b8b8b';

        $client->request('PUT', sprintf('/api/projects/%s/organs/%s', $project->getUuid(), $originalUuid), [], [], [], json_encode([
            'uuid' => $newUuid,
            'title' => 'Changed Organ Title'
        ]));

        $this->assertResponseIsSuccessful();
        
        $em = static::getContainer()->get('doctrine')->getManager();
        $em->refresh($organ);
        $this->assertEquals($originalUuid, $organ->getUuid());
    }

    /**
     * testUpdateTaskCriticalFields: Attempt to change task UUID, createdAt, deletedAt via PUT.
     */
    public function testUpdateTaskCriticalFields(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        $organ = OrganFactory::createOne(['project' => $project]);
        $task = TaskFactory::createOne(['organ' => $organ, 'manager' => $user]);
        ProjectMemberFactory::createOne(['user' => $user, 'project' => $project, 'globalRole' => ProjectGlobalRole::ADMIN]);

        $this->login($client, $user);
        $originalUuid = $task->getUuid();
        $originalCreatedAt = $task->getCreatedAt();
        
        $newUuid = '90857321-4d32-4e5a-8b8b-8b8b8b8b8b8b';
        $newDate = '2000-01-01T00:00:00Z';

        $client->request('PUT', sprintf('/api/projects/%s/organs/%s/tasks/%s', $project->getUuid(), $organ->getUuid(), $originalUuid), [], [], [], json_encode([
            'uuid' => $newUuid,
            'createdAt' => $newDate,
            'deletedAt' => $newDate,
            'title' => 'Changed Task Title'
        ]));

        $this->assertResponseIsSuccessful();
        
        $em = static::getContainer()->get('doctrine')->getManager();
        $em->refresh($task);
        $this->assertEquals($originalUuid, $task->getUuid());
        $this->assertEquals($originalCreatedAt->format('Y-m-d H:i:s'), $task->getCreatedAt()->format('Y-m-d H:i:s'));
        $this->assertNull($task->getDeletedAt());
    }

    /**
     * testSelfPromotion: A MEMBER tries to promote themselves to ADMIN via PUT /api/projects/{uuid}/members/{memberUuid}.
     */
    public function testSelfPromotion(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        $member = ProjectMemberFactory::createOne(['user' => $user, 'project' => $project, 'globalRole' => ProjectGlobalRole::MEMBER]);

        $this->login($client, $user);

        $client->request('PUT', sprintf('/api/projects/%s/members/%s', $project->getUuid(), $member->getUuid()), [], [], [], json_encode([
            'role' => 'ADMIN'
        ]));

        $this->assertResponseStatusCodeSame(403);
        
        $em = static::getContainer()->get('doctrine')->getManager();
        $em->refresh($member);
        $this->assertEquals(ProjectGlobalRole::MEMBER, $member->getGlobalRole());
    }

    /**
     * testInviteWithAdminRole: A MANAGER tries to invite someone with ADMIN role.
     */
    public function testInviteWithAdminRole(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $user, 'project' => $project, 'globalRole' => ProjectGlobalRole::MANAGER]);

        $this->login($client, $user);

        $client->request('POST', sprintf('/api/projects/%s/members/invite', $project->getUuid()), [], [], [], json_encode([
            'email' => 'newadmin@example.com',
            'role' => 'ADMIN'
        ]));

        // Based on ProjectMemberController::invite, Managers can only invite members with MEMBER role.
        $this->assertResponseStatusCodeSame(403);
    }
}
