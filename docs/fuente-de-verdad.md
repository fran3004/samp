# SAMP — Fuente de verdad

Versión: 2026-11-26. Reemplaza al diseño `docs/superpowers/specs/2026-11-17-samp-design.md` y al plan `docs/superpowers/plans/2026-11-17-samp-implementation.md`, ambos escritos sobre la v1.0.

## 1. Orden de autoridad
1. **Este documento** (decisiones del equipo tomadas después de la v2.0 y ajustadas el 2026-11-26).
2. **`docs/referencias/SAMP-Documentacion-Tecnica-v2.0.pdf`** (requisitos RF-01 a RF-26, módulos, IA, algoritmos) y **`SAMP-UML-Base-de-Datos.svg`** (46 tablas).
3. `docs/referencias/guia-poo-prog2-01.pdf` y rúbrica RAP Momento 1 (criterios del curso Programación II: 4 pilares POO, 4 capas, consola + `ArrayList`, archivos JSON).
4. La v1.0 (`Especificacion_Tecnica_SAMP.pdf`): solo como historial.

Si dos fuentes chocan, manda la de menor número.

## 2. Decisiones finales

| Tema | Decisión |
|---|---|
| Nombre oficial | **SAMP — Sistema Automatizado de Mejores Precios**. |
| Arquitectura | Monolito modular Spring Boot 3.x + Thymeleaf (réplica fiel de `mockups/v2/`). Un solo despliegue en Render. Sin frontend separado. |
| Modos de ejecución | El **mismo JAR** soporta 3 perfiles: **web** (por defecto, Thymeleaf + Supabase), **`scraper`** (sin servidor web, escribe en Supabase desde GitHub Actions) y **`console`** (sin servidor web, menú interactivo con `ArrayList` + CRUD en archivos JSON con Gson, para cumplir Fase 2 e ítem 5.4 de la rúbrica). |
| Base de datos | Supabase PostgreSQL 17 (`samp`, `fwdjrtmmiqsurxgeruee`, `us-east-1`) con `pgvector`, usada como PostgreSQL normal (JDBC + Flyway). Sin SDK ni auth de Supabase. Localmente: Docker Compose con `pgvector/pgvector:pg17`. |
| Tiendas objetivo (Colombia) | 5 tiendas para Colombia (`.com.co`): **Mercado Libre**, **Amazon**, **Temu**, **Falabella** y **AliExpress**. |
| Extracción y respaldo | Se intenta scraping HTTP/Jsoup (Playwright solo si requiere JS) respetando `robots.txt` y pausas de 1–3 s, **sin evadir captchas ni bloqueos**. Toda tienda que bloquee o restrinja el acceso opera con datos **`DEMO`** sembrados y rotulados explícitamente en la interfaz (`stores.data_kind`). |
| Selección de conector | **Automática con respaldo**: cada tienda puede tener varios conectores ordenados por `priority` y `success_rate`; si uno falla pasa al siguiente; circuit breaker por conector. |
| Calidad | Cada oferta se valida (campos obligatorios, rangos). Las faltas se registran en `data_quality_issues` y se calcula `completeness_pct` por ejecución. |
| IA y Armador general | Un único asistente de IA (chat + modo «armar» para cualquier categoría con presupuesto y consultas sobre el catálogo). `AiProvider` intercambiable: **Gemini** (plan gratuito) con **`MockProvider`** por defecto hasta configurar API key. Las tablas de armado (`builds`, `build_items`, `build_validations`, `item_specs`, `item_benchmarks`, `compat_rules`) se generalizan manteniendo las 52 tablas totales. |
| Embeddings | `vector(768)`, un único modelo por índice; cambiarlo exige reindexar (`content_hash`). |
| Usuarios | Tablas `users`, `roles`, `user_roles` (M:N) como la v2.0. Roles: `VISITOR`, `REGISTERED`, `ADMIN`. |
| Paquetes (4 capas, en inglés) | `co.edu.upc.samp.domain` (entidades), `co.edu.upc.samp.repository` (datos), `co.edu.upc.samp.service` (servicios), `co.edu.upc.samp.presentation` (presentación). Todo identificador de código, clase, carpeta y archivo va en inglés; la presentación solo llama a `service` y `service` a `repository`. Cada clase lleva Javadoc. |
| Imágenes | Solo se guarda la URL (`product_images`, `store_product_images`). No se copian archivos. |
| Datos monetarios | `numeric(14,2)`; precio de referencia en USD con tasa y fecha guardadas; visualización por defecto en COP. |
| Legalidad y transparencia | Cada conector guarda `evidence_url` y `robots_checked_at`. Se respeta `robots.txt`. No se evaden captchas ni bloqueos. Lo desconocido (envío/impuestos) nunca se muestra como cero. |

## 3. Herramientas definidas

| Área | Herramienta |
|---|---|
| Lenguaje y build | Java 21 LTS, Maven 3.9 con Maven Wrapper |
| Framework | Spring Boot 3.x: Web, Security, Data JPA, Validation, Actuator, Thymeleaf |
| Datos | PostgreSQL + `pgvector` (Supabase), Hibernate, Flyway |
| Extracción | Jsoup (HTML), HttpClient/WebClient (APIs y feeds), Playwright para Java (solo si hace falta JS) |
| Resiliencia | **Resilience4j** para rate limiter, retry con backoff y circuit breaker (se descarta Bucket4j para no duplicar librerías) |
| Caché | Caffeine |
| IA | Interfaz propia `AiProvider` (Spring AI opcional), Gemini, `MockProvider`; ZXing para códigos de barras |
| API | REST con Springdoc OpenAPI; SSE para el chat de IA |
| Pruebas | JUnit 5, Mockito, Testcontainers (imagen con pgvector), ArchUnit, WireMock, MockMvc |
| CI y programación | GitHub Actions: build y pruebas, y workflow programado de scraping |
| Hosting | Render (Docker, `eclipse-temurin:21`), Supabase |
| Local | Docker Compose con PostgreSQL + pgvector y `MockProvider` |

## 4. Flujo de datos

```
GitHub Actions (cron) ──► java -jar samp.jar --spring.profiles.active=scraper
                                   │ ScrapeOrchestrator
                                   │  1. ConnectorSelector elige conector por tienda
                                   │  2. search() en API / feed / scraping
                                   │  3. validar → normalizar → emparejar → calcular Pf
                                   ▼
                          Supabase PostgreSQL ◄──── Render: Spring Boot + Thymeleaf ◄── usuario (web o móvil)
                                                              │
                                                              └──► Gemini (IA) con herramientas de solo lectura
```

## 5. Valores propuestos (el equipo puede cambiarlos)
- Cron de scraping: cada 6 horas (`0 */6 * * *`), con prioridad para favoritos y productos con alertas.
- TTL de frescura por defecto: 24 h (`ttl_minutes = 1440`). Oferta más vieja se marca desactualizada.
- Circuit breaker: abre tras 3 fallos consecutivos; reintenta en media apertura a los 30 minutos.
- Umbral de calidad: una ejecución con `completeness_pct < 80` se marca `PARTIAL` y se alerta al administrador.
- Pausa entre peticiones a una misma tienda: 1 a 3 s aleatorios; `User-Agent` identificable.

## 6. Pendientes abiertos
1. Tiendas concretas a scrapear además de eBay y Mercado Libre, y país de Mercado Libre. Cada una necesita `evidence_url`.
2. Historial para RF-22: pide al menos 30 días. Antes del 30-nov hay que sembrar datos `DEMO` (columna `stores.data_kind`).
3. Origen de `component_specs` y `benchmark_scores` para el armador de PC.
4. Recorte de alcance de implementación (13 días, 3 personas) y reescritura del plan de implementación sobre esta base.
5. Si las IPs de Actions se bloquean: activar el runner `self-hosted`.
6. El logo dice «Automático» y la spec «Automatizado»: elegir un nombre.

## 7. Cambios del UML respecto a la v2.0
Ver `docs/uml/er-base-de-datos.md`: 46 → **52 tablas**.
- Nuevas: `raw_offers`, `store_product_attributes`, `store_product_images`, `data_quality_issues`, `ai_validation_results`, `pc_build_validations`.
- Columnas añadidas en `store_connectors`, `connector_executions`, `stores`, `store_products`, `offers`, `price_history`, `price_alerts`, `pc_builds`, `image_queries`, `ai_conversations`.

## 8. Contradicciones de la v2.0 resueltas aquí
| Contradicción | Resolución |
|---|---|
| Rate limit «Bucket4j / Resilience4j» | Resilience4j |
| Spring Scheduler vs. Render gratuito que se duerme | La programación vive en GitHub Actions, no en Render |
| Neon vs. Supabase | Supabase |
| Scraping «último recurso» | Híbrido: se usa el conector que mejor funcione, con respaldo automático y permiso documentado |
| Fig. 2 con 5 herramientas de IA vs. tabla con 10 | Son 10 (`AiTool`) |
| RF-23 marcado como IA | Es búsqueda (escáner de código de barras en el navegador) |
| `price_history` por `offer_id` vs. una oferta vigente | `offers` es append-only con `is_current`; `price_history` conserva `offer_id` y añade `store_product_id` |
