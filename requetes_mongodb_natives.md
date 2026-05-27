# Récapitulatif des Requêtes MongoDB Natives (Sans ODM)

Ce document répertorie les 4 requêtes complexes du projet refactorisées pour utiliser directement le pilote natif de MongoDB PHP (bypassant Doctrine ODM) et démontrant une maîtrise des concepts NoSQL avancés.

---

## 1. Requête 1 : Agrégation en temps réel des statistiques d'aujourd'hui

*   **Fichier** : ProjectController.php
*   **Méthode** : `stats`
*   **Description** : Calcule l'ensemble des indicateurs de la journée pour un projet (tâches créées, complétées, annulées, commentaires, pièces jointes, consultations, transitions de statuts et utilisateurs actifs) en **une seule opération de base de données**.
*   **Pipeline d'Agrégation** :
    1.  `$match` : Filtre les logs du jour pour le projet.
    2.  `$group` : Regroupement global (`_id: null`) utilisant des accumulateurs conditionnels :
        *   `$sum` avec `$cond` pour compter les types d'actions.
        *   `$sum` avec `$and` pour filtrer les transitions de statuts spécifiques (`STATUS_CHANGE` vers `DONE` ou `CANCELED`).
        *   `$addToSet` pour rassembler la liste unique des utilisateurs actifs.
        *   `$push` avec `$concat` et `$ifNull` pour générer la liste des transitions de statuts (`OLD_TO_NEW`).
*   **Code** :
    ```php
    $auditLogCollection = $this->dm->getDocumentCollection(\App\Document\AuditLog::class);
    
    $pipeline = [
        ['$match' => [
            'projectUuid' => $project->getUuid(),
            'createdAt' => [
                '$gte' => new \MongoDB\BSON\UTCDateTime($todayStart),
                '$lte' => new \MongoDB\BSON\UTCDateTime($todayEnd)
            ]
        ]],
        ['$group' => [
            '_id' => null,
            'tasksCreated' => ['$sum' => ['$cond' => [['$eq' => ['$actionType', 'CREATE']], 1, 0]]],
            'commentsAdded' => ['$sum' => ['$cond' => [['$eq' => ['$actionType', 'COMMENT_ADD']], 1, 0]]],
            'attachmentsAdded' => ['$sum' => ['$cond' => [['$eq' => ['$actionType', 'ATTACHMENT_ADD']], 1, 0]]],
            'consultations' => ['$sum' => ['$cond' => [['$eq' => ['$actionType', 'CONSULTATION']], 1, 0]]],
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

    $todayAggregationCursor = $auditLogCollection->aggregate($pipeline);
    ```

---

## 2. Requête 2 : Récupération des historiques de statistiques quotidiennes

*   **Fichier** : ProjectController.php
*   **Méthode** : `stats`
*   **Description** : Récupère l'historique des statistiques agrégées globales du projet (jusqu'à la veille). La requête utilise `find()` sur la collection native `daily_stats` avec des filtres temporels et un tri croissant sur la date (`sort => ['date' => 1]`). La date BSON `UTCDateTime` est convertie en objet `DateTime` classique en PHP avec la méthode `toDateTime()`.
*   **Code** :
    ```php
    $dailyStatCollection = $this->dm->getDocumentCollection(\App\Document\DailyStat::class);
    $stats = $dailyStatCollection->find(
        [
            'projectUuid' => $project->getUuid(),
            'organUuid' => null, // Global projet
            'date' => [
                '$gte' => new \MongoDB\BSON\UTCDateTime($start),
                '$lt' => new \MongoDB\BSON\UTCDateTime($todayStart)
            ]
        ],
        [
            'sort' => ['date' => 1]
        ]
    );
    ```

---

## 3. Requête 3 : Alimentation du journal d'audit paginé et filtré

*   **Fichier** : ProjectController.php
*   **Méthode** : `audit`
*   **Description** : Sert à alimenter le tableau de la timeline d'audit du projet avec tri, filtrage temporel et pagination.
    1.  Calcul du nombre total de documents correspondants (pour la pagination) via la méthode native `countDocuments()`.
    2.  Récupération des logs correspondants via `find()` en appliquant des options de tri décroissant, de limite (`limit`) et de décalage (`skip`).
    3.  L'identifiant unique MongoDB `_id` de type `ObjectId` est casté en chaîne de caractères via `(string)$log['_id']` pour le frontend.
*   **Code** :
    ```php
    // 1. Comptage total natif
    $totalCount = $collection->countDocuments($filter);

    // 2. Recherche et pagination natives
    $options = [
        'sort' => ['createdAt' => -1],
        'limit' => $limit,
        'skip' => $offset
    ];
    $logs = $collection->find($filter, $options);
    ```

---

## 4. Requête 4 : Agrégation de fin de journée avec duplication et dépliage

*   **Fichier** : StatsAggregationService.php
*   **Méthode** : `aggregateForDate`
*   **Description** : Agrège l'ensemble des logs d'une journée par Projet et par Organe en **une seule opération de base de données**. Cette requête utilise des concepts d'agrégation avancés :
    1.  `$match` : Filtre les logs de la journée ciblée.
    2.  `$project` (Duplication conditionnelle) : Projette chaque log dans un tableau de cibles (`targets`). Si le log a un `organUuid`, il est dupliqué sous deux cibles : une pour le niveau projet-global `{projectUuid, organUuid: null}`, et une pour le niveau organe `{projectUuid, organUuid: organUuid}`. Sinon, il ne génère qu'une seule cible (projet-global).
    3.  `$unwind` : Déplie le tableau `targets` pour matérialiser les duplicatas.
    4.  `$group` : Regroupe par cible (`_id: {projectUuid, organUuid}`) et calcule les indicateurs fins de la journée (créations, complétions, annulations, commentaires, ajouts de pièces jointes, utilisateurs uniques actifs, et répartition des vues projets, organes et tâches) de manière isolée et simultanée.
*   **Code** :
    ```php
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
                ['$ne' => ['$organUuid', null]],
                [
                    ['projectUuid' => '$projectUuid', 'organUuid' => null],
                    ['projectUuid' => '$projectUuid', 'organUuid' => '$organUuid']
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
    ```
