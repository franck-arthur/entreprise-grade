package com.enterprise.app.infrastructure.persistence;

import com.enterprise.app.domain.model.Region;
import com.enterprise.app.domain.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RegionRepositoryImpl implements RegionRepository {

    private final JpaRegionRepository jpaRegionRepository;

    @Override
    public Region save(Region region) {
        return jpaRegionRepository.save(region);
    }

    @Override
    public Optional<Region> findById(Long id) {
        return jpaRegionRepository.findById(id);
    }

    @Override
    public Optional<Region> findByCode(String code) {
        return jpaRegionRepository.findByCode(code);
    }

    @Override
    public Optional<Region> findByNom(String nom) {
        return jpaRegionRepository.findByNom(nom);
    }

    @Override
    public List<Region> findAll() {
        return jpaRegionRepository.findAll();
    }

    @Override
    public List<Region> findAllByActifTrue() {
        return jpaRegionRepository.findAllByActifTrue();
    }

    @Override
    public Page<Region> findAll(Pageable pageable) {
        return jpaRegionRepository.findAll(pageable);
    }

    @Override
    public Page<Region> findAllByActifTrue(Pageable pageable) {
        return jpaRegionRepository.findAllByActifTrue(pageable);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpaRegionRepository.existsByCode(code);
    }

    @Override
    public boolean existsByNom(String nom) {
        return jpaRegionRepository.existsByNom(nom);
    }

    @Override
    public boolean existsByCodeAndIdNot(String code, Long id) {
        return jpaRegionRepository.existsByCodeAndIdNot(code, id);
    }

    @Override
    public boolean existsByNomAndIdNot(String nom, Long id) {
        return jpaRegionRepository.existsByNomAndIdNot(nom, id);
    }

    @Override
    public void deleteById(Long id) {
        jpaRegionRepository.deleteById(id);
    }

    @Override
    public long count() {
        return jpaRegionRepository.count();
    }

    @Override
    public long countByActifTrue() {
        return jpaRegionRepository.countByActifTrue();
    }
}