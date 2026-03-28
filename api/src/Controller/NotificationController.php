<?php

declare(strict_types=1);

namespace App\Controller;

use App\Entity\Notification;
use App\Entity\User;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use Symfony\Component\Mercure\Authorization;
use Symfony\Component\DependencyInjection\Attribute\Autowire;

#[Route('/notifications', name: 'notifications_')]
class NotificationController extends AbstractController
{
    public function __construct(
        #[Autowire('%notification_base_url%')]
        private readonly string $baseUrl
    ) {}

    #[Route('', name: 'index', methods: ['GET'])]
    public function index(EntityManagerInterface $entityManager): JsonResponse
    {
        /** @var User $user */
        $user = $this->getUser();
        if (!$user) return $this->json(['message' => 'Not authenticated'], Response::HTTP_UNAUTHORIZED);

        $notifications = $entityManager->getRepository(Notification::class)->findBy(
            ['user' => $user, 'deletedAt' => null],
            ['createdAt' => 'DESC'],
            50
        );

        $data = [];
        foreach ($notifications as $n) {
            $data[] = [
                'uuid' => $n->getUuid(),
                'type' => $n->getType(),
                'message' => $n->getMessage(),
                'isRead' => $n->isRead(),
                'taskUuid' => $n->getTask()?->getUuid(),
                'createdAt' => $n->getCreatedAt()->format(\DateTimeInterface::ATOM),
            ];
        }

        return $this->json($data);
    }

    #[Route('/subscribe', name: 'subscribe_url', methods: ['GET'])]
    public function getSubscribeUrl(Authorization $authorization): JsonResponse
    {
        /** @var User $user */
        $user = $this->getUser();
        if (!$user) return $this->json(['message' => 'Not authenticated'], Response::HTTP_UNAUTHORIZED);

        $topic = sprintf('%s/users/%s/notifications', rtrim($this->baseUrl, '/'), $user->getUuid());
        
        // Generate the JWT for Mercure subscription
        $token = $authorization->createCookie($this->getHubRequest($topic));

        return $this->json([
            'hubUrl' => $this->getParameter('mercure.default_hub'),
            'topic' => $topic,
            'token' => $token->getValue()
        ]);
    }

    private function getHubRequest(string $topic): \Symfony\Component\HttpFoundation\Request
    {
        $request = new \Symfony\Component\HttpFoundation\Request();
        $request->attributes->set('_mercure_subscribe', [$topic]);
        return $request;
    }

    #[Route('/{uuid}/read', name: 'mark_as_read', methods: ['PATCH'])]
    public function markAsRead(string $uuid, EntityManagerInterface $entityManager): JsonResponse
    {
        /** @var User $user */
        $user = $this->getUser();
        $notification = $entityManager->getRepository(Notification::class)->findOneBy(['uuid' => $uuid, 'user' => $user]);

        if (!$notification) return $this->json(['message' => 'Notification not found'], Response::HTTP_NOT_FOUND);

        $notification->setIsRead(true);
        $entityManager->flush();

        return $this->json(['message' => 'Notification marked as read']);
    }
}
