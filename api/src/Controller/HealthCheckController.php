<?php

declare(strict_types=1);

namespace App\Controller;

use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/health', name: 'health_')]
class HealthCheckController extends AbstractController
{
    #[Route('', name: 'check', methods: ['GET'])]
    public function index(EntityManagerInterface $entityManager): JsonResponse
    {
        $dbStatus = 'OK';
        try {
            $entityManager->getConnection()->getNativeConnection();
        } catch (\Exception $e) {
            $dbStatus = 'Error: ' . $e->getMessage();
        }

        return $this->json([
            'status' => 'OK',
            'services' => [
                'api' => 'up',
                'database' => $dbStatus,
            ],
            'timestamp' => (new \DateTime())->format(\DateTimeInterface::ATOM),
        ]);
    }
}
