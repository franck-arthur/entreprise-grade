-- Migration pour les changements de statuts et modalités Formation
-- Auteur: Claude Code
-- Date: 2024-12-24
-- Référence: Documentation FORMATION_TESTS_DOCUMENTATION.md

-- =============================================================================
-- 1. MISE À JOUR DES MODALITÉS DE FORMATION
-- =============================================================================

-- Mettre à jour EN_PRESENTIEL vers PRESENTIEL
UPDATE formations
SET modalite = 'PRESENTIEL'
WHERE modalite = 'EN_PRESENTIEL';

-- EN_LIGNE reste EN_LIGNE (pas de changement)

-- Supprimer l'ancienne contrainte
ALTER TABLE formations DROP CONSTRAINT IF EXISTS formations_modalite_check;

-- Ajouter la nouvelle contrainte avec les nouvelles valeurs
ALTER TABLE formations ADD CONSTRAINT formations_modalite_check
    CHECK (modalite IN ('PRESENTIEL', 'EN_LIGNE'));

-- =============================================================================
-- 2. MISE À JOUR DES STATUTS DE PARTICIPATION
-- =============================================================================

-- Convertir INSCRIT vers ABSENT (nouveau statut par défaut)
UPDATE formation_participations
SET statut_participation = 'ABSENT'
WHERE statut_participation = 'INSCRIT';

-- Convertir ANNULE vers ABSENT
UPDATE formation_participations
SET statut_participation = 'ABSENT'
WHERE statut_participation = 'ANNULE';

-- Supprimer l'ancienne contrainte de statut
ALTER TABLE formation_participations DROP CONSTRAINT IF EXISTS formation_participations_statut_participation_check;

-- Ajouter la nouvelle contrainte avec seulement PRESENT et ABSENT
ALTER TABLE formation_participations ADD CONSTRAINT formation_participations_statut_participation_check
    CHECK (statut_participation IN ('PRESENT', 'ABSENT'));

-- Mettre à jour la valeur par défaut
ALTER TABLE formation_participations ALTER COLUMN statut_participation SET DEFAULT 'ABSENT';

-- =============================================================================
-- 3. MISE À JOUR DES COMMENTAIRES
-- =============================================================================

COMMENT ON COLUMN formations.modalite IS 'PRESENTIEL (nécessite ville et lieu) ou EN_LIGNE (nécessite lien_participation)';
COMMENT ON COLUMN formation_participations.statut_participation IS 'PRESENT (confirmé par admin) ou ABSENT (par défaut)';

-- =============================================================================
-- 4. STATISTIQUES DE MIGRATION (pour vérification)
-- =============================================================================

-- Afficher le nombre de formations par modalité après migration
-- SELECT modalite, COUNT(*) as count FROM formations GROUP BY modalite;

-- Afficher le nombre de participations par statut après migration
-- SELECT statut_participation, COUNT(*) as count FROM formation_participations GROUP BY statut_participation;