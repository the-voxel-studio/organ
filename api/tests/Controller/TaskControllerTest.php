<?php

declare(strict_types=1);

namespace App\Tests\Controller;

use App\Enum\ProjectGlobalRole;
use App\Enum\TaskStatus;
use App\Factory\OrganFactory;
use App\Factory\ProjectFactory;
use App\Factory\ProjectMemberFactory;
use App\Factory\TaskFactory;
use App\Factory\UserFactory;
use App\Tests\ApiTestCase;

class TaskControllerTest extends ApiTestCase
{
    public function testGetTasksIndexSuccess(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        // L'utilisateur est ADMIN du projet
        ProjectMemberFactory::createOne([
            'user' => $user, 
            'project' => $project,
            'globalRole' => ProjectGlobalRole::ADMIN
        ]);
        $organ = OrganFactory::createOne(['project' => $project]);
        
        // Créer 2 tâches pour cet organe
        TaskFactory::createMany(2, ['organ' => $organ]);

        $this->login($client, $user);
        $client->request('GET', sprintf('/api/projects/%s/organs/%s/tasks', $project->getUuid(), $organ->getUuid()));

        $this->assertResponseIsSuccessful();
        $data = $this->getResponseContent($client);
        
        $this->assertCount(2, $data);
    }

    public function testCreateTaskSuccess(): void
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
        $client->request('POST', sprintf('/api/projects/%s/organs/%s/tasks', $project->getUuid(), $organ->getUuid()), [], [], [], json_encode([
            'title' => 'New Task via API',
            'status' => TaskStatus::TODO->value,
            'priority' => 10
        ]));

        $this->assertResponseStatusCodeSame(201);
        $data = $this->getResponseContent($client);
        
        $this->assertEquals('New Task via API', $data['title']);
    }

    public function testUpdateTaskSuccess(): void
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
        $task = TaskFactory::createOne(['organ' => $organ, 'title' => 'Old Task']);

        $this->login($client, $user);
        $client->request('PATCH', sprintf('/api/projects/%s/organs/%s/tasks/%s', $project->getUuid(), $organ->getUuid(), $task->getUuid()), [], [], [], json_encode([
            'title' => 'Updated Task'
        ]));

        $this->assertResponseIsSuccessful();
        $data = $this->getResponseContent($client);
        
        $this->assertEquals('Updated Task', $data['title']);
    }

    public function testSoftDeleteAndRestoreTask(): void
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
        $task = TaskFactory::createOne(['organ' => $organ, 'title' => 'Task to delete']);

        $this->login($client, $user);
        $projectUuid = $project->getUuid();
        $organUuid = $organ->getUuid();
        $taskUuid = $task->getUuid();

        // 1. Soft delete
        $client->request('DELETE', sprintf('/api/projects/%s/organs/%s/tasks/%s', $projectUuid, $organUuid, $taskUuid));
        $this->assertResponseIsSuccessful();
        $this->assertEquals('Task deleted successfully', $this->getResponseContent($client)['message']);

        // 2. Verify it's hidden from index
        $client->request('GET', sprintf('/api/projects/%s/organs/%s/tasks', $projectUuid, $organUuid));
        $this->assertResponseIsSuccessful();
        $this->assertCount(0, $this->getResponseContent($client));

        // 3. Try to update (should fail with 403)
        $client->request('PATCH', sprintf('/api/projects/%s/organs/%s/tasks/%s', $projectUuid, $organUuid, $taskUuid), [], [], [], json_encode([
            'title' => 'Should fail'
        ]));
        $this->assertResponseStatusCodeSame(403);
        $this->assertEquals('Task is deleted and cannot be updated', $this->getResponseContent($client)['message']);

        // 4. Restore
        $client->request('POST', sprintf('/api/projects/%s/organs/%s/tasks/%s/restore', $projectUuid, $organUuid, $taskUuid));
        $this->assertResponseIsSuccessful();
        
        // 5. Verify it's visible again (show route)
        $client->request('GET', sprintf('/api/projects/%s/organs/%s/tasks/%s', $projectUuid, $organUuid, $taskUuid));
        $this->assertResponseIsSuccessful();
        $this->assertEquals('Task to delete', $this->getResponseContent($client)['title']);

        // 6. Verify update works again
        $client->request('PATCH', sprintf('/api/projects/%s/organs/%s/tasks/%s', $projectUuid, $organUuid, $taskUuid), [], [], [], json_encode([
            'title' => 'Restored and Updated'
        ]));
        $this->assertResponseIsSuccessful();
        $this->assertEquals('Restored and Updated', $this->getResponseContent($client)['title']);
    }
}
