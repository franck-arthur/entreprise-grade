package com.enterprise.app.domain.model;

public enum ModaliteFormation {
    PRESENTIEL("En présentiel"),
    EN_LIGNE("En ligne");

    private final String libelle;

    ModaliteFormation(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}