package com.enterprise.app.presentation.controller.v1;

import com.enterprise.app.application.dto.SecteurDTO;
import com.enterprise.app.application.mapper.SecteurMapper;
import com.enterprise.app.domain.model.Secteur;
import com.enterprise.app.domain.repository.SecteurRepository;
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
@RequestMapping("/api/v1/secteurs")
@RequiredArgsConstructor
@Slf4j
public class SecteurController {

    private final SecteurRepository secteurRepository;
    private final SecteurMapper secteurMapper;

    @GetMapping
    public ResponseEntity<List<SecteurDTO>> getAllSecteurs(
            @RequestParam(defaultValue = "false") boolean actifsOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "nom") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        if (actifsOnly) {
            Page<Secteur> secteursPage = secteurRepository.findAllByActifTrue(pageable);
            List<SecteurDTO> secteurs = secteursPage.getContent()
                    .stream()
                    .map(secteurMapper::toDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(secteurs);
        } else {
            Page<Secteur> secteursPage = secteurRepository.findAll(pageable);
            List<SecteurDTO> secteurs = secteursPage.getContent()
                    .stream()
                    .map(secteurMapper::toDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(secteurs);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<SecteurDTO> getSecteurById(@PathVariable Long id) {
        Optional<Secteur> secteur = secteurRepository.findById(id);
        return secteur
                .map(s -> ResponseEntity.ok(secteurMapper.toDTO(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<SecteurDTO> getSecteurByCode(@PathVariable String code) {
        Optional<Secteur> secteur = secteurRepository.findByCode(code);
        return secteur
                .map(s -> ResponseEntity.ok(secteurMapper.toDTO(s)))
                .orElse(ResponseEntity.notFound().build());
    }
}