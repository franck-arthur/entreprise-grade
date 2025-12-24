package com.enterprise.app.testing.config;

import com.enterprise.app.application.mapper.FormationMapper;
import com.enterprise.app.application.mapper.FormationParticipationMapper;
import com.enterprise.app.application.service.FormationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Classe de base pour les tests Web MVC.
 * Fournit une configuration commune et des utilitaires pour les tests de contrôleurs.
 */
@WebMvcTest
public abstract class BaseWebMvcTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean
    protected FormationService formationService;

    @MockBean
    protected FormationMapper formationMapper;

    @MockBean
    protected FormationParticipationMapper participationMapper;

    protected static final String API_V1_FORMATIONS = "/api/v1/formations";

    /**
     * Effectue une requête GET simplifiée.
     */
    protected ResultActions performGet(String url, Object... params) throws Exception {
        return mockMvc.perform(get(url, params));
    }

    /**
     * Effectue une requête POST avec un corps JSON.
     */
    protected ResultActions performPost(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    /**
     * Effectue une requête PUT avec un corps JSON.
     */
    protected ResultActions performPut(String url, Object body, Object... params) throws Exception {
        return mockMvc.perform(put(url, params)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    /**
     * Effectue une requête DELETE.
     */
    protected ResultActions performDelete(String url, Object... params) throws Exception {
        return mockMvc.perform(delete(url, params).with(csrf()));
    }

    /**
     * Sérialise un objet en JSON.
     */
    protected String toJson(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }
}