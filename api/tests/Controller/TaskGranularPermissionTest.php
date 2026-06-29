<?php

declare(strict_types=1);

namespace App\Tests\Controller;

use App\Entity\Permission;
use App\Entity\OrganRole;
use App\Enum\ProjectGlobalRole;
use App\Factory\OrganFactory;
use App\Factory\OrganRoleFactory;
use App\Factory\PermissionFactory;
use App\Factory\ProjectFactory;
use App\Factory\ProjectMemberFactory;
use App\Factory\TaskFactory;
use App\Factory\UserFactory;
use App\Factory\UserOrganRoleFactory;
use App\Tests\ApiTestCase;
use Doctrine\ORM\EntityManagerInterface;

class TaskGranularPermissionTest extends ApiTestCase
{
    private function createRoleWithPermissions($organ, string $name, array $permNames): OrganRole
    {
        /** @var EntityManagerInterface $em */
        $em = static::getContainer()->get(EntityManagerInterface::class);
        
        $role = new OrganRole();
        $role->setOrgan($organ);
        $role->setName($name);
        $em->persist($role);
        
        foreach ($permNames as $pName) {
            $perm = $em->getRepository(Permission::class)->findOneBy(['name' => $pName]);
            if (!$perm) {
                $perm = new Permission();
                $perm->setName($pName);
                $em->persist($perm);
            }
            $role->addPermission($perm);
        }
        
        $em->flush();
        return $role;
    }

    public function testTaskEditAllPermission(): void
    {
        $client = static::createClient();
        
        $project = ProjectFactory::createOne(['title' => 'Project '.uniqid()]);
        $organ = OrganFactory::createOne(['project' => $project, 'title' => 'Organ '.uniqid()]);
        
        $role = $this->createRoleWithPermissions($organ, 'Editor All', ['ORGAN_VIEW', 'TASK_EDIT_ALL']);
        
        $user = UserFactory::createOne(['email' => 'user_'.uniqid().'@test.com']);
        ProjectMemberFactory::createOne(['user' => $user, 'project' => $project, 'globalRole' => ProjectGlobalRole::MEMBER]);
        UserOrganRoleFactory::createOne(['user' => $user, 'role' => $role]);

        $otherUser = UserFactory::createOne();
        $task = TaskFactory::createOne([
            'organ' => $organ,
            'manager' => $otherUser,
            'createdBy' => $otherUser,
            'title' => 'Other Task'
        ]);

        $this->login($client, $user);
        
        $client->request('PATCH', sprintf('/api/projects/%s/organs/%s/tasks/%s', $project->getUuid(), $organ->getUuid(), $task->getUuid()), [], [], [], json_encode([
            'title' => 'Updated by Editor All'
        ]));
        
        $this->assertResponseIsSuccessful();
    }

    public function testTaskEditOwnRestriction(): void
    {
        $client = static::createClient();
        $project = ProjectFactory::createOne(['title' => 'Project '.uniqid()]);
        $organ = OrganFactory::createOne(['project' => $project, 'title' => 'Organ '.uniqid()]);
        
        $role = $this->createRoleWithPermissions($organ, 'Editor Own', ['ORGAN_VIEW', 'TASK_EDIT_OWN']);
        
        $user = UserFactory::createOne(['email' => 'user_'.uniqid().'@test.com']);
        ProjectMemberFactory::createOne(['user' => $user, 'project' => $project, 'globalRole' => ProjectGlobalRole::MEMBER]);
        UserOrganRoleFactory::createOne(['user' => $user, 'role' => $role]);

        $otherUser = UserFactory::createOne();
        $task = TaskFactory::createOne([
            'organ' => $organ,
            'manager' => $otherUser,
            'createdBy' => $otherUser,
            'title' => 'Other Task'
        ]);

        $this->login($client, $user);
        
        $client->request('PATCH', sprintf('/api/projects/%s/organs/%s/tasks/%s', $project->getUuid(), $organ->getUuid(), $task->getUuid()), [], [], [], json_encode([
            'title' => 'Try Update'
        ]));
        
        $this->assertResponseStatusCodeSame(403);
    }

    public function testTaskDeleteAllPermission(): void
    {
        $client = static::createClient();
        $project = ProjectFactory::createOne(['title' => 'Project '.uniqid()]);
        $organ = OrganFactory::createOne(['project' => $project, 'title' => 'Organ '.uniqid()]);
        
        $role = $this->createRoleWithPermissions($organ, 'Deleter All', ['ORGAN_VIEW', 'TASK_DELETE_ALL']);
        
        $user = UserFactory::createOne(['email' => 'user_'.uniqid().'@test.com']);
        ProjectMemberFactory::createOne(['user' => $user, 'project' => $project, 'globalRole' => ProjectGlobalRole::MEMBER]);
        UserOrganRoleFactory::createOne(['user' => $user, 'role' => $role]);

        $otherUser = UserFactory::createOne();
        $task = TaskFactory::createOne([
            'organ' => $organ,
            'manager' => $otherUser,
            'createdBy' => $otherUser,
            'title' => 'Other Task'
        ]);

        $this->login($client, $user);
        
        $client->request('DELETE', sprintf('/api/projects/%s/organs/%s/tasks/%s', $project->getUuid(), $organ->getUuid(), $task->getUuid()));
        
        $this->assertResponseIsSuccessful();
    }

    public function testTaskDeleteOwnRestriction(): void
    {
        $client = static::createClient();
        $project = ProjectFactory::createOne(['title' => 'Project '.uniqid()]);
        $organ = OrganFactory::createOne(['project' => $project, 'title' => 'Organ '.uniqid()]);
        
        $role = $this->createRoleWithPermissions($organ, 'Deleter Own', ['ORGAN_VIEW', 'TASK_DELETE_OWN']);
        
        $user = UserFactory::createOne(['email' => 'user_'.uniqid().'@test.com']);
        ProjectMemberFactory::createOne(['user' => $user, 'project' => $project, 'globalRole' => ProjectGlobalRole::MEMBER]);
        UserOrganRoleFactory::createOne(['user' => $user, 'role' => $role]);

        $otherUser = UserFactory::createOne();
        $task = TaskFactory::createOne([
            'organ' => $organ,
            'manager' => $otherUser,
            'createdBy' => $otherUser,
            'title' => 'Other Task'
        ]);

        $this->login($client, $user);
        
        $client->request('DELETE', sprintf('/api/projects/%s/organs/%s/tasks/%s', $project->getUuid(), $organ->getUuid(), $task->getUuid()));
        
        $this->assertResponseStatusCodeSame(403);
    }
}
