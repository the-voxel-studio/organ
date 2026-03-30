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
}
