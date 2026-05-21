-- =========================================================================
-- SEED DATA FOR ORGAN PROJECT (UTF8MB4 SUPPORT)
-- =========================================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

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

SET FOREIGN_KEY_CHECKS = 1;

-- =========================================================================
-- USERS (Fixed UUIDs for NoSQL sync)
-- =========================================================================
INSERT INTO users (id, uuid, first_name, last_name, email, password, is_verified, jwt_version) VALUES
(1, '550e8400-e29b-41d4-a716-446655440000', 'Jean', 'Dupont', 'admin@organ.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 1, 1),
(2, '550e8400-e29b-41d4-a716-446655440001', 'Marie', 'Curie', 'manager@organ.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 1, 1),
(3, '550e8400-e29b-41d4-a716-446655440002', 'Albert', 'Einstein', 'dev@organ.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 1, 1),
(4, '550e8400-e29b-41d4-a716-446655440003', 'Nikola', 'Tesla', 'user@organ.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 1, 1),
(5, '550e8400-e29b-41d4-a716-446655440004', 'Grace', 'Hopper', 'grace@organ.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 1, 1);

-- =========================================================================
-- PROJECTS
-- =========================================================================
INSERT INTO projects (id, uuid, title, description, color, status, icon_type, icon_data, deleted_at) VALUES
(1, '660e8400-e29b-41d4-a716-446655440000', 'Organ Platform', 'Developing the core Organ project management platform.', '#4A90E2', 'ACTIVE', 'EMOJI', '🧬', NULL),
(2, '660e8400-e29b-41d4-a716-446655440001', 'Marketing 2026', 'Global marketing strategy and content creation.', '#F5A623', 'ACTIVE', 'EMOJI', '📢', NULL);

-- =========================================================================
-- PROJECT MEMBERS
-- =========================================================================
INSERT INTO project_members (uuid, project_id, user_id, global_role) VALUES
(UUID(), 1, 1, 'ADMIN'), (UUID(), 1, 2, 'MANAGER'), (UUID(), 1, 3, 'MEMBER'), (UUID(), 1, 4, 'MEMBER'), (UUID(), 1, 5, 'MEMBER'),
(UUID(), 2, 1, 'ADMIN'), (UUID(), 2, 5, 'MANAGER'), (UUID(), 2, 4, 'MEMBER'), (UUID(), 2, 3, 'MEMBER');

-- =========================================================================
-- ORGANS
-- =========================================================================
INSERT INTO organs (id, uuid, project_id, title, description, icon_type, icon_data, highlight_color, deleted_at) VALUES
(1, '770e8400-e29b-41d4-a716-446655440000', 1, 'API Core', 'Symfony 7 backend architecture.', 'EMOJI', '🐘', '#800080', NULL),
(2, '770e8400-e29b-41d4-a716-446655440001', 1, 'Web Interface', 'Modern SPA with Angular.', 'EMOJI', '🎨', '#00FF00', NULL);

-- =========================================================================
-- TASKS
-- =========================================================================
INSERT INTO tasks (id, uuid, organ_id, created_by, manager_id, title, description, status, priority, estimated_hours, deleted_at) VALUES
(1, '880e8400-e29b-41d4-a716-446655440000', 1, 1, 2, 'Design API Auth', 'Setup JWT and Refresh Token logic.', 'DONE', 3, 4.0, NULL),
(2, '880e8400-e29b-41d4-a716-446655440001', 1, 1, 2, 'Organ CRUD', 'Implement all endpoints for Organ management.', 'IN_PROGRESS', 2, 8.0, NULL),
(3, '880e8400-e29b-41d4-a716-446655440002', 1, 3, 1, 'Entity Validation', 'Add Symfony constraints to all entities.', 'TODO', 1, 4.0, NULL);

-- =========================================================================
-- TASK ATTACHMENTS (Mocking MongoDB links)
-- =========================================================================
INSERT INTO task_attachments (uuid, task_id, uploaded_by, file_name, file_path, file_size, file_type, mongo_file_id, file_version, checksum, created_at) VALUES
(UUID(), 1, 1, 'architecture.png', 'mongo://645e12345678901234567890', 1048576, 'image/png', '645e12345678901234567890', 1, 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', NOW());

-- =========================================================================
-- TASK COMMENTS
-- =========================================================================
INSERT INTO task_comments (uuid, task_id, user_id, content, created_at, updated_at) VALUES
(UUID(), 1, 2, 'Authentication logic is approved.', NOW(), NOW());
