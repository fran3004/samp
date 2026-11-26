package co.edu.upc.samp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas integrales de la capa de presentación web (Thymeleaf, CSS/JS modulares, API REST
 * y control de acceso por roles en Spring Security) usando {@link MockMvc}.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
@AutoConfigureMockMvc
class WebPresentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicPagesAndModularAssetsShouldRenderSuccessfully() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mercado Libre Colombia")))
                .andExpect(content().string(containsString("/css/pages/index.css")));

        mockMvc.perform(get("/resultados").param("q", "Sony"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sony WH-1000XM5")));

        mockMvc.perform(get("/producto/0027242923232"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/js/pages/producto.js")));

        mockMvc.perform(get("/asistente"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/js/pages/asistente.js")));

        mockMvc.perform(get("/escanear").param("gtin", "3017620422003"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nutella")));

        mockMvc.perform(get("/comparar"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/css/pages/comparar.css")));

        mockMvc.perform(get("/css/base/tokens.css")).andExpect(status().isOk());
        mockMvc.perform(get("/js/core/icons-sprite.js")).andExpect(status().isOk());
    }

    @Test
    void aiRestEndpointShouldReturnValidatedToolsResponse() throws Exception {
        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Arma un kit de estudio\",\"mode\":\"BUILD\",\"budgetCop\":1500000,\"category\":\"ESTUDIO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validationPassed").value(true))
                .andExpect(jsonPath("$.providerUsed").value("mock"))
                .andExpect(jsonPath("$.toolsInvoked").isArray());
    }

    @Test
    void anonymousUserShouldBeRedirectedToLoginForProtectedRoutes() throws Exception {
        mockMvc.perform(get("/cuenta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/entrar"));

        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/entrar"));
    }

    @Test
    @WithMockUser(username = "comprador@samp.co", roles = "REGISTERED")
    void registeredUserCanAccessAccountButNotAdmin() throws Exception {
        mockMvc.perform(get("/cuenta")).andExpect(status().isOk());
        mockMvc.perform(get("/admin")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@samp.co", roles = "ADMIN")
    void adminUserCanAccessAdminPanel() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Panel de Administración SAMP")));
    }
}
