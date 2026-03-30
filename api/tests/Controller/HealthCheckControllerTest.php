<?php

declare(strict_types=1);

namespace App\Tests\Controller;

use Symfony\Bundle\FrameworkBundle\Test\WebTestCase;
use Zenstruck\Foundry\Test\Factories;

class HealthCheckControllerTest extends WebTestCase
{
    use Factories;

    public function testHealthCheckIsSuccessful(): void
    {
        $client = static::createClient();
        $client->request('GET', '/api/health');

        $this->assertResponseIsSuccessful();
        $this->assertResponseHeaderSame('Content-Type', 'application/json');

        $content = json_decode($client->getResponse()->getContent(), true);
        
        $this->assertEquals('OK', $content['status']);
        $this->assertEquals('up', $content['services']['api']);
        $this->assertEquals('OK', $content['services']['database']);
        $this->assertArrayHasKey('timestamp', $content);
    }
}
