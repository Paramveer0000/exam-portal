-- Support questions with multiple dimensions (e.g., visual + auditory learning styles).
-- Add new dimension types: Learning Preference (Visual, Auditory, Kinesthetic) and
-- Career Interest Assessment (13 domains).

-- Dimension master table.
CREATE TABLE dimensions (
    dimension_code VARCHAR(20) NOT NULL PRIMARY KEY,
    dimension_type VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed all valid dimensions.
INSERT INTO dimensions (dimension_code, dimension_type, display_name, description) VALUES
-- Multiple Intelligences (MI) — 9 types
('LOGICAL', 'MI', 'Logical-Mathematical', 'Mathematical, logical reasoning'),
('MUSICAL', 'MI', 'Musical-Rhythmic', 'Music, rhythm, sound sensitivity'),
('NATURALIST', 'MI', 'Naturalistic', 'Nature, environment, patterns'),
('VERBAL', 'MI', 'Verbal-Linguistic', 'Language, words, reading/writing'),
('INTERPERSONAL', 'MI', 'Interpersonal', 'Social, collaborative, empathy'),
('KINESTHETIC', 'MI', 'Bodily-Kinesthetic', 'Movement, physical coordination'),
('SPATIAL', 'MI', 'Visual-Spatial', 'Visual, spatial reasoning, imagery'),
('INTRAPERSONAL', 'MI', 'Intrapersonal', 'Self-awareness, introspection'),
('EXISTENTIAL', 'MI', 'Existential', 'Philosophy, meaning, purpose'),

-- RIASEC Career Model — 6 types
('R', 'RIASEC', 'Realistic', 'Hands-on, practical, technical work'),
('I', 'RIASEC', 'Investigative', 'Research, analysis, problem-solving'),
('A', 'RIASEC', 'Artistic', 'Creative expression, design, arts'),
('S', 'RIASEC', 'Social', 'People-focused, helping, teaching'),
('E', 'RIASEC', 'Enterprising', 'Leadership, persuasion, business'),
('C', 'RIASEC', 'Conventional', 'Organization, details, structure'),

-- Learning Preference — 3 types
('VISUAL_LEARNING', 'LEARNING_PREF', 'Visual Learning', 'Learns best through images and diagrams'),
('AUDITORY_LEARNING', 'LEARNING_PREF', 'Auditory Learning', 'Learns best through listening and discussion'),
('KINESTHETIC_LEARNING', 'LEARNING_PREF', 'Kinesthetic Learning', 'Learns best through hands-on practice'),

-- Career Interest Assessment — 13 domains
('ENGINEERING', 'CAREER_INTEREST', 'Engineering', 'Design, build, innovate'),
('MEDICAL', 'CAREER_INTEREST', 'Medical', 'Healthcare, healing, wellness'),
('MANAGEMENT', 'CAREER_INTEREST', 'Management', 'Leadership, organization, strategy'),
('ARTS', 'CAREER_INTEREST', 'Arts', 'Creative fields, design, culture'),
('COMMERCE', 'CAREER_INTEREST', 'Commerce', 'Business, trade, entrepreneurship'),
('GOVERNMENT', 'CAREER_INTEREST', 'Government', 'Public service, policy, administration'),
('ENTREPRENEURSHIP', 'CAREER_INTEREST', 'Entrepreneurship', 'Starting businesses, innovation'),
('TEACHING', 'CAREER_INTEREST', 'Teaching', 'Education, training, mentoring'),
('PSYCHOLOGY', 'CAREER_INTEREST', 'Psychology', 'Behavioral science, counseling'),
('DESIGN', 'CAREER_INTEREST', 'Design', 'Product design, UX, fashion'),
('HOSPITALITY', 'CAREER_INTEREST', 'Hospitality', 'Travel, tourism, service'),
('TECHNOLOGY', 'CAREER_INTEREST', 'Technology', 'IT, software, digital innovation'),
('ENVIRONMENT', 'CAREER_INTEREST', 'Environment', 'Sustainability, conservation, climate');

-- Many-to-many: questions can belong to multiple dimensions.
CREATE TABLE question_dimensions (
    question_id BIGINT NOT NULL,
    dimension_code VARCHAR(20) NOT NULL,
    PRIMARY KEY (question_id, dimension_code),
    FOREIGN KEY (question_id) REFERENCES questions(ques_id) ON DELETE CASCADE,
    FOREIGN KEY (dimension_code) REFERENCES dimensions(dimension_code) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Backfill: each existing question contributes its primary dimension to the join table.
INSERT INTO question_dimensions (question_id, dimension_code)
SELECT ques_id, dimension FROM questions WHERE dimension IS NOT NULL;

-- Index for fast dimension lookups.
CREATE INDEX idx_question_dimensions_dimension ON question_dimensions(dimension_code);
