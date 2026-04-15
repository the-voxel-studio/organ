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

Avant de lancer l'application, vous devez configurer les variables d'environnement. Le projet utilise deux fichiers `.env` qui sont ignorés par Git et doivent être créés manuellement.

### 1. Fichier `.env` à la racine

Ce fichier configure les services de base de `docker-compose`. Créez un fichier nommé `.env` à la racine du projet et remplissez-le comme suit. Les valeurs fournies ici sont des exemples pour un environnement de développement local.

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

### 2. Fichier `.env.local` pour l'API

Ce fichier configure l'application Symfony (le backend). Conformément aux bonnes pratiques de Symfony, il est recommandé de créer un fichier `.env.local` dans le dossier `/api` pour surcharger les valeurs par défaut du fichier `.env` de l'API.

Créez le fichier `api/.env.local` et ajoutez les variables suivantes. La plus importante est `DATABASE_URL`, qui doit correspondre aux identifiants définis dans le fichier `.env` de la racine.

```dotenv
# api/.env.local

# Clé secrète pour la sécurité de l'application (à changer pour une chaîne aléatoire)
APP_SECRET=votre_super_secret_a_remplacer

# URL de connexion à la base de données
# Assurez-vous que les identifiants correspondent à ceux du .env à la racine
DATABASE_URL="mysql://app_user:app_password@database:3306/app_database?serverVersion=8.0&charset=utf8mb4"

# URL du service de cache (Valkey/Redis)
REDIS_URL=redis://organ_valkey:6379

# Origines autorisées pour les requêtes CORS (Cross-Origin)
# Permet à votre frontend (ex: http://localhost:8000) de communiquer avec l'API
CORS_ALLOW_ORIGIN='^https?://(localhost|127\.0\.0\.1)(:[0-9]+)?$'
```

### 3. Fichier `.env` pour le Front

Ce fichier configure l'application Symfony (le frontend). Créez un fichier `.env` dans le dossier `/front`.

```dotenv
# front/.env

###> symfony/framework-bundle ###
APP_ENV=dev
APP_SECRET=9214736f860136209ed889248737f5d0
###< symfony/framework-bundle ###

###> symfony/routing ###
# URL de base du front pour la génération d'URL (CLI/Notifications)
DEFAULT_URI=http://localhost:8000
###< symfony/routing ###

###> Configuration API ###
# URL de l'API pour les appels HttpClient
API_URL=http://localhost:8001
###< Configuration API ###
```

---

## Installation Locale

Pour configurer le projet localement, vous aurez besoin de [Git](https://git-scm.com/) et [Docker](https://www.docker.com/) installés sur votre machine.

1.  **Clonez le dépôt :**
    ```bash
    git clone <repository-url>
    cd organ
    ```
    *(N'oubliez pas de remplacer `<repository-url>` par l'URL réelle du dépôt Git.)*

2.  **Configurez les variables d'environnement** comme décrit dans la section "Configuration de l'Environnement" ci-dessus.

3.  **Construisez et Démarrez les Conteneurs :**
    ```bash
    docker-compose build
    docker-compose up -d
    ```

4.  **Installez les dépendances de l'API :**
    Le répertoire `vendor` de l'API n'étant pas suivi par Git, vous devez installer les dépendances PHP à l'intérieur du conteneur.
    ```bash
    docker-compose exec api-php composer install
    docker-compose exec front-php composer install
    ```

5.  **Accédez aux Services :**
    *   **Frontend :** [http://localhost:8000](http://localhost:8000)
    *   **API Backend :** [http://localhost:8001](http://localhost:8001)
    *   **phpMyAdmin :** [http://localhost:8080](http://localhost:8080)
    *   **Mailpit (Visualiseur d'emails) :** [http://localhost:8025](http://localhost:8025)
    *   **Hub Mercure :** [http://localhost:9090](http://localhost:9090)
