<?php

declare(strict_types=1);

namespace App\EventListener;

use App\Entity\User;
use App\Entity\UserSession;
use App\Repository\UserSessionRepository;
use App\Service\DeviceDetectorService;
use Doctrine\Bundle\DoctrineBundle\Attribute\AsEntityListener;
use Doctrine\ORM\Event\PrePersistEventArgs;
use Doctrine\ORM\Events;
use Symfony\Component\HttpFoundation\RequestStack;

#[AsEntityListener(event: Events::prePersist, method: 'prePersist', entity: UserSession::class)]
#[AsEntityListener(event: Events::preUpdate, method: 'preUpdate', entity: UserSession::class)]
class RefreshTokenListener
{
    public function __construct(
        private RequestStack $requestStack,
        private DeviceDetectorService $deviceDetector,
        private UserSessionRepository $userSessionRepository
    ) {}

    public function prePersist(UserSession $session, PrePersistEventArgs $event): void
    {
        $request = $this->requestStack->getCurrentRequest();
        if (!$request) {
            return;
        }

        $deviceInfo = $this->deviceDetector->getDeviceInfo($request);
        $user = $session->getUser();

        if (!$user instanceof User) {
            return;
        }

        // Remplir les données de l'appareil
        $session->setIpAddress($deviceInfo['ipAddress']);
        $session->setUserAgent($deviceInfo['userAgent']);
        $session->setDeviceName($deviceInfo['deviceName']);
        $session->setBrowserName($deviceInfo['browserName']);
        $session->setLocation($deviceInfo['location']);
        $session->setLastUsedAt(new \DateTime()); // Définit l'utilisation initiale

        // Logique "Un seul par appareil"
        $existingSession = $this->userSessionRepository->findExistingSession(
            $user,
            $deviceInfo['location'],
            $deviceInfo['deviceName'],
            $deviceInfo['browserName']
        );

        if ($existingSession) {
            $em = $event->getObjectManager();
            $em->remove($existingSession);
        }
    }

    public function preUpdate(UserSession $session): void
    {
        $session->setLastUsedAt(new \DateTime());
    }
}
