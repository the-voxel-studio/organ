# Plan d'Évolution du Projet : Organ (Phase WE4B & SI40-B)

Ce document détaille la stratégie technique pour l'intégration du framework **Angular** (UE WE4B) et de la base de données **NoSQL** (UE SI40), conformément aux exigences académiques.

---

## 1. Architecture Logicielle (WE4B)
L'objectif est de transformer l'interface actuelle (générée en Twig/Stimulus) en une **Single Page Application (SPA)** fluide.

### Composants et Modularité
- **Refonte UI** : Création de composants Angular réutilisables (`TaskCard`, `UserAvatar`, `ProjectSidebar`, `KanbanBoard`).
- **Communication** : Utilisation intensive des décorateurs `@Input` et `@Output` pour la gestion des données entre composants parents et enfants.
- **Routage Dynamique** : Mise en place de `RouterModule` pour gérer les vues par projet et par organe via des paramètres d'URL (`/projects/:projectUuid/organs/:organUuid`).

### Services et Logique Métier
- **HttpClient** : Centralisation des appels API vers le backend Symfony existant.
- **RxJS** : Gestion des flux de données asynchrones (Observables) pour les notifications en temps réel et les mises à jour de listes.
- **Guards** : Implémentation de `CanActivate` pour protéger les routes en fonction des rles JWT et des permissions récupérées depuis l'API.

---

## 2. Base de Données NoSQL (SI40)
L'intégration de **MongoDB** viendra compléter MySQL pour gérer les données non structurées, les historiques à fort volume et l'analyse de performance.

### A. Stockage des Historiques et Logs (Obligatoire)
Migration de l'historique des actions (création, modification, suppression) vers MongoDB.
- **Format JSON** : Chaque document stockera l'empreinte complète d'une action, incluant le `diff` des données (old vs new), l'utilisateur, l'IP, et le User-Agent.
- **Justification** : Flexibilité du schéma pour des actions variées et performance lors de l'insertion de logs massifs.

### B. Métadonnées de Fichiers Avancées
Enrichissement de la gestion des pièces jointes via NoSQL.
- **Données Semi-structurées** : Stockage des métadonnées spécifiques au type de fichier (ex: EXIF pour les images, nombre de pages pour les PDF, durée pour les vidéos).
- **Audit & Sécurité** : Conservation d'un historique d'accès (qui a téléchargé quoi et quand) dans une collection NoSQL dédiée.

### C. Analyse des Performances et Statistiques (Insights)
Création d'un moteur d'analyse pour aider à la décision au sein des projets.
- **Aggregation Pipelines** : Utilisation du framework d'agrégation de MongoDB pour calculer des indicateurs clés (KPIs) :
    - **Velocity Chart** : Nombre de tâches terminées par unité de temps.
    - **Cycle Time** : Temps moyen passé par une tâche dans chaque statut.
    - **Bottleneck Detection** : Identification des étapes de l'organe où les tâches stagnent le plus longtemps.
- **Visualisation** : Intégration de graphiques dynamiques dans le frontend Angular (via Chart.js) consommant les pipelines d'agrégation NoSQL.

---

## 3. Modifications API & Backend (Symfony)
L'intégration du NoSQL et le passage à Angular nécessitent des évolutions structurelles au niveau de l'API.

### Plugins et Dépendances
- **Doctrine MongoDB ODM** : Installation de `doctrine/mongodb-odm-bundle` pour mapper les documents JSON vers des objets PHP.
- **API Documentation** : Mise à jour de `nelmio/api-doc-bundle` pour inclure les nouveaux schémas de données NoSQL (Analytics & Logs).

### Modifications des Routes Existantes
Les routes de modification de données devront désormais déclencher des évènements de log NoSQL :
- **Hooks d'Activité** : Refonte de `TaskController` et `TaskAttachmentController` pour injecter un `ActivityLoggerService`. Chaque action réussie (POST/PUT/DELETE) enverra un document asynchrone vers MongoDB.
- **Dénormalisation** : Certaines routes GET seront optimisées pour renvoyer des données agrégées (SQL + NoSQL) en une seule requête pour le frontend Angular.

### Nouvelles Routes API (Spécifiques SI40-B)
Les nouvelles routes suivront la hiérarchie RESTful déjà établie dans le projet :
- **Analytiques d'Organe** :
    - `/api/projects/{projectUuid}/organs/{organUuid}/analytics/velocity`
    - `/api/projects/{projectUuid}/organs/{organUuid}/analytics/cycle-time`
- **Audit de Tâche** :
    - `/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/audit-logs`
- **Métadonnées de Fichier** :
    - `/api/projects/{projectUuid}/organs/{organUuid}/tasks/{taskUuid}/attachments/{attachmentUuid}/metadata`

---

## 4. Stratégie d'Implémentation Hybride (Polyglot Persistence)
- **MySQL** : Reste la "Source de Vérité" pour les entités structurées (Users, Projects, Organs, Tasks).
- **MongoDB** : Devient la base de données pour l'intelligence métier (Analytics, Audit Trail, Logs).
- **Symfony (Backend)** : Utilisation simultanée de Doctrine ORM (SQL) et Doctrine ODM (NoSQL) pour orchestrer les données vers le client Angular.
