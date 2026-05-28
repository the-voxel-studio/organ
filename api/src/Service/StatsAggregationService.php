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

        $collection = $this->dm->getDocumentCollection(AuditLog::class);

        $pipeline = [
            ['$match' => [
                'createdAt' => [
                    '$gte' => new \MongoDB\BSON\UTCDateTime($start),
                    '$lte' => new \MongoDB\BSON\UTCDateTime($end)
                ]
            ]],
            ['$project' => [
                'userUuid' => '$userUuid',
                'actionType' => '$actionType',
                'newValues' => '$newValues',
                'oldValues' => '$oldValues',
                'taskUuid' => '$taskUuid',
                'targets' => ['$cond' => [
                    ['$ne' => [['$ifNull' => ['$organUuid', null]], null]],
                    [
                        ['projectUuid' => '$projectUuid', 'organUuid' => null],
                        ['projectUuid' => '$projectUuid', 'organUuid' => ['$ifNull' => ['$organUuid', null]]]
                    ],
                    [
                        ['projectUuid' => '$projectUuid', 'organUuid' => null]
                    ]
                ]]
            ]],
            ['$unwind' => '$targets'],
            ['$group' => [
                '_id' => [
                    'projectUuid' => '$targets.projectUuid',
                    'organUuid' => '$targets.organUuid'
                ],
                'tasksCreated' => ['$sum' => ['$cond' => [['$eq' => ['$actionType', 'CREATE']], 1, 0]]],
                'commentsAdded' => ['$sum' => ['$cond' => [['$eq' => ['$actionType', 'COMMENT_ADD']], 1, 0]]],
                'attachmentsAdded' => ['$sum' => ['$cond' => [['$eq' => ['$actionType', 'ATTACHMENT_ADD']], 1, 0]]],
                'consultations' => ['$sum' => ['$cond' => [['$eq' => ['$actionType', 'CONSULTATION']], 1, 0]]],
                'taskViews' => ['$sum' => ['$cond' => [
                    ['$and' => [
                        ['$eq' => ['$actionType', 'CONSULTATION']],
                        ['$ne' => ['$taskUuid', null]]
                    ]], 1, 0
                ]]],
                'organViews' => ['$sum' => ['$cond' => [
                    ['$and' => [
                        ['$eq' => ['$actionType', 'CONSULTATION']],
                        ['$eq' => ['$taskUuid', null]],
                        ['$ne' => ['$targets.organUuid', null]]
                    ]], 1, 0
                ]]],
                'projectViews' => ['$sum' => ['$cond' => [
                    ['$and' => [
                        ['$eq' => ['$actionType', 'CONSULTATION']],
                        ['$eq' => ['$taskUuid', null]],
                        ['$eq' => ['$targets.organUuid', null]]
                    ]], 1, 0
                ]]],
                'tasksCompleted' => ['$sum' => ['$cond' => [
                    ['$and' => [
                        ['$eq' => ['$actionType', 'STATUS_CHANGE']],
                        ['$eq' => ['$newValues.status', 'DONE']]
                    ]], 1, 0
                ]]],
                'tasksCanceled' => ['$sum' => ['$cond' => [
                    ['$and' => [
                        ['$eq' => ['$actionType', 'STATUS_CHANGE']],
                        ['$eq' => ['$newValues.status', 'CANCELED']]
                    ]], 1, 0
                ]]],
                'activeUsers' => ['$addToSet' => '$userUuid'],
                'statusTransitions' => ['$push' => ['$cond' => [
                    ['$eq' => ['$actionType', 'STATUS_CHANGE']],
                    ['$concat' => [
                        ['$ifNull' => ['$oldValues.status', 'UNKNOWN']],
                        '_TO_',
                        ['$ifNull' => ['$newValues.status', 'UNKNOWN']]
                    ]],
                    '$$REMOVE'
                ]]]
            ]]
        ];

        $cursor = $collection->aggregate($pipeline);

        $statsToPersist = [];

        foreach ($cursor as $row) {
            $pUuid = $row['_id']['projectUuid'] ?? null;
            $oUuid = $row['_id']['organUuid'] ?? null;
            if (!$pUuid) {
                continue;
            }

            $cacheKey = sprintf('%s_%s', $pUuid, $oUuid ?? 'null');

            if (isset($statsToPersist[$cacheKey])) {
                $stat = $statsToPersist[$cacheKey];
            } else {
                // Find or create DailyStat document
                $existing = $this->dm->getRepository(DailyStat::class)->findOneBy([
                    'date' => $start,
                    'projectUuid' => $pUuid,
                    'organUuid' => $oUuid
                ]);

                $stat = $existing ?? new DailyStat();
                if (!$existing) {
                    $stat->setDate($start);
                    $stat->setProjectUuid($pUuid);
                    $stat->setOrganUuid($oUuid);
                }
                $statsToPersist[$cacheKey] = $stat;
            }

            $stat->setTasksCreated($stat->getTasksCreated() + ($row['tasksCreated'] ?? 0));
            $stat->setTasksCompleted($stat->getTasksCompleted() + ($row['tasksCompleted'] ?? 0));
            $stat->setTasksCanceled($stat->getTasksCanceled() + ($row['tasksCanceled'] ?? 0));
            $stat->setCommentsAdded($stat->getCommentsAdded() + ($row['commentsAdded'] ?? 0));
            $stat->setAttachmentsAdded($stat->getAttachmentsAdded() + ($row['attachmentsAdded'] ?? 0));

            $activeUsers = isset($row['activeUsers']) ? (array)$row['activeUsers'] : [];
            $existingExtra = $stat->getExtra();
            $existingActiveUsers = $existingExtra['active_users'] ?? [];
            $allActiveUsers = array_unique(array_merge($existingActiveUsers, $activeUsers));
            $stat->setMembersActive(count($allActiveUsers));

            $statusChanges = $stat->getStatusChanges();
            $statusTransitions = isset($row['statusTransitions']) ? (array)$row['statusTransitions'] : [];
            foreach ($statusTransitions as $transition) {
                $statusChanges[$transition] = ($statusChanges[$transition] ?? 0) + 1;
            }
            $stat->setStatusChanges($statusChanges);

            $extra = [
                'consultations' => ($existingExtra['consultations'] ?? 0) + ($row['consultations'] ?? 0),
                'task_views' => ($existingExtra['task_views'] ?? 0) + ($row['taskViews'] ?? 0),
                'organ_views' => ($existingExtra['organ_views'] ?? 0) + ($row['organViews'] ?? 0),
                'project_views' => ($existingExtra['project_views'] ?? 0) + ($row['projectViews'] ?? 0),
                'active_users' => $allActiveUsers
            ];
            $stat->setExtra($extra);

            $this->dm->persist($stat);
        }

        $this->dm->flush();
    }
}
