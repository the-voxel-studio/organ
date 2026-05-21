<?php

namespace App\Service;

use App\Document\AuditLog;
use App\Entity\Organ;
use App\Entity\Project;
use App\Entity\Task;
use App\Entity\User;
use Doctrine\ODM\MongoDB\DocumentManager;
use Symfony\Component\HttpFoundation\RequestStack;

class AuditLogService
{
    public function __construct(
        private DocumentManager $dm,
        private RequestStack $requestStack
    ) {}

    public function log(
        ?Task $task,
        ?User $user,
        string $actionType,
        ?string $fieldName = null,
        ?array $oldValues = null,
        ?array $newValues = null,
        ?Organ $organ = null,
        ?Project $project = null
    ): void {
        $request = $this->requestStack->getCurrentRequest();
        
        // Resolve hierarchy
        if ($task) {
            $organ = $task->getOrgan();
            $project = $organ->getProject();
        } elseif ($organ) {
            $project = $organ->getProject();
        }

        $auditLog = new AuditLog();
        if ($task) {
            $auditLog->setTaskUuid($task->getUuid());
        }
        if ($organ) {
            $auditLog->setOrganUuid($organ->getUuid());
        }
        if ($project) {
            $auditLog->setProjectUuid($project->getUuid());
        }
        
        if ($user) {
            $auditLog->setUserUuid($user->getUuid());
        }

        $auditLog->setActionType($actionType);
        $auditLog->setFieldName($fieldName);
        $auditLog->setOldValues($oldValues);
        $auditLog->setNewValues($newValues);

        $auditLog->setContext([
            'ip' => $request?->getClientIp(),
            'user_agent' => $request?->headers->get('User-Agent'),
            'route' => $request?->attributes->get('_route'),
            'method' => $request?->getMethod(),
        ]);

        $this->dm->persist($auditLog);
        $this->dm->flush();
    }

    public function logConsultation(
        ?User $user,
        ?Project $project = null,
        ?Organ $organ = null,
        ?Task $task = null
    ): void {
        $this->log($task, $user, 'CONSULTATION', null, null, null, $organ, $project);
    }
}
