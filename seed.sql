-- =========================================================================
-- SEED DATA FOR ORGAN PROJECT (UTF8MB4 SUPPORT)
-- =========================================================================

-- Ensure connection uses utf8mb4 for emojis
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- Disable foreign key checks for cleanup
SET FOREIGN_KEY_CHECKS = 0;

-- Delete all data
DELETE FROM notifications;
DELETE FROM task_dependencies;
DELETE FROM task_tags;
DELETE FROM tags;
DELETE FROM task_comments;
DELETE FROM task_attachments;
DELETE FROM task_links;
DELETE FROM task_assignees;
DELETE FROM tasks;
DELETE FROM user_organ_roles;
DELETE FROM organ_role_permissions;
DELETE FROM organ_roles;
DELETE FROM organ_links;
DELETE FROM organs;
DELETE FROM project_invitations;
DELETE FROM project_members;
DELETE FROM project_drive_configs;
DELETE FROM projects;
DELETE FROM user_sessions;
DELETE FROM users;

-- Reset auto-increment counters
ALTER TABLE notifications AUTO_INCREMENT = 1;
ALTER TABLE task_dependencies AUTO_INCREMENT = 1;
ALTER TABLE task_tags AUTO_INCREMENT = 1;
ALTER TABLE tags AUTO_INCREMENT = 1;
ALTER TABLE task_comments AUTO_INCREMENT = 1;
ALTER TABLE task_attachments AUTO_INCREMENT = 1;
ALTER TABLE task_links AUTO_INCREMENT = 1;
ALTER TABLE task_assignees AUTO_INCREMENT = 1;
ALTER TABLE tasks AUTO_INCREMENT = 1;
ALTER TABLE user_organ_roles AUTO_INCREMENT = 1;
ALTER TABLE organ_roles AUTO_INCREMENT = 1;
ALTER TABLE organ_links AUTO_INCREMENT = 1;
ALTER TABLE organs AUTO_INCREMENT = 1;
ALTER TABLE project_invitations AUTO_INCREMENT = 1;
ALTER TABLE project_members AUTO_INCREMENT = 1;
ALTER TABLE project_drive_configs AUTO_INCREMENT = 1;
ALTER TABLE projects AUTO_INCREMENT = 1;
ALTER TABLE user_sessions AUTO_INCREMENT = 1;
ALTER TABLE users AUTO_INCREMENT = 1;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- =========================================================================
-- USERS
-- Standard BCrypt hash for "password": $2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi
-- =========================================================================
INSERT INTO users (id, uuid, first_name, last_name, email, password, is_verified, jwt_version) VALUES
(1, UUID(), 'Jean', 'Dupont', 'admin@organ.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 1, 1),
(2, UUID(), 'Marie', 'Curie', 'manager@organ.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 1, 1),
(3, UUID(), 'Albert', 'Einstein', 'dev@organ.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 1, 1),
(4, UUID(), 'Nikola', 'Tesla', 'user@organ.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 1, 1),
(5, UUID(), 'Grace', 'Hopper', 'grace@organ.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 1, 1);

-- =========================================================================
-- PROJECTS
-- =========================================================================
INSERT INTO projects (id, uuid, title, description, color, status, icon_type, icon_data, deleted_at) VALUES
(1, UUID(), 'Organ Platform', 'Developing the core Organ project management platform.', '#4A90E2', 'ACTIVE', 'EMOJI', '🧬', NULL),
(2, UUID(), 'Marketing 2026', 'Global marketing strategy and content creation.', '#F5A623', 'ACTIVE', 'EMOJI', '📢', NULL),
(3, UUID(), 'Old Archive', 'Archived project from 2025.', '#9B9B9B', 'ARCHIVED', 'EMOJI', '📦', NULL),
(4, UUID(), 'Deleted Project', 'This project is in the trash.', '#D0021B', 'ACTIVE', 'EMOJI', '🗑️', NOW());

-- =========================================================================
-- PROJECT MEMBERS
-- =========================================================================
INSERT INTO project_members (uuid, project_id, user_id, global_role) VALUES
(UUID(), 1, 1, 'ADMIN'), (UUID(), 1, 2, 'MANAGER'), (UUID(), 1, 3, 'MEMBER'), (UUID(), 1, 4, 'MEMBER'), (UUID(), 1, 5, 'MEMBER'),
(UUID(), 2, 1, 'ADMIN'), (UUID(), 2, 5, 'MANAGER'), (UUID(), 2, 4, 'MEMBER'), (UUID(), 2, 3, 'MEMBER'),
(UUID(), 3, 1, 'ADMIN'),
(UUID(), 4, 1, 'ADMIN');

-- =========================================================================
-- ORGANS
-- =========================================================================
INSERT INTO organs (id, uuid, project_id, title, description, icon_type, icon_data, highlight_color, deleted_at) VALUES
(1, UUID(), 1, 'API Core', 'Symfony 7 backend architecture.', 'EMOJI', '🐘', '#800080', NULL),
(2, UUID(), 1, 'Web Interface', 'Modern dashboard with Stimulus/Turbo.', 'EMOJI', '🎨', '#00FF00', NULL),
(3, UUID(), 2, 'Branding', 'Logos, colors and typography.', 'EMOJI', '🎨', '#F5A623', NULL),
(4, UUID(), 2, 'Social Media', 'Content for X and LinkedIn.', 'EMOJI', '🐦', '#1DA1F2', NULL),
(5, UUID(), 1, 'Infrastructure', 'Docker, CI/CD and Valkey setup.', 'EMOJI', '🏗️', '#4A90E2', NULL),
(6, UUID(), 2, 'Trash Organ', 'This organ was deleted.', 'EMOJI', '👻', '#000000', NOW());

-- =========================================================================
-- ORGAN ROLES
-- =========================================================================
INSERT INTO organ_roles (id, uuid, organ_id, name, icon_type, icon_data) VALUES
(1, UUID(), 1, 'Backend Lead', 'EMOJI', '👑'),
(2, UUID(), 1, 'Developer', 'EMOJI', '💻'),
(3, UUID(), 2, 'Frontend Lead', 'EMOJI', '🎨'),
(4, UUID(), 3, 'Designer', 'EMOJI', '✏️'),
(5, UUID(), 4, 'Community Manager', 'EMOJI', '🤳'),
(6, UUID(), 5, 'DevOps', 'EMOJI', '⚙️'),
(7, UUID(), 1, 'Manager', 'EMOJI', '💼');

-- Permissions mapping
INSERT INTO organ_role_permissions (role_id, permission_id) SELECT 1, id FROM permissions;
INSERT INTO organ_role_permissions (role_id, permission_id) SELECT 3, id FROM permissions;
INSERT INTO organ_role_permissions (role_id, permission_id) SELECT 6, id FROM permissions;
INSERT INTO organ_role_permissions (role_id, permission_id) SELECT 7, id FROM permissions WHERE name IN ('ORGAN_VIEW', 'TASK_CREATE', 'TASK_EDIT_OWN', 'COMMENT_CREATE', 'ATTACHMENT_ADD', 'TASK_STATUS_CHANGE_OWN', 'TASK_PRIORITY_CHANGE_OWN', 'TASK_DATES_MANAGE_OWN', 'TASK_ESTIMATE_MANAGE_OWN', 'TASK_ASSIGN_SELF', 'TASK_VALIDATE', 'TASK_LINK_MANAGE_OWN', 'TASK_TAG_MANAGE_OWN', 'TASK_TAG_HARD_DELETE', 'TASK_DEPENDENCY_MANAGE_OWN', 'COMMENT_EDIT_OWN', 'COMMENT_DELETE_OWN', 'COMMENT_HARD_DELETE_OWN', 'ATTACHMENT_DELETE_OWN', 'ATTACHMENT_HARD_DELETE_OWN');
INSERT INTO organ_role_permissions (role_id, permission_id) SELECT 2, id FROM permissions WHERE name IN ('ORGAN_VIEW', 'TASK_CREATE', 'TASK_EDIT_OWN', 'COMMENT_CREATE', 'ATTACHMENT_ADD');
INSERT INTO organ_role_permissions (role_id, permission_id) SELECT 4, id FROM permissions WHERE name IN ('ORGAN_VIEW', 'TASK_CREATE', 'TASK_EDIT_ALL', 'COMMENT_CREATE');
INSERT INTO organ_role_permissions (role_id, permission_id) SELECT 5, id FROM permissions WHERE name IN ('ORGAN_VIEW', 'TASK_CREATE', 'TASK_STATUS_CHANGE_ALL', 'COMMENT_CREATE');

-- User Roles mapping
INSERT INTO user_organ_roles (user_id, role_id) VALUES 
(1, 1), (2, 7), (3, 2), (4, 3), (5, 4), (2, 5), (1, 6);

-- =========================================================================
-- TAGS
-- =========================================================================
INSERT INTO tags (id, uuid, project_id, name, color) VALUES
(1, UUID(), 1, 'Bug', '#FF0000'),
(2, UUID(), 1, 'Feature', '#00FF00'),
(3, UUID(), 1, 'Security', '#000000'),
(4, UUID(), 2, 'Visual', '#E02020'),
(5, UUID(), 2, 'Urgent', '#F5A623'),
(6, UUID(), 2, 'Copywriting', '#4A90E2');

-- =========================================================================
-- TASKS
-- =========================================================================
INSERT INTO tasks (id, uuid, organ_id, created_by, manager_id, title, description, status, priority, estimated_hours, deleted_at) VALUES
(1, UUID(), 1, 1, 2, 'Design API Auth', 'Setup JWT and Refresh Token logic.', 'DONE', 3, 4.0, NULL),
(2, UUID(), 1, 1, 2, 'Organ CRUD', 'Implement all endpoints for Organ management.', 'IN_PROGRESS', 2, 8.0, NULL),
(3, UUID(), 1, 3, 1, 'Entity Validation', 'Add Symfony constraints to all entities.', 'TODO', 1, 4.0, NULL),
(4, UUID(), 1, 1, 1, 'Fix UUID bug', 'Some UUIDs are not properly formatted.', 'DONE', 3, 1.0, NOW()),
(5, UUID(), 2, 4, 1, 'Dashboard Layout', 'Responsive grid with Sidebar.', 'DONE', 3, 12.0, NULL),
(6, UUID(), 2, 4, 1, 'Stimulus Modal', 'Reusable modal controller.', 'IN_PROGRESS', 2, 6.0, NULL),
(7, UUID(), 2, 3, 4, 'Form Validation UI', 'Display API errors on forms.', 'WAITING', 2, 4.5, NULL),
(8, UUID(), 3, 5, 1, 'Logo Concept', 'Draft 3 options for the main logo.', 'DONE', 3, 8.0, NULL),
(9, UUID(), 3, 5, 1, 'Color Guidelines', 'Define HEX and RGB for branding.', 'DONE', 2, 2.0, NULL),
(10, UUID(), 3, 5, 1, 'Iconography Set', 'Design 20 custom SVG icons.', 'TODO', 1, 20.0, NULL),
(11, UUID(), 3, 5, 1, 'Rejected Font', 'Tried and failed.', 'CANCELED', 1, 0.0, NOW()),
(12, UUID(), 4, 1, 5, 'Launch Campaign', 'Teaser posts on X.', 'DONE', 3, 5.0, NULL),
(13, UUID(), 4, 4, 5, 'LinkedIn Thread', 'Explaining the tech stack.', 'IN_PROGRESS', 2, 3.0, NULL),
(14, UUID(), 4, 2, 1, 'Weekly Newsletter', 'Updates for early adopters.', 'TODO', 2, 4.0, NULL),
(15, UUID(), 5, 1, 1, 'Docker Compose Fix', 'Optimization for local dev.', 'DONE', 2, 2.0, NULL),
(16, UUID(), 5, 1, 1, 'Valkey Integration', 'Cache layer for sessions.', 'IN_PROGRESS', 3, 6.0, NULL);

-- =========================================================================
-- TASK ASSIGNEES
-- =========================================================================
INSERT INTO task_assignees (task_id, user_id) VALUES
(1, 1), (1, 3), (2, 3), (3, 3), (5, 4), (6, 4), (6, 3), (7, 3), (8, 5), (9, 5), (10, 5), (12, 1), (12, 4), (13, 4), (15, 1), (16, 1);

-- =========================================================================
-- TASK TAGS
-- =========================================================================
INSERT INTO task_tags (task_id, tag_id) VALUES
(1, 2), (1, 3), (2, 2), (3, 2), (4, 1), (5, 2), (6, 2), (8, 4), (9, 4), (10, 4), (12, 5), (13, 6);

-- =========================================================================
-- TASK COMMENTS
-- =========================================================================
INSERT INTO task_comments (uuid, task_id, user_id, content, created_at, updated_at) VALUES
(UUID(), 1, 2, 'Authentication logic is approved.', NOW(), NOW()),
(UUID(), 2, 3, 'Adding support for nested organs.', NOW(), NOW()),
(UUID(), 5, 1, 'The sidebar is a bit too wide on tablets.', NOW(), NOW()),
(UUID(), 8, 1, 'The second option is perfect.', NOW(), NOW());

-- =========================================================================
-- TASK ATTACHMENTS (Mocking MongoDB links)
-- =========================================================================
INSERT INTO task_attachments (uuid, task_id, uploaded_by, file_name, file_path, file_size, file_type, mongo_file_id, file_version, checksum, created_at) VALUES
(UUID(), 1, 1, 'architecture.png', 'mongo://645e12345678901234567890', 1048576, 'image/png', '645e12345678901234567890', 1, 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', NOW());
