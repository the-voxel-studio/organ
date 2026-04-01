<?php

declare(strict_types=1);

namespace App\Tests\Controller;

use App\Entity\Project;
use App\Enum\IconType;
use App\Enum\ProjectGlobalRole;
use App\Enum\ProjectStatus;
use App\Factory\ProjectFactory;
use App\Factory\ProjectMemberFactory;
use App\Factory\UserFactory;
use App\Tests\ApiTestCase;

class ProjectControllerTest extends ApiTestCase
{
    public function testGetProjectsIndexSuccess(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        
        // Créer 3 projets pour cet utilisateur
        ProjectMemberFactory::createMany(3, [
            'user' => $user,
            'globalRole' => ProjectGlobalRole::ADMIN
        ]);

        // Créer 1 projet pour un autre utilisateur
        ProjectMemberFactory::createOne();

        $this->login($client, $user);
        $client->request('GET', '/api/projects');

        $this->assertResponseIsSuccessful();
        $data = $this->getResponseContent($client);
        
        $this->assertCount(3, $data);
        $this->assertArrayHasKey('uuid', $data[0]);
        $this->assertArrayHasKey('title', $data[0]);
    }

    public function testGetProjectShowSuccess(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne(['title' => 'My Special Project']);
        ProjectMemberFactory::createOne([
            'user' => $user, 
            'project' => $project,
            'globalRole' => ProjectGlobalRole::ADMIN
        ]);

        $this->login($client, $user);
        $client->request('GET', '/api/projects/' . $project->getUuid());

        $this->assertResponseIsSuccessful();
        $data = $this->getResponseContent($client);
        
        $this->assertEquals('My Special Project', $data['title']);
        $this->assertEquals(ProjectGlobalRole::ADMIN->value, $data['role']);
    }

    public function testGetProjectShowForbidden(): void
    {
        $client = static::createClient();
        $userA = UserFactory::createOne();
        $userB = UserFactory::createOne();
        
        $projectOfA = ProjectFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $userA, 'project' => $projectOfA]);

        // User B essaie de voir le projet de A
        $this->login($client, $userB);
        $client->request('GET', '/api/projects/' . $projectOfA->getUuid());

        $this->assertResponseStatusCodeSame(403);
    }

    public function testCreateProjectSuccess(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();

        $this->login($client, $user);
        $client->request('POST', '/api/projects', [], [], [], json_encode([
            'title' => 'New Project via API',
            'description' => 'Test description',
            'status' => ProjectStatus::ACTIVE->value,
            'iconType' => IconType::EMOJI->value,
            'iconData' => '🚀'
        ]));

        $this->assertResponseStatusCodeSame(201);
        $data = $this->getResponseContent($client);
        
        $this->assertArrayHasKey('uuid', $data);
        $this->assertEquals('New Project via API', $data['title']);
        $this->assertEquals(ProjectGlobalRole::ADMIN->value, $data['role']);
    }

    public function testUpdateProjectSuccess(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne(['title' => 'Old Title']);
        ProjectMemberFactory::createOne([
            'user' => $user, 
            'project' => $project,
            'globalRole' => ProjectGlobalRole::ADMIN
        ]);

        $this->login($client, $user);
        $client->request('PUT', '/api/projects/' . $project->getUuid(), [], [], [], json_encode([
            'title' => 'Updated Title',
            'status' => ProjectStatus::ARCHIVED->value
        ]));

        $this->assertResponseIsSuccessful();
        $data = $this->getResponseContent($client);
        
        $this->assertEquals('Updated Title', $data['title']);
        $this->assertEquals(ProjectStatus::ARCHIVED->value, $data['status']);
    }

    public function testUpdateProjectForbiddenForNonAdmin(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        ProjectMemberFactory::createOne([
            'user' => $user, 
            'project' => $project,
            'globalRole' => ProjectGlobalRole::MEMBER // Simple membre
        ]);

        $this->login($client, $user);
        $client->request('PUT', '/api/projects/' . $project->getUuid(), [], [], [], json_encode([
            'title' => 'Hack title'
        ]));

        $this->assertResponseStatusCodeSame(403);
    }

    public function testDeleteProjectSuccess(): void
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
        $client->request('DELETE', '/api/projects/' . $project->getUuid());

        $this->assertResponseStatusCodeSame(204);
        
        // Vérifier le soft delete
        $client->request('GET', '/api/projects/' . $project->getUuid());
        $this->assertResponseStatusCodeSame(404);
    }

    public function testGetProjectStatsSqlSuccess(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        ProjectMemberFactory::createOne([
            'user' => $user, 
            'project' => $project,
            'globalRole' => ProjectGlobalRole::ADMIN
        ]);

        $organ = \App\Factory\OrganFactory::createOne(['project' => $project, 'title' => 'Organ A']);
        \App\Factory\TaskFactory::createMany(2, [
            'organ' => $organ, 
            'status' => \App\Enum\TaskStatus::TODO,
            'estimatedHours' => '2.5'
        ]);
        \App\Factory\TaskFactory::createOne([
            'organ' => $organ, 
            'status' => \App\Enum\TaskStatus::DONE,
            'estimatedHours' => '1.0'
        ]);

        $this->login($client, $user);
        $client->request('GET', '/api/projects/' . $project->getUuid() . '/stats');

        $this->assertResponseIsSuccessful();
        $data = $this->getResponseContent($client);
        
        $this->assertCount(2, $data); // TODO and DONE groups
        
        // Find TODO stats
        $todoStats = null;
        foreach ($data as $s) {
            if ($s['status'] === 'TODO') {
                $todoStats = $s;
                break;
            }
        }
        $this->assertNotNull($todoStats);
        $this->assertEquals(2, $todoStats['task_count']);
        $this->assertEquals(5.0, (float)$todoStats['total_hours']);
    }
}
