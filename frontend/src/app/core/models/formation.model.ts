/**
 * Formation related models and types.
 */

export enum ModaliteFormation {
  PRESENTIEL = 'PRESENTIEL',
  DISTANCIEL = 'DISTANCIEL',
  MIXTE = 'MIXTE'
}

export enum FormationStatut {
  PLANIFIE = 'PLANIFIE',
  EN_COURS = 'EN_COURS',
  TERMINE = 'TERMINE',
  ANNULE = 'ANNULE'
}

export interface SecteurDTO {
  id: number;
  code: string;
  libelle: string;
  description?: string;
}

export interface RegionDTO {
  id: number;
  code: string;
  libelle: string;
  description?: string;
}

export interface Formation {
  id: number;
  libelle: string;
  formateurs: string;
  description: string;
  dateFormation: string; // LocalDate as ISO string
  heureDebut: string; // LocalTime as HH:mm
  heureFin: string; // LocalTime as HH:mm
  secteur: SecteurDTO;
  region: RegionDTO;
  modalite: ModaliteFormation;
  nbParticipants: number;
  lieu: string;
  ville: string;
  lienParticipation?: string;
  statut: FormationStatut;
  nbParticipantsInscrits: number;
  complet: boolean;
  createdAt: string; // LocalDateTime as ISO string
  updatedAt: string; // LocalDateTime as ISO string
}

export interface CreateFormationRequest {
  libelle: string;
  formateurs: string;
  description: string;
  dateFormation: string;
  heureDebut: string;
  heureFin: string;
  secteurId: number;
  regionId: number;
  modalite: ModaliteFormation;
  nbParticipants: number;
  lieu: string;
  ville: string;
  lienParticipation?: string;
  statut: FormationStatut;
}

export interface UpdateFormationRequest {
  libelle: string;
  formateurs: string;
  description: string;
  dateFormation: string;
  heureDebut: string;
  heureFin: string;
  secteurId: number;
  regionId: number;
  modalite: ModaliteFormation;
  nbParticipants: number;
  lieu: string;
  ville: string;
  lienParticipation?: string;
  statut: FormationStatut;
}

export interface FormationParticipation {
  id: number;
  userId: number;
  formationId: number;
  dateInscription: string;
  statut: string;
  remarques?: string;
}

export interface FormationFilters {
  secteurId?: number;
  regionId?: number;
  modalite?: ModaliteFormation;
  statut?: FormationStatut;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}