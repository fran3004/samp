package co.edu.upc.samp.presentacion.consola;

import co.edu.upc.samp.entidades.DataKind;
import co.edu.upc.samp.entidades.Product;
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
 * <p>Cubre la Fase 2 del curso (aplicativo completo por consola con {@code ArrayList} y archivos JSON):
 * ingreso con roles ({@code ADMIN} / {@code REGISTERED}), CRUD completo de tiendas y productos,
 * registro de usuarios, búsqueda multi-tienda transparente y asistente de IA general.
 */
@Component
@Profile({"consola", "console"})
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
                if (!scanner.hasNextLine()) {
                    break;
                }
                String option = scanner.nextLine().trim();
                try {
                    switch (option) {
                        case "1" -> runSearch(scanner);
                        case "2" -> runAiAssistant(scanner);
                        case "3" -> listStores();
                        case "4" -> listProducts();
                        case "5" -> requireAdmin(currentUser, () -> createStore(scanner));
                        case "6" -> requireAdmin(currentUser, () -> updateStoreKind(scanner));
                        case "7" -> requireAdmin(currentUser, () -> deleteStore(scanner));
                        case "8" -> requireAdmin(currentUser, () -> createProduct(scanner));
                        case "9" -> requireAdmin(currentUser, () -> updateProduct(scanner));
                        case "10" -> requireAdmin(currentUser, () -> deleteProduct(scanner));
                        case "11" -> requireAdmin(currentUser, () -> listUsers());
                        case "0" -> {
                            running = false;
                            System.out.println("Hasta pronto. Todos los cambios quedaron guardados en JSON.");
                        }
                        default -> System.out.println("Opción no válida. Elige un número del menú.");
                    }
                } catch (SampException ex) {
                    System.out.println("[AVISO] " + ex.getMessage());
                    System.out.println("        Sugerencia: " + ex.getUserHint());
                } catch (NumberFormatException ex) {
                    System.out.println("[AVISO] Debes ingresar un número válido.");
                } catch (IllegalArgumentException ex) {
                    System.out.println("[AVISO] Valor inválido: " + ex.getMessage());
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
                System.out.println("Bienvenido, " + user.getDisplayName() + " [" + (user.hasRole("ADMIN") ? "ADMIN" : "REGISTERED") + "]");
                return user;
            }
            System.out.println("Credenciales inválidas (intento " + attempt + " de 3).");
        }
        return null;
    }

    private void printMenu(User user) {
        System.out.println("\n--- MENÚ PRINCIPAL ---");
        System.out.println("1.  Buscar y comparar producto (texto o GTIN)");
        System.out.println("2.  Consultar o armar configuración con Asistente IA");
        System.out.println("3.  Listar tiendas (stores.json)");
        System.out.println("4.  Listar productos canónicos (products.json)");
        if (user.hasRole("ADMIN")) {
            System.out.println("5.  [ADMIN] Crear tienda");
            System.out.println("6.  [ADMIN] Actualizar origen REAL/DEMO de tienda");
            System.out.println("7.  [ADMIN] Eliminar tienda por ID");
            System.out.println("8.  [ADMIN] Crear producto");
            System.out.println("9.  [ADMIN] Actualizar producto");
            System.out.println("10. [ADMIN] Eliminar producto por ID");
            System.out.println("11. [ADMIN] Listar usuarios (users.json)");
        }
        System.out.println("0.  Salir");
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
        System.out.println("\nTiendas en stores.json:");
        for (Store s : catalogService.listStores()) {
            System.out.printf("  ID=%d | %-16s | %-26s | %s | %s%n",
                    s.getId(), s.getCode(), s.getName(), s.getDefaultCurrency(), s.getDataKind());
        }
    }

    private void listProducts() {
        System.out.println("\nProductos en products.json:");
        for (Product p : catalogService.listProducts()) {
            String gtin = p.getIdentifiers().isEmpty() ? "Sin GTIN" : p.getIdentifiers().getFirst().getValue();
            System.out.printf("  ID=%d | %-38s | Modelo: %-14s | GTIN: %s%n",
                    p.getId(), p.getName(), p.getModel() == null ? "-" : p.getModel(), gtin);
        }
    }

    private void listUsers() {
        System.out.println("\nUsuarios en users.json:");
        for (User u : catalogService.listUsers()) {
            System.out.printf("  ID=%d | %-22s | %-22s | Admin=%s%n",
                    u.getId(), u.getDisplayName(), u.getEmail(), u.hasRole("ADMIN"));
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
        System.out.println("Tienda creada con ID=" + saved.getId() + " y guardada en stores.json.");
    }

    private void updateStoreKind(Scanner scanner) {
        System.out.print("ID de la tienda: ");
        Long id = Long.parseLong(scanner.nextLine().trim());
        System.out.print("Nuevo origen (REAL o DEMO): ");
        DataKind kind = DataKind.valueOf(scanner.nextLine().trim().toUpperCase());
        Store updated = catalogService.updateStoreDataKind(id, kind);
        System.out.println("Tienda actualizada: " + updated.getName() + " -> " + updated.getDataKind());
    }

    private void deleteStore(Scanner scanner) {
        System.out.print("ID de la tienda a eliminar: ");
        Long id = Long.parseLong(scanner.nextLine().trim());
        catalogService.deleteStore(id);
        System.out.println("Tienda ID=" + id + " eliminada de stores.json.");
    }

    private void createProduct(Scanner scanner) {
        System.out.print("Nombre del producto: ");
        String name = scanner.nextLine();
        System.out.print("Marca: ");
        String brand = scanner.nextLine();
        System.out.print("Categoría: ");
        String category = scanner.nextLine();
        System.out.print("Modelo: ");
        String model = scanner.nextLine();
        System.out.print("GTIN (opcional): ");
        String gtin = scanner.nextLine();
        Product saved = catalogService.createProduct(name, brand, category, model, gtin);
        System.out.println("Producto creado con ID=" + saved.getId() + " en products.json.");
    }

    private void updateProduct(Scanner scanner) {
        System.out.print("ID del producto: ");
        Long id = Long.parseLong(scanner.nextLine().trim());
        System.out.print("Nuevo nombre: ");
        String name = scanner.nextLine();
        System.out.print("Nuevo modelo: ");
        String model = scanner.nextLine();
        Product updated = catalogService.updateProduct(id, name, model);
        System.out.println("Producto actualizado: " + updated.getName());
    }

    private void deleteProduct(Scanner scanner) {
        System.out.print("ID del producto a eliminar: ");
        Long id = Long.parseLong(scanner.nextLine().trim());
        catalogService.deleteProduct(id);
        System.out.println("Producto ID=" + id + " eliminado de products.json.");
    }

    private void requireAdmin(User user, Runnable action) {
        if (!user.hasRole("ADMIN")) {
            System.out.println("[ACCESO DENEGADO] Esta operación requiere perfil ADMIN.");
            return;
        }
        action.run();
    }
}
