<?php

declare(strict_types=1);

namespace App\Tests;

use App\Entity\User;
use Lexik\Bundle\JWTAuthenticationBundle\Services\JWTTokenManagerInterface;
use Symfony\Bundle\FrameworkBundle\KernelBrowser;
use Symfony\Bundle\FrameworkBundle\Test\WebTestCase;
use Zenstruck\Foundry\Test\Factories;
use Zenstruck\Foundry\Test\ResetDatabase;

abstract class ApiTestCase extends WebTestCase
{
    use Factories;
    use ResetDatabase;

    protected function setUp(): void
    {
        parent::setUp();
        
        // On boot le kernel temporairement pour vider le cache
        $kernel = static::bootKernel();
        if ($kernel->getContainer()->has('cache.app')) {
            $kernel->getContainer()->get('cache.app')->clear();
        }
        // On éteint le kernel pour que createClient() puisse le redémarrer proprement
        static::ensureKernelShutdown();
    }

    protected function login(KernelBrowser $client, object $user): void
    {
        /** @var JWTTokenManagerInterface $jwtManager */
        $jwtManager = static::getContainer()->get(JWTTokenManagerInterface::class);
        $token = $jwtManager->create($user);

        $client->setServerParameter('HTTP_Authorization', sprintf('Bearer %s', $token));
    }

    /**
     * Helper pour décoder le JSON des réponses
     */
    protected function getResponseContent(KernelBrowser $client): array
    {
        $content = $client->getResponse()->getContent();
        return json_decode($content, true);
    }
}
