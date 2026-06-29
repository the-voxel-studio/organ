<?php

declare(strict_types=1);

namespace App\EventListener;

use App\Service\AuditLogService;
use Symfony\Component\EventDispatcher\Attribute\AsEventListener;
use Symfony\Component\HttpKernel\Event\TerminateEvent;
use Symfony\Component\HttpKernel\KernelEvents;

final class AuditLogTerminateListener
{
    public function __construct(
        private readonly AuditLogService $auditLogService
    ) {}

    #[AsEventListener(event: KernelEvents::TERMINATE)]
    public function onKernelTerminate(TerminateEvent $event): void
    {
        $this->auditLogService->flushBuffer();
    }
}
