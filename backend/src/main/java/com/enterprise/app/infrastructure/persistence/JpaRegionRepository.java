package com.enterprise.app.infrastructure.persistence;

import com.enterprise.app.domain.model.Region;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaRegionRepository extends JpaRepository<Region, Long> {

    Optional<Region> findByCode(String code);

    Optional<Region> findByNom(String nom);

    List<Region> findAllByActifTrue();

    Page<Region> findAllByActifTrue(Pageable pageable);

    boolean existsByCode(String code);

    boolean existsByNom(String nom);

    boolean existsByCodeAndIdNot(String code, Long id);

    boolean existsByNomAndIdNot(String nom, Long id);

    long countByActifTrue();
}