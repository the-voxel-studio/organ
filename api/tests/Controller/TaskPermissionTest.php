<?php

declare(strict_types=1);

namespace App\Tests\Controller;

use App\Entity\Permission;
use App\Entity\TaskAssignee;
use App\Enum\ProjectGlobalRole;
use App\Enum\TaskStatus;
use App\Factory\OrganFactory;
use App\Factory\OrganRoleFactory;
use App\Factory\PermissionFactory;
use App\Factory\ProjectFactory;
use App\Factory\ProjectMemberFactory;
use App\Factory\TaskFactory;
use App\Factory\UserFactory;
use App\Factory\UserOrganRoleFactory;
use App\Tests\ApiTestCase;

class TaskPermissionTest extends ApiTestCase
{
    private function setupTaskEnvironment(): array
    {
        // 1. Création de l'infrastructure de base
        $project = ProjectFactory::createOne();
        $organ = OrganFactory::createOne(['project' => $project]);
        
        // 2. Création des permissions nécessaires
        // ORGAN_VIEW pour voir l'organe, TASK_EDIT_OWN pour éditer ses propres tâches (Manager/Assignee)
        $permView = PermissionFactory::createOne(['name' => 'ORGAN_VIEW']);
        $permEditOwn = PermissionFactory::createOne(['name' => 'TASK_EDIT_OWN']);
        $permStatusOwn = PermissionFactory::createOne(['name' => 'TASK_STATUS_CHANGE_OWN']);

        // 3. Création du rôle "Worker" avec ces permissions
        $role = OrganRoleFactory::createOne(['organ' => $organ, 'name' => 'Worker']);
        $role->addPermission($permView);
        $role->addPermission($permEditOwn);
        $role->addPermission($permStatusOwn);
        
        $em = static::getContainer()->get('doctrine')->getManager();
        $em->persist($role);
        $em->flush();

        // 4. Création des utilisateurs Lambda (Simple Membres du projet)
        $manager = UserFactory::createOne();
        $assignee = UserFactory::createOne();
        
        ProjectMemberFactory::createOne(['user' => $manager, 'project' => $project, 'globalRole' => ProjectGlobalRole::MEMBER]);
        ProjectMemberFactory::createOne(['user' => $assignee, 'project' => $project, 'globalRole' => ProjectGlobalRole::MEMBER]);

        // 5. Attribution du rôle "Worker" aux deux utilisateurs dans l'organe
        UserOrganRoleFactory::createOne(['user' => $manager, 'role' => $role]);
        UserOrganRoleFactory::createOne(['user' => $assignee, 'role' => $role]);

        // 6. Création de la tâche gérée par l'un et affectée à l'autre
        $task = TaskFactory::createOne([
            'organ' => $organ,
            'manager' => $manager,
            'title' => 'Collaborative Task',
            'status' => TaskStatus::TODO
        ]);
        
        // Créer l'assignation manuellement car c'est une entité de relation
        $taskAssignee = new TaskAssignee();
        $taskAssignee->setTask($task);
        $taskAssignee->setUser($assignee);
        
        $em = static::getContainer()->get('doctrine')->getManager();
        $em->persist($taskAssignee);
        $em->flush();

        return [$project, $organ, $task, $manager, $assignee];
    }

    public function testAssigneeCanUpdateDescriptionButNotPriority(): void
    {
        $client = static::createClient();
        [$project, $organ, $task, $manager, $assignee] = $this->setupTaskEnvironment();

        // On se connecte en tant qu'Assignee
        $this->login($client, $assignee);
        
        // 1. L'Assignee peut modifier la description (TASK_EDIT_OWN + logique métier dans TaskService)
        $client->request('PATCH', sprintf('/api/projects/%s/organs/%s/tasks/%s', $project->getUuid(), $organ->getUuid(), $task->getUuid()), [], [], [], json_encode([
            'description' => 'Updated by Assignee'
        ]));
        $this->assertResponseIsSuccessful();

        // 2. Mais l'Assignee ne peut pas modifier la priorité (réservé au Manager dans TaskService::canEditField)
        $client->request('PATCH', sprintf('/api/projects/%s/organs/%s/tasks/%s', $project->getUuid(), $organ->getUuid(), $task->getUuid()), [], [], [], json_encode([
            'priority' => 50
        ]));
        $this->assertResponseStatusCodeSame(403);
    }

    public function testManagerCanSetToDoneButAssigneeCannot(): void
    {
        $client = static::createClient();
        [$project, $organ, $task, $manager, $assignee] = $this->setupTaskEnvironment();

        // 1. L'Assignee essaie de mettre à DONE -> Refusé (logic métier canChangeStatus : DONE nécessite d'être Manager)
        $this->login($client, $assignee);
        $client->request('PATCH', sprintf('/api/projects/%s/organs/%s/tasks/%s', $project->getUuid(), $organ->getUuid(), $task->getUuid()), [], [], [], json_encode([
            'status' => TaskStatus::DONE->value
        ]));
        $this->assertResponseStatusCodeSame(403);

        // 2. Le Manager essaie de mettre à DONE -> Succès
        $this->login($client, $manager);
        $client->request('PATCH', sprintf('/api/projects/%s/organs/%s/tasks/%s', $project->getUuid(), $organ->getUuid(), $task->getUuid()), [], [], [], json_encode([
            'status' => TaskStatus::DONE->value
        ]));
        $this->assertResponseIsSuccessful();
    }
}
