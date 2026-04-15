# Organ : Gestion de Projets Collaboratifs

Organ est une application de gestion de projets collaboratifs dont la principale caractéristique est un système de permissions hautement granulaire et personnalisable.

## Fonctionnalités Clés

### Fonctionnalités Principales
*   **Gestion Avancée des Permissions et des Rôles :** Un contrôle fin des droits au niveau des sous-projets (appelés "organes"). Les administrateurs peuvent définir des rôles sur mesure à partir d'une liste détaillée de permissions (par exemple, valider, créer, modifier ou supprimer des tâches pour soi-même ou pour d'autres).
*   **Gestion de Tâches Sophistiquée :** Définissez des tâches avec un nom, une échéance, un statut (en attente, en cours, terminée) et une échelle de priorité de 1 à 10. Un filtre dédié permet de trier par priorité et par date d'échéance.
*   **Outils d'Organisation et de Suivi :** Gérez un calendrier avec des échéances et une intégration potentielle avec Google Calendar. Toutes les notifications sont gérées en interne dans l'application.

### Expérience Utilisateur (UX)
*   **Tableau de Bord :** Le tableau de bord principal met en évidence les tâches urgentes et utilise un dégradé de couleurs (du rose au rouge) pour représenter visuellement les priorités des projets.
*   **Authentification Sécurisée :** Hachage de mot de passe standard et une option de connexion avec Google.
*   **Panneau de Navigation :** Un panneau de navigation unique comprend des outils comme une "Roulette de choix au hasard" et une liste de liens externes.

### Fonctionnalités Techniques
*   **Gestion des Comptes Utilisateurs :** Opérations CRUD (Créer, Lire, Mettre à jour, Supprimer) complètes pour les comptes utilisateurs.
*   **Sécurité :** La communication sécurisée avec la base de données et le hachage des mots de passe sont mis en œuvre.
*   **Journalisation des Activités :** Des journaux de sécurité et d'activité sont maintenus au niveau du projet.

---

## Architecture Technique

L'application est composée de plusieurs microservices orchestrés par Docker Compose. Chaque service s'exécute dans son propre conteneur et communique sur un réseau partagé.

*   **Frontend (`front-nginx`, `front-php`) :** La partie de l'application visible par l'utilisateur, construite avec Symfony et JavaScript. Elle gère l'interface utilisateur et interagit avec l'API backend.
*   **Backend (`api-nginx`, `api-php`) :** Le cœur de l'application, une API Symfony qui gère la logique métier, les données et l'authentification des utilisateurs.
*   **Base de Données (`database`) :** Une base de données MySQL pour la persistance des données.
*   **Cache (`valkey`) :** Un système de stockage de données en mémoire utilisé pour la mise en cache, basé sur Valkey (un fork de Redis).
*   **Hub Temps Réel (`mercure`) :** Un hub Mercure pour pousser des mises à jour en temps réel aux clients.
*   **Admin Base de Données (`phpmyadmin`) :** Une interface web pour gérer la base de données MySQL.
*   **Récupérateur d'Emails (`mailpit`) :** Un serveur d'emails local pour intercepter et visualiser les emails envoyés par l'application pendant le développement.

### Schéma du Backend

Le diagramme suivant illustre l'architecture générale de l'API backend :

![Schéma du Backend](docs/Backend.png)

---

## Configuration de l'Environnement

Avant de lancer l'application, vous devez configurer les variables d'environnement. Le projet utilise plusieurs fichiers `.env` qui sont ignorés par Git et doivent être créés manuellement.

### 1. Fichier `.env` à la racine

Ce fichier configure les services de base de `docker-compose`. Créez un fichier nommé `.env` à la racine du projet.

```dotenv
# Identifiants pour le service MySQL
MYSQL_ROOT_PASSWORD=root_password
MYSQL_DATABASE=app_database
MYSQL_USER=app_user
MYSQL_PASSWORD=app_password

# Identifiants pour le service phpMyAdmin
PMA_USER=root
PMA_PASSWORD=root_password
```

### 2. Fichier `.env.local` pour l'API Backend

Créez le fichier `api/.env.local` pour surcharger les valeurs par défaut.

```dotenv
# api/.env.local

# Clé secrète Symfony
APP_SECRET=votre_super_secret_a_remplacer

# URL de connexion à la base de données (doit correspondre au .env racine)
DATABASE_URL="mysql://app_user:app_password@database:3306/app_database?serverVersion=8.0&charset=utf8mb4"

# URL du service de cache (Valkey/Redis)
REDIS_URL=redis://organ_valkey:6379

# Origines autorisées pour les requêtes CORS
CORS_ALLOW_ORIGIN='^https?://(localhost|127\.0\.0\.1)(:[0-9]+)?$'

# Configuration Google Auth
GOOGLE_CLIENT_ID=votre_client_id_google.apps.googleusercontent.com

# Configuration Mercure
MERCURE_URL=http://mercure/.well-known/mercure
MERCURE_PUBLIC_URL=http://localhost:9090/.well-known/mercure
MERCURE_JWT_TOKEN="!ChangeThisMercureHubJWTSecretKey!"
NOTIFICATION_BASE_URL=http://localhost:8000

# Configuration JWT (LexikJWT)
JWT_PASSPHRASE=organ_passphrase
```

### 3. Fichier `.env.local` pour le Frontend

Créez le fichier `front/.env.local`.

```dotenv
# front/.env.local

APP_ENV=dev
APP_SECRET=votre_secret_front

# URL de base du front
DEFAULT_URI=http://localhost:8000

# URL de l'API (nom du service Docker)
API_URL=http://api-nginx

# Configuration Google Auth (doit être le même que l'API)
GOOGLE_CLIENT_ID=votre_client_id_google.apps.googleusercontent.com
```

---

## Installation et Lancement

Pour configurer le projet localement :

1.  **Clonez le dépôt :**
    ```bash
    git clone <repository-url>
    cd organ
    ```

2.  **Configurez les fichiers `.env`** comme décrit ci-dessus.

3.  **Construisez et Démarrez les Conteneurs :**
    ```bash
    docker-compose build
    docker-compose up -d
    ```

4.  **Installation des dépendances et initialisation :**

    *   **Installer Composer pour les deux services :**
        ```bash
        docker-compose exec api-php composer install
        docker-compose exec front-php composer install
        ```

    *   **Générer les clés JWT (API) :**
        ```bash
        docker-compose exec api-php php bin/console lexik:jwt:generate-keypair
        ```

    *   **Initialiser la Base de Données :**
        ```bash
        docker-compose exec api-php php bin/console doctrine:migrations:migrate --no-interaction
        ```

5.  **Build des Assets (Tailwind) :**
    Si vous êtes sur Windows ou si le mode `watch` ne fonctionne pas, vous devez recompiler manuellement les styles Tailwind après chaque modification CSS :
    ```bash
    docker-compose exec front-php php bin/console tailwind:build
    ```

6.  **Accès aux Services :**
    *   **Frontend :** [http://localhost:8000](http://localhost:8000)
    *   **API Backend :** [http://localhost:8001](http://localhost:8001)
    *   **phpMyAdmin :** [http://localhost:8080](http://localhost:8080)
    *   **Mailpit :** [http://localhost:8025](http://localhost:8025)
