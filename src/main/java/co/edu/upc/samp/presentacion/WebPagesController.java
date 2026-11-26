package co.edu.upc.samp.presentacion;

import co.edu.upc.samp.entidades.SearchQuery;
import co.edu.upc.samp.entidades.excepciones.SampException;
import co.edu.upc.samp.servicios.BuyOrWaitAdvisor;
import co.edu.upc.samp.servicios.SearchService;
import co.edu.upc.samp.servicios.ia.AiOrchestrator;
import co.edu.upc.samp.servicios.ia.ToolRegistry;
import co.edu.upc.samp.servicios.seguridad.AuthService;
import com.google.gson.Gson;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Controlador principal de vistas Thymeleaf de SAMP.
 *
 * <p>Sirve todas las pantallas fieles a {@code mockups/v2/} conectándolas exclusivamente
 * a la capa de {@code servicios} para mantener intacta la arquitectura de 4 capas.
 */
@Controller
public class WebPagesController {

    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-CO"));

    private final SearchService searchService;
    private final AiOrchestrator aiOrchestrator;
    private final ToolRegistry toolRegistry;
    private final AuthService authService;
    private final Gson gson = new Gson();

    /**
     * Construye el controlador inyectando únicamente servicios de negocio.
     *
     * @param searchService  servicio de búsqueda paralela y comparación
     * @param aiOrchestrator orquestador del asistente de IA general
     * @param toolRegistry   registro de las 10 herramientas de IA
     * @param authService    servicio de autenticación y usuarios
     */
    public WebPagesController(
            SearchService searchService,
            AiOrchestrator aiOrchestrator,
            ToolRegistry toolRegistry,
            AuthService authService
    ) {
        this.searchService = searchService;
        this.aiOrchestrator = aiOrchestrator;
        this.toolRegistry = toolRegistry;
        this.authService = authService;
    }

    /**
     * Página de inicio pública con buscador, estado de las 5 tiendas y ofertas destacadas.
     */
    @GetMapping({"/", "/index"})
    public String home(Model model) {
        SearchService.SearchResponse featured = searchService.search(new SearchQuery("", "", "", "COP", 20), false);
        model.addAttribute("featuredGroups", featured.groups());
        model.addAttribute("storeStatuses", featured.storeStatuses());
        model.addAttribute("totalOffers", featured.totalOffers());
        return "index";
    }

    /**
     * Página de resultados de búsqueda por texto o GTIN, con filtros y transparencia por tienda.
     */
    @GetMapping("/resultados")
    public String results(
            @RequestParam(name = "q", defaultValue = "") String queryText,
            @RequestParam(name = "gtin", defaultValue = "") String gtin,
            @RequestParam(name = "currency", defaultValue = "COP") String currency,
            @RequestParam(name = "onlyComplete", defaultValue = "false") boolean onlyComplete,
            @RequestParam(name = "live", defaultValue = "false") boolean live,
            Model model
    ) {
        SearchQuery query = new SearchQuery(queryText, gtin, "", currency, 30);
        SearchService.SearchResponse response = searchService.search(query, live);

        List<SearchService.ProductComparisonGroup> groups = response.groups();
        if (onlyComplete) {
            groups = groups.stream()
                    .map(g -> {
                        var completeOffers = g.offers().stream()
                                .filter(SearchService.ComparedOffer::completeTotal)
                                .toList();
                        if (completeOffers.isEmpty()) {
                            return null;
                        }
                        return new SearchService.ProductComparisonGroup(
                                g.groupKey(), g.canonicalTitle(), g.brand(), g.gtin(), g.imageUrl(),
                                g.exactMatch(), g.matchBadge(), completeOffers, completeOffers.getFirst(), g.buyAdvice()
                        );
                    })
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }

        List<SearchService.ProductComparisonGroup> exactGroups = groups.stream()
                .filter(g -> g.exactMatch() || g.offers().size() > 1)
                .toList();
        List<SearchService.ProductComparisonGroup> otherGroups = groups.stream()
                .filter(g -> !g.exactMatch() && g.offers().size() == 1)
                .toList();

        model.addAttribute("queryText", queryText.isBlank() ? (gtin.isBlank() ? "todas las ofertas" : "GTIN " + gtin) : queryText);
        model.addAttribute("rawQuery", queryText);
        model.addAttribute("gtin", gtin);
        model.addAttribute("currency", query.targetCurrency());
        model.addAttribute("onlyComplete", onlyComplete);
        model.addAttribute("live", live);
        model.addAttribute("groups", groups);
        model.addAttribute("exactGroups", exactGroups);
        model.addAttribute("otherGroups", otherGroups);
        model.addAttribute("storeStatuses", response.storeStatuses());
        model.addAttribute("totalOffers", response.totalOffers());
        return "resultados";
    }

    /**
     * Ficha detallada de producto con tabla transparente de ofertas, historial 30 días y veredicto IA.
     */
    @GetMapping({"/producto", "/producto/{groupKey}"})
    public String productDetail(
            @PathVariable(name = "groupKey", required = false) String groupKey,
            @RequestParam(name = "currency", defaultValue = "COP") String currency,
            Model model
    ) {
        SearchService.SearchResponse all = searchService.search(new SearchQuery("", "", "", currency, 30), false);
        SearchService.ProductComparisonGroup selected = (groupKey != null && !groupKey.isBlank())
                ? searchService.findGroupByKey(groupKey, currency)
                : null;
        if (selected == null && !all.groups().isEmpty()) {
            selected = all.groups().getFirst();
        }
        final SearchService.ProductComparisonGroup group = selected;
        model.addAttribute("group", group);
        model.addAttribute("relatedGroups", all.groups().stream()
                .filter(g -> group == null || !g.groupKey().equals(group.groupKey()))
                .limit(4)
                .toList());
        model.addAttribute("currency", currency);
        if (group != null) {
            List<List<Object>> chartPoints = group.buyAdvice().history().stream()
                    .map(p -> List.<Object>of(p.date().format(SHORT_DATE), p.priceCop().longValue()))
                    .toList();
            model.addAttribute("chartPointsJson", gson.toJson(chartPoints));
        } else {
            model.addAttribute("chartPointsJson", "[]");
        }
        return "producto";
    }

    /**
     * Comparador lado a lado de productos reales del catálogo.
     */
    @GetMapping("/comparar")
    public String compare(
            @RequestParam(name = "currency", defaultValue = "COP") String currency,
            Model model
    ) {
        SearchService.SearchResponse all = searchService.search(new SearchQuery("", "", "", currency, 10), false);
        model.addAttribute("comparedGroups", all.groups().stream().limit(3).toList());
        model.addAttribute("currency", currency);
        return "comparar";
    }

    /**
     * Asistente de IA unificado (chat de consultas + modo armar cualquier categoría con presupuesto).
     */
    @GetMapping({"/asistente", "/armador-pc"})
    public String assistant(
            @RequestParam(name = "modo", defaultValue = "CHAT") String mode,
            @RequestParam(name = "q", required = false) String initialQuestion,
            @RequestParam(name = "presupuesto", required = false) Long budgetCop,
            @RequestParam(name = "categoria", defaultValue = "GENERAL") String category,
            Model model
    ) {
        String prompt = (initialQuestion != null && !initialQuestion.isBlank())
                ? initialQuestion
                : ("BUILD".equalsIgnoreCase(mode)
                        ? "Arma una configuración equilibrada con el mejor total en las 5 tiendas"
                        : "¿Conviene comprar o esperar por los Sony WH-1000XM5?");

        AiOrchestrator.ChatReply initialReply = aiOrchestrator.ask(prompt, mode, budgetCop, category);
        SearchService.SearchResponse catalog = searchService.search(new SearchQuery("", "", "", "COP", 6), false);

        model.addAttribute("mode", mode.toUpperCase());
        model.addAttribute("initialQuestion", prompt);
        model.addAttribute("initialReply", initialReply);
        model.addAttribute("budgetCop", budgetCop);
        model.addAttribute("category", category);
        model.addAttribute("availableTools", toolRegistry.allTools().values());
        model.addAttribute("suggestedGroups", catalog.groups().stream().limit(3).toList());
        return "asistente";
    }

    /**
     * Pantalla de escáner de código de barras (GTIN/EAN) y búsqueda visual.
     */
    @GetMapping("/escanear")
    public String scanner(
            @RequestParam(name = "gtin", required = false) String gtin,
            Model model
    ) {
        String lookupCode = (gtin == null || gtin.isBlank()) ? "3017620422003" : gtin.trim();
        SearchService.SearchResponse found = searchService.search(SearchQuery.byGtin(lookupCode), false);
        model.addAttribute("scannedGtin", lookupCode);
        model.addAttribute("scannedGroup", found.groups().isEmpty() ? null : found.groups().getFirst());
        model.addAttribute("visualMatches", searchService.search(new SearchQuery("", "", "", "COP", 3), false).groups());
        return "escanear";
    }

    /**
     * Pantalla de inicio de sesión y registro de cuenta con validación.
     */
    @GetMapping("/entrar")
    public String loginPage(
            @RequestParam(name = "error", required = false) String error,
            Model model
    ) {
        model.addAttribute("loginError", error != null);
        return "entrar";
    }

    /**
     * Procesa el registro de una nueva cuenta de usuario validando entradas y duplicados.
     */
    @PostMapping("/registro")
    public String registerUser(
            @RequestParam("displayName") String displayName,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            RedirectAttributes redirectAttributes
    ) {
        try {
            authService.register(email, password, displayName);
            redirectAttributes.addFlashAttribute("registerSuccess",
                    "Cuenta creada correctamente. Ahora puedes entrar con tu correo y contraseña.");
            return "redirect:/entrar";
        } catch (SampException ex) {
            redirectAttributes.addFlashAttribute("registerError", ex.getMessage());
            redirectAttributes.addFlashAttribute("registerHint", ex.getUserHint());
            return "redirect:/entrar#crear";
        }
    }

    /**
     * Panel de cuenta de usuario (favoritos, alertas, notificaciones, armados y preferencias).
     */
    @GetMapping("/cuenta")
    public String account(Model model) {
        SearchService.SearchResponse all = searchService.search(new SearchQuery("", "", "", "COP", 6), false);
        model.addAttribute("favoriteGroups", all.groups());
        return "cuenta";
    }

    /**
     * Panel de administración protegido por rol {@code ADMIN}.
     */
    @GetMapping("/admin")
    public String admin(Model model) {
        SearchService.SearchResponse snapshot = searchService.search(new SearchQuery("", "", "", "COP", 20), false);
        model.addAttribute("storeStatuses", snapshot.storeStatuses());
        model.addAttribute("totalOffers", snapshot.totalOffers());
        model.addAttribute("users", authService.findAllUsers());
        model.addAttribute("toolsCount", toolRegistry.allTools().size());
        return "admin";
    }

    /**
     * Catálogo de estados del sistema (carga, vacío, fallo parcial y errores).
     */
    @GetMapping("/estados")
    public String systemStates() {
        return "estados";
    }
}
