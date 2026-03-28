<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\Permission;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\Routing\Attribute\Route;

#[Route('/permissions', name: 'permissions_')]
class PermissionController extends AbstractController
{
    #[Route('/available', name: 'available', methods: ['GET'])]
    public function available(EntityManagerInterface $entityManager): JsonResponse
    {
        $permissions = $entityManager->getRepository(Permission::class)->findAll();
        
        $data = [];
        foreach ($permissions as $perm) {
            $data[] = [
                'uuid' => $perm->getUuid(),
                'name' => $perm->getName(),
            ];
        }

        return $this->json($data);
    }
}
