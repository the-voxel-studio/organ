# Organ : Gestion de Projets Collaboratifs

Organ est une application de gestion de projets collaboratifs dont la principale caractéristique est un système de permissions hautement granulaire et personnalisable.

## Fonctionnalités Clés

### Fonctionnalités Principales
*   **Gestion Avancée des Permissions et des Rôles :** Un contrôle fin des droits au niveau des sous-projets (appelés "Organ"). Les administrateurs peuvent définir des rôles sur mesure à partir d'une liste détaillée de permissions (par exemple, valider, créer, modifier ou supprimer des tâches pour soi-même ou pour d'autres).
*   **Gestion de Tâches Sophistiquée :** Définissez des tâches avec un nom, une échéance, un statut (en attente, en cours, terminée) et une échelle de priorité de 1 à 10. Un filtre dédié permet de trier par priorité et par statut.
*   **Outils d'Organisation et de Suivi :** Gérez un calendrier avec des échéances et une intégration potentielle avec Google Calendar. Toutes les notifications sont gérées en interne dans l'application.

### Expérience Utilisateur (UX)
*   **Tableau de Bord :** Le tableau de bord principal met en évidence les tâches urgentes et utilise un dégradé de couleurs (du rose au rouge) pour représenter visuellement les priorités des projets.
*   **Authentification Sécurisée :** Hachage de mot de passe standard et une option de connexion avec Google.
*   **Panneau de Navigation :** Un panneau de navigation unique comprend des outils et une liste de liens externes.

### Fonctionnalités Techniques
*   **Gestion des Comptes Utilisateurs :** Opérations CRUD (Créer, Lire, Mettre à jour, Supprimer) complètes pour les comptes utilisateurs.
*   **Sécurité :** La communication sécurisée avec la base de données et le hachage des mots de passe sont mis en œuvre.

---

## Architecture Technique

L'application est composée de plusieurs microservices orchestrés par Docker Compose. Chaque service s'exécute dans son propre conteneur et communique sur un réseau partagé.

*   **Frontend PHP (`front-nginx`, `front-php`) :** L'interface utilisateur historique construite avec Symfony (Twig) et JavaScript (Stimulus/Tailwind). Elle est accessible sur [http://localhost:8000](http://localhost:8000).
*   **Frontend Angular (`front-angular`) :** Le nouveau frontend moderne construit avec Angular, accessible sur [http://localhost:4200](http://localhost:4200).
*   **Backend (`api-nginx`, `api-php`) :** Le cœur de l'application, une API Symfony qui gère la logique métier, les données et l'authentification des utilisateurs. Accessible sur [http://localhost:8001](http://localhost:8001).
*   **Base de Données relationnelle (`database`) :** Une base de données MySQL pour la persistance des données relationnelles structurées.
*   **Base de Données NoSQL (`mongodb`) :** Une base de données MongoDB pour stocker l'historique, les données analytiques et les fichiers.
*   **Cache (`valkey`) :** Un système de stockage de données en mémoire utilisé pour la mise en cache, basé sur Valkey (un fork de Redis).
*   **Hub Temps Réel (`mercure`) :** Un hub Mercure pour pousser des mises à jour en temps réel aux clients.
*   **Admin Base de Données (`phpmyadmin`) :** Une interface web pour gérer la base de données MySQL.

### Schéma du Backend

Le diagramme suivant illustre l'architecture générale de l'API backend :

![Schéma du Backend](docs/Backend.png)

---

## Configuration de l'Environnement

Le projet nécessite quatres fichiers `.env`. **Ne réfléchissez pas trop :** les valeurs ci-dessous sont des exemples valides pour le développement local. Copiez-collez sans crainte.

> **Important :** Dans les exemples ci-dessous, les valeurs entourées de `< >` (ex: `<VOTRE_ID_CLIENT>`) doivent être remplacées par vos propres valeurs réelles obtenues lors de la configuration de vos services (Google Cloud, etc.) ou des placeholders valides.

> **Règle d'or des secrets :** Pour `APP_SECRET`, `JWT_PASSPHRASE` et surtout `MERCURE_JWT_SECRET`, utilisez des chaînes de caractères **longues (min. 32 caractères)**. Une clé trop courte (ex: "123") fera planter le serveur API sans message d'erreur explicite.

### 1. Racine du projet (`.env`)
Configure les identifiants techniques pour Docker.
```dotenv
# Identifiants pour le service MySQL
MYSQL_ROOT_PASSWORD=root_password
MYSQL_DATABASE=app_database
MYSQL_USER=app_user
MYSQL_PASSWORD=app_password

# Identifiants pour le service phpMyAdmin
PMA_USER=root
PMA_PASSWORD=root_password

# Mercure Realtime Hub
MERCURE_JWT_SECRET=!ChangeThisMercureHubJWTSecretKey!

# Identifiants pour le service MongoDB
MONGODB_ROOT_USER=root
MONGODB_ROOT_PASSWORD=mongodb_root_password
MONGODB_DATABASE=organ

# Identifiants Google
GOOGLE_CLIENT_ID=<VOTRE_ID_CLIENT>.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=<VOTRE_SECRET_CLIENT>
```

### 2. API Backend (`api/.env`)
```dotenv
###> SYMFONY CORE ###
APP_ENV=dev
# IMPORTANT: 32 caractères minimum
APP_SECRET=a_very_long_random_string_of_at_least_32_chars
APP_SHARE_DIR=var/share
DEFAULT_URI=http://localhost
###< SYMFONY CORE ###

###> INFRASTRUCTURE ###
TRUSTED_PROXIES=127.0.0.1,REMOTE_ADDR
TRUSTED_HEADERS=x-forwarded-for,x-forwarded-host,x-forwarded-proto,x-forwarded-port
CORS_ALLOW_ORIGIN='^https?://(localhost|127\.0\.0\.1):(8000|8001|4200)$'
###< INFRASTRUCTURE ###

###> DATABASE & CACHE ###
DATABASE_URL="mysql://app_user:app_password@database:3306/app_database?serverVersion=8.0.32&charset=utf8mb4"
REDIS_URL=redis://organ_valkey:6379
MONGODB_URI="mongodb://root:mongodb_root_password@mongodb:27017/?authSource=admin"
MONGODB_DB="organ_dev"
###< DATABASE & CACHE ###

###> AUTHENTICATION ###
JWT_SECRET_KEY=%kernel.project_dir%/config/jwt/private.pem
JWT_PUBLIC_KEY=%kernel.project_dir%/config/jwt/public.pem
# IMPORTANT: 32 caractères minimum
JWT_PASSPHRASE=another_long_random_passphrase_for_jwt
GOOGLE_CLIENT_ID=<VOTRE_ID_CLIENT>.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=<VOTRE_SECRET_CLIENT>
COOKIE_SECURE=false
###< AUTHENTICATION ###

###> REALTIME (Mercure) ###
MERCURE_URL=http://mercure/.well-known/mercure
MERCURE_PUBLIC_URL=http://localhost:8000/hub
# IMPORTANT: 32 caractères minimum obligatoire (doit être identique à la racine)
MERCURE_JWT_SECRET=!ChangeThisMercureHubJWTSecretKey!
NOTIFICATION_BASE_URL=http://localhost:8000
###< REALTIME ###

###> NVIDIA AI CHATBOT ###
NVIDIA_API_KEY=<nvapi-clé_api>
NVIDIA_MODEL=mistralai/mistral-medium-3.5-128b #Model avec Free Endpoints (Overkill pour l'utilisation)
###< NVIDIA AI CHATBOT ###
```

### 3. Frontend PHP (`front_php/.env`)
```dotenv
###> SYMFONY CORE ###
APP_ENV=dev
APP_SECRET=another_secret_for_the_front_app
###< SYMFONY CORE ###

###> SYMFONY ROUTING ###
DEFAULT_URI=http://localhost:8000
###< SYMFONY ROUTING ###

###> Configuration API ###
API_URL=http://api-nginx
###< Configuration API ###

###> AUTHENTICATION ###
GOOGLE_CLIENT_ID=<VOTRE_ID_CLIENT>.apps.googleusercontent.com
###< AUTHENTICATION ###
```
### 4. Frontend Angular (`front_angular/.env`)
```dotenv
NG_APP_GOOGLE_CLIENT_ID=<VOTRE_ID_CLIENT>.apps.googleusercontent.com
```

---

## 🛠 Tuto : Configurer Google Cloud (OAuth & Drive)

Pour que la connexion Google et l'export Drive fonctionnent, vous devez créer une "Application" chez Google :

1.  **Console Google Cloud :** Allez sur [console.cloud.google.com](https://console.cloud.google.com).
2.  **Projet :** Créez un nouveau projet (bouton en haut à gauche).
3.  **Bibliothèque API :** Cherchez "Google Drive API" et cliquez sur **Activer**.
4.  **Écran de consentement OAuth :**
    *   Type : **Externe**.
    *   **Infos de base :** Donnez un nom ("Organ Dev") et votre mail de support.
    *   **Scopes (Champs d'application) :** Cliquez sur "Ajouter ou supprimer des champs". Ajoutez manuellement le scope : `https://www.googleapis.com/auth/drive.file` (C'est lui qui permet de gérer les fichiers créés par l'app).
    *   **Utilisateurs de test :** Ajoutez votre adresse Gmail. Sans cela, vous ne pourrez pas vous connecter.
5.  **Identifiants :**
    *   Cliquez sur "Créer des identifiants" -> **ID de client OAuth**.
    *   Type : **Application Web**.
    *   **Origines JavaScript autorisées :** `http://localhost:8000` et `http://localhost:8001`.
    *   **URIs de redirection autorisés :** `http://localhost:8000/login/google/check`.
6.  **Récupération :** Copiez l'**ID Client** et le **Secret Client** dans vos fichiers `.env`.

---

## Installation et Lancement

> **Architecture AMD64 :** Le projet force la plateforme `linux/amd64` dans Docker pour garantir que Composer fonctionne sur Mac et PC de la même manière.

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
        
5.  **Build des Assets (Tailwind) :**
    Si vous êtes sur Windows ou si le mode `watch` ne fonctionne pas, vous devez recompiler manuellement les styles Tailwind après chaque modification CSS :
    ```bash
    docker-compose exec front-php php bin/console tailwind:build
    ```
6.  **Astuce Cache Symfony (Le message KO) :**
    Si vous lancez un `cache:clear` (ou n'importe quelle commande Symfony) et qu'elle renvoie un message **KO** (ex: "Directory not empty") :
    **C'est normal.** Symfony essaie de supprimer des dossiers pendant qu'il écrit dedans. 
    **Solution :** Relancez simplement la commande `docker-compose exec container php bin/console cache:clear` jusqu'à ce que le message disparaisse.

### Accès aux Services :
*   **Application PHP:** [http://localhost:8000](http://localhost:8000)
*   **Application Angular:** [http://localhost:4200](http://localhost:4200)
*   **Documentation API :** [http://localhost:8001/api/docs](http://localhost:8001/api/docs)

---

## Données de Test (Seed)

Pour faciliter le développement, un fichier `seed.sql` est disponible à la racine du projet. Il contient des utilisateurs, des projets, des Organs et des tâches de démonstration.

### 1. Import via phpMyAdmin
1.  Accédez à phpMyAdmin : [http://localhost:8080](http://localhost:8080).
2.  Connectez-vous avec les identifiants définis dans votre fichier `.env` (par défaut `root` / `root_password`).
3.  Sélectionnez la base de données `app_database` dans la colonne de gauche.
4.  Cliquez sur l'onglet **Importer** en haut de la page.
5.  Choisissez le fichier `seed.sql` présent à la racine du projet.
6.  Cliquez sur **Importer** en bas de page.

### 2. Import MongoDb
A la racine du projet, executez cette commande : 
```bash
docker compose exec -T api-php php bin/console app:seed:mock-analytics
```

### 3. Comptes de Test
Tous les comptes ci-dessous utilisent le mot de passe : `password`

*   **Administrateur :** `admin@organ.com` (Jean Dupont)
*   **Manager :** `manager@organ.com` (Marie Curie)
*   **Développeur :** `dev@organ.com` (Albert Einstein)
*   **Utilisateur :** `user@organ.com` (Nikola Tesla)
*   **Membre :** `grace@organ.com` (Grace Hopper)
