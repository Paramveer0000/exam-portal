-- New dimension groups: Emotional Intelligence (EQ) and Leadership & Soft Skills.
-- Codes are short mnemonic strings, not the numeric list ids they were sourced
-- from -- display_name carries the full descriptive text.

INSERT INTO dimensions (dimension_code, dimension_type, display_name, description) VALUES
-- EQ
('ANGER_MGMT', 'EQ', 'Self-Regulation / Anger Management', NULL),
('RESILIENCE', 'EQ', 'Self-Motivation / Resilience', NULL),
('RESPONSE_CTRL', 'EQ', 'Self-Regulation / Response Control', NULL),
('EMPATHY', 'EQ', 'Empathy / Social Awareness', NULL),
('RELATIONSHIP_MGMT', 'EQ', 'Accountability / Relationship Management', NULL),
('CONFLICT_CTRL', 'EQ', 'Emotional Regulation / Conflict Control', NULL),
('ENCOURAGEMENT', 'EQ', 'Social Skills / Encouraging Others', NULL),
('FEEDBACK_ACCEPT', 'EQ', 'Self-Awareness / Feedback Acceptance', NULL),
('STRESS_MGMT', 'EQ', 'Stress Management', NULL),
('IMPULSE_CTRL', 'EQ', 'Impulse Control / Thoughtful Communication', NULL),
('SELF_MOTIVATION', 'EQ', 'Self-Motivation', NULL),

-- Leadership & Soft Skills
('OWNERSHIP', 'LEADERSHIP', 'Responsibility / Ownership', NULL),
('COMMUNICATION', 'LEADERSHIP', 'Communication', NULL),
('TEAMWORK', 'LEADERSHIP', 'Teamwork', NULL),
('DECISION_CONF', 'LEADERSHIP', 'Decision-Making Confidence', NULL),
('ADAPTABILITY', 'LEADERSHIP', 'Adaptability', NULL),
('CONFLICT_RES', 'LEADERSHIP', 'Conflict Resolution', NULL);
