-- Données de test pour les formations
-- À exécuter après la migration V004

-- Insertion de formations d'exemple
INSERT INTO formations (id, libelle, formateurs, description, date_formation, heure_debut, heure_fin, secteur, region, modalite, nb_participants, lieu, ville, lien_participation) VALUES
    ('123e4567-e89b-12d3-a456-426614174001', 'Formation Spring Boot Avancé', 'Jean Dupont, Marie Martin', 'Formation pratique sur Spring Boot et microservices avec TP', '2024-02-15', '09:00', '17:00', 'IT', 'Île-de-France', 'HYBRIDE', 20, 'Centre de formation Paris', 'Paris', 'https://zoom.us/j/123456789'),
    ('123e4567-e89b-12d3-a456-426614174002', 'Sécurité Informatique', 'Pierre Durand', 'Sensibilisation aux bonnes pratiques de sécurité', '2024-01-20', '14:00', '16:00', 'IT', 'Lyon', 'PRESENTIEL', 15, 'Bureaux Lyon', 'Lyon', NULL),
    ('123e4567-e89b-12d3-a456-426614174003', 'Gestion de Projet Agile', 'Sophie Laurent', 'Méthodologies Scrum et Kanban', '2023-12-10', '09:00', '12:00', 'Management', 'Toulouse', 'DISTANCIEL', 25, NULL, NULL, 'https://teams.microsoft.com/l/meetup-join/...'),
    ('123e4567-e89b-12d3-a456-426614174004', 'Angular et TypeScript', 'Marc Rousseau', 'Développement frontend avec Angular 16+', '2024-03-01', '09:30', '17:30', 'IT', 'Nantes', 'PRESENTIEL', 18, 'Campus Nantes', 'Nantes', NULL);

-- Exemple d'inscriptions (nécessite des utilisateurs existants)
-- Commenté car les UUIDs doivent correspondre aux utilisateurs réels
/*
INSERT INTO formation_participations (formation_id, user_id, statut_participation, date_inscription) VALUES
    ('123e4567-e89b-12d3-a456-426614174001', 'user-uuid-1', 'INSCRIT', CURRENT_TIMESTAMP),
    ('123e4567-e89b-12d3-a456-426614174001', 'user-uuid-2', 'PRESENT', CURRENT_TIMESTAMP),
    ('123e4567-e89b-12d3-a456-426614174002', 'user-uuid-3', 'ABSENT', CURRENT_TIMESTAMP);
*/