<?php

declare(strict_types=1);

namespace App\Tests\Controller;

use App\Factory\UserFactory;
use App\Tests\ApiTestCase;

class UserControllerTest extends ApiTestCase
{
    public function testGetMeSuccess(): void
    {
        $client = static::createClient();

        // 1. Créer un utilisateur via la Factory
        $user = UserFactory::createOne([
            'email' => 'test@example.com',
            'firstName' => 'John',
            'lastName' => 'Doe',
        ]);

        // 2. Authentifier le client
        $this->login($client, $user);

        // 3. Faire la requête
        $client->request('GET', '/api/users/me');

        // 4. Assertions
        $this->assertResponseIsSuccessful();
        $this->assertResponseHeaderSame('Content-Type', 'application/json');

        $data = $this->getResponseContent($client);

        $this->assertEquals('test@example.com', $data['email']);
        $this->assertEquals('John', $data['firstName']);
        $this->assertEquals('Doe', $data['lastName']);
        $this->assertArrayHasKey('uuid', $data);
        $this->assertTrue($data['isVerified']);
    }

    public function testGetMeUnauthorized(): void
    {
        $client = static::createClient();
        $client->request('GET', '/api/users/me');

        // Lexik JWT retourne généralement un 401 si le header est absent
        $this->assertResponseStatusCodeSame(401);
    }
}
