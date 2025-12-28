package com.enterprise.app.domain.repository;

import com.enterprise.app.domain.model.Region;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface RegionRepository {

    Region save(Region region);

    Optional<Region> findById(Long id);

    Optional<Region> findByCode(String code);

    Optional<Region> findByNom(String nom);

    List<Region> findAll();

    List<Region> findAllByActifTrue();

    Page<Region> findAll(Pageable pageable);

    Page<Region> findAllByActifTrue(Pageable pageable);

    boolean existsByCode(String code);

    boolean existsByNom(String nom);

    boolean existsByCodeAndIdNot(String code, Long id);

    boolean existsByNomAndIdNot(String nom, Long id);

    void deleteById(Long id);

    long count();

    long countByActifTrue();
}