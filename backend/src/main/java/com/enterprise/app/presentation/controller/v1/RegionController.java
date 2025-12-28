package com.enterprise.app.presentation.controller.v1;

import com.enterprise.app.application.dto.RegionDTO;
import com.enterprise.app.application.mapper.RegionMapper;
import com.enterprise.app.domain.model.Region;
import com.enterprise.app.domain.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/regions")
@RequiredArgsConstructor
@Slf4j
public class RegionController {

    private final RegionRepository regionRepository;
    private final RegionMapper regionMapper;

    @GetMapping
    public ResponseEntity<List<RegionDTO>> getAllRegions(
            @RequestParam(defaultValue = "false") boolean actifsOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "nom") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        if (actifsOnly) {
            Page<Region> regionsPage = regionRepository.findAllByActifTrue(pageable);
            List<RegionDTO> regions = regionsPage.getContent()
                    .stream()
                    .map(regionMapper::toDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(regions);
        } else {
            Page<Region> regionsPage = regionRepository.findAll(pageable);
            List<RegionDTO> regions = regionsPage.getContent()
                    .stream()
                    .map(regionMapper::toDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(regions);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<RegionDTO> getRegionById(@PathVariable Long id) {
        Optional<Region> region = regionRepository.findById(id);
        return region
                .map(r -> ResponseEntity.ok(regionMapper.toDTO(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<RegionDTO> getRegionByCode(@PathVariable String code) {
        Optional<Region> region = regionRepository.findByCode(code);
        return region
                .map(r -> ResponseEntity.ok(regionMapper.toDTO(r)))
                .orElse(ResponseEntity.notFound().build());
    }
}