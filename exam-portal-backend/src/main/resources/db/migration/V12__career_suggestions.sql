-- Career-guidance lookup: each field is driven by one or more dimensions
-- (comma-separated dimension codes, MI names or RIASEC letters). The scoring
-- service ranks fields by base_weight x the student's scores on those
-- dimensions. Data lives here (not in Java) so schools can be given new
-- fields with a migration instead of a release.
CREATE TABLE career_suggestions (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    field        VARCHAR(80)  NOT NULL,
    label        VARCHAR(255) NOT NULL,
    dimensions   VARCHAR(120) NOT NULL,
    base_weight  DOUBLE       NOT NULL DEFAULT 1,
    PRIMARY KEY (id)
);

INSERT INTO career_suggestions (field, label, dimensions, base_weight) VALUES
('Engineering & Technology', 'Software, mechanical, civil, electronics and emerging tech roles', 'LOGICAL,SPATIAL,R,I', 1.0),
('Data & Research Science',  'Data analysis, pure sciences, research and academia',              'LOGICAL,NATURALIST,I', 1.0),
('Medicine & Health Care',   'Doctor, nursing, physiotherapy, allied health services',           'NATURALIST,INTERPERSONAL,I,S', 1.0),
('Business & Management',    'Entrepreneurship, operations, product and general management',     'INTERPERSONAL,LOGICAL,E', 1.0),
('Finance & Accounting',     'Chartered accountancy, banking, auditing, financial analysis',     'LOGICAL,C', 1.0),
('Law & Public Policy',      'Legal practice, civil services, governance and policy',            'VERBAL,EXISTENTIAL,E,S', 1.0),
('Media & Communication',    'Journalism, content, advertising and public relations',            'VERBAL,INTERPERSONAL,A,E', 1.0),
('Design & Creative Arts',   'Graphic/product design, architecture, fine arts, animation',       'SPATIAL,MUSICAL,A', 1.0),
('Performing Arts & Music',  'Music, dance, theatre, film and performance careers',              'MUSICAL,KINESTHETIC,A', 1.0),
('Sports & Physical Sciences', 'Athletics, coaching, physiotherapy, defence and fitness',        'KINESTHETIC,NATURALIST,R', 1.0),
('Education & Social Work',  'Teaching, counselling, psychology and community service',          'INTERPERSONAL,INTRAPERSONAL,S', 1.0),
('Environment & Agriculture','Environmental science, agriculture, veterinary and forestry',     'NATURALIST,EXISTENTIAL,R,I', 1.0);
