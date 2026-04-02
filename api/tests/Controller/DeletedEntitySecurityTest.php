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

class DeletedEntitySecurityTest extends ApiTestCase
{
    public function testCannotAccessTaskOfDeletedOrgan(): void
    {
        $client = static::createClient();
        $project = ProjectFactory::createOne();
        $admin = UserFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $admin, 'project' => $project, 'globalRole' => ProjectGlobalRole::ADMIN]);

        $organ = OrganFactory::createOne(['project' => $project, 'deletedAt' => new \DateTime()]);
        $task = TaskFactory::createOne(['organ' => $organ, 'deletedAt' => null]);

        $this->login($client, $admin);
        
        // Try to access task of deleted organ
        $client->request('GET', sprintf('/api/projects/%s/organs/%s/tasks/%s', $project->getUuid(), $organ->getUuid(), $task->getUuid()));
        $this->assertResponseStatusCodeSame(404);
        
        // Try to update task of deleted organ
        $client->request('PUT', sprintf('/api/projects/%s/organs/%s/tasks/%s', $project->getUuid(), $organ->getUuid(), $task->getUuid()), [], [], [], json_encode([
            'title' => 'New Title'
        ]));
        $this->assertResponseStatusCodeSame(404);
    }

    public function testCannotAccessOrganOfDeletedProject(): void
    {
        $client = static::createClient();
        $project = ProjectFactory::createOne(['deletedAt' => new \DateTime()]);
        $admin = UserFactory::createOne();
        // Note: ProjectMember might still exist but the project itself is deleted
        ProjectMemberFactory::createOne(['user' => $admin, 'project' => $project, 'globalRole' => ProjectGlobalRole::ADMIN]);

        $organ = OrganFactory::createOne(['project' => $project, 'deletedAt' => null]);

        $this->login($client, $admin);
        
        // Try to access organ of deleted project
        $client->request('GET', sprintf('/api/projects/%s/organs/%s', $project->getUuid(), $organ->getUuid()));
        $this->assertResponseStatusCodeSame(404);
    }

    public function testCannotAccessTaskOfDeletedProject(): void
    {
        $client = static::createClient();
        $project = ProjectFactory::createOne(['deletedAt' => new \DateTime()]);
        $admin = UserFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $admin, 'project' => $project, 'globalRole' => ProjectGlobalRole::ADMIN]);

        $organ = OrganFactory::createOne(['project' => $project, 'deletedAt' => null]);
        $task = TaskFactory::createOne(['organ' => $organ, 'deletedAt' => null]);

        $this->login($client, $admin);
        
        // Try to access task of deleted project
        $client->request('GET', sprintf('/api/projects/%s/organs/%s/tasks/%s', $project->getUuid(), $organ->getUuid(), $task->getUuid()));
        $this->assertResponseStatusCodeSame(404);
    }

    public function testCannotCreateTaskInDeletedOrgan(): void
    {
        $client = static::createClient();
        $project = ProjectFactory::createOne();
        $admin = UserFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $admin, 'project' => $project, 'globalRole' => ProjectGlobalRole::ADMIN]);

        $organ = OrganFactory::createOne(['project' => $project, 'deletedAt' => new \DateTime()]);

        $this->login($client, $admin);
        
        $client->request('POST', sprintf('/api/projects/%s/organs/%s/tasks', $project->getUuid(), $organ->getUuid()), [], [], [], json_encode([
            'title' => 'Forbidden Task'
        ]));
        $this->assertResponseStatusCodeSame(404);
    }

    public function testCannotUpdateDeletedTask(): void
    {
        $client = static::createClient();
        $project = ProjectFactory::createOne();
        $admin = UserFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $admin, 'project' => $project, 'globalRole' => ProjectGlobalRole::ADMIN]);

        $organ = OrganFactory::createOne(['project' => $project]);
        $task = TaskFactory::createOne(['organ' => $organ, 'deletedAt' => new \DateTime()]);

        $this->login($client, $admin);
        
        $client->request('PUT', sprintf('/api/projects/%s/organs/%s/tasks/%s', $project->getUuid(), $organ->getUuid(), $task->getUuid()), [], [], [], json_encode([
            'title' => 'Updated Title'
        ]));
        
        // TaskController::update returns 403 for deleted task
        $this->assertResponseStatusCodeSame(403);
    }
}
