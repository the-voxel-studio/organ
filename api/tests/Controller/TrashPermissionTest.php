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

class TrashPermissionTest extends ApiTestCase
{
    public function testOnlyAuthorizedUsersCanAccessOrganTrash(): void
    {
        $client = static::createClient();
        $project = ProjectFactory::createOne();
        $admin = UserFactory::createOne();
        $member = UserFactory::createOne();

        ProjectMemberFactory::createOne(['user' => $admin, 'project' => $project, 'globalRole' => ProjectGlobalRole::ADMIN]);
        ProjectMemberFactory::createOne(['user' => $member, 'project' => $project, 'globalRole' => ProjectGlobalRole::MEMBER]);

        // 1. Admin can access trash
        $this->login($client, $admin);
        $client->request('GET', sprintf('/api/projects/%s/organs/trash', $project->getUuid()));
        $this->assertResponseIsSuccessful();

        // 2. Member cannot access trash
        $this->login($client, $member);
        $client->request('GET', sprintf('/api/projects/%s/organs/trash', $project->getUuid()));
        $this->assertResponseStatusCodeSame(403);
    }

    public function testDeletedItemsAreNotVisibleInRegularList(): void
    {
        $client = static::createClient();
        $project = ProjectFactory::createOne();
        $admin = UserFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $admin, 'project' => $project, 'globalRole' => ProjectGlobalRole::ADMIN]);

        $organActive = OrganFactory::createOne(['project' => $project, 'deletedAt' => null]);
        $organDeleted = OrganFactory::createOne(['project' => $project, 'deletedAt' => new \DateTime()]);

        $this->login($client, $admin);
        
        // Check organs index
        $client->request('GET', sprintf('/api/projects/%s/organs', $project->getUuid()));
        $data = $this->getResponseContent($client);
        
        $this->assertCount(1, $data);
        $this->assertEquals($organActive->getUuid(), $data[0]['uuid']);
    }

    public function testDirectAccessToDeletedItemReturns404(): void
    {
        $client = static::createClient();
        $project = ProjectFactory::createOne();
        $admin = UserFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $admin, 'project' => $project, 'globalRole' => ProjectGlobalRole::ADMIN]);

        $organDeleted = OrganFactory::createOne(['project' => $project, 'deletedAt' => new \DateTime()]);

        $this->login($client, $admin);
        $client->request('GET', sprintf('/api/projects/%s/organs/%s', $project->getUuid(), $organDeleted->getUuid()));
        
        // OrganController::show uses findOneBy(['deletedAt' => null])
        $this->assertResponseStatusCodeSame(404);
    }
}
