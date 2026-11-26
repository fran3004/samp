package co.edu.upc.samp.presentacion.consola;

import co.edu.upc.samp.entidades.DataKind;
import co.edu.upc.samp.entidades.SearchQuery;
import co.edu.upc.samp.entidades.Store;
import co.edu.upc.samp.entidades.User;
import co.edu.upc.samp.entidades.excepciones.SampException;
import co.edu.upc.samp.servicios.ConsoleCatalogService;
import co.edu.upc.samp.servicios.SearchService;
import co.edu.upc.samp.servicios.ia.AiOrchestrator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Scanner;

/**
 * Interfaz interactiva por consola para el perfil {@code consola} ({@code --spring.profiles.active=consola}).
 *
 * <p>Cubre explícitamente la Fase 2 del curso (aplicativo funcional por consola respaldado por
 * {@code ArrayList} y archivos JSON) con control de acceso por roles ({@code ADMIN} / {@code REGISTERED}),
 * CRUD completo de tiendas, búsqueda transparente y consulta al asistente de IA general.
 */
@Component
@Profile("consola")
public class ConsoleRunner implements CommandLineRunner {

    private final ConsoleCatalogService catalogService;
    private final SearchService searchService;
    private final AiOrchestrator aiOrchestrator;

    /**
     * Construye el ejecutor de consola conectándolo únicamente con la capa {@code servicios}.
     *
     * @param catalogService servicio de archivos JSON + {@code ArrayList}
     * @param searchService  servicio de búsqueda y comparación
     * @param aiOrchestrator orquestador del asistente de IA general
     */
    public ConsoleRunner(
            ConsoleCatalogService catalogService,
            SearchService searchService,
            AiOrchestrator aiOrchestrator
    ) {
        this.catalogService = catalogService;
        this.searchService = searchService;
        this.aiOrchestrator = aiOrchestrator;
    }

    @Override
    public void run(String... args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("==========================================================");
            System.out.println("   SAMP — Sistema Automatizado de Mejores Precios");
            System.out.println("   Modo Consola (ArrayList + Persistencia en JSON)");
            System.out.println("==========================================================");

            User currentUser = promptLogin(scanner);
            if (currentUser == null) {
                System.out.println("Sesión terminada.");
                return;
            }

            boolean running = true;
            while (running) {
                printMenu(currentUser);
                String option = scanner.nextLine().trim();
                try {
                    switch (option) {
                        case "1" -> runSearch(scanner);
                        case "2" -> runAiAssistant(scanner);
                        case "3" -> listStores();
                        case "4" -> requireAdmin(currentUser, () -> createStore(scanner));
                        case "5" -> requireAdmin(currentUser, () -> updateStoreKind(scanner));
                        case "6" -> requireAdmin(currentUser, () -> deleteStore(scanner));
                        case "0" -> {
                            running = false;
                            System.out.println("Hasta pronto. Tus cambios quedaron guardados en JSON.");
                        }
                        default -> System.out.println("Opción no válida. Elige un número del menú.");
                    }
                } catch (SampException ex) {
                    System.out.println("[AVISO] " + ex.getMessage());
                    System.out.println("        Sugerencia: " + ex.getUserHint());
                } catch (NumberFormatException ex) {
                    System.out.println("[AVISO] Debes ingresar un valor numérico válido.");
                }
            }
        }
    }

    private User promptLogin(Scanner scanner) {
        System.out.println("Cuentas iniciales: admin@samp.co / AdminSamp2026!  |  comprador@samp.co / Comprador2026!");
        for (int attempt = 1; attempt <= 3; attempt++) {
            System.out.print("Correo: ");
            if (!scanner.hasNextLine()) return null;
            String email = scanner.nextLine().trim();
            System.out.print("Contraseña: ");
            if (!scanner.hasNextLine()) return null;
            String password = scanner.nextLine();
            var userOpt = catalogService.authenticate(email, password);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                System.out.println("Bienvenido, " + user.getDisplayName() + " (" + (user.hasRole("ADMIN") ? "ADMIN" : "REGISTERED") + ")");
                return user;
            }
            System.out.println("Credenciales inválidas (intento " + attempt + " de 3).");
        }
        return null;
    }

    private void printMenu(User user) {
        System.out.println("\n--- MENÚ PRINCIPAL ---");
        System.out.println("1. Buscar y comparar producto (texto o GTIN)");
        System.out.println("2. Consultar o armar configuración con Asistente IA");
        System.out.println("3. Listar tiendas (desde archivo stores.json)");
        if (user.hasRole("ADMIN")) {
            System.out.println("4. [ADMIN] Registrar nueva tienda (Crear)");
            System.out.println("5. [ADMIN] Cambiar origen REAL/DEMO de una tienda (Actualizar)");
            System.out.println("6. [ADMIN] Eliminar tienda por ID (Borrar)");
        }
        System.out.println("0. Salir");
        System.out.print("Opción: ");
    }

    private void runSearch(Scanner scanner) {
        System.out.print("Escribe producto o código GTIN: ");
        String term = scanner.nextLine().trim();
        SearchQuery query = term.matches("\\d{8,14}") ? SearchQuery.byGtin(term) : SearchQuery.byText(term);
        var response = searchService.search(query, false);
        if (response.groups().isEmpty()) {
            System.out.println("No se encontraron ofertas para «" + term + "».");
            return;
        }
        for (var group : response.groups()) {
            System.out.printf("%n* %s [%s] — Consejo 30d: %s%n",
                    group.canonicalTitle(), group.matchBadge(), group.buyAdvice().recommendation());
            for (var offer : group.offers()) {
                String ship = offer.shippingKnown() ? (offer.shippingCost().isZero() ? "Gratis" : offer.shippingCost().toString()) : "Desconocido";
                System.out.printf("   - %-25s | $%,.0f %s | Envío: %-11s | %s | %s%n",
                        offer.storeName(), offer.totalInTarget().getAmount(), offer.totalInTarget().getCurrency(),
                        ship, offer.dataKind(), offer.comparisonLabel());
            }
        }
    }

    private void runAiAssistant(Scanner scanner) {
        System.out.print("¿Modo (1=Consultar, 2=Armar con presupuesto)? ");
        String mode = "2".equals(scanner.nextLine().trim()) ? "BUILD" : "CHAT";
        Long budget = null;
        String category = "GENERAL";
        if ("BUILD".equals(mode)) {
            System.out.print("Categoría libre (ej. ESTUDIO, HOGAR, PC): ");
            category = scanner.nextLine().trim();
            System.out.print("Presupuesto máximo en COP: ");
            budget = Long.parseLong(scanner.nextLine().trim());
        }
        System.out.print("Tu solicitud: ");
        String prompt = scanner.nextLine();
        var reply = aiOrchestrator.ask(prompt, mode, budget, category);
        System.out.println("\n[IA " + reply.providerUsed() + " | herramientas: " + reply.toolsInvoked() + "]");
        System.out.println(reply.answer());
    }

    private void listStores() {
        System.out.println("\nTiendas guardadas en stores.json:");
        for (Store s : catalogService.listStores()) {
            System.out.printf("  ID=%d | %-16s | %-26s | %s | %s%n",
                    s.getId(), s.getCode(), s.getName(), s.getDefaultCurrency(), s.getDataKind());
        }
    }

    private void createStore(Scanner scanner) {
        System.out.print("Código único (ej. EXITO_CO): ");
        String code = scanner.nextLine();
        System.out.print("Nombre comercial: ");
        String name = scanner.nextLine();
        System.out.print("URL base (https://...): ");
        String url = scanner.nextLine();
        System.out.print("Moneda (COP/USD): ");
        String cur = scanner.nextLine();
        Store saved = catalogService.createStore(code, name, url, cur, DataKind.DEMO);
        System.out.println("Tienda creada con ID=" + saved.getId() + " y persistida en stores.json.");
    }

    private void updateStoreKind(Scanner scanner) {
        System.out.print("ID de la tienda a actualizar: ");
        Long id = Long.parseLong(scanner.nextLine().trim());
        System.out.print("Nuevo origen (REAL o DEMO): ");
        DataKind kind = DataKind.valueOf(scanner.nextLine().trim().toUpperCase());
        Store updated = catalogService.updateStoreDataKind(id, kind);
        System.out.println("Tienda actualizada: " + updated.getName() + " ahora es " + updated.getDataKind());
    }

    private void deleteStore(Scanner scanner) {
        System.out.print("ID de la tienda a eliminar: ");
        Long id = Long.parseLong(scanner.nextLine().trim());
        catalogService.deleteStore(id);
        System.out.println("Tienda con ID=" + id + " eliminada de stores.json.");
    }

    private void requireAdmin(User user, Runnable action) {
        if (!user.hasRole("ADMIN")) {
            System.out.println("[ACCESO DENEGADO] Esta operación requiere perfil ADMIN.");
            return;
        }
        action.run();
    }
}
