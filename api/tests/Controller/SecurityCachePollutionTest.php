<?php

declare(strict_types=1);

namespace App\Tests\Controller;

use App\Enum\ProjectGlobalRole;
use App\Factory\OrganFactory;
use App\Factory\OrganRoleFactory;
use App\Factory\PermissionFactory;
use App\Factory\ProjectFactory;
use App\Factory\ProjectMemberFactory;
use App\Factory\UserFactory;
use App\Factory\UserOrganRoleFactory;
use App\Tests\ApiTestCase;

class SecurityCachePollutionTest extends ApiTestCase
{
    /**
     * testOrganListCacheLeak: 
     * 1. Admin (who sees all organs) lists organs -> Populates project-wide cache.
     * 2. Member (who sees NO organs) lists organs -> Receives Admin's cached list.
     * Expect: Member receives an EMPTY list.
     */
    public function testOrganListCacheLeak(): void
    {
        $client = static::createClient();
        $project = ProjectFactory::createOne();
        $organ = OrganFactory::createOne(['project' => $project, 'title' => 'Secret Organ']);
        
        $admin = UserFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $admin, 'project' => $project, 'globalRole' => ProjectGlobalRole::ADMIN]);

        $member = UserFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $member, 'project' => $project, 'globalRole' => ProjectGlobalRole::MEMBER]);

        // 1. Admin lists organs
        $this->login($client, $admin);
        $client->request('GET', sprintf('/api/projects/%s/organs', $project->getUuid()));
        $this->assertResponseIsSuccessful();
        $adminData = $this->getResponseContent($client);
        $this->assertCount(1, $adminData);
        $this->assertEquals('Secret Organ', $adminData[0]['title']);

        // 2. Member lists organs (should NOT see the organ)
        $this->login($client, $member);
        $client->request('GET', sprintf('/api/projects/%s/organs', $project->getUuid()));
        $this->assertResponseIsSuccessful();
        $memberData = $this->getResponseContent($client);
        
        // This is EXPECTED to FAIL if there is a cache leak
        $this->assertCount(0, $memberData, 'Member should not see any organs because they have no permissions on them');
    }
}
