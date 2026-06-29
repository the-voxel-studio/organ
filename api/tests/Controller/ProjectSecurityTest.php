<?php

declare(strict_types=1);

namespace App\Tests\Controller;

use App\Enum\ProjectGlobalRole;
use App\Factory\ProjectFactory;
use App\Factory\ProjectMemberFactory;
use App\Factory\UserFactory;
use App\Tests\ApiTestCase;

class ProjectSecurityTest extends ApiTestCase
{
    /**
     * Case 1: A user who is NOT a member of a project tries to GET /api/projects/{uuid}. Expect 403.
     */
    public function testGetProjectUnauthorized(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();

        // No membership created for $user in $project

        $this->login($client, $user);
        $client->request('GET', '/api/projects/' . $project->getUuid());

        $this->assertResponseStatusCodeSame(403);
    }

    /**
     * Case 2: A project MANAGER tries to PUT /api/projects/{uuid}. Expect 403 (only ADMIN allowed).
     */
    public function testUpdateProjectAsManager(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        ProjectMemberFactory::createOne([
            'user' => $user,
            'project' => $project,
            'globalRole' => ProjectGlobalRole::MANAGER
        ]);

        $this->login($client, $user);
        $client->request('PUT', '/api/projects/' . $project->getUuid(), [], [], [], json_encode([
            'title' => 'Updated Title'
        ]));

        $this->assertResponseStatusCodeSame(403);
    }

    /**
     * Case 3: A project MANAGER tries to DELETE /api/projects/{uuid}. Expect 403 (only ADMIN allowed).
     */
    public function testDeleteProjectAsManager(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        ProjectMemberFactory::createOne([
            'user' => $user,
            'project' => $project,
            'globalRole' => ProjectGlobalRole::MANAGER
        ]);

        $this->login($client, $user);
        $client->request('DELETE', '/api/projects/' . $project->getUuid());

        $this->assertResponseStatusCodeSame(403);
    }

    /**
     * Case 4: A project MANAGER tries to POST /api/projects/{uuid}/restore. Expect 403 (only ADMIN allowed).
     */
    public function testRestoreProjectAsManager(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne([
            'deletedAt' => new \DateTime()
        ]);
        ProjectMemberFactory::createOne([
            'user' => $user,
            'project' => $project,
            'globalRole' => ProjectGlobalRole::MANAGER
        ]);

        $this->login($client, $user);
        $client->request('POST', '/api/projects/' . $project->getUuid() . '/restore');

        $this->assertResponseStatusCodeSame(403);
    }

    /**
     * Case 5: An ADMIN tries to update a project that has a deletedAt timestamp. Expect 403.
     */
    public function testUpdateDeletedProject(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne([
            'deletedAt' => new \DateTime()
        ]);
        ProjectMemberFactory::createOne([
            'user' => $user,
            'project' => $project,
            'globalRole' => ProjectGlobalRole::ADMIN
        ]);

        $this->login($client, $user);
        $client->request('PUT', '/api/projects/' . $project->getUuid(), [], [], [], json_encode([
            'title' => 'Attempted Update'
        ]));

        $this->assertResponseStatusCodeSame(403);
    }

    /**
     * Case 6: An ADMIN tries to restore a project that is NOT deleted. Expect 400.
     */
    public function testRestoreActiveProject(): void
    {
        $client = static::createClient();
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne([
            'deletedAt' => null
        ]);
        ProjectMemberFactory::createOne([
            'user' => $user,
            'project' => $project,
            'globalRole' => ProjectGlobalRole::ADMIN
        ]);

        $this->login($client, $user);
        $client->request('POST', '/api/projects/' . $project->getUuid() . '/restore');

        $this->assertResponseStatusCodeSame(400);
    }
}
