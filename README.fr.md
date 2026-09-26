# Organ : Plateforme Collaborative de Gestion de Projets & d'Organes

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
   * [Le Modèle Tripartite : Projet > Organe > Tâche](#le-modèle-tripartite--projet--organe--tâche)
   * [Liberté de Stockage : BYOS ("Bring Your Own Storage")](#liberté-de-stockage--byos-bring-your-own-storage)
   * [Sécurité & Permissions Granulaires (RBAC)](#sécurité--permissions-granulaires-rbac)
   * [Gestion de l'Urgence & Échelle Chromatique](#gestion-de-lurgence--échelle-chromatique)
   * [Traçabilité, Audit Log & Analytics](#traçabilité-audit-log--analytics)
   * [Assistant IA Contextuel Intégré](#assistant-ia-contextuel-intégré)
   * [Écosystème Multi-Plateforme](#écosystème-multi-plateforme)
2. [Architecture Technique & Microservices](#-architecture-technique--microservices)
   * [Cartographie des Services](#cartographie-des-services)
   * [Schéma de l'Architecture](#schéma-de-larchitecture)
3. [Configuration de l'Environnement](#-configuration-de-lenvironnement)
   * [1. Racine du projet (`.env`)](#1-racine-du-projet-env)
   * [2. API Backend (`api/.env`)](#2-api-backend-apienv)
   * [3. Frontend Angular (`front_angular/.env`)](#3-frontend-angular-front_angularenv)
   * [4. Frontend Historique PHP (`front_php/.env`)](#4-frontend-historique-php-front_phpenv)
4. [Tutoriel : Configuration Google Cloud (OAuth 2.0 & Drive)](#-tutoriel--configuration-google-cloud-oauth-20--drive)
5. [Installation et Lancement](#-installation-et-lancement)
6. [Application Mobile Android](#-application-mobile-android)
7. [Données de Démonstration (Seed) & Comptes de Test](#-données-de-démonstration-seed--comptes-de-test)
8. [Tests & Qualité du Code](#-tests--qualité-du-code)
9. [Licence](#-licence)

---

## 💡 Qu'est-ce qu'Organ ?

### Genèse et Philosophie

**Organ** est né d'un constat partagé par trois étudiants en informatique aux sensibilités complémentaires : le créatif guidé par le visuel, le rigoureux obsédé par la hiérarchie et l'efficace cherchant l'action immédiate.

Face aux solutions du marché, deux impasses majeures se présentaient régulièrement :
* **Le « Mur de Tâches »** : Les to-do lists classiques, simples au départ, qui s'étirent en listes interminables, anxiogènes et sans perspective d'ensemble.
* **Le « Labyrinthe »** : Les plateformes documentaires sur-imbriquées (pages dans des sous-pages à l'infini) où l'on finit par passer plus de temps à ranger et chercher qu'à réaliser.

Organ a été conçu pour réconcilier ces approches : **fournir une structure humaine, modulaire et hautement sécurisée**, sans jamais sacrifier la clarté visuelle ni la rapidité d'exécution.

---

### Le Modèle Tripartite : Projet > Organe > Tâche

Organ repose sur une hiérarchie stricte à 3 niveaux qui élimine le désordre cognitif :

```text
📁 PROJET (Vision Stratégique & Équipe Globale)
│
├── 🫀 ORGANE A (Module métier, pôle, sprint ou sous-projet dédié)
│    ├── 📋 Tâche 1 (Priorité 8/10, échéance, validation requise)
│    ├── 📋 Tâche 2 (Dépendance : bloquée par la Tâche 1)
│    └── 📋 Tâche 3
│
└── 🫀 ORGANE B (Module externe accessible à des prestataires ou clients)
     ├── 📋 Tâche 4
     └── 📋 Tâche 5
```

1. **Le Projet :** L'enveloppe globale de haut niveau (titre, description, code couleur d'identité, icône ou emoji personnalisé, stockage Google Drive associé et équipe globale).
2. **L'Organe (Organ) :** L'unité fonctionnelle autonome au sein du projet. Un Organe peut représenter un pôle métier (*Design*, *Backend*, *Marketing*), un jalon (*Phase 1*, *Sprint 3*), ou un espace de sous-traitance dédié. Chaque Organe dispose de son propre **tableau Kanban**, de son panneau de liens Web rapides et surtout de son **système de rôles et de permissions local**.
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

* **Cloisonnement strict par Organe :** Un utilisateur invité sur un projet peut n'avoir accès qu'à un seul Organe spécifique. L'existence des autres Organes du projet lui demeure totalement invisible.
* **Rôles Globaux vs Rôles Locaux :**
  * *Rôles globaux de projet* : `ADMIN`, `MANAGER`, `MEMBER`.
  * *Rôles locaux d'Organe* : définis sur-mesure par les gestionnaires de l'Organe avec un nom et une icône dédiée.
* **Plus de 40 permissions unitaires paramétrables :**
  * *Gestion de l'Organe* : consultation, modification, gestion des membres locaux, configuration des rôles, gestion des liens.
  * *Gestion des Tâches* : création, modification (uniquement les siennes vs toutes), suppression logique (*soft delete*) ou définitive (*hard delete*), changement de statut, modification de priorité, planification des dates, modification des estimations d'heures.
  * *Affectation et Workflow* : s'auto-assigner, assigner des tiers, valider officiellement une tâche (`TASK_VALIDATE`).
  * *Ressources liées* : gestion des pièces jointes, des liens Web, des tags et des dépendances entre tâches.
  * *Commentaires* : rédaction, édition de ses propres commentaires vs modération de tous les messages.
* **Protection renforcée contre les failles d'accès :** L'API intègre une vérification systématique contre les IDOR (*Insecure Direct Object Reference*), le *mass assignment*, et garantit l'invalidation automatique du cache en cas de révocation de droits.
* **Cycle de vie sécurisé (Corbeille à 2 niveaux) :** Suppression douce (*soft delete*) permettant la restauration depuis la corbeille locale d'un Organe ou la corbeille globale d'un projet, avec séparation stricte des droits de purge définitive.

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
  * Un diagramme en barres de la charge de travail par Organe.
  * Des métriques clés de consultation et d'engagement de l'équipe.

---

### Assistant IA Contextuel Intégré

Organ intègre un tuteur et assistant virtuel propulsé par les modèles d'intelligence artificielle de pointe via **NVIDIA AI Foundation Endpoints** (modèles *Mistral AI*) :
* **Conscience contextuelle complète :** L'assistant sait en temps réel où se trouve l'utilisateur dans l'application, quels projets, Organes et membres composent son environnement.
* **Accompagnement pédagogique :** Explications détaillées pas-à-pas des processus métier (par exemple, comment onboarder un nouveau collaborateur extérieur dans un Organe).
* **Déclenchement d'actions :** L'assistant est interfacé avec le frontend pour guider la navigation ou proposer la création directe de structures adaptées.

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
| `database` | **MySQL 8.0** | Base relationnelle (utilisateurs, projets, organes, permissions, tâches) | `3306` |
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

Pour explorer immédiatement toutes les fonctionnalités (tableaux Kanban garnis, indicateurs statistiques, arborescence d'Organes), le projet fournit un jeu de données complet.

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
