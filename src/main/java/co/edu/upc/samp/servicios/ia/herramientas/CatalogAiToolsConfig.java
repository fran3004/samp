package co.edu.upc.samp.servicios.ia.herramientas;

import co.edu.upc.samp.entidades.SearchQuery;
import co.edu.upc.samp.servicios.BuyOrWaitAdvisor;
import co.edu.upc.samp.servicios.SearchService;
import co.edu.upc.samp.servicios.ia.AiTool;
import co.edu.upc.samp.servicios.ia.BuildValidationService;
import co.edu.upc.samp.servicios.ia.CompatibilityRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Registro de las 10 herramientas de solo lectura ({@link AiTool}) exigidas en la especificación v2.0
 * para que el asistente de IA consulte el catálogo, compare ofertas, revise historiales y arme
 * configuraciones de cualquier categoría sin inventar datos.
 */
@Configuration
public class CatalogAiToolsConfig {

    @Bean
    public AiTool searchProductsTool(SearchService searchService) {
        return new AiTool() {
            @Override
            public String name() {
                return "buscar_productos";
            }

            @Override
            public String description() {
                return "Busca productos en las 5 tiendas configuradas para Colombia por texto o GTIN.";
            }

            @Override
            public ToolResult execute(Map<String, Object> args) {
                String q = String.valueOf(args.getOrDefault("query", ""));
                var response = searchService.search(SearchQuery.byText(q), false);
                if (response.groups().isEmpty()) {
                    return new ToolResult(name(), false, "Sin resultados para «" + q + "».", List.of());
                }
                String summary = response.groups().stream()
                        .limit(5)
                        .map(g -> String.format("%s — desde $%,.0f COP en %s (%s, %s)",
                                g.canonicalTitle(),
                                g.bestOffer().totalInTarget().getAmount(),
                                g.bestOffer().storeName(),
                                g.bestOffer().dataKind(),
                                g.bestOffer().completeTotal() ? "total completo" : "costos pendientes"))
                        .collect(Collectors.joining("; "));
                return new ToolResult(name(), true, summary, response.groups());
            }
        };
    }

    @Bean
    public AiTool getProductTool(SearchService searchService) {
        return new AiTool() {
            @Override
            public String name() {
                return "obtener_producto";
            }

            @Override
            public String description() {
                return "Obtiene ficha detallada y atributos verificados de un producto por su clave o GTIN.";
            }

            @Override
            public ToolResult execute(Map<String, Object> args) {
                String key = String.valueOf(args.getOrDefault("key", ""));
                var group = searchService.findGroupByKey(key, "COP");
                if (group == null) {
                    return new ToolResult(name(), false, "No se encontró ficha para " + key, null);
                }
                String summary = String.format("%s (%s) | GTIN: %s | %d oferta(s) activas.",
                        group.canonicalTitle(), group.brand(),
                        group.gtin() == null ? "Sin GTIN" : group.gtin(),
                        group.offers().size());
                return new ToolResult(name(), true, summary, group);
            }
        };
    }

    @Bean
    public AiTool compareOffersTool(SearchService searchService) {
        return new AiTool() {
            @Override
            public String name() {
                return "comparar_ofertas";
            }

            @Override
            public String description() {
                return "Compara todas las ofertas equivalentes de un producto ordenadas por total estimado.";
            }

            @Override
            public ToolResult execute(Map<String, Object> args) {
                String q = String.valueOf(args.getOrDefault("query", ""));
                var response = searchService.search(SearchQuery.byText(q), false);
                if (response.groups().isEmpty()) {
                    return new ToolResult(name(), false, "Sin ofertas para comparar.", List.of());
                }
                var top = response.groups().getFirst();
                String lines = top.offers().stream()
                        .map(o -> String.format("%s: $%,.0f COP (%s, envío: %s, dato %s)",
                                o.storeName(),
                                o.totalInTarget().getAmount(),
                                o.comparisonLabel(),
                                o.shippingKnown() ? (o.shippingCost().isZero() ? "Gratis" : o.shippingCost()) : "Desconocido",
                                o.dataKind()))
                        .collect(Collectors.joining(" | "));
                return new ToolResult(name(), true, top.canonicalTitle() + " -> " + lines, top.offers());
            }
        };
    }

    @Bean
    public AiTool priceHistoryTool(SearchService searchService) {
        return new AiTool() {
            @Override
            public String name() {
                return "historial_precios";
            }

            @Override
            public String description() {
                return "Consulta el comportamiento histórico de los últimos 30 días de un producto.";
            }

            @Override
            public ToolResult execute(Map<String, Object> args) {
                String q = String.valueOf(args.getOrDefault("query", ""));
                var response = searchService.search(SearchQuery.byText(q), false);
                if (response.groups().isEmpty()) {
                    return new ToolResult(name(), false, "Sin historial disponible.", List.of());
                }
                BuyOrWaitAdvisor.BuyAdvice advice = response.groups().getFirst().buyAdvice();
                String summary = String.format("Últimos %d días (DEMO): mínimo $%,.0f COP, promedio $%,.0f COP, máximo $%,.0f COP.",
                        advice.history().size(), advice.min30dCop(), advice.avg30dCop(), advice.max30dCop());
                return new ToolResult(name(), true, summary, advice.history());
            }
        };
    }

    @Bean
    public AiTool buyOrWaitTool(SearchService searchService) {
        return new AiTool() {
            @Override
            public String name() {
                return "prediccion_compra";
            }

            @Override
            public String description() {
                return "Emite la recomendación transparente Comprar o Esperar (RF-22) basada en 30 días.";
            }

            @Override
            public ToolResult execute(Map<String, Object> args) {
                String q = String.valueOf(args.getOrDefault("query", ""));
                var response = searchService.search(SearchQuery.byText(q), false);
                if (response.groups().isEmpty()) {
                    return new ToolResult(name(), false, "No hay suficiente historial para recomendar.", null);
                }
                BuyOrWaitAdvisor.BuyAdvice advice = response.groups().getFirst().buyAdvice();
                return new ToolResult(name(), true, advice.recommendation() + ": " + advice.reason(), advice);
            }
        };
    }

    @Bean
    public AiTool listStoresTool() {
        return new AiTool() {
            @Override
            public String name() {
                return "listar_tiendas";
            }

            @Override
            public String description() {
                return "Lista las 5 tiendas configuradas para Colombia con sus fortalezas y políticas.";
            }

            @Override
            public ToolResult execute(Map<String, Object> args) {
                String summary = "Tiendas activas en Colombia: Mercado Libre (24-48h, Mercado Pago), "
                        + "Amazon (envío gratis >$35 USD, impuestos claros), Temu (envío estándar gratis 1-2 semanas), "
                        + "Falabella (retiro gratis en sucursal, garantías físicas) y AliExpress Choice (10-15 días).";
                return new ToolResult(name(), true, summary, List.of(
                        "MERCADOLIBRE_CO", "AMAZON_CO", "TEMU_CO", "FALABELLA_CO", "ALIEXPRESS_CO"));
            }
        };
    }

    @Bean
    public AiTool searchBuildItemsTool(SearchService searchService) {
        return new AiTool() {
            @Override
            public String name() {
                return "buscar_componentes";
            }

            @Override
            public String description() {
                return "Selecciona artículos del catálogo para armar cualquier lista o configuración dentro del presupuesto.";
            }

            @Override
            public ToolResult execute(Map<String, Object> args) {
                String q = String.valueOf(args.getOrDefault("query", "")).toLowerCase();
                long budget = args.get("budgetCop") instanceof Number n ? n.longValue() : Long.MAX_VALUE;
                var response = searchService.search(SearchQuery.byText(""), false);
                var candidates = response.groups().stream()
                        .map(SearchService.ProductComparisonGroup::bestOffer)
                        .filter(o -> o.totalInTarget().getAmount().longValue() <= budget)
                        .toList();

                boolean techIntent = q.contains("pc") || q.contains("escritorio") || q.contains("setup")
                        || q.contains("estudio") || q.contains("audio") || q.contains("gaming");
                boolean homeIntent = q.contains("hogar") || q.contains("cocina") || q.contains("freidora");

                List<SearchService.ComparedOffer> prioritized = candidates.stream()
                        .filter(o -> {
                            String cat = o.attributes().getOrDefault("categoria", "").toLowerCase();
                            if (techIntent) {
                                return cat.contains("audio") || cat.contains("periféricos") || cat.contains("accesorios");
                            }
                            if (homeIntent) {
                                return cat.contains("hogar") || cat.contains("alimentos");
                            }
                            return q.isBlank() || o.title().toLowerCase().contains(q) || cat.contains(q);
                        })
                        .toList();

                List<SearchService.ComparedOffer> pool = prioritized.isEmpty() ? candidates : prioritized;
                List<SearchService.ComparedOffer> selected = new java.util.ArrayList<>();
                long running = 0L;
                for (var item : pool) {
                    long cost = item.totalInTarget().getAmount().longValue();
                    if (running + cost <= budget && selected.size() < 4) {
                        selected.add(item);
                        running += cost;
                    }
                }
                if (selected.isEmpty()) {
                    return new ToolResult(name(), false, "Ningún artículo cabe individualmente en ese presupuesto.", List.of());
                }
                String summary = selected.stream()
                        .map(o -> String.format("%s ($%,.0f COP en %s)",
                                o.title(), o.totalInTarget().getAmount(), o.storeName()))
                        .collect(Collectors.joining(" + "));
                return new ToolResult(name(), true, "Artículos seleccionados: " + summary, selected);
            }
        };
    }

    @Bean
    public AiTool validateBuildTool(BuildValidationService validationService) {
        return new AiTool() {
            @Override
            public String name() {
                return "validar_compatibilidad";
            }

            @Override
            public String description() {
                return "Valida presupuesto, costos desconocidos y compatibilidad técnica de una lista armada.";
            }

            @Override
            @SuppressWarnings("unchecked")
            public ToolResult execute(Map<String, Object> args) {
                String category = String.valueOf(args.getOrDefault("category", "GENERAL"));
                BigDecimal budget = args.get("budgetCop") instanceof Number n
                        ? BigDecimal.valueOf(n.longValue())
                        : BigDecimal.ZERO;
                List<SearchService.ComparedOffer> items = args.get("items") instanceof List<?> list
                        ? (List<SearchService.ComparedOffer>) list
                        : List.of();
                var evals = validationService.validate(new CompatibilityRule.BuildProposal(category, budget, items));
                String summary = evals.stream()
                        .map(e -> (e.passed() ? "[OK] " : "[" + e.severity() + "] ") + e.message())
                        .collect(Collectors.joining(" | "));
                return new ToolResult(name(), true, summary, evals);
            }
        };
    }

    @Bean
    public AiTool calculateTotalTool(BuildValidationService validationService) {
        return new AiTool() {
            @Override
            public String name() {
                return "calcular_total";
            }

            @Override
            public String description() {
                return "Suma el total estimado en COP de los artículos elegidos indicando si faltan costos.";
            }

            @Override
            @SuppressWarnings("unchecked")
            public ToolResult execute(Map<String, Object> args) {
                List<SearchService.ComparedOffer> items = args.get("items") instanceof List<?> list
                        ? (List<SearchService.ComparedOffer>) list
                        : List.of();
                BigDecimal total = validationService.calculateTotal(items);
                boolean allComplete = items.stream().allMatch(SearchService.ComparedOffer::completeTotal);
                String summary = String.format("Total estimado de %d artículo(s): $%,.0f COP (%s).",
                        items.size(), total, allComplete ? "costos completos" : "total incompleto por envíos/impuestos pendientes");
                return new ToolResult(name(), true, summary, total);
            }
        };
    }

    @Bean
    public AiTool helpKnowledgeTool() {
        return new AiTool() {
            @Override
            public String name() {
                return "consultar_ayuda";
            }

            @Override
            public String description() {
                return "Explica cómo funciona SAMP, su transparencia de precios y la diferencia entre REAL y DEMO.";
            }

            @Override
            public ToolResult execute(Map<String, Object> args) {
                String summary = "SAMP compara precios entre Mercado Libre, Amazon, Temu, Falabella y AliExpress "
                        + "mostrando siempre moneda original, fecha de consulta, si el dato es REAL o DEMO y señalando "
                        + "como desconocido cualquier envío o impuesto no confirmado. No realiza pagos dentro del sitio.";
                return new ToolResult(name(), true, summary, null);
            }
        };
    }
}
