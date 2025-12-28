package com.enterprise.app.infrastructure.persistence;

import com.enterprise.app.domain.model.Secteur;
import com.enterprise.app.domain.repository.SecteurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SecteurRepositoryImpl implements SecteurRepository {

    private final JpaSecteurRepository jpaSecteurRepository;

    @Override
    public Secteur save(Secteur secteur) {
        return jpaSecteurRepository.save(secteur);
    }

    @Override
    public Optional<Secteur> findById(Long id) {
        return jpaSecteurRepository.findById(id);
    }

    @Override
    public Optional<Secteur> findByCode(String code) {
        return jpaSecteurRepository.findByCode(code);
    }

    @Override
    public Optional<Secteur> findByNom(String nom) {
        return jpaSecteurRepository.findByNom(nom);
    }

    @Override
    public List<Secteur> findAll() {
        return jpaSecteurRepository.findAll();
    }

    @Override
    public List<Secteur> findAllByActifTrue() {
        return jpaSecteurRepository.findAllByActifTrue();
    }

    @Override
    public Page<Secteur> findAll(Pageable pageable) {
        return jpaSecteurRepository.findAll(pageable);
    }

    @Override
    public Page<Secteur> findAllByActifTrue(Pageable pageable) {
        return jpaSecteurRepository.findAllByActifTrue(pageable);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaSecteurRepository.existsByCode(code);
    }

    @Override
    public boolean existsByNom(String nom) {
        return jpaSecteurRepository.existsByNom(nom);
    }

    @Override
    public boolean existsByCodeAndIdNot(String code, Long id) {
        return jpaSecteurRepository.existsByCodeAndIdNot(code, id);
    }

    @Override
    public boolean existsByNomAndIdNot(String nom, Long id) {
        return jpaSecteurRepository.existsByNomAndIdNot(nom, id);
    }

    @Override
    public void deleteById(Long id) {
        jpaSecteurRepository.deleteById(id);
    }

    @Override
    public long count() {
        return jpaSecteurRepository.count();
    }

    @Override
    public long countByActifTrue() {
        return jpaSecteurRepository.countByActifTrue();
    }
}