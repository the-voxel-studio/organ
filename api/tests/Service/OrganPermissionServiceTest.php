<?php

declare(strict_types=1);

namespace App\Tests\Service;

use App\Entity\Organ;
use App\Entity\Permission;
use App\Entity\Project;
use App\Entity\User;
use App\Enum\ProjectGlobalRole;
use App\Service\OrganPermissionService;
use App\Service\ProjectMembershipService;
use App\Factory\UserFactory;
use App\Factory\ProjectFactory;
use App\Factory\OrganFactory;
use App\Factory\ProjectMemberFactory;
use App\Factory\OrganRoleFactory;
use App\Factory\PermissionFactory;
use App\Factory\UserOrganRoleFactory;
use Symfony\Bundle\FrameworkBundle\Test\KernelTestCase;
use Zenstruck\Foundry\Test\Factories;
use Zenstruck\Foundry\Test\ResetDatabase;

class OrganPermissionServiceTest extends KernelTestCase
{
    use ResetDatabase, Factories;

    private OrganPermissionService $service;

    protected function setUp(): void
    {
        self::bootKernel();
        $this->service = self::getContainer()->get(OrganPermissionService::class);
    }

    public function testHasPermissionAsProjectAdmin(): void
    {
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        $organ = OrganFactory::createOne(['project' => $project]);
        
        ProjectMemberFactory::createOne([
            'user' => $user,
            'project' => $project,
            'globalRole' => ProjectGlobalRole::ADMIN
        ]);

        $this->assertTrue($this->service->hasPermission($user, $organ, 'ANY_PERMISSION'));
    }

    public function testHasPermissionAsProjectManager(): void
    {
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        $organ = OrganFactory::createOne(['project' => $project]);
        
        ProjectMemberFactory::createOne([
            'user' => $user,
            'project' => $project,
            'globalRole' => ProjectGlobalRole::MANAGER
        ]);

        $this->assertTrue($this->service->hasPermission($user, $organ, 'ORGAN_VIEW'));
        $this->assertTrue($this->service->hasPermission($user, $organ, 'ORGAN_EDIT'));
    }

    public function testHasPermissionViaOrganRole(): void
    {
        $user = UserFactory::createOne();
        $project = ProjectFactory::createOne();
        $organ = OrganFactory::createOne(['project' => $project]);
        
        ProjectMemberFactory::createOne([
            'user' => $user,
            'project' => $project,
            'globalRole' => ProjectGlobalRole::MEMBER
        ]);

        $role = OrganRoleFactory::createOne(['organ' => $organ]);
        $permission = PermissionFactory::createOne(['name' => 'TASK_CREATE']);
        $role->addPermission($permission);
        
        // Use the EntityManager to flush the relation
        $em = self::getContainer()->get('doctrine')->getManager();
        $em->persist($role);
        $em->flush();

        UserOrganRoleFactory::createOne([
            'user' => $user,
            'role' => $role
        ]);

        $this->assertTrue($this->service->hasPermission($user, $organ, 'TASK_CREATE'));
        $this->assertFalse($this->service->hasPermission($user, $organ, 'ORGAN_VIEW'));
    }
}
