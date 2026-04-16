<?php

declare(strict_types=1);

namespace App\Tests\Security;

use App\Factory\UserFactory;
use App\Tests\ApiTestCase;
use Zenstruck\Foundry\Test\Factories;
use Zenstruck\Foundry\Test\ResetDatabase;

class UserRestorationTest extends ApiTestCase
{
    use ResetDatabase, Factories;

    public function testSoftDeletedUserIsRestoredOnLogin(): void
    {
        $client = static::createClient();
        
        // 1. Create a soft-deleted user
        $user = UserFactory::createOne([
            'email' => 'deleted@example.com',
            'password' => password_hash('password', PASSWORD_BCRYPT),
            'deletedAt' => new \DateTime('-1 day'),
        ]);

        // 2. Try to login
        $client->request('POST', '/api/auth/login', [], [], ['CONTENT_TYPE' => 'application/json'], json_encode([
            'username' => 'deleted@example.com',
            'password' => 'password',
        ]));

        // If UserChecker blocks them, this will be 401
        // If UserRestoreListener works, this should be 200
        $this->assertResponseIsSuccessful();
        
        // 3. Verify user is restored in database
        $em = self::getContainer()->get('doctrine')->getManager();
        $em->clear();
        $updatedUser = $em->getRepository(\App\Entity\User::class)->findOneBy(['email' => 'deleted@example.com']);
        $this->assertNull($updatedUser->getDeletedAt());
    }

    public function testSoftDeletedUserIsBlockedFromOtherRoutes(): void
    {
        $client = static::createClient();
        
        // 1. Create an active user
        $user = UserFactory::createOne([
            'email' => 'deleted_blocked@example.com',
            'password' => '$2y$13$X3K6/9xG6vP5r6p1vHhW1.O8k0y1z6p1vHhW1.O8k0y1z6p1vHhW1', // 'password'
        ]);

        // 2. Generate a token (using helper)
        $this->login($client, $user);
        
        // 3. Manually soft-delete them
        $em = self::getContainer()->get('doctrine')->getManager();
        $userEntity = $em->getRepository(\App\Entity\User::class)->find($user->getId());
        $userEntity->setDeletedAt(new \DateTime('-1 day'));
        $em->flush();
        $em->clear();

        // 4. Try to access a protected route
        $client->request('GET', '/api/users/me');

        // Should be 401 because UserChecker::checkPreAuth throws CustomUserMessageAccountStatusException
        $this->assertResponseStatusCodeSame(401);
        $this->assertStringContainsString('Your account is scheduled for deletion. Please log in again to reactivate it.', $client->getResponse()->getContent());
    }
}
