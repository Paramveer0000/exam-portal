-- Single source of truth for role rows (previously seeded from Java on each boot).
-- INSERT IGNORE keeps this idempotent on databases that already contain USER/ADMIN.
INSERT IGNORE INTO roles (role_name, role_description) VALUES
    ('USER',        'Default role assigned to every self-registered user (student).'),
    ('ADMIN',       'Manages own categories, quizzes, questions and reports.'),
    ('SUPER_ADMIN', 'Manages admins and platform settings; inherits all ADMIN abilities.');
