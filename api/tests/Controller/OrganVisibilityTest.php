<?php

declare(strict_types=1);

namespace App\Tests\Controller;

use App\Entity\Organ;
use App\Entity\Project;
use App\Entity\ProjectMember;
use App\Entity\User;
use App\Enum\ProjectGlobalRole;
use App\Tests\ApiTestCase;
use Doctrine\ORM\EntityManagerInterface;

class OrganVisibilityTest extends ApiTestCase
{
    public function testProjectManagerCanSeeAllOrgansDetailsAndTasks(): void
    {
        $client = static::createClient();
        $em = static::getContainer()->get(EntityManagerInterface::class);

        $manager = new User();
        $manager->setEmail('manager_final_'.uniqid().'@test.com');
        $em->persist($manager);

        $project = new Project();
        $project->setTitle('Project Manager Final');
        $em->persist($project);

        $membership = new ProjectMember();
        $membership->setUser($manager);
        $membership->setProject($project);
        $membership->setGlobalRole(ProjectGlobalRole::MANAGER);
        $em->persist($membership);

        $organ = new Organ();
        $organ->setProject($project);
        $organ->setTitle('Organ Manager Final');
        $em->persist($organ);

        $em->flush();

        $this->login($client, $manager);

        $client->request('GET', sprintf('/api/projects/%s/organs/%s', $project->getUuid(), $organ->getUuid()));
        $this->assertResponseIsSuccessful();
    }

    public function testProjectMemberIsRestrictedToTheirOrgans(): void
    {
        $client = static::createClient();
        $em = static::getContainer()->get(EntityManagerInterface::class);

        $member = new User();
        $member->setEmail('member_final_'.uniqid().'@test.com');
        $em->persist($member);

        $project = new Project();
        $project->setTitle('Project Member Final');
        $em->persist($project);

        $membership = new ProjectMember();
        $membership->setUser($member);
        $membership->setProject($project);
        $membership->setGlobalRole(ProjectGlobalRole::MEMBER);
        $em->persist($membership);

        $organ = new Organ();
        $organ->setProject($project);
        $organ->setTitle('Organ Member Final');
        $em->persist($organ);

        $em->flush();

        $this->login($client, $member);

        $client->request('GET', sprintf('/api/projects/%s/organs/%s', $project->getUuid(), $organ->getUuid()));
        $this->assertResponseStatusCodeSame(403);
    }
}
