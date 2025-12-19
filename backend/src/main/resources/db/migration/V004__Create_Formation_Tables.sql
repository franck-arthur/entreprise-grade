-- Migration pour les tables de formation
-- Auteur: Claude Code
-- Date: 2024-12-18

-- Table des formations
CREATE TABLE formations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    libelle VARCHAR(200) NOT NULL,
    formateurs VARCHAR(500) NOT NULL,
    description TEXT,
    date_formation DATE NOT NULL,
    heure_debut TIME NOT NULL,
    heure_fin TIME NOT NULL,
    secteur VARCHAR(100) NOT NULL,
    region VARCHAR(100) NOT NULL,
    modalite VARCHAR(20) NOT NULL CHECK (modalite IN ('PRESENTIEL', 'DISTANCIEL', 'HYBRIDE')),
    nb_participants INTEGER NOT NULL CHECK (nb_participants > 0),
    lieu VARCHAR(200),
    ville VARCHAR(100),
    lien_participation VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Index pour optimiser les performances
CREATE INDEX idx_formation_date ON formations(date_formation);
CREATE INDEX idx_formation_secteur ON formations(secteur);
CREATE INDEX idx_formation_region ON formations(region);
CREATE INDEX idx_formation_modalite ON formations(modalite);
CREATE INDEX idx_formation_date_secteur ON formations(date_formation, secteur);
CREATE INDEX idx_formation_date_region ON formations(date_formation, region);

-- Table des participations aux formations
CREATE TABLE formation_participations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    formation_id UUID NOT NULL REFERENCES formations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    statut_participation VARCHAR(20) NOT NULL DEFAULT 'INSCRIT' CHECK (statut_participation IN ('INSCRIT', 'PRESENT', 'ABSENT', 'ANNULE')),
    date_inscription TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_presence TIMESTAMP,
    commentaire VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,

    -- Contrainte unique pour éviter les doublons d'inscription
    CONSTRAINT uk_formation_user UNIQUE (formation_id, user_id)
);

-- Index pour optimiser les performances
CREATE INDEX idx_formation_participation_formation ON formation_participations(formation_id);
CREATE INDEX idx_formation_participation_user ON formation_participations(user_id);
CREATE INDEX idx_formation_participation_statut ON formation_participations(statut_participation);
CREATE INDEX idx_formation_participation_formation_statut ON formation_participations(formation_id, statut_participation);

-- Ajout du champ date_derniere_formation à la table users
ALTER TABLE users ADD COLUMN date_derniere_formation TIMESTAMP;

-- Commentaires sur les tables
COMMENT ON TABLE formations IS 'Table des formations proposées dans l''entreprise';
COMMENT ON TABLE formation_participations IS 'Table des inscriptions et participations aux formations';

COMMENT ON COLUMN formations.statut IS 'Calculé dynamiquement: A_VENIR, EN_COURS, TERMINEE';
COMMENT ON COLUMN formation_participations.statut_participation IS 'INSCRIT, PRESENT, ABSENT, ANNULE';
COMMENT ON COLUMN users.date_derniere_formation IS 'Date de la dernière formation suivie (présence marquée)';

-- Triggers pour updated_at (si pas déjà définis)
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_formations_updated_at BEFORE UPDATE ON formations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_formation_participations_updated_at BEFORE UPDATE ON formation_participations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();