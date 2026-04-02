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
        
        // $data = $this->getResponseContent($client);
        // $this->assertArrayHasKey('token', $data);

        // 3. Verify user is restored in database
        $em = self::getContainer()->get('doctrine')->getManager();
        $em->clear();
        $updatedUser = $em->getRepository(\App\Entity\User::class)->findOneBy(['email' => 'deleted@example.com']);
        $this->assertNull($updatedUser->getDeletedAt());
    }
}
