package com.enterprise.app.domain.repository;

import com.enterprise.app.domain.model.Secteur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface SecteurRepository {

    Secteur save(Secteur secteur);

    Optional<Secteur> findById(Long id);

    Optional<Secteur> findByCode(String code);

    Optional<Secteur> findByNom(String nom);

    List<Secteur> findAll();

    List<Secteur> findAllByActifTrue();

    Page<Secteur> findAll(Pageable pageable);

    Page<Secteur> findAllByActifTrue(Pageable pageable);

    boolean existsByCode(String code);

    boolean existsByNom(String nom);

    boolean existsByCodeAndIdNot(String code, Long id);

    boolean existsByNomAndIdNot(String nom, Long id);

    void deleteById(Long id);

    long count();

    long countByActifTrue();
}