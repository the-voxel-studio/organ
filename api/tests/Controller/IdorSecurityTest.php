<?php

declare(strict_types=1);

namespace App\Tests\Controller;

use App\Enum\ProjectGlobalRole;
use App\Factory\OrganFactory;
use App\Factory\ProjectDriveConfigFactory;
use App\Factory\ProjectFactory;
use App\Factory\ProjectMemberFactory;
use App\Factory\TagFactory;
use App\Factory\TaskFactory;
use App\Factory\UserFactory;
use App\Tests\ApiTestCase;

class IdorSecurityTest extends ApiTestCase
{
    /**
     * testAccessOrganFromAnotherProject: User A (Project A) tries to access Organ B (Project B).
     */
    public function testAccessOrganFromAnotherProject(): void
    {
        $client = static::createClient();
        
        $projectA = ProjectFactory::createOne();
        $userA = UserFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $userA, 'project' => $projectA, 'globalRole' => ProjectGlobalRole::ADMIN]);

        $projectB = ProjectFactory::createOne();
        $organB = OrganFactory::createOne(['project' => $projectB]);

        $this->login($client, $userA);

        // Try to GET organ from Project B using Project A's context (if the API allowed it)
        $client->request('GET', sprintf('/api/projects/%s/organs/%s', $projectA->getUuid(), $organB->getUuid()));
        $this->assertResponseStatusCodeSame(404);

        // Try to GET organ from Project B using Project B's context
        $client->request('GET', sprintf('/api/projects/%s/organs/%s', $projectB->getUuid(), $organB->getUuid()));
        $this->assertResponseStatusCodeSame(403);
    }

    /**
     * testAccessTaskFromAnotherProject: User A (Project A) tries to access Task B (Project B).
     */
    public function testAccessTaskFromAnotherProject(): void
    {
        $client = static::createClient();
        
        $projectA = ProjectFactory::createOne();
        $userA = UserFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $userA, 'project' => $projectA, 'globalRole' => ProjectGlobalRole::ADMIN]);

        $projectB = ProjectFactory::createOne();
        $organB = OrganFactory::createOne(['project' => $projectB]);
        $taskB = TaskFactory::createOne(['organ' => $organB]);

        $this->login($client, $userA);

        // Try to GET task from Project B context
        $client->request('GET', sprintf('/api/projects/%s/organs/%s/tasks/%s', $projectB->getUuid(), $organB->getUuid(), $taskB->getUuid()));
        $this->assertResponseStatusCodeSame(403);

        // Try to GET task from Project A context (if the API allowed it)
        $client->request('GET', sprintf('/api/projects/%s/organs/%s/tasks/%s', $projectA->getUuid(), $organB->getUuid(), $taskB->getUuid()));
        // Depending on how routing/lookup is done, this might be 404 (Organ not in project)
        $this->assertResponseStatusCodeSame(404);
    }

    /**
     * testAccessTagFromAnotherProject: User A (Project A) tries to access Tag B (Project B).
     */
    public function testAccessTagFromAnotherProject(): void
    {
        $client = static::createClient();
        
        $projectA = ProjectFactory::createOne();
        $userA = UserFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $userA, 'project' => $projectA, 'globalRole' => ProjectGlobalRole::ADMIN]);

        $projectB = ProjectFactory::createOne();
        $tagB = TagFactory::createOne(['project' => $projectB]);

        $this->login($client, $userA);

        // 1. Access from another project context (Project B)
        $client->request('GET', sprintf('/api/projects/%s/tags', $projectB->getUuid()));
        $this->assertResponseStatusCodeSame(403);

        $client->request('PUT', sprintf('/api/projects/%s/tags/%s', $projectB->getUuid(), $tagB->getUuid()), [], [], [], json_encode(['name' => 'Hacked']));
        $this->assertResponseStatusCodeSame(403);

        $client->request('DELETE', sprintf('/api/projects/%s/tags/%s', $projectB->getUuid(), $tagB->getUuid()));
        $this->assertResponseStatusCodeSame(403);

        // 2. Access to another project resource using own project context
        $client->request('PUT', sprintf('/api/projects/%s/tags/%s', $projectA->getUuid(), $tagB->getUuid()), [], [], [], json_encode(['name' => 'Hacked']));
        $this->assertResponseStatusCodeSame(404);

        $client->request('DELETE', sprintf('/api/projects/%s/tags/%s', $projectA->getUuid(), $tagB->getUuid()));
        $this->assertResponseStatusCodeSame(404);
    }

    /**
     * testAccessProjectDriveConfigFromAnotherProject: User A (Project A) tries to access ProjectDriveConfig B (Project B).
     */
    public function testAccessProjectDriveConfigFromAnotherProject(): void
    {
        $client = static::createClient();
        
        $projectA = ProjectFactory::createOne();
        $userA = UserFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $userA, 'project' => $projectA, 'globalRole' => ProjectGlobalRole::ADMIN]);

        $projectB = ProjectFactory::createOne();
        ProjectDriveConfigFactory::createOne(['project' => $projectB]);

        $this->login($client, $userA);

        // 1. Access from another project context (Project B)
        $client->request('GET', sprintf('/api/projects/%s/drive-config', $projectB->getUuid()));
        $this->assertResponseStatusCodeSame(403);

        $client->request('POST', sprintf('/api/projects/%s/drive-config', $projectB->getUuid()), [], [], [], json_encode(['driveFolderId' => 'hacked', 'refreshToken' => 'hacked']));
        $this->assertResponseStatusCodeSame(403);

        $client->request('DELETE', sprintf('/api/projects/%s/drive-config', $projectB->getUuid()));
        $this->assertResponseStatusCodeSame(403);
    }
}
