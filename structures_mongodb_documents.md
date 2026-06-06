# Description des Structures de Documents MongoDB (Schémas NoSQL)

Ce document répertorie et détaille la structure logique (schémas NoSQL sous forme de JSON) des 3 collections MongoDB utilisées dans le projet **Organ**. Bien que MongoDB soit une base de données orientée documents ("schemaless"), l'application utilise l'ODM Doctrine pour mapper ces structures à des classes PHP.

---

## Table des Matières
1. [Collection `audit_logs` (AuditLog)](#1-collection-audit_logs--auditlog)
2. [Collection `daily_stats` (DailyStat)](#2-collection-daily_stats--dailystat)
3. [Collection `file_storage` (FileStorage)](#3-collection-file_storage--filestorage)

---

## 1. Collection `audit_logs` : Journal d'Audit

*   **Classe PHP** : `App\Document\AuditLog`
*   **Description** : Enregistre l'ensemble des actions et modifications réalisées au sein de l'application (créations de tâches, ajouts de commentaires, transitions de statuts, etc.) à des fins d'analyse, d'historique et d'agrégation de statistiques.
*   **Index définis** :
    *   `taskUuid: 1` (ordre croissant) + `createdAt: -1` (ordre décroissant)
    *   `organUuid: 1` (ordre croissant) + `createdAt: -1` (ordre décroissant)
    *   `projectUuid: 1` (ordre croissant) + `createdAt: -1` (ordre décroissant)
    *   `userUuid: 1` (ordre croissant)

### Structure Logique (Schéma JSON explicatif)

```json
{
  "_id": {
    "type": "BSON ObjectId",
    "description": "Identifiant unique interne du log"
  },
  "taskUuid": {
    "type": "string | null",
    "description": "UUID de la tâche concernée par l'action (le cas échéant)"
  },
  "organUuid": {
    "type": "string | null",
    "description": "UUID de l'organe concerné par l'action (le cas échéant)"
  },
  "projectUuid": {
    "type": "string | null",
    "description": "UUID du projet global de l'action (obligatoire pour filtrer par projet)"
  },
  "userUuid": {
    "type": "string",
    "description": "UUID de l'utilisateur ayant déclenché l'action"
  },
  "actionType": {
    "type": "string",
    "description": "Type d'action réalisée (Exemples : 'CREATE', 'COMMENT_ADD', 'ATTACHMENT_ADD', 'STATUS_CHANGE', 'CONSULTATION')"
  },
  "fieldName": {
    "type": "string | null",
    "description": "Nom du champ modifié lors d'une mise à jour (ex: 'status', 'title')"
  },
  "oldValues": {
    "type": "object (hash) | null",
    "description": "Clés/Valeurs représentant l'état du document avant l'action"
  },
  "newValues": {
    "type": "object (hash) | null",
    "description": "Clés/Valeurs représentant le nouvel état du document après l'action"
  },
  "context": {
    "type": "object (hash) | null",
    "description": "Détails contextuels supplémentaires liés à l'action (ex: données du navigateur, IP, etc.)"
  },
  "createdAt": {
    "type": "BSON UTCDateTime",
    "description": "Date et heure de création de l'enregistrement d'audit"
  }
}
```

### Exemple Réel de Document JSON

```json
{
  "_id": {
    "$oid": "6661a35efe2bc7a34fa9b724"
  },
  "taskUuid": "8f8e02b2-8419-482a-a92c-567c132890ef",
  "organUuid": "a244c017-bfd1-460d-85e6-427bb0337894",
  "projectUuid": "f199859f-d31c-43db-b27b-99d63c5d8001",
  "userUuid": "c2005423-f32f-488f-b98a-227bb8f1ab33",
  "actionType": "STATUS_CHANGE",
  "fieldName": "status",
  "oldValues": {
    "status": "TODO"
  },
  "newValues": {
    "status": "DONE"
  },
  "context": {
    "userAgent": "Mozilla/5.0 (X11; Linux x86_64)...",
    "ip": "172.18.0.1"
  },
  "createdAt": {
    "$date": "2026-06-06T10:09:44.000Z"
  }
}
```

---

## 2. Collection `daily_stats` : Statistiques Quotidiennes

*   **Classe PHP** : `App\Document\DailyStat`
*   **Description** : Contient les statistiques consolidées de fin de journée. Chaque document représente une agrégation de l'activité (créations de tâches, complétions, volumes de fichiers joints, etc.) pour un jour donné, soit à l'échelle d'un projet global (si `organUuid` est `null`), soit à l'échelle d'un organe précis.
*   **Index définis** :
    *   `UniqueIndex`: `date: 1` + `projectUuid: 1` + `organUuid: 1` (Empêche les doublons pour un même contexte temporel et spatial)

### Structure Logique (Schéma JSON explicatif)

```json
{
  "_id": {
    "type": "BSON ObjectId",
    "description": "Identifiant unique interne de la statistique quotidienne"
  },
  "date": {
    "type": "BSON UTCDateTime",
    "description": "Date de la journée de statistiques (minuit UTC)"
  },
  "projectUuid": {
    "type": "string",
    "description": "UUID du projet associé aux statistiques"
  },
  "organUuid": {
    "type": "string | null",
    "description": "UUID de l'organe si la statistique est sectorisée par organe, sinon null pour les statistiques globales du projet"
  },
  "tasksCreated": {
    "type": "integer",
    "description": "Nombre total de tâches créées au cours de cette journée"
  },
  "tasksCompleted": {
    "type": "integer",
    "description": "Nombre total de tâches marquées comme terminées ('DONE') au cours de cette journée"
  },
  "tasksCanceled": {
    "type": "integer",
    "description": "Nombre total de tâches annulées ('CANCELED') au cours de cette journée"
  },
  "commentsAdded": {
    "type": "integer",
    "description": "Nombre de commentaires ajoutés au cours de cette journée"
  },
  "attachmentsAdded": {
    "type": "integer",
    "description": "Nombre de pièces jointes ajoutées au cours de cette journée"
  },
  "attachmentsSize": {
    "type": "integer",
    "description": "Taille cumulée (en octets) des pièces jointes ajoutées au cours de cette journée"
  },
  "membersActive": {
    "type": "integer",
    "description": "Nombre d'utilisateurs uniques ayant interagi/été actifs sur ce projet/organe durant la journée"
  },
  "statusChanges": {
    "type": "object (hash)",
    "description": "Compteur détaillé des transitions de statuts (ex: { 'TODO_TO_DOING': 3, 'DOING_TO_DONE': 2 })"
  },
  "extra": {
    "type": "object (hash)",
    "description": "Métadonnées libres additionnelles utilisées pour des évolutions futures sans modification de schéma"
  }
}
```

### Exemple Réel de Document JSON

```json
{
  "_id": {
    "$oid": "6661aa44fe2bc7a34fa9b802"
  },
  "date": {
    "$date": "2026-06-05T00:00:00.000Z"
  },
  "projectUuid": "f199859f-d31c-43db-b27b-99d63c5d8001",
  "organUuid": "a244c017-bfd1-460d-85e6-427bb0337894",
  "tasksCreated": 5,
  "tasksCompleted": 3,
  "tasksCanceled": 0,
  "commentsAdded": 12,
  "attachmentsAdded": 2,
  "attachmentsSize": 2048576,
  "membersActive": 4,
  "statusChanges": {
    "TODO_TO_DOING": 4,
    "DOING_TO_DONE": 3
  },
  "extra": {
    "reaggregation": false,
    "triggeredBy": "CronSystem"
  }
}
```

---

## 3. Collection `file_storage` : Stockage Binaire de Fichiers

*   **Classe PHP** : `App\Document\FileStorage`
*   **Description** : Gère le stockage direct des fichiers binaires joints aux tâches. Permet d'éviter de stocker les fichiers lourds dans le système de fichiers classique du conteneur en exploitant la flexibilité du stockage binaire BSON MongoDB.
*   **Index définis** :
    *   Index par défaut sur `_id` uniquement.

### Structure Logique (Schéma JSON explicatif)

```json
{
  "_id": {
    "type": "BSON ObjectId",
    "description": "Identifiant unique interne du fichier stocké"
  },
  "taskAttachmentUuid": {
    "type": "string",
    "description": "UUID de l'entité TaskAttachment liée pour faire correspondre le document au modèle relationnel SQL"
  },
  "filename": {
    "type": "string",
    "description": "Nom d'origine du fichier (ex: 'rapport_mensuel.pdf')"
  },
  "mimeType": {
    "type": "string",
    "description": "Type MIME officiel du fichier (ex: 'application/pdf', 'image/png')"
  },
  "size": {
    "type": "integer",
    "description": "Taille du fichier en octets"
  },
  "checksum": {
    "type": "string",
    "description": "Somme de contrôle de sécurité SHA-256 du fichier pour détecter les doublons ou corruptions"
  },
  "content": {
    "type": "BSON Binary (binData)",
    "description": "Contenu brut du fichier encodé en binaire (Subtype 0)"
  },
  "metadata": {
    "type": "object (hash) | null",
    "description": "Métadonnées spécifiques au fichier (ex: dimensions pour les images, durée pour les audios)"
  },
  "uploadedAt": {
    "type": "BSON UTCDateTime",
    "description": "Date et heure de téléversement du fichier"
  },
  "version": {
    "type": "integer",
    "description": "Numéro de version du document (valeur par défaut : 1)"
  }
}
```

### Exemple Réel de Document JSON

```json
{
  "_id": {
    "$oid": "6661b12efe2bc7a34fa9b951"
  },
  "taskAttachmentUuid": "e7c65cde-4541-4564-8393-27a9cf1863db",
  "filename": "workspace_mockup.png",
  "mimeType": "image/png",
  "size": 154820,
  "checksum": "d5a6f23c911db694aa2b20a324b13a8f4c2049e39401bfd92dcd6e87f8931102",
  "content": {
    "$binary": {
      "base64": "iVBORw0KGgoAAAANSUhEUgAAAlgAAAGQCAYAAA...",
      "subType": "00"
    }
  },
  "metadata": {
    "width": 1920,
    "height": 1080,
    "dpi": 72
  },
  "uploadedAt": {
    "$date": "2026-06-06T10:09:44.000Z"
  },
  "version": 1
}
```
