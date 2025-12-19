-- Migration pour mettre à jour les modalités de formation
-- Auteur: Claude Code
-- Date: 2024-12-18

-- Mettre à jour les valeurs existantes vers les nouvelles modalités
UPDATE formations SET modalite = 'EN_PRESENTIEL' WHERE modalite = 'PRESENTIEL';
UPDATE formations SET modalite = 'EN_LIGNE' WHERE modalite = 'DISTANCIEL';

-- Supprimer l'ancienne contrainte
ALTER TABLE formations DROP CONSTRAINT IF EXISTS formations_modalite_check;

-- Ajouter la nouvelle contrainte avec les nouvelles valeurs
ALTER TABLE formations ADD CONSTRAINT formations_modalite_check
    CHECK (modalite IN ('EN_PRESENTIEL', 'EN_LIGNE'));

-- Commentaire sur la modalité
COMMENT ON COLUMN formations.modalite IS 'EN_PRESENTIEL (nécessite ville et lieu) ou EN_LIGNE (nécessite lien_participation)';