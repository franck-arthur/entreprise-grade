package com.enterprise.app.testing.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Utilitaires pour simplifier les tests MockMvc.
 * Fournit des méthodes réutilisables pour les requêtes HTTP communes.
 */
public class MockMvcTestHelper {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public MockMvcTestHelper(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    /**
     * Effectue une requête GET et vérifie le statut 200 OK.
     */
    public ResultActions performGetAndExpectOk(String url, Object... params) throws Exception {
        return mockMvc.perform(get(url, params))
                .andExpect(status().isOk());
    }

    /**
     * Effectue une requête GET avec des paramètres de requête.
     */
    public ResultActions performGetWithParams(String url, String paramName, String paramValue) throws Exception {
        return mockMvc.perform(get(url).param(paramName, paramValue));
    }

    /**
     * Effectue une requête POST avec un corps JSON et vérifie le statut 201 Created.
     */
    public ResultActions performPostAndExpectCreated(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    /**
     * Effectue une requête POST et vérifie le statut 400 Bad Request.
     */
    public ResultActions performPostAndExpectBadRequest(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Effectue une requête PUT avec un corps JSON et vérifie le statut 200 OK.
     */
    public ResultActions performPutAndExpectOk(String url, Object body, Object... params) throws Exception {
        return mockMvc.perform(put(url, params)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    /**
     * Effectue une requête DELETE et vérifie le statut 204 No Content.
     */
    public ResultActions performDeleteAndExpectNoContent(String url, Object... params) throws Exception {
        return mockMvc.perform(delete(url, params)
                .with(csrf()))
                .andExpect(status().isNoContent());
    }

    /**
     * Effectue une requête et vérifie le statut 403 Forbidden.
     */
    public ResultActions performAndExpectForbidden(String method, String url, Object body) throws Exception {
        switch (method.toUpperCase()) {
            case "GET":
                return mockMvc.perform(get(url))
                        .andExpect(status().isForbidden());
            case "POST":
                return mockMvc.perform(post(url)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                        .andExpect(status().isForbidden());
            case "PUT":
                return mockMvc.perform(put(url)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                        .andExpect(status().isForbidden());
            case "DELETE":
                return mockMvc.perform(delete(url)
                        .with(csrf()))
                        .andExpect(status().isForbidden());
            default:
                throw new IllegalArgumentException("Méthode HTTP non supportée: " + method);
        }
    }

    /**
     * Vérifie qu'une réponse JSON contient un tableau avec la taille attendue.
     */
    public static ResultMatcher expectJsonArrayOfSize(int size) {
        return jsonPath("$").value(org.hamcrest.Matchers.hasSize(size));
    }

    /**
     * Vérifie qu'une réponse JSON contient un tableau vide.
     */
    public static ResultMatcher expectEmptyJsonArray() {
        return expectJsonArrayOfSize(0);
    }

    /**
     * Vérifie qu'une réponse de page contient les propriétés attendues.
     */
    public static ResultMatcher[] expectPageResponse(int contentSize, int totalElements, int totalPages) {
        return new ResultMatcher[] {
            jsonPath("$.content").value(org.hamcrest.Matchers.hasSize(contentSize)),
            jsonPath("$.totalElements").value(totalElements),
            jsonPath("$.totalPages").value(totalPages),
            jsonPath("$.size").exists(),
            jsonPath("$.number").exists()
        };
    }

    /**
     * Vérifie qu'une réponse contient un message d'erreur.
     */
    public static ResultMatcher expectErrorMessage(String expectedMessage) {
        return jsonPath("$.message").value(expectedMessage);
    }

    /**
     * Vérifie qu'une réponse JSON contient un champ spécifique avec une valeur.
     */
    public static ResultMatcher expectJsonField(String fieldPath, Object expectedValue) {
        return jsonPath(fieldPath).value(expectedValue);
    }

    /**
     * Vérifie qu'une réponse JSON contient un champ spécifique.
     */
    public static ResultMatcher expectJsonFieldExists(String fieldPath) {
        return jsonPath(fieldPath).exists();
    }

    /**
     * Vérifie qu'une réponse JSON ne contient pas un champ spécifique.
     */
    public static ResultMatcher expectJsonFieldNotExists(String fieldPath) {
        return jsonPath(fieldPath).doesNotExist();
    }

    /**
     * Sérialise un objet en JSON pour les tests.
     */
    public String toJson(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    /**
     * Crée un ensemble de matchers pour vérifier les en-têtes de réponse standards.
     */
    public static ResultMatcher[] expectStandardHeaders() {
        return new ResultMatcher[] {
            header().string("Content-Type", "application/json"),
        };
    }

    /**
     * Vérifie qu'une réponse contient les en-têtes de cache appropriés.
     */
    public static ResultMatcher[] expectCacheHeaders() {
        return new ResultMatcher[] {
            header().exists("Cache-Control"),
            header().exists("ETag")
        };
    }
}