-- =========================================================================
-- UTILISATEURS (Comptes locaux JWT + Google OAuth)
-- =========================================================================
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    first_name VARCHAR(100) NULL,
    last_name VARCHAR(100) NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    password VARCHAR(255) NULL,
    google_id VARCHAR(255) NULL UNIQUE,
    is_verified TINYINT(1) DEFAULT 0 NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at DATETIME NULL
);

-- =========================================================================
-- SESSIONS UTILISATEURS (Refresh Tokens & Appareils connectés)
-- =========================================================================
CREATE TABLE user_sessions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    user_id INT NOT NULL,
    refresh_token VARCHAR(128) NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL,
    valid DATETIME NOT NULL,
    ip_address VARCHAR(45) NULL, 
    user_agent VARCHAR(500) NULL, 
    device_name VARCHAR(100) NULL, 
    browser_name VARCHAR(100) NULL, 
    location VARCHAR(100) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_used_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_sessions_user ON user_sessions(user_id);
CREATE INDEX idx_sessions_expires ON user_sessions(valid);

-- =========================================================================
-- PROJETS (Niveau Global)
-- =========================================================================
CREATE TABLE projects (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    title VARCHAR(150) NOT NULL,
    description LONGTEXT NULL,
    status VARCHAR(255) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, ARCHIVED, INACTIVE', -- ACTIVE ARCHIVED INACTIVE
    icon_type VARCHAR(255) NOT NULL DEFAULT 'EMOJI' COMMENT 'SVG, BLOB, EMOJI', -- EMOJI SVG BLOB
    icon_data LONGTEXT NULL, 
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at DATETIME NULL
);

CREATE TABLE project_drive_configs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    project_id INT NOT NULL UNIQUE,
    drive_folder_id VARCHAR(255) NOT NULL,
    encrypted_refresh_token LONGTEXT NOT NULL,
    is_active TINYINT(1) DEFAULT 1 NOT NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- =========================================================================
-- MEMBRES DU PROJET (Rôles Globaux Fixes)
-- =========================================================================
CREATE TABLE project_members (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    project_id INT NOT NULL,
    user_id INT NOT NULL,
    global_role VARCHAR(255) DEFAULT 'MEMBER' NOT NULL COMMENT 'ADMIN, MANAGER, MEMBER', -- ADMIN MANAGER MEMBER 
    UNIQUE KEY (project_id, user_id),
    deleted_at DATETIME NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE project_invitations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    project_id INT NOT NULL,
    email VARCHAR(180) NOT NULL,
    role VARCHAR(255) NOT NULL DEFAULT 'MEMBER',
    token VARCHAR(64) NOT NULL UNIQUE,
    invited_by INT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expires_at DATETIME NOT NULL,
    accepted_at DATETIME NULL,
    
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (invited_by) REFERENCES users(id) ON DELETE SET NULL
);

-- =========================================================================
-- ORGANS (Groupes de tâches au sein d'un projet)
-- =========================================================================
CREATE TABLE organs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    project_id INT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description LONGTEXT NULL,
    icon_type VARCHAR(255) DEFAULT 'EMOJI' NOT NULL COMMENT 'SVG, BLOB, EMOJI',
    icon_data LONGTEXT NULL,
    highlight_color VARCHAR(9) DEFAULT '#000000' NOT NULL, 
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at DATETIME NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- =========================================================================
-- LIENS DES ORGANS
-- =========================================================================
CREATE TABLE organ_links (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    organ_id INT NOT NULL,
    url VARCHAR(2083) NOT NULL,
    description VARCHAR(255) NULL,
    deleted_at DATETIME NULL,
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
    uuid VARCHAR(36) NOT NULL UNIQUE,
    organ_id INT NOT NULL,
    name VARCHAR(50) NOT NULL,
    deleted_at DATETIME NULL,
    INDEX idx_organ (organ_id),
    FOREIGN KEY (organ_id) REFERENCES organs(id) ON DELETE CASCADE
);

CREATE TABLE organ_role_permissions (
    role_id INT NOT NULL,
    permission_id SMALLINT NOT NULL,
    PRIMARY KEY(role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES organ_roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

CREATE TABLE user_organ_roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    UNIQUE KEY (user_id, role_id),
    deleted_at DATETIME NULL,
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
    description LONGTEXT NULL,

    status VARCHAR(255) NOT NULL DEFAULT 'TODO' COMMENT 'TODO, IN_PROGRESS, WAITING, DONE, CANCELED', -- TODO IN_PROGRESS WAITING DONE CANCELED
    status_message LONGTEXT NULL,
    estimated_hours DECIMAL(10, 2) NULL,
    priority SMALLINT NOT NULL DEFAULT 1,

    start_date DATETIME NULL,
    expires_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    validated_at DATETIME NULL,
    deleted_at DATETIME NULL,

    FOREIGN KEY (organ_id) REFERENCES organs(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (manager_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (validated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- =========================================================================
-- ASSIGNÉS AUX TÂCHES
-- =========================================================================
CREATE TABLE task_assignees (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_id INT NOT NULL,
    user_id INT NOT NULL,
    UNIQUE KEY (task_id, user_id),
    deleted_at DATETIME NULL,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =========================================================================
-- LIENS DES TÂCHES
-- =========================================================================
CREATE TABLE task_links (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    task_id INT NOT NULL,
    url VARCHAR(2083) NOT NULL,
    description VARCHAR(255) NULL,
    deleted_at DATETIME NULL,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

-- =========================================================================
-- PIÈCES JOINTES DES TÂCHES
-- =========================================================================
CREATE TABLE task_attachments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    task_id INT NOT NULL,
    uploaded_by INT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size INT NOT NULL,
    file_type VARCHAR(100) NULL,
    created_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE RESTRICT
);

-- =========================================================================
-- COMMENTAIRES DES TÂCHES
-- =========================================================================
CREATE TABLE task_comments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    task_id INT NOT NULL,
    user_id INT NOT NULL,
    content LONGTEXT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =========================================================================
-- TAGS (Étiquettes personnalisables par projet)
-- =========================================================================
CREATE TABLE tags (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    project_id INT NOT NULL,
    name VARCHAR(50) NOT NULL,
    color VARCHAR(9) DEFAULT '#808080' NOT NULL,
    deleted_at DATETIME NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- =========================================================================
-- ASSOCIATION TÂCHES <-> TAGS
-- =========================================================================
CREATE TABLE task_tags (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_id INT NOT NULL,
    tag_id INT NOT NULL,
    UNIQUE KEY (task_id, tag_id),
    deleted_at DATETIME NULL,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

-- =========================================================================
-- DÉPENDANCES ENTRE TÂCHES (A bloqué B)
-- =========================================================================
CREATE TABLE task_dependencies (
    id INT AUTO_INCREMENT PRIMARY KEY,
    task_id INT NOT NULL,
    depends_on_task_id INT NOT NULL,
    UNIQUE KEY (task_id, depends_on_task_id),
    deleted_at DATETIME NULL,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (depends_on_task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

-- =========================================================================
-- HISTORIQUE DES TÂCHES (Audit Log)
-- =========================================================================
CREATE TABLE task_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    task_id INT NOT NULL,
    user_id INT NULL,
    action_type VARCHAR(50) NOT NULL,
    field_name VARCHAR(50) NULL,
    old_value LONGTEXT NULL,
    new_value LONGTEXT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- =========================================================================
-- NOTIFICATIONS UTILISATEURS
-- =========================================================================
CREATE TABLE notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    user_id INT NOT NULL,
    task_id INT NULL,
    type VARCHAR(50) NOT NULL,
    message LONGTEXT NOT NULL,
    is_read TINYINT(1) DEFAULT 0 NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at DATETIME NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL
);

-- =========================================================================
-- INDEXES ADDITIONNELS POUR OPTIMISATIONS
-- =========================================================================
CREATE INDEX idx_tasks_organ_status ON tasks(organ_id, status);
CREATE INDEX idx_tasks_manager_status ON tasks(manager_id, status);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read);
CREATE INDEX idx_task_history_timeline ON task_history(task_id, created_at);
CREATE INDEX idx_projects_deleted_at ON projects(deleted_at);
CREATE INDEX idx_organs_deleted_at ON organs(deleted_at);
CREATE INDEX idx_tasks_deleted_at ON tasks(deleted_at);
CREATE INDEX idx_project_members_deleted_at ON project_members (deleted_at);

-- =========================================================================
-- PERMISSIONS GENERALES
-- =========================================================================
INSERT INTO permissions (name) VALUES 
('ORGAN_VIEW'),
('ORGAN_EDIT'),
('ORGAN_MANAGE_ROLES'),
('ORGAN_LINK_MANAGE'),
('TASK_CREATE'),
('TASK_EDIT_OWN'),
('TASK_EDIT_ALL'),
('TASK_DELETE_OWN'),
('TASK_DELETE_ALL'),
('TASK_STATUS_CHANGE_OWN'),
('TASK_STATUS_CHANGE_ALL'),
('TASK_PRIORITY_CHANGE_OWN'),
('TASK_PRIORITY_CHANGE_ALL'),
('TASK_DATES_MANAGE_OWN'),
('TASK_DATES_MANAGE_ALL'),
('TASK_ESTIMATE_MANAGE_OWN'),
('TASK_ESTIMATE_MANAGE_ALL'),
('TASK_ASSIGN_SELF'),
('TASK_ASSIGN_OTHERS'),
('TASK_VALIDATE'),
('TASK_LINK_MANAGE_OWN'),
('TASK_LINK_MANAGE_ALL'),
('TASK_TAG_MANAGE_OWN'),
('TASK_TAG_MANAGE_ALL'),
('TASK_DEPENDENCY_MANAGE_OWN'),
('TASK_DEPENDENCY_MANAGE_ALL'),
('COMMENT_CREATE'),
('COMMENT_EDIT_OWN'),
('COMMENT_EDIT_ALL'),
('COMMENT_DELETE_OWN'),
('COMMENT_DELETE_ALL'),
('ATTACHMENT_ADD'),
('ATTACHMENT_DELETE_OWN'),
('ATTACHMENT_DELETE_ALL');