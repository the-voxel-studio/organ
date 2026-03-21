-- =========================================================================
-- UTILISATEURS (Comptes locaux JWT + Google OAuth)
-- =========================================================================
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NULL,
    last_name VARCHAR(100) NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password VARCHAR(255) NULL,
    google_id VARCHAR(255) NULL UNIQUE,
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL -- [SOFT DELETE]
);

-- =========================================================================
-- PROJETS (Niveau Global)
-- =========================================================================
CREATE TABLE projects (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE, -- [CORRECTION] Virgule manquante ajoutée
    title VARCHAR(150) NOT NULL,
    description TEXT NULL,
    status ENUM('ACTIVE', 'ARCHIVED') DEFAULT 'ACTIVE',
    icon_type ENUM('svg', 'blob', 'emoji') DEFAULT 'emoji',
    icon_data TEXT NULL, 
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL -- [SOFT DELETE]
);

CREATE TABLE project_drive_configs (
    project_id INT PRIMARY KEY,
    drive_folder_id VARCHAR(255) NOT NULL,
    encrypted_refresh_token TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- =========================================================================
-- MEMBRES DU PROJET (Rôles Globaux Fixes)
-- =========================================================================
CREATE TABLE project_members (
    project_id INT NOT NULL,
    user_id INT NOT NULL,
    global_role ENUM('ADMIN', 'MEMBER') DEFAULT 'MEMBER', 
    PRIMARY KEY (project_id, user_id),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =========================================================================
-- ORGANS (Groupes de tâches au sein d'un projet)
-- =========================================================================
CREATE TABLE organs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) UNIQUE, -- [CORRECTION] Virgule manquante ajoutée
    project_id INT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT NULL,
    icon_type ENUM('svg', 'blob', 'emoji') DEFAULT 'emoji',
    icon_data TEXT NULL,
    highlight_color VARCHAR(7) DEFAULT '#000000', 
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL, -- [SOFT DELETE]
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- =========================================================================
-- LIENS DES ORGANS
-- =========================================================================
CREATE TABLE organ_links (
    id INT AUTO_INCREMENT PRIMARY KEY,
    organ_id INT NOT NULL,
    url VARCHAR(2083) NOT NULL,
    description VARCHAR(255) NULL,
    FOREIGN KEY (organ_id) REFERENCES organs(id) ON DELETE CASCADE
);

-- =========================================================================
-- GESTION DES RÔLES DYNAMIQUES
-- =========================================================================
CREATE TABLE permissions (
    id SMALLINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE organ_roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    organ_id INT NOT NULL,
    name VARCHAR(50) NOT NULL,
    INDEX idx_organ (organ_id), 
    FOREIGN KEY (organ_id) REFERENCES organs(id) ON DELETE CASCADE
);

CREATE TABLE organ_role_permissions (
    role_id INT NOT NULL,
    permission_id SMALLINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES organ_roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

CREATE TABLE user_organ_roles (
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    INDEX idx_role_user (role_id, user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES organ_roles(id) ON DELETE CASCADE
);

-- =========================================================================
-- TÂCHES 
-- =========================================================================
CREATE TABLE tasks (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    organ_id INT NOT NULL,
    
    created_by INT NULL,
    manager_id INT NULL,
    validated_by INT NULL,
    
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    
    status ENUM('TODO', 'IN_PROGRESS', 'WAITING', 'DONE', 'CANCELED') DEFAULT 'TODO',
    status_message TEXT NULL,
    priority TINYINT NOT NULL DEFAULT 1 CHECK (priority BETWEEN 1 AND 10),
    
    start_date DATETIME NULL,
    expires_at DATETIME NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    validated_at DATETIME NULL,
    deleted_at DATETIME NULL, -- [SOFT DELETE]
    
    FOREIGN KEY (organ_id) REFERENCES organs(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (manager_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (validated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- =========================================================================
-- ASSIGNÉS AUX TÂCHES
-- =========================================================================
CREATE TABLE task_assignees (
    task_id INT NOT NULL,
    user_id INT NOT NULL,
    PRIMARY KEY (task_id, user_id),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =========================================================================
-- LIENS DES TÂCHES
-- =========================================================================
CREATE TABLE task_links (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_id INT NOT NULL,
    url VARCHAR(2083) NOT NULL,
    description VARCHAR(255) NULL,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

-- =========================================================================
-- NOTIFICATIONS IN-APP
-- =========================================================================
CREATE TABLE notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    task_id INT NULL,
    message VARCHAR(255) NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

-- =========================================================================
-- COMMENTAIRES DES TÂCHES
-- =========================================================================
CREATE TABLE task_comments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_id INT NOT NULL,
    user_id INT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL, -- [SOFT DELETE] (Optionnel, utile si on peut supprimer un com)
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =========================================================================
-- HISTORIQUE DES TÂCHES
-- =========================================================================
CREATE TABLE task_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_id INT NOT NULL,
    user_id INT NULL,
    action VARCHAR(50) NOT NULL,
    old_value TEXT NULL,
    new_value TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- =========================================================================
-- TAGS & ÉTIQUETTES
-- =========================================================================
CREATE TABLE tags (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    name VARCHAR(50) NOT NULL,
    color VARCHAR(7) DEFAULT '#808080',
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE TABLE task_tags (
    task_id INT NOT NULL,
    tag_id INT NOT NULL,
    PRIMARY KEY (task_id, tag_id),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

-- =========================================================================
-- PIÈCES JOINTES
-- =========================================================================
CREATE TABLE task_attachments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_id INT NOT NULL,
    uploaded_by INT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(2083) NOT NULL,
    file_size INT NULL,
    file_type VARCHAR(50) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL, -- [SOFT DELETE] (Important pour nettoyer S3 plus tard)
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE
);

-- =========================================================================
-- DÉPENDANCES ENTRE TÂCHES
-- =========================================================================
CREATE TABLE task_dependencies (
    task_id INT NOT NULL,
    depends_on_task_id INT NOT NULL,
    PRIMARY KEY (task_id, depends_on_task_id),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (depends_on_task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

-- =========================================================================
-- INDEX D'OPTIMISATION
-- =========================================================================

CREATE INDEX idx_tasks_organ_status ON tasks(organ_id, status);
CREATE INDEX idx_tasks_manager_status ON tasks(manager_id, status);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read);
CREATE INDEX idx_task_history_timeline ON task_history(task_id, created_at);

-- Nouveaux index pour optimiser les requêtes de Soft Delete (filtrage classique)
CREATE INDEX idx_projects_deleted_at ON projects(deleted_at);
CREATE INDEX idx_organs_deleted_at ON organs(deleted_at);
CREATE INDEX idx_tasks_deleted_at ON tasks(deleted_at);