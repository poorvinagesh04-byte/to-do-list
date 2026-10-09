-- ============================================================================
-- Secure Smart To-Do List and Reminder Application - Database Schema
-- Compatible with MySQL 8+, PostgreSQL 14+, and H2 (PostgreSQL/MySQL mode)
-- ============================================================================

-- 1. USERS TABLE
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    role VARCHAR(30) NOT NULL DEFAULT 'ROLE_USER',
    theme VARCHAR(20) NOT NULL DEFAULT 'system',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);

-- 2. CATEGORIES TABLE
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    name VARCHAR(50) NOT NULL,
    color VARCHAR(20) NOT NULL DEFAULT '#4f46e5',
    icon VARCHAR(50) NOT NULL DEFAULT 'folder',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_categories_user ON categories(user_id);

-- 3. TASKS TABLE
CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    scheduled_date DATE NOT NULL,
    start_time TIME NULL,
    deadline TIMESTAMP NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tasks_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);

CREATE INDEX idx_tasks_user_date ON tasks(user_id, scheduled_date);
CREATE INDEX idx_tasks_user_status ON tasks(user_id, status);
CREATE INDEX idx_tasks_user_priority ON tasks(user_id, priority);
CREATE INDEX idx_tasks_deadline ON tasks(deadline);

-- 4. SUBTASKS TABLE
CREATE TABLE IF NOT EXISTS subtasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_subtasks_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

CREATE INDEX idx_subtasks_task_id ON subtasks(task_id);

-- 5. RECURRING SCHEDULES TABLE
CREATE TABLE IF NOT EXISTS recurring_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL UNIQUE,
    recurrence_type VARCHAR(20) NOT NULL, -- NONE, DAILY, WEEKLY, MONTHLY, YEARLY
    recurrence_interval INT NOT NULL DEFAULT 1,
    recurrence_end_date DATE NULL,
    last_generated_date DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recurring_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

CREATE INDEX idx_recurring_task_id ON recurring_schedules(task_id);

-- 6. REMINDERS TABLE
CREATE TABLE IF NOT EXISTS reminders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reminder_datetime TIMESTAMP NOT NULL,
    offset_minutes INT NOT NULL DEFAULT 0,
    delivery_method VARCHAR(30) NOT NULL DEFAULT 'IN_APP', -- IN_APP, BROWSER, PUSH
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',        -- PENDING, SENT, DISMISSED, CANCELLED
    deduplication_key VARCHAR(150) NOT NULL UNIQUE,
    sent_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reminders_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_reminders_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_reminders_status_time ON reminders(status, reminder_datetime);
CREATE INDEX idx_reminders_user_id ON reminders(user_id);
CREATE INDEX idx_reminders_task_id ON reminders(task_id);

-- 7. TASK HISTORY TABLE
CREATE TABLE IF NOT EXISTS task_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NULL,
    user_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL, -- CREATED, UPDATED, COMPLETED, REOPENED, DELETED, RESCHEDULED
    details TEXT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_history_user_time ON task_history(user_id, timestamp);

-- 8. PUSH NOTIFICATION SUBSCRIPTIONS TABLE
CREATE TABLE IF NOT EXISTS push_subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    endpoint TEXT NOT NULL,
    p256dh VARCHAR(255) NOT NULL,
    auth VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_push_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_push_user_id ON push_subscriptions(user_id);

-- 9. DEFAULT SYSTEM CATEGORIES SEED DATA
-- Default system categories (user_id is NULL for global presets)
INSERT INTO categories (user_id, name, color, icon) VALUES
    (NULL, 'College', '#3b82f6', 'graduation-cap'),
    (NULL, 'Work', '#10b981', 'briefcase'),
    (NULL, 'Personal', '#8b5cf6', 'user'),
    (NULL, 'Health', '#ef4444', 'heart-pulse'),
    (NULL, 'Shopping', '#f59e0b', 'shopping-cart'),
    (NULL, 'Finance', '#14b8a6', 'credit-card');

