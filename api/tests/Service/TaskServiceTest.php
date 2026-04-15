<?php

declare(strict_types=1);

namespace App\Tests\Service;

use App\Entity\Task;
use App\Entity\User;
use App\Enum\ProjectGlobalRole;
use App\Enum\TaskStatus;
use App\Factory\OrganFactory;
use App\Factory\ProjectFactory;
use App\Factory\ProjectMemberFactory;
use App\Factory\OrganRoleFactory;
use App\Factory\PermissionFactory;
use App\Factory\UserOrganRoleFactory;
use App\Factory\TaskAssigneeFactory;
use App\Factory\TaskFactory;
use App\Factory\UserFactory;
use App\Service\TaskService;
use Symfony\Bundle\FrameworkBundle\Test\KernelTestCase;
use Zenstruck\Foundry\Test\Factories;
use Zenstruck\Foundry\Test\ResetDatabase;

class TaskServiceTest extends KernelTestCase
{
    use ResetDatabase, Factories;

    private TaskService $service;

    protected function setUp(): void
    {
        self::bootKernel();
        $this->service = self::getContainer()->get(TaskService::class);
    }

    private function grantPermissions(User $user, $organ, array $permissions): void
    {
        $role = OrganRoleFactory::createOne(['organ' => $organ]);
        foreach ($permissions as $permName) {
            $permission = PermissionFactory::createOne(['name' => $permName]);
            $role->addPermission($permission);
        }
        
        $em = self::getContainer()->get('doctrine')->getManager();
        $em->persist($role);
        $em->flush();

        UserOrganRoleFactory::createOne([
            'user' => $user,
            'role' => $role
        ]);
    }

    public function testCanAsProjectAdmin(): void
    {
        $project = ProjectFactory::createOne();
        $user = UserFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $user, 'project' => $project, 'globalRole' => ProjectGlobalRole::ADMIN]);

        $organ = OrganFactory::createOne(['project' => $project]);
        $task = TaskFactory::createOne(['organ' => $organ]);

        $this->assertTrue($this->service->can($user, $task, 'TASK_EDIT'));
        $this->assertTrue($this->service->can($user, $task, 'TASK_DELETE'));
    }

    public function testCanAsTaskManager(): void
    {
        $project = ProjectFactory::createOne();
        $user = UserFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $user, 'project' => $project, 'globalRole' => ProjectGlobalRole::MEMBER]);

        $organ = OrganFactory::createOne(['project' => $project]);
        $this->grantPermissions($user, $organ, ['TASK_EDIT_OWN', 'TASK_DELETE_OWN', 'TASK_VALIDATE_OWN']);

        // User is the manager of the task
        $task = TaskFactory::createOne(['organ' => $organ, 'manager' => $user]);

        // Manager should be able to edit and delete
        $this->assertTrue($this->service->can($user, $task, 'TASK_EDIT'));
        $this->assertTrue($this->service->can($user, $task, 'TASK_DELETE'));
        $this->assertTrue($this->service->can($user, $task, 'TASK_VALIDATE'));
    }

    public function testCanAsTaskAssignee(): void
    {
        $project = ProjectFactory::createOne();
        $user = UserFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $user, 'project' => $project, 'globalRole' => ProjectGlobalRole::MEMBER]);

        $organ = OrganFactory::createOne(['project' => $project]);
        $this->grantPermissions($user, $organ, ['TASK_EDIT_OWN', 'TASK_DELETE_OWN', 'TASK_VALIDATE_OWN']);

        $task = TaskFactory::createOne(['organ' => $organ]);
        TaskAssigneeFactory::createOne(['task' => $task, 'user' => $user]);

        // Assignee can edit but NOT delete (based on TaskService::can)
        $this->assertTrue($this->service->can($user, $task, 'TASK_EDIT'));
        $this->assertFalse($this->service->can($user, $task, 'TASK_DELETE'));
        $this->assertFalse($this->service->can($user, $task, 'TASK_VALIDATE'));
    }

    public function testCanEditField(): void
    {
        $project = ProjectFactory::createOne();
        $user = UserFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $user, 'project' => $project, 'globalRole' => ProjectGlobalRole::MEMBER]);

        $organ = OrganFactory::createOne(['project' => $project]);
        $this->grantPermissions($user, $organ, ['TASK_EDIT_OWN']);

        $task = TaskFactory::createOne(['organ' => $organ]);
        TaskAssigneeFactory::createOne(['task' => $task, 'user' => $user]);

        // Assignee can edit title
        $this->assertTrue($this->service->canEditField($user, $task, 'title'));
        // Assignee CANNOT edit priority (only Manager/Admin)
        $this->assertFalse($this->service->canEditField($user, $task, 'priority'));

        // Manager can edit priority
        $task->setManager($user);

        
        $this->assertTrue($this->service->canEditField($user, $task, 'priority'));
    }

    public function testCanChangeStatus(): void
    {
        $project = ProjectFactory::createOne();
        $user = UserFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $user, 'project' => $project, 'globalRole' => ProjectGlobalRole::MEMBER]);

        $organ = OrganFactory::createOne(['project' => $project]);
        $this->grantPermissions($user, $organ, ['TASK_STATUS_CHANGE_OWN']);

        $task = TaskFactory::createOne(['organ' => $organ]);
        TaskAssigneeFactory::createOne(['task' => $task, 'user' => $user]);

        $this->assertTrue($this->service->canChangeStatus($user, $task, TaskStatus::IN_PROGRESS));
        
        // Assignee CANNOT change to DONE (only Manager/Admin)
        $this->assertFalse($this->service->canChangeStatus($user, $task, TaskStatus::DONE));

        // Manager CAN change to DONE
        $task->setManager($user);

        
        $this->assertTrue($this->service->canChangeStatus($user, $task, TaskStatus::DONE));
    }

    public function testCannotAsUnrelatedUser(): void
    {
        $project = ProjectFactory::createOne();
        $user = UserFactory::createOne();
        ProjectMemberFactory::createOne(['user' => $user, 'project' => $project, 'globalRole' => ProjectGlobalRole::MEMBER]);

        $organ = OrganFactory::createOne(['project' => $project]);
        $this->grantPermissions($user, $organ, ['TASK_EDIT_OWN', 'TASK_DELETE_OWN', 'TASK_VALIDATE_OWN', 'TASK_STATUS_CHANGE_OWN']);

        $task = TaskFactory::createOne(['organ' => $organ]);

        // User is just a project member, not an assignee, not manager, not creator
        $this->assertFalse($this->service->can($user, $task, 'TASK_EDIT'));
        $this->assertFalse($this->service->can($user, $task, 'TASK_DELETE'));
        $this->assertFalse($this->service->can($user, $task, 'TASK_VALIDATE'));
        $this->assertFalse($this->service->canEditField($user, $task, 'title'));
        $this->assertFalse($this->service->canChangeStatus($user, $task, TaskStatus::IN_PROGRESS));
    }
}
