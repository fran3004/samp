package co.edu.upc.samp.presentacion;

import co.edu.upc.samp.servicios.seguridad.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Inyecta información de sesión y rol autenticado en todas las vistas Thymeleaf
 * para adaptar dinámicamente la barra de navegación y el control de acceso visual.
 */
@ControllerAdvice
public class GlobalViewModelAdvice {

    private final AuthService authService;

    /**
     * Crea el asesor global de vista comunicándose únicamente con la capa {@code servicios}.
     *
     * @param authService servicio de autenticación
     */
    public GlobalViewModelAdvice(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Pobla atributos de usuario autenticado en el modelo de cada vista.
     *
     * @param authentication contexto de seguridad actual
     * @param model          modelo de Thymeleaf
     */
    @ModelAttribute
    public void populateUserContext(Authentication authentication, Model model) {
        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName());

        model.addAttribute("isAuthenticated", authenticated);
        if (!authenticated) {
            model.addAttribute("isAdmin", false);
            model.addAttribute("currentUserName", "Visitante");
            model.addAttribute("currentUserEmail", "");
            model.addAttribute("currentUserInitials", "VI");
            return;
        }

        String email = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        String displayName = authService.findByEmail(email)
                .map(u -> u.getDisplayName())
                .orElse(email);

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("currentUserName", displayName);
        model.addAttribute("currentUserEmail", email);
        model.addAttribute("currentUserInitials", computeInitials(displayName));
    }

    private String computeInitials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        return name.length() >= 2 ? name.substring(0, 2).toUpperCase() : name.toUpperCase();
    }
}
