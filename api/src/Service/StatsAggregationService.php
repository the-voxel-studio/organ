<?php

namespace App\Service;

use App\Document\AuditLog;
use App\Document\DailyStat;
use Doctrine\ODM\MongoDB\DocumentManager;

class StatsAggregationService
{
    public function __construct(
        private DocumentManager $dm
    ) {}

    public function aggregateForDate(\DateTimeInterface $date): void
    {
        $start = (clone $date)->setTime(0, 0, 0);
        $end = (clone $date)->setTime(23, 59, 59);

        // Fetch all audit logs for this day
        $logs = $this->dm->getRepository(AuditLog::class)->createQueryBuilder()
            ->field('createdAt')->gte($start)
            ->field('createdAt')->lte($end)
            ->getQuery()
            ->execute();

        $statsMap = []; // [projectId][organId] => DailyStat

        foreach ($logs as $log) {
            $pUuid = $log->getProjectUuid();
            $oUuid = $log->getOrganUuid();

            // Project level
            $this->updateStat($statsMap, $pUuid, null, $log, $start);
            // Organ level
            $this->updateStat($statsMap, $pUuid, $oUuid, $log, $start);
        }

        foreach ($statsMap as $pUuid => $organs) {
            foreach ($organs as $oUuid => $stat) {
                $this->dm->persist($stat);
            }
        }

        $this->dm->flush();
    }

    private function updateStat(array &$statsMap, string $projectUuid, ?string $organUuid, AuditLog $log, \DateTimeInterface $date): void
    {
        if (!isset($statsMap[$projectUuid])) {
            $statsMap[$projectUuid] = [];
        }

        $key = $organUuid ?? 'GLOBAL';
        if (!isset($statsMap[$projectUuid][$key])) {
            // Try to find existing stat in DB to upsert
            $existing = $this->dm->getRepository(DailyStat::class)->findOneBy([
                'date' => $date,
                'projectUuid' => $projectUuid,
                'organUuid' => $organUuid
            ]);

            if ($existing) {
                $statsMap[$projectUuid][$key] = $existing;
            } else {
                $stat = new DailyStat();
                $stat->setDate($date);
                $stat->setProjectUuid($projectUuid);
                $stat->setOrganUuid($organUuid);
                $statsMap[$projectUuid][$key] = $stat;
            }
        }

        /** @var DailyStat $stat */
        $stat = $statsMap[$projectUuid][$key];

        switch ($log->getActionType()) {
            case 'CREATE':
                $stat->setTasksCreated($stat->getTasksCreated() + 1);
                break;
            case 'STATUS_CHANGE':
                $newVal = $log->getNewValues()['status'] ?? null;
                if ($newVal === 'DONE') {
                    $stat->setTasksCompleted($stat->getTasksCompleted() + 1);
                } elseif ($newVal === 'CANCELED') {
                    $stat->setTasksCanceled($stat->getTasksCanceled() + 1);
                }
                
                $oldVal = $log->getOldValues()['status'] ?? 'UNKNOWN';
                $transition = "{$oldVal}_TO_{$newVal}";
                $changes = $stat->getStatusChanges();
                $changes[$transition] = ($changes[$transition] ?? 0) + 1;
                $stat->setStatusChanges($changes);
                break;
            case 'COMMENT_ADD':
                $stat->setCommentsAdded($stat->getCommentsAdded() + 1);
                break;
            case 'ATTACHMENT_ADD':
                $stat->setAttachmentsAdded($stat->getAttachmentsAdded() + 1);
                break;
            case 'CONSULTATION':
                $extra = $stat->getExtra();
                $extra['consultations'] = ($extra['consultations'] ?? 0) + 1;
                $stat->setExtra($extra);
                break;
        }
        
        // Tracking active members
        $extra = $stat->getExtra();
        if (!isset($extra['active_users'])) {
            $extra['active_users'] = [];
        }
        if ($log->getUserUuid() && !in_array($log->getUserUuid(), $extra['active_users'])) {
            $extra['active_users'][] = $log->getUserUuid();
            $stat->setMembersActive(count($extra['active_users']));
            $stat->setExtra($extra);
        }
    }
}
