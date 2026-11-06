# Organ : Plateforme Collaborative de Gestion de Projets & d'Organs

[English](README.md) | **Français**

[![Licence: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![PHP](https://img.shields.io/badge/PHP-8.4-777BB4.svg?logo=php&logoColor=white)](https://www.php.net/)
[![Symfony](https://img.shields.io/badge/Symfony-8.0-black.svg?logo=symfony&logoColor=white)](https://symfony.com/)
[![Angular](https://img.shields.io/badge/Angular-21-DD0031.svg?logo=angular&logoColor=white)](https://angular.dev/)
[![Android](https://img.shields.io/badge/Android-Jetpack%20Compose-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com/compose)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg?logo=docker&logoColor=white)](https://www.docker.com/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1.svg?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![MongoDB](https://img.shields.io/badge/MongoDB-7.0-47A248.svg?logo=mongodb&logoColor=white)](https://www.mongodb.com/)
[![Valkey](https://img.shields.io/badge/Valkey-7.2-CC342D.svg?logo=redis&logoColor=white)](https://valkey.io/)
[![Mercure](https://img.shields.io/badge/Mercure-Realtime-00A389.svg)](https://mercure.rocks/)

---

## 📖 Sommaire

1. [Qu'est-ce qu'Organ ?](#-quest-ce-quorgan-)
   * [Genèse et Philosophie](#genèse-et-philosophie)
   * [Le Modèle Tripartite : Projet > Organ > Tâche](#le-modèle-tripartite--projet--organ--tâche)
   * [Liberté de Stockage : BYOS ("Bring Your Own Storage")](#liberté-de-stockage--byos-bring-your-own-storage)
   * [Sécurité & Permissions Granulaires (RBAC)](#sécurité--permissions-granulaires-rbac)
   * [Gestion de l'Urgence & Échelle Chromatique](#gestion-de-lurgence--échelle-chromatique)
   * [Traçabilité, Audit Log & Analytics](#traçabilité-audit-log--analytics)
   * [Assistant IA Contextuel Intégré](#assistant-ia-contextuel-intégré)
   * [Écosystème Multi-Plateforme](#écosystème-multi-plateforme)
2. [Architecture Technique & Microservices](#-architecture-technique--microservices)
   * [Cartographie des Services](#cartographie-des-services)
   * [Schéma de l'Architecture](#schéma-de-larchitecture)
3. [L'Assistant IA : une expérimentation semi-agentique](#-lassistant-ia--une-expérimentation-semi-agentique)
   * [Pourquoi « semi-agent » ?](#pourquoi--semi-agent--)
   * [Comment ça marche](#comment-ça-marche)
   * [Les garde-fous en place](#les-garde-fous-en-place)
   * [Les limites : un aller simple, sans retour](#les-limites--un-aller-simple-sans-retour)
   * [Ce qu'il faudrait pour un vrai agent](#ce-quil-faudrait-pour-un-vrai-agent)
4. [Configuration de l'Environnement](#-configuration-de-lenvironnement)
   * [1. Racine du projet (`.env`)](#1-racine-du-projet-env)
   * [2. API Backend (`api/.env`)](#2-api-backend-apienv)
   * [3. Frontend Angular (`front_angular/.env`)](#3-frontend-angular-front_angularenv)
   * [4. Frontend Historique PHP (`front_php/.env`)](#4-frontend-historique-php-front_phpenv)
5. [Tutoriel : Configuration Google Cloud (OAuth 2.0 & Drive)](#-tutoriel--configuration-google-cloud-oauth-20--drive)
6. [Installation et Lancement](#-installation-et-lancement)
7. [Application Mobile Android](#-application-mobile-android)
8. [Données de Démonstration (Seed) & Comptes de Test](#-données-de-démonstration-seed--comptes-de-test)
9. [Tests & Qualité du Code](#-tests--qualité-du-code)
10. [Licence](#-licence)

---

## 💡 Qu'est-ce qu'Organ ?

### Genèse et Philosophie

**Organ** est né d'un constat partagé par trois étudiants en informatique aux sensibilités complémentaires : le créatif guidé par le visuel, le rigoureux obsédé par la hiérarchie et l'efficace cherchant l'action immédiate.

Face aux solutions du marché, deux impasses majeures se présentaient régulièrement :
* **Le « Mur de Tâches »** : Les to-do lists classiques, simples au départ, qui s'étirent en listes interminables, anxiogènes et sans perspective d'ensemble.
* **Le « Labyrinthe »** : Les plateformes documentaires sur-imbriquées (pages dans des sous-pages à l'infini) où l'on finit par passer plus de temps à ranger et chercher qu'à réaliser.

Organ a été conçu pour réconcilier ces approches : **fournir une structure humaine, modulaire et hautement sécurisée**, sans jamais sacrifier la clarté visuelle ni la rapidité d'exécution.

---

### Le Modèle Tripartite : Projet > Organ > Tâche

Organ repose sur une hiérarchie stricte à 3 niveaux qui élimine le désordre cognitif :

```text
📁 PROJET (Vision Stratégique & Équipe Globale)
│
├── 🫀 ORGAN A (Module métier, pôle, sprint ou sous-projet dédié)
│    ├── 📋 Tâche 1 (Priorité 8/10, échéance, validation requise)
│    ├── 📋 Tâche 2 (Dépendance : bloquée par la Tâche 1)
│    └── 📋 Tâche 3
│
└── 🫀 ORGAN B (Module externe accessible à des prestataires ou clients)
     ├── 📋 Tâche 4
     └── 📋 Tâche 5
```

1. **Le Projet :** L'enveloppe globale de haut niveau (titre, description, code couleur d'identité, icône ou emoji personnalisé, stockage Google Drive associé et équipe globale).
2. **L'Organ :** L'unité fonctionnelle autonome au sein du projet. Un Organ peut représenter un pôle métier (*Design*, *Backend*, *Marketing*), un jalon (*Phase 1*, *Sprint 3*), ou un espace de sous-traitance dédié. Chaque Organ dispose de son propre **tableau Kanban**, de son panneau de liens Web rapides et surtout de son **système de rôles et de permissions local**.
3. **La Tâche (Task) :** La brique de travail atomique, enrichie pour une efficacité maximale :
   * Statut dynamique (`À faire`, `En cours`, `En attente`, `Terminé`, `Annulé`) avec message explicatif d'état.
   * Évaluation de priorité de 1 à 10.
   * Dates de début et d'échéance précises avec estimation en heures.
   * Rôles dédiés par tâche : créateur, responsable technique (*manager*), et validateur officiel (*sign-off*).
   * Système de dépendances intelligentes : une tâche indique clairement si elle dépend d'une autre avant de pouvoir être initiée.
   * Tags transversaux colorés par projet, commentaires d'équipe et pièces jointes.

---

### Liberté de Stockage : BYOS ("Bring Your Own Storage")

Contrairement aux applications SaaS traditionnelles qui exploitent l'effet de verrouillage propriétaire (*vendor lock-in*) et facturent des abonnements au gigaoctet, Organ promeut le principe du **BYOS (Bring Your Own Storage)** :

* **Zéro surcoût de stockage :** Vous pouvez connecter directement votre compte **Google Drive** à vos projets Organ. Vos fichiers restent stockés sur votre propre espace cloud, sous votre contrôle juridique et technique.
* **Architecture Hybride :** Les pièces jointes peuvent être stockées soit directement sur l'infrastructure Organ (dans la base NoSQL **MongoDB GridFS/Documents**), soit sur votre espace **Google Drive** personnel ou d'entreprise (`drive://<fileId>`).
* **Intégrité & Versioning :** Chaque fichier uploadé fait l'objet d'un calcul de hachage **SHA-256**, d'un suivi de taille et de type MIME, ainsi que d'un numéro de version pour garantir la traçabilité des livrables.

---

### Sécurité & Permissions Granulaires (RBAC)

Organ dispose d'un moteur de permissions parmi les plus complets du secteur, conçu pour collaborer en toute confiance avec des équipes mixtes (salariés, freelances, clients, sous-traitants) :

* **Cloisonnement strict par Organ :** Un utilisateur invité sur un projet peut n'avoir accès qu'à un seul Organ spécifique. L'existence des autres Organs du projet lui demeure totalement invisible.
* **Rôles Globaux vs Rôles Locaux :**
  * *Rôles globaux de projet* : `ADMIN`, `MANAGER`, `MEMBER`.
  * *Rôles locaux d'Organ* : définis sur-mesure par les gestionnaires de l'Organ avec un nom et une icône dédiée.
* **Plus de 40 permissions unitaires paramétrables :**
  * *Gestion de l'Organ* : consultation, modification, gestion des membres locaux, configuration des rôles, gestion des liens.
  * *Gestion des Tâches* : création, modification (uniquement les siennes vs toutes), suppression logique (*soft delete*) ou définitive (*hard delete*), changement de statut, modification de priorité, planification des dates, modification des estimations d'heures.
  * *Affectation et Workflow* : s'auto-assigner, assigner des tiers, valider officiellement une tâche (`TASK_VALIDATE`).
  * *Ressources liées* : gestion des pièces jointes, des liens Web, des tags et des dépendances entre tâches.
  * *Commentaires* : rédaction, édition de ses propres commentaires vs modération de tous les messages.
* **Protection renforcée contre les failles d'accès :** L'API intègre une vérification systématique contre les IDOR (*Insecure Direct Object Reference*), le *mass assignment*, et garantit l'invalidation automatique du cache en cas de révocation de droits.
* **Cycle de vie sécurisé (Corbeille à 2 niveaux) :** Suppression douce (*soft delete*) permettant la restauration depuis la corbeille locale d'un Organ ou la corbeille globale d'un projet, avec séparation stricte des droits de purge définitive.

---

### Gestion de l'Urgence & Échelle Chromatique

Organ évite la surcharge mentale grâce à un traitement visuel immédiat :
* Une notation de priorité calibrée de **1 (très basse) à 10 (urgente / critique)**.
* Une palette chromatique dynamique allant du rose pastel (*bubblegum*) au rouge écarlate : le tableau de bord et les cartes mettent instantanément en relief les urgences sans nécessiter de lecture fastidieuse.
* La détection des tâches bloquées : si une dépendance n'est pas soldée, le collaborateur sait immédiatement pourquoi il doit attendre sans perdre de temps en réunions de synchronisation.

---

### Traçabilité, Audit Log & Analytics

Toutes les actions clés réalisées au sein de l'espace de travail sont historisées dans une collection **MongoDB dédiée** (`AuditLog`) :
* Suivi des événements : création, consultation, modification de statuts, ajustement de délais, attribution de managers, ajouts de pièces jointes.
* Historique temporel (*timeline*) consultable directement sur la vue détaillée de chaque tâche.
* Agrégation quotidienne de KPI (`DailyStat`) exécutée par commande cron ou déclenchement manuel, permettant d'afficher sur le frontend :
  * Un graphique linéaire d'activité globale du projet.
  * Un diagramme en anneau (*doughnut chart*) de répartition des statuts de tâches.
  * Un diagramme en barres de la charge de travail par Organ.
  * Des métriques clés de consultation et d'engagement de l'équipe.

---

### Assistant IA Contextuel Intégré

Organ intègre un assistant virtuel branché sur un LLM *Mistral AI* via **NVIDIA NIM** (API compatible OpenAI). Il ne se contente pas de répondre : il peut **produire une suite d'actions** (créer un projet, des Organs, des rôles, des tâches, naviguer) que l'application exécute ensuite pour l'utilisateur.
* **Contexte injecté :** page courante, projets accessibles, Organs, rôles et membres du projet ouvert.
* **Mode pédagogique (`/ask`) :** explique pas à pas comment faire soi-même (par exemple inviter un collaborateur externe dans un Organ).
* **Modes actifs (`/create`, `/edit`, `/role`) :** génère un plan d'actions exécuté automatiquement, avec un interrupteur « lecture seule » pour le désactiver.

> [!NOTE]
> C'est une **expérimentation étudiante**, une première approche, encore rudimentaire, de l'IA agentique. Son fonctionnement et ses défauts (importants) sont détaillés dans la section [L'Assistant IA : une expérimentation semi-agentique](#-lassistant-ia--une-expérimentation-semi-agentique).

---

### Écosystème Multi-Plateforme

L'expérience Organ est accessible sur tous les supports grâce à une flotte de clients adaptés :

* **Web Moderne (Frontend Principal - Angular 21) :**
  * Single Page Application ultra-fluide construite sous **Angular 21** et **Tailwind CSS 4**.
  * Rendu réactif, animations GSAP, tableaux Kanban interactifs, visualisation Chart.js, panneau d'administration des rôles et assistant IA intégré.
* **Mobile Android Natif :**
  * Application développée en **Kotlin** avec l'UI déclarative moderne **Jetpack Compose** et Material Design 3.
  * Authentification moderne avec **Google Identity Credential Manager**, stockage chiffré des tokens de session via `EncryptedSharedPreferences`, navigation fluide et gestion de la corbeille.
* **Web Historique (Frontend PHP / Symfony Twig) :**
  * Interface historique d'origine basée sur Symfony Twig, JavaScript Stimulus et Tailwind CSS (conservée pour rétrocompatibilité, aujourd'hui dépréciée au profit du frontend Angular).
* **Push Temps Réel (Mercure Hub) :**
  * Notifications instantanées poussées via Server-Sent Events (SSE) : changements de statuts, nominations de managers, commentaires et assignations visibles en temps réel par les collaborateurs connectés.

---

## 🏗 Architecture Technique & Microservices

Organ repose sur une architecture orientée microservices conteneurisée via **Docker Compose**. Chaque brique est spécialisée et isolée sur un réseau applicatif privé sécurisé.

### Cartographie des Services

| Service Docker | Technologie | Rôle & Fonctionnalité | Port Externe |
| :--- | :--- | :--- | :--- |
| `front-angular` | **Angular 21 / Node.js** | Frontend Web moderne, SPA réactive | [http://localhost:4200](http://localhost:4200) |
| `api-nginx` | **Nginx Alpine** | Reverse proxy HTTP & terminaison web pour l'API | [http://localhost:8001](http://localhost:8001) |
| `api-php` | **PHP 8.4-FPM / Symfony 8** | Cœur applicatif, logique métier, contrôleurs REST, JWT & RBAC | Interne (FastCGI) |
| `database` | **MySQL 8.0** | Base relationnelle (utilisateurs, projets, Organs, permissions, tâches) | `3306` |
| `mongodb` | **MongoDB 7.0** | Base NoSQL (journaux d'audit, statistiques quotidiennes, fichiers) | `27017` |
| `valkey` | **Valkey 7.2** | Cache mémoire ultra-rapide (fork moderne de Redis) | `6379` |
| `mercure` | **Mercure Hub** | Hub de diffusion d'événements temps réel (Server-Sent Events) | [http://localhost:9090](http://localhost:9090) |
| `phpmyadmin` | **phpMyAdmin** | Interface web d'administration de la base MySQL | [http://localhost:8080](http://localhost:8080) |
| `front-nginx` | **Nginx Alpine** | Serveur web pour le frontend PHP historique | [http://localhost:8000](http://localhost:8000) |
| `front-php` | **PHP 8.4-FPM** | Frontend historique Symfony Twig (déprécié) | Interne (FastCGI) |
| `android` | **Kotlin / Jetpack Compose** | Application mobile native Android (dossier `android/`) | Standalone |

---

### Schéma de l'Architecture

```mermaid
flowchart TB
    subgraph Clients["Clients Utilisateurs"]
        ANG["Frontend Angular 21<br/>:4200"]
        AND["Application Android<br/>(Jetpack Compose)"]
        PHP_F["Frontend PHP Twig<br/>(Déprécié :8000)"]
    end

    subgraph Ingress["Entrée Web & Reverse Proxy"]
        API_NGX["api-nginx<br/>:8001"]
    end

    subgraph CoreBackend["Cœur Backend"]
        API_PHP["api-php (Symfony 8 / PHP 8.4)<br/>- Lexik JWT & Refresh Tokens<br/>- Moteur de permissions RBAC<br/>- Doctrine ORM & ODM<br/>- Intégration IA Mistral"]
    end

    subgraph DataStorage["Persistance & Cache"]
        MYSQL[("MySQL 8.0<br/>Données relationnelles<br/>Projets, Organs, Tâches, Rôles")]
        MONGO[("MongoDB 7.0<br/>AuditLog, DailyStat,<br/>Fichiers GridFS")]
        VALKEY[("Valkey 7.2<br/>Cache applicatif & Sessions")]
    end

    subgraph Realtime["Temps Réel"]
        MERCURE["Mercure Hub :9090<br/>Server-Sent Events (SSE)"]
    end

    subgraph ExternalServices["Services Externes"]
        GOOGLE["Google Cloud APIs<br/>- OAuth 2.0 Identity<br/>- Google Drive API"]
        NVIDIA["NVIDIA AI Endpoints<br/>Mistral AI LLM"]
    end

    ANG -->|HTTP REST / JWT| API_NGX
    AND -->|HTTP REST / JWT| API_NGX
    PHP_F -->|HTTP REST| API_NGX
    ANG -.->|Abonnement SSE| MERCURE

    API_NGX -->|FastCGI| API_PHP

    API_PHP -->|SQL / Doctrine ORM| MYSQL
    API_PHP -->|BSON / Doctrine ODM| MONGO
    API_PHP -->|Redis Protocol| VALKEY
    API_PHP -->|Publish SSE| MERCURE
    API_PHP -->|OAuth & Fichiers BYOS| GOOGLE
    API_PHP -->|Requêtes Inference IA| NVIDIA
```

#### Schéma Détaillé de l'API Backend
![Schéma du Backend](docs/Backend.png)

---

## 🤖 L'Assistant IA : une expérimentation semi-agentique

> [!IMPORTANT]
> **Un travail étudiant, et une première fois.** Organ est un projet étudiant, et le cahier des charges ne demandait aucune IA. L'assistant est un **ajout volontaire**, en plus du reste. C'est aussi la **toute première fois** que nous intégrions un LLM dans une application, **sans connaissance approfondie du sujet** au départ. Cette section ne vend donc pas une fonctionnalité aboutie : elle documente **une première expérimentation**, ce qu'elle fait, comment elle le fait, et surtout **ce qu'elle nous a appris par ses défauts**.

### Pourquoi « semi-agent » ?

On peut situer un assistant IA sur trois paliers :

| Palier | Ce que fait le modèle | Exemple |
| :--- | :--- | :--- |
| **Chatbot** | Il répond par du texte, c'est tout. | FAQ conversationnelle |
| **Semi-agent** *(Organ)* | Il **produit un plan d'actions** que l'application exécute, mais **en une seule passe et sans jamais voir le résultat**. | « Crée-moi un projet Marketing avec 3 tâches » |
| **Agent** | Il agit, **observe le résultat**, corrige et recommence **en boucle** jusqu'à atteindre l'objectif. | Agents de code, assistants à outils |

L'assistant d'Organ fait plus que parler : il agit réellement sur les données. Mais il agit **à l'aveugle**. Il écrit tout son plan d'un coup, avant que quoi que ce soit ne soit exécuté, puis il n'a plus aucun moyen de savoir ce qui s'est passé. C'est un **aller simple** (*one-way trip*, *one pass*), un état **préliminaire** de l'IA agentique, auquel manque ce qui fait un agent : **la boucle d'observation**.

### Comment ça marche

```mermaid
sequenceDiagram
    autonumber
    actor U as Utilisateur
    participant F as Angular<br/>(ChatbotService)
    participant A as API Symfony<br/>(ChatbotController)
    participant DB as MySQL
    participant L as LLM Mistral<br/>(NVIDIA NIM)
    participant E as Angular<br/>(ActionExecutor)

    U->>F: "/create un projet Marketing avec 3 tâches"
    F->>A: POST /api/chatbot/message<br/>{history, context: route, commande, actionsEnabled}
    A->>DB: Contexte filtré par le RBAC<br/>(projets, Organs, rôles, membres)
    A->>L: System prompt géant + contexte JSON + 10 derniers messages
    L-->>A: {"message": "...", "actions": [ ... ]}
    A->>A: Extraction / réparation du JSON
    A-->>F: message + plan d'actions
    F->>E: executeActions(actions)
    loop Pour chaque action, dans l'ordre
        E->>A: Appel REST classique (JWT de l'utilisateur)
        A-->>E: Ressource créée (UUID)
    end
    E-->>U: Toast récapitulatif + rafraîchissement
    Note over L,E: Le LLM n'est jamais informé du résultat des actions
```

**1. Le frontend prépare la demande** (`front_angular/src/app/services/chatbot/chatbot.service.ts`)
L'utilisateur choisit une commande : `/ask` (par défaut, explication), `/create`, `/edit` ou `/role`. Le service envoie l'historique de conversation et un petit contexte : la route courante, la commande et l'état de l'interrupteur « actions autorisées ».

**2. Le backend construit le contexte et le prompt** (`api/src/Controller/ChatbotController.php`)
* Il charge depuis la base les **projets accessibles** à l'utilisateur et, si la route contient un UUID de projet, les **Organs, leurs rôles et les membres** de ce projet. Ce chargement passe par `ChatbotContextBuilder` (`api/src/Service/ChatbotContextBuilder.php`), qui applique les mêmes règles d'accès que l'API REST.
* Il assemble un **system prompt** de plus d'une centaine de lignes : description fonctionnelle, plan du site, règles métier, règles de branding, liste des 30+ permissions, **schéma JSON de chaque action possible**, règles de concision et de budget de tokens. Une consigne propre à la commande choisie y est ajoutée.
* Il envoie le tout, avec les **10 derniers messages**, à l'API NVIDIA NIM (`temperature: 0.2`, `max_tokens: 1024`, `response_format: json_object`). Le modèle est configurable via `NVIDIA_MODEL`.

**3. Le modèle répond par un plan**
Le LLM doit renvoyer un objet JSON strict :

```json
{
  "message": "Je crée le projet **Marketing** avec un Organ *Campagnes* et 2 tâches.",
  "actions": [
    { "type": "create_project", "id": "proj1", "payload": { "title": "Marketing", "color": "#FF7DD4", "iconType": "EMOJI", "iconData": "📣" } },
    { "type": "create_organ", "id": "org1", "projectUuid": "proj1", "payload": { "title": "Campagnes", "highlightColor": "#355EE4", "iconType": "EMOJI", "iconData": "🎯" } },
    { "type": "create_task", "id": "task1", "projectUuid": "proj1", "organUuid": "org1", "payload": { "title": "Définir la cible", "priority": 7 } },
    { "type": "create_task", "projectUuid": "proj1", "organUuid": "org1", "payload": { "title": "Rédiger les visuels", "priority": 5, "dependencyUuids": ["task1"] } },
    { "type": "navigate", "path": "/organ/proj1/org1" }
  ]
}
```

Les identifiants `proj1`, `org1`, `task1` sont des **variables temporaires** : le modèle ne connaît pas encore les vrais UUID, puisque rien n'a encore été créé. Vocabulaire disponible : `navigate`, `create_project`, `create_organ`, `create_role`, `copy_role`, `paste_role`, `create_task`, `update_task`.

**4. Le backend « répare » la réponse si besoin**
Les LLM ne respectent pas toujours le format. Le contrôleur extrait le texte entre la première `{` et la dernière `}`, corrige les retours à la ligne illégaux et, si la réponse a été **tronquée** par la limite de tokens, `repairTruncatedJson()` parcourt le texte caractère par caractère pour **récupérer uniquement les actions complètes**.

**5. Le frontend exécute le plan** (`front_angular/src/app/services/chatbot/chatbot-action-executor.service.ts`)
Les actions sont jouées **séquentiellement**, via **les mêmes services REST** que l'interface classique. À chaque création, le vrai UUID renvoyé par l'API est associé à sa variable temporaire, et cette correspondance est substituée dans les actions suivantes. `copy_role` / `paste_role` passent par le `localStorage`. À la fin, un toast récapitule ce qui a été créé et la page est rafraîchie.

### Les garde-fous en place

* **L'IA n'a pas plus de droits que l'utilisateur.** Les actions passent par l'API REST standard avec le JWT de l'utilisateur, donc **tout le moteur RBAC s'applique**. Une action interdite à l'utilisateur échoue aussi pour l'IA.
* **Aucune suppression possible.** C'est interdit dans le prompt, mais surtout **le type d'action n'existe pas dans l'exécuteur**. La protection est structurelle, elle ne repose pas seulement sur l'obéissance du modèle.
* **Mode lecture seule.** Un interrupteur dans le panneau coupe l'exécution. La consigne est envoyée au modèle **et** appliquée côté client : même si le modèle renvoie des actions, elles sont ignorées.
* **Un contexte soumis au RBAC.** `ChatbotContextBuilder` ne fait pas confiance à la route envoyée par le client : rien n'est injecté si l'utilisateur n'est pas membre du projet, un simple membre ne voit que les Organs pour lesquels il a `ORGAN_VIEW`, et les rôles ne sont listés qu'avec `ORGAN_MANAGE_ROLES`. Ces règles sont couvertes par des tests (`api/tests/Service/ChatbotContextBuilderTest.php`). Voir la limite n°11 pour l'historique de cette correction.
* **Le rôle `system` est réservé au serveur.** Les messages d'historique envoyés par le client ne peuvent être que `user` ou `assistant` : impossible d'y glisser de fausses instructions système.
* **Sortie contrainte** : JSON imposé, température basse.

### Les limites : un aller simple, sans retour

C'est la partie la plus instructive de l'expérience. Mis à l'usage, le système montre des défauts **majeurs**, qui ont presque tous la même cause : **l'absence de boucle de rétroaction**.

**1. Une seule passe, plan figé à l'avance.**
Un message déclenche **un seul appel** au LLM. Le plan complet est écrit **avant** toute exécution, sur la base d'un monde qui n'existe pas encore (d'où les variables `proj1`, `task1`…). Le modèle ne peut pas adapter l'étape 3 au résultat de l'étape 2.

**2. Aucune observation des erreurs.**
Si une action échoue (permission refusée, champ invalide, UUID inexistant), l'exécuteur affiche un toast d'erreur et s'arrête. Le modèle **n'en est jamais informé** : l'historique ne conserve que le *texte* de ses messages, ni les actions qu'il a émises ni leur résultat. Au tour suivant, il « croit » avoir réussi, puisque son propre message disait « Je crée… ». Il ne peut ni se corriger, ni réessayer, ni même expliquer l'échec.

**3. Pas de retour arrière.**
Les actions ne forment pas une transaction. Si la 4ᵉ action échoue, les 3 premières restent créées. L'utilisateur se retrouve avec un état **partiel** et doit faire le ménage lui-même.

**4. Le succès est annoncé avant d'avoir eu lieu.**
Le `message` affiché est rédigé par le modèle **avant** l'exécution : il décrit une *intention*, pas un résultat. Le toast final « opération terminée avec succès » compte seulement les réponses HTTP 2xx. Rien ne vérifie que ce qui a été créé correspond à la demande.

**5. Un contexte en partie aveugle, sans moyen d'en demander plus.**
Le modèle ne voit que ce qu'on injecte dans le prompt. Or la **liste des tâches n'y figure pas** : pour `/edit` (`update_task`), il lui faut un `taskUuid` qu'il ne connaît pas, et il est alors tenté d'en inventer un. Faute d'outils de lecture, il ne peut pas aller chercher l'information manquante. Tout doit être deviné ou pré-chargé.

**6. Troncature et exécution partielle silencieuse.**
Avec 1024 tokens de sortie, un plan ambitieux est coupé en plein milieu. Le mécanisme de réparation sauve les actions complètes, ce qui veut dire qu'**une partie du plan est exécutée sans prévenir**, parfois sans la navigation finale. Les règles du prompt (« 3 ou 4 tâches maximum », « descriptions d'une phrase ») sont des **rustines** contre ce problème, pas une solution.

**7. Un contrôle uniquement par le prompt.**
Le system prompt est rempli de « CRITIQUE », « ABSOLUMENT », « IMPÉRATIVEMENT », « Ne dis JAMAIS ». C'est le symptôme d'un système qui ne peut **qu'espérer** que le modèle obéisse : aucune validation de schéma n'est faite sur les actions avant exécution, et un type inconnu est simplement ignoré avec un `console.warn`.

**8. Des mécanismes fragiles.**
* La substitution des variables temporaires est un `replaceAll` sur **toutes** les chaînes : `task1` est aussi remplacé à l'intérieur de `task10`, ou dans un titre qui contiendrait ce texte.
* L'extraction du JSON repose sur des heuristiques (première et dernière accolade, regex de réparation).

**9. Coût, latence et confidentialité.**
À chaque message, le prompt complet, la liste des projets et les **noms et e-mails des membres** sont renvoyés à un fournisseur tiers. Il n'y a pas de streaming : l'utilisateur attend la réponse entière, puis l'exécution.

**10. Une dépendance forte au fournisseur.**
Le modèle utilisé au départ (*Mixtral 8x22B*) a été retiré du catalogue de l'API NVIDIA, ce qui nous a obligés à passer à *Mistral Medium 3.5*. Or le prompt avait été mis au point avec le premier modèle, et rien ne garantit qu'un autre modèle se comporte de la même façon. Sans jeu de tests ou d'évaluation des réponses, impossible de vérifier que l'assistant fonctionne toujours aussi bien après le changement.

**11. Une faille de cloisonnement, découverte puis corrigée.**
Dans sa première version, le contrôleur déduisait le projet de la route **envoyée par le client**, sans vérifier que l'utilisateur en était membre. Il injectait aussi **tous** les Organs du projet, y compris ceux auxquels l'utilisateur n'avait pas accès, ainsi que leurs rôles. En forgeant une route, on pouvait donc faire entrer dans le prompt les Organs et les **e-mails des membres** d'un projet étranger, puis les obtenir en interrogeant le modèle. Le client pouvait en plus envoyer des messages avec le rôle `system` pour réécrire les consignes.
Les deux failles sont corrigées (voir les garde-fous ci-dessus). Nous les avons découvertes en relisant le code pour rédiger cette documentation, et la leçon dépasse ce projet : **une IA branchée sur les données est un nouveau chemin d'accès à ces données**. Elle doit passer par exactement les mêmes contrôles que le reste de l'API, sinon elle contourne silencieusement tout le moteur de permissions.

### Ce qu'il faudrait pour un vrai agent

| Organ aujourd'hui | Ce que ferait une approche agentique |
| :--- | :--- |
| JSON libre dans le texte, réparé à la main | **Tool calling natif** (*function calling*) avec des schémas validés |
| Plan complet écrit d'avance | **Boucle** *raisonner → appeler un outil → observer → continuer*, avec un nombre maximal d'itérations |
| Exécution côté client, résultat jamais remonté | Exécution **côté serveur**, avec le résultat (UUID, erreur) **renvoyé au modèle** à chaque étape |
| Tout le contexte pré-chargé dans le prompt (filtré par le RBAC) | **Outils de lecture** (`list_tasks`, `get_organ`…) passant par les mêmes vérifications RBAC que l'API : le modèle va chercher ce dont il a besoin, et seulement ce qu'il a le droit de voir |
| Variables temporaires `proj1` | De vrais UUID, puisque chaque création est observée avant la suivante |
| Erreur = arrêt + toast | Erreur = **observation** : le modèle peut corriger, réessayer ou expliquer |
| Pas d'annulation | **Aperçu du plan et confirmation** avant écriture, transaction ou actions de compensation |
| Historique textuel seulement | Historique incluant **les appels d'outils et leurs résultats** |

### Ce que nous en retenons

Ce module n'était pas attendu, et il reste imparfait. Il nous a pourtant permis de comprendre concrètement la différence entre **un modèle qui génère des instructions** et **un agent qui poursuit un objectif**, et cette différence, c'est **la boucle**. Presque tout ce que nous avons dû bricoler (réparation de JSON, plafonds de tâches, règles en majuscules, variables temporaires) compense l'absence de retour d'information. Le panneau d'aide de l'assistant le reconnaît d'ailleurs : il conseille à l'utilisateur de formuler ses demandes « étape par étape » et de « vérifier le résultat » avant de continuer. Autrement dit, **c'est l'humain qui joue le rôle de la boucle** que le système ne sait pas faire. C'est à notre sens la principale leçon de cette expérimentation, et ce sera le point de départ d'une prochaine version.

---

## ⚙️ Configuration de l'Environnement

L'exécution locale du projet requiert la configuration de quatre fichiers d'environnement `.env`.

> [!IMPORTANT]
> **Règle impérative des secrets techniques :** Pour `APP_SECRET`, `JWT_PASSPHRASE` et surtout `MERCURE_JWT_SECRET`, utilisez impérativement des chaînes aléatoires de **32 caractères minimum**. Une clé trop courte provoquera l'arrêt immédiat des services sans journal d'erreur explicite.

---

### 1. Racine du projet (`.env`)
Configure l'infrastructure globale Docker Compose :
```dotenv
# ==========================================
# IDENTIFIANTS DU SERVICE MYSQL
# ==========================================
MYSQL_ROOT_PASSWORD=root_password
MYSQL_DATABASE=app_database
MYSQL_USER=app_user
MYSQL_PASSWORD=app_password

# ==========================================
# PHPMYADMIN
# ==========================================
PMA_USER=root
PMA_PASSWORD=root_password

# ==========================================
# HUB TEMPS RÉEL MERCURE (Min 32 caractères)
# ==========================================
MERCURE_JWT_SECRET=!ChangeThisMercureHubJWTSecretKey!

# ==========================================
# BASE DE DONNÉES NOSQL MONGODB
# ==========================================
MONGODB_ROOT_USER=root
MONGODB_ROOT_PASSWORD=mongodb_root_password
MONGODB_DATABASE=organ

# ==========================================
# IDENTIFIANTS GOOGLE CLOUD (OAuth & Drive)
# ==========================================
GOOGLE_CLIENT_ID=<VOTRE_ID_CLIENT>.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=<VOTRE_SECRET_CLIENT>
```

---

### 2. API Backend (`api/.env`)
Configure le cœur applicatif Symfony :
```dotenv
###> SYMFONY CORE ###
APP_ENV=dev
APP_SECRET=a_very_long_random_string_of_at_least_32_chars
APP_SHARE_DIR=var/share
DEFAULT_URI=http://localhost
###< SYMFONY CORE ###

###> INFRASTRUCTURE & RÉSEAU ###
TRUSTED_PROXIES=127.0.0.1,REMOTE_ADDR
TRUSTED_HEADERS=x-forwarded-for,x-forwarded-host,x-forwarded-proto,x-forwarded-port
CORS_ALLOW_ORIGIN='^https?://(localhost|127\.0\.0\.1):(8000|8001|4200)$'
###< INFRASTRUCTURE & RÉSEAU ###

###> BASES DE DONNÉES & CACHE ###
DATABASE_URL="mysql://app_user:app_password@database:3306/app_database?serverVersion=8.0.32&charset=utf8mb4"
REDIS_URL=redis://organ_valkey:6379
MONGODB_URI="mongodb://root:mongodb_root_password@mongodb:27017/?authSource=admin"
MONGODB_DB="organ_dev"
###< BASES DE DONNÉES & CACHE ###

###> AUTHENTIFICATION & SÉCURITÉ ###
JWT_SECRET_KEY=%kernel.project_dir%/config/jwt/private.pem
JWT_PUBLIC_KEY=%kernel.project_dir%/config/jwt/public.pem
JWT_PASSPHRASE=another_long_random_passphrase_for_jwt
GOOGLE_CLIENT_ID=<VOTRE_ID_CLIENT>.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=<VOTRE_SECRET_CLIENT>
COOKIE_SECURE=false
###< AUTHENTIFICATION & SÉCURITÉ ###

###> TEMPS RÉEL (MERCURE) ###
MERCURE_URL=http://mercure/.well-known/mercure
MERCURE_PUBLIC_URL=http://localhost:8000/hub
MERCURE_JWT_SECRET=!ChangeThisMercureHubJWTSecretKey!
NOTIFICATION_BASE_URL=http://localhost:8000
###< TEMPS RÉEL ###

###> ASSISTANT IA NVIDIA ###
NVIDIA_API_KEY=<nvapi-clé_api>
NVIDIA_MODEL=mistralai/mistral-medium-3.5-128b
###< ASSISTANT IA NVIDIA ###
```

---

### 3. Frontend Angular (`front_angular/.env`)
Configure le client Angular moderne :
```dotenv
NG_APP_GOOGLE_CLIENT_ID=<VOTRE_ID_CLIENT>.apps.googleusercontent.com
```

---

### 4. Frontend Historique PHP (`front_php/.env`)
Configure l'interface Twig dépréciée :
```dotenv
###> SYMFONY CORE ###
APP_ENV=dev
APP_SECRET=another_secret_for_the_front_app
###< SYMFONY CORE ###

###> SYMFONY ROUTING ###
DEFAULT_URI=http://localhost:8000
###< SYMFONY ROUTING ###

###> CONFIGURATION API ###
API_URL=http://api-nginx
###< CONFIGURATION API ###

###> AUTHENTIFICATION ###
GOOGLE_CLIENT_ID=<VOTRE_ID_CLIENT>.apps.googleusercontent.com
###< AUTHENTIFICATION ###
```

---

## 🛠 Tutoriel : Configuration Google Cloud (OAuth 2.0 & Drive)

Pour que l'authentification Google Sign-In et le stockage décentralisé sur Google Drive soient opérationnels, suivez les étapes de configuration :

1. **Accès à la console :** Rendez-vous sur la [Google Cloud Console](https://console.cloud.google.com).
2. **Création d'un projet :** Créez un nouveau projet (ex: `Organ-Dev`).
3. **Activation de l'API Google Drive :** Dans le menu *APIs & Services* > *Library*, recherchez **Google Drive API** et cliquez sur **Activer**.
4. **Écran de consentement OAuth :**
   * Type d'utilisateur : **Externe**.
   * Renseignez le nom de l'application (`Organ`) et votre adresse de support.
   * **Champs d'application (Scopes) :** Ajoutez le scope impératif suivant :
     ```text
     https://www.googleapis.com/auth/drive.file
     ```
     *(Ce scope limite l'accès d'Organ uniquement aux fichiers créés ou ouverts par l'application elle-même, garantissant une stricte confidentialité des autres données Drive de l'utilisateur).*
   * **Utilisateurs de test :** En phase de développement (*Testing*), ajoutez les adresses Gmail des collaborateurs autorisés à se connecter.
5. **Création des Identifiants OAuth :**
   * Rendez-vous dans *Identifiants* > *Créer des identifiants* > *ID de client OAuth*.
   * Type d'application : **Application Web**.
   * **Origines JavaScript autorisées :**
     * `http://localhost:4200`
     * `http://localhost:8000`
     * `http://localhost:8001`
   * **URIs de redirection autorisés :**
     * `http://localhost:8000/login/google/check`
     * `http://localhost:4200/login`
6. **Copie des secrets :** Reportez l'**ID Client** et le **Secret Client** obtenus dans vos différents fichiers `.env`.

---

## 🚀 Installation et Lancement

> [!NOTE]
> Le fichier `docker-compose.yml` force l'architecture `linux/amd64` pour garantir une stricte parité d'exécution entre macOS (Apple Silicon), Windows (WSL2) et Linux.

### 1. Cloner le projet
```bash
git clone https://github.com/the-voxel-studio/organ.git
cd organ
```

### 2. Renseigner les fichiers d'environnement
Créez les fichiers `.env`, `api/.env`, `front_angular/.env` et `front_php/.env` selon les modèles décrits ci-dessus.

### 3. Construire et démarrer la flotte Docker
```bash
docker compose build
docker compose up -d
```

### 4. Initialisation des dépendances et de la sécurité Backend
Installez les dépendances Composer et générez la paire de clés cryptographiques RSA pour l'authentification JWT :
```bash
# Dépendances de l'API Symfony
docker compose exec api-php composer install

# Dépendances du frontend PHP historique
docker compose exec front-php composer install

# Génération des clés asymétriques JWT
docker compose exec api-php php bin/console lexik:jwt:generate-keypair
```

### 5. Compilation des styles Tailwind (Frontend PHP uniquement)
*Le frontend Angular compile automatiquement ses styles.* Pour le frontend historique PHP :
```bash
docker compose exec front-php php bin/console tailwind:build
```

> [!TIP]
> **Résolution du message `Directory not empty` lors du cache:clear :**
> Lors d'un nettoyage manuel du cache Symfony (`php bin/console cache:clear`), un message non-bloquant `Directory not empty` peut survenir lorsque Symfony supprime et réécrit simultanément ses caches. Il suffit de réexécuter la commande pour confirmer la finalisation.

---

### 🌐 Adresses d'Accès aux Services

| Composant | Adresse URL | Description |
| :--- | :--- | :--- |
| **Application Web Angular** | [http://localhost:4200](http://localhost:4200) | Frontend moderne officiel (recommandé) |
| **Documentation Swagger de l'API** | [http://localhost:8001/api/docs](http://localhost:8001/api/docs) | Spécification OpenAPI / documentation des endpoints |
| **Gestionnaire BDD phpMyAdmin** | [http://localhost:8080](http://localhost:8080) | Administration graphique de MySQL |
| **Hub Temps Réel Mercure** | [http://localhost:9090](http://localhost:9090) | Serveur SSE de diffusion en direct |
| **Application Web PHP Twig** | [http://localhost:8000](http://localhost:8000) | Interface historique (dépréciée) |

---

## 📱 Application Mobile Android

Le dépôt héberge une application native Android moderne située dans le répertoire `android/`.

### Caractéristiques Techniques Mobile
* **Langage & Framework :** 100% Kotlin avec **Jetpack Compose** et **Material 3**.
* **Authentification Avancée :** Support natif du **Google Credential Manager** (`androidx.credentials`) pour une connexion en un clic.
* **Architecture Réseau :** Retrofit 2, OkHttp 3, Coroutines & Flow pour la synchronisation asynchrone avec l'API Organ.
* **Sécurité embarquée :** Stockage sécurisé des jetons d'accès et de rafraîchissement au sein d'un keystore chiffré via `EncryptedSharedPreferences`.
* **Graphismes & Ergonomie :** Animations vectorielles Lottie, gestion des thèmes et du sélecteur de couleurs (`colorpicker-compose`).

### Compilation & Lancement
1. Ouvrez le dossier `android/` dans **Android Studio** (Koala ou plus récent, JDK 17+).
2. Configurez l'adresse IP de l'hôte dans la configuration réseau (utilisez `http://10.0.2.2:8001` si vous testez avec l'émulateur officiel Android afin de cibler l'API de votre machine locale).
3. Compilez et lancez l'application via le bouton **Run** ou en ligne de commande :
   ```bash
   cd android
   ./gradlew assembleDebug
   ```

---

## 🧪 Données de Démonstration (Seed) & Comptes de Test

Pour explorer immédiatement toutes les fonctionnalités (tableaux Kanban garnis, indicateurs statistiques, arborescence d'Organs), le projet fournit un jeu de données complet.

### 1. Importation des Données MySQL
* **Option A (phpMyAdmin) :**
  1. Rendez-vous sur [http://localhost:8080](http://localhost:8080).
  2. Connectez-vous avec `root` / `root_password`.
  3. Cliquez sur la base `app_database` dans la barre latérale.
  4. Rendez-vous dans l'onglet **Importer**, sélectionnez le fichier `seed.sql` situé à la racine du projet et validez.
* **Option B (Ligne de commande Docker) :**
  ```bash
  docker compose exec -T database mysql -uapp_user -papp_password app_database < seed.sql
  ```

### 2. Génération des Métriques MongoDB (Analytics)
Pour alimenter les graphiques d'activité et les statistiques quotidiennes de l'application :
```bash
docker compose exec -T api-php php bin/console app:seed:mock-analytics
```

### 3. Comptes Utilisateurs Préconfigurés
Tous les comptes de démonstration partagent le même mot de passe : `password`

| Rôle Métier | Nom d'utilisateur | Email de Connexion | Mot de Passe |
| :--- | :--- | :--- | :--- |
| **Administrateur** | Jean Dupont | `admin@organ.com` | `password` |
| **Manager** | Marie Curie | `manager@organ.com` | `password` |
| **Développeur** | Albert Einstein | `dev@organ.com` | `password` |
| **Utilisateur** | Nikola Tesla | `user@organ.com` | `password` |
| **Membre** | Grace Hopper | `grace@organ.com` | `password` |

---

## 🧪 Tests & Qualité du Code

Le projet met l'accent sur la robustesse et la sécurité des données, particulièrement sur l'isolation des permissions et la non-régression.

### Tests Backend (Symfony & PHPUnit)
La suite de tests inclut des vérifications unitaires, fonctionnelles et de sécurité critique (IDOR, mass assignment, pollution de cache, permissions granulaires) :
```bash
docker compose exec api-php bin/phpunit
```

### Tests Frontend (Angular & Vitest)
```bash
cd front_angular
npm test
```

### Tests Mobile Android
```bash
cd android
./gradlew test
```

---

## 📄 Licence

Ce projet est distribué sous les termes de la licence libre **GNU General Public License v3 (GPLv3)**.  
Consultez le fichier [LICENSE](LICENSE) pour plus de détails.
