> **REEMPLAZADO por `docs/fuente-de-verdad.md` (2026-11-17).** Este plan de 18 tareas se escribió sobre la v1.0 (16 tablas, sin IA, sin scraping). Hay que reescribirlo sobre la v2.0 y el UML de 52 tablas antes de ejecutarlo. No lo ejecutes tal cual.

# SAMP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Construir el MVP de SAMP (comparador de precios) con los cuatro pilares de POO visibles en el dominio, listo para entregar el 2026-11-30.

**Architecture:** Monolito modular Spring Boot (`com.samp`), un paquete por módulo con `entities`, `repositories` y `services` (más `controllers` y `dto`). Ruta principal: búsqueda en vivo en paralelo a los conectores → ingestión (normalizar, emparejar, guardar oferta e historial) → comparación → página Thymeleaf.

**Tech Stack:** Java 21, Spring Boot 3.x (versión exacta fijada en `pom.xml`), Spring MVC + Thymeleaf, Spring Data JPA, PostgreSQL 16, Flyway, Spring Security, Jakarta Validation, JUnit 5, Mockito, Testcontainers, Jackson, `java.net.http.HttpClient`, Bucket4j y Caffeine (fase final), Maven Wrapper.

**Spec:** `docs/superpowers/specs/2026-11-17-samp-design.md` (complementa a `docs/referencias/Especificacion_Tecnica_SAMP.pdf`; si chocan, manda la spec del PDF).

## Global Constraints

- Java 21; Spring Boot 3.x con versión concreta fijada en `pom.xml`; no subir de versión mayor sin correr la suite.
- Estilo del profesor: paquetes `entities`/`repositories`/`services`; nombres de clases y métodos en inglés, mensajes al usuario en español; tipos explícitos; orden atributos → constructores → getters/setters; `@Override` siempre; interfaz `IXxxService` + `XxxServiceImpl`; constantes `private static final`.
- Prohibido: `var`, `record`, Lombok. Preferir `for` mejorado y `.equals()` a streams complicados.
- Dinero: `BigDecimal` (`NUMERIC(19,4)` en BD), nunca `double`/`float`. Fechas `Instant` / `timestamptz` en UTC.
- Desconocido = `null`/estado explícito, nunca cero.
- Moneda de referencia: USD. Visualización por defecto: COP (configurable). País de destino por defecto: `CO`.
- Tasas: `open.er-api.com/v6/latest/USD`; respaldo `api.frankfurter.dev` (no incluye COP).
- Los tres catálogos de demostración (`dummyjson.com`, `fakestoreapi.com`, `api.escuelajs.co`) se marcan `DEMO`; Open Food Facts Prices se marca `REAL`.
- Conectores: timeouts, reintentos con backoff, nunca registrar secretos, sin evasión de CAPTCHA ni controles de acceso, sin scraping prohibido.
- Secretos solo en variables de entorno; `.env.example` lleva nombres sin valores. Admin inicial: `SAMP_ADMIN_EMAIL`, `SAMP_ADMIN_PASSWORD`.
- Permisos comprobados en el servidor; BCrypt; CSRF activo; consultas JPA parametrizadas.
- Datos de demostración fuera de las migraciones estructurales (`db/seed/`, solo perfil `dev`).
- Cada pilar tiene una prueba repetible (se listan en cada tarea con la etiqueta **[Pilar]**).

## Review Focus

Entradas o fallos que la spec implica y que ningún requisito prueba por sí solo; cada uno tiene su prueba en la tarea indicada.

1. Búsqueda con texto vacío, solo espacios o caracteres como `%`, `'`, `"`, emoji: debe devolver lista vacía con mensaje, nunca error 500 (Tareas 5 y 12).
2. Respuesta de una API con campos `null`, precio no numérico, negativo o cero: esa publicación se descarta y se registra, sin romper la ingestión de las demás (Tarea 10).
3. Moneda sin tasa disponible: se muestra el precio original, `convertedPrice` queda `null` y el total estimado se marca incompleto; nunca cero (Tareas 7 y 8).
4. Registro con un email ya existente en otra capitalización (`Ana@x.com` vs `ana@x.com`) o con espacios: se rechaza o normaliza (Tarea 4).
5. Todos los conectores fallan a la vez: la página responde 200 con un aviso y los datos ya guardados, sin excepción (Tarea 12).

## Reparto y calendario (propuesta)

| Persona | Tareas | Días |
|---|---|---|
| A (identity, currency, favorites, admin) | 3, 4, 7, 14, 15, 16 | 17–28 nov |
| B (catalog, matching, search, comparison) | 5, 11, 12, 13, 17 | 20–28 nov |
| C (base, stores, offers, connectors) | 1, 2, 6, 8, 9, 10 | 17–26 nov |
| Los tres | 18 | 29 nov |

Las tareas 1 y 2 bloquean a todos: terminarlas antes del 19 nov. Entrega: 30 nov.

---

### Task 1: Proyecto base

**Files:**
- Create: `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/`, `.gitignore`, `.env.example`
- Create: `src/main/java/com/samp/SampApplication.java`
- Create: `src/main/java/com/samp/common/ValidationException.java`, `src/main/java/com/samp/common/NotFoundException.java`
- Create: `src/main/resources/application.yml`, `src/main/resources/application-dev.yml`
- Create: `src/test/java/com/samp/support/PostgresIT.java`
- Test: `src/test/java/com/samp/SampApplicationIT.java`

**Interfaces:**
- Produces: `public class ValidationException extends RuntimeException { public ValidationException(String message) }`, `public class NotFoundException extends RuntimeException { public NotFoundException(String message) }`; `public abstract class PostgresIT` (contenedor `postgres:16-alpine` con `@ServiceConnection`, `@SpringBootTest`) que extienden todas las pruebas de integración.

- [ ] **Step 1: Instalar JDK 21 y comprobar.** Run: `java -version`. Expected: la salida empieza con `openjdk version "21` (la máquina tiene Java 1.8).
- [ ] **Step 2: Generar el proyecto** con start.spring.io: `curl -o samp.zip https://start.spring.io/starter.zip -d type=maven-project -d language=java -d javaVersion=21 -d groupId=com -d artifactId=samp -d name=samp -d packageName=com.samp -d dependencies=web,thymeleaf,data-jpa,validation,security,flyway,postgresql,testcontainers,actuator`, usando la última versión 3.x que acepte el servicio (`-d bootVersion=<3.x.y>`); descomprimir en la raíz del proyecto. Añadir `org.flywaydb:flyway-database-postgresql`, `org.thymeleaf.extras:thymeleaf-extras-springsecurity6` y `spring-security-test`.
- [ ] **Step 3: Escribir la prueba que falla.** `SampApplicationIT extends PostgresIT` con `contextLoads()` y `healthEndpointIsUp()` (GET `/actuator/health` → 200, cuerpo contiene `"status":"UP"`).
- [ ] **Step 4: Run test to verify it fails.** Run: `./mvnw -q test -Dtest=SampApplicationIT`. Expected: FAIL (no existe `PostgresIT` o no hay migraciones).
- [ ] **Step 5: Implementar** `PostgresIT`, `application.yml` (`spring.jpa.hibernate.ddl-auto=validate`, `spring.flyway.locations=classpath:db/migration`, `management.endpoints.web.exposure.include=health`, datasource por variables `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`) y `application-dev.yml` (`spring.flyway.locations=classpath:db/migration,classpath:db/seed`). Crear `src/main/resources/db/migration/V1__baseline.sql` vacío con un comentario para que Flyway arranque; la Tarea 2 lo reemplaza por `V1__create_identity.sql`.
- [ ] **Step 6: Run test to verify it passes.** Run: `./mvnw -q test -Dtest=SampApplicationIT`. Expected: `Tests run: 2, Failures: 0`.
- [ ] **Step 7: Commit.** `git init` (aún no es repositorio), `git add .` (comprobar que `.env` y `target/` están en `.gitignore`), `git commit -m "chore: proyecto base Spring Boot"`.

### Task 2: Migraciones Flyway

**Files:**
- Delete: `src/main/resources/db/migration/V1__baseline.sql`
- Create: `src/main/resources/db/migration/V1__create_identity.sql`, `V2__create_catalog.sql`, `V3__create_stores_and_connectors.sql`, `V4__create_offers_history_currency.sql`, `V5__create_favorites_alerts_notifications_audit.sql`
- Test: `src/test/java/com/samp/MigrationIT.java`

**Interfaces:**
- Produces: tablas `users` (con `user_type` discriminador, `email` único case-insensitive, `preferred_country`, `preferred_currency`, `enabled`), `brands`, `categories`, `products`, `product_identifiers`, `product_attributes`, `stores`, `store_products`, `store_product_attributes`, `connector_executions`, `offers`, `price_history`, `exchange_rates`, `favorites`, `price_alerts`, `notifications`, `audit_logs`. Columnas de `stores`: `code` único, `name`, `country_code`, `domain`, `currency_code`, `data_kind` (`REAL`/`DEMO`), `active`, `shipping_policy_type` (`FREE`/`FLAT_RATE`/`UNKNOWN`), `flat_shipping_amount`, `tax_included` (nullable), `tax_rate` (nullable). `store_products`: `UNIQUE (store_id, external_id)`, `product_id` nullable, `match_method` (`GTIN`/`BRAND_REFERENCE`/`ATTRIBUTES`/`NONE`), `similar_to_product_id` nullable. Precios `NUMERIC(19,4)`, tiempos `timestamptz`.

- [ ] **Step 1: Escribir las pruebas que fallan** en `MigrationIT extends PostgresIT`: `allExpectedTablesExist()` (consulta `information_schema.tables` y comprueba los 16 nombres de arriba); `storeProductUniqueByStoreAndExternalId()` (dos inserts iguales → `DataIntegrityViolationException`); `priceHistoryAllowsManyRowsPerStoreProduct()` (dos filas de historial con precios distintos → 2 filas); `userEmailIsUniqueIgnoringCase()` (`Ana@x.com` y `ana@x.com` → violación de unicidad).
- [ ] **Step 2: Run test to verify it fails.** Run: `./mvnw -q test -Dtest=MigrationIT`. Expected: FAIL, tablas inexistentes.
- [ ] **Step 3: Escribir las cinco migraciones** con claves primarias y foráneas, índices en columnas de búsqueda (`products.name`, `product_identifiers.value`, `store_products.gtin`, `offers.store_product_id`, `price_history.store_product_id`) y la unicidad de email con un índice sobre `lower(email)`.
- [ ] **Step 4: Run test to verify it passes.** Run: `./mvnw -q test -Dtest=MigrationIT`. Expected: `Tests run: 4, Failures: 0`.
- [ ] **Step 5: Commit.** `git add src && git commit -m "feat: migraciones Flyway V1-V5"`.

### Task 3: Jerarquía de usuarios [Pilar: Herencia + Polimorfismo]

**Files:**
- Create: `src/main/java/com/samp/identity/entities/User.java`, `RegisteredUser.java`, `AdminUser.java`
- Create: `src/main/java/com/samp/identity/repositories/UserRepository.java`
- Test: `src/test/java/com/samp/identity/entities/UserHierarchyTest.java`, `src/test/java/com/samp/identity/repositories/UserRepositoryIT.java`

**Interfaces:**
- Produces:
  - `public abstract class User` (JPA `@Inheritance(SINGLE_TABLE)`, discriminador `user_type`): atributos privados `Long id`, `String email`, `String name`, `String passwordHash`, `boolean enabled`; constructores `User()` y `User(Long id, String email, String name, String passwordHash)`; getters/setters; `public abstract boolean canManageStores();` `public abstract String getRoleName();`.
  - `public class RegisteredUser extends User` (`preferredCountry`, `preferredCurrency`; `canManageStores()` = `false`; `getRoleName()` = `"ROLE_USER"`; constructor llama `super(id, email, name, passwordHash)`).
  - `public class AdminUser extends User` (`canManageStores()` = `true`; `getRoleName()` = `"ROLE_ADMIN"`).
  - `public interface UserRepository extends JpaRepository<User, Long> { Optional<User> findByEmailIgnoreCase(String email); boolean existsByEmailIgnoreCase(String email); }`

- [ ] **Step 1: Escribir las pruebas que fallan.** `adminInheritsEmailAndNameFromUser()`: `new AdminUser(1L,"a@x.com","Ana","hash").getEmail()` = `"a@x.com"`. `polymorphicCallOnUserType()`: lista `List<User>` con un `RegisteredUser` y un `AdminUser`; `canManageStores()` da `false` y `true`, `getRoleName()` da `"ROLE_USER"` y `"ROLE_ADMIN"`. `emptyConstructorAndFullConstructorBothWork()` (sobrecarga). `userRepositoryFindsSubtypeByEmailIgnoringCase()` en `UserRepositoryIT`: guarda un `AdminUser` `Ana@x.com`, `findByEmailIgnoreCase("ana@x.com")` devuelve una instancia de `AdminUser`.
- [ ] **Step 2: Run test to verify it fails.** Run: `./mvnw -q test -Dtest='UserHierarchyTest,UserRepositoryIT'`. Expected: FAIL, clases inexistentes.
- [ ] **Step 3: Implementar** las tres entidades con atributos `private`, constructores y accesores escritos a mano, y `@Override` en los métodos abstractos.
- [ ] **Step 4: Run test to verify it passes.** Mismo comando. Expected: `Tests run: 4, Failures: 0`.
- [ ] **Step 5: Commit.** `git add src && git commit -m "feat: jerarquía User, RegisteredUser, AdminUser"`.

### Task 4: Registro, seguridad y administrador inicial

**Files:**
- Create: `src/main/java/com/samp/identity/services/IUserService.java`, `UserServiceImpl.java`
- Create: `src/main/java/com/samp/security/SecurityConfig.java`, `SampUserDetailsService.java`, `AdminBootstrap.java`
- Create: `src/main/java/com/samp/identity/controllers/AuthController.java`
- Create: `src/main/resources/templates/auth/login.html`, `auth/register.html`, `layout/base.html`
- Test: `src/test/java/com/samp/identity/services/UserServiceImplTest.java`, `src/test/java/com/samp/security/SecurityIT.java`

**Interfaces:**
- Consumes: `UserRepository`, `RegisteredUser`, `AdminUser` (Tarea 3); `ValidationException` (Tarea 1).
- Produces: `public interface IUserService { RegisteredUser register(String email, String name, String rawPassword); Optional<User> findByEmail(String email); void ensureAdmin(String email, String rawPassword); }`. `AdminBootstrap` (`ApplicationRunner`) lee `SAMP_ADMIN_EMAIL` y `SAMP_ADMIN_PASSWORD`; si ambas existen y no hay `AdminUser`, llama `ensureAdmin`. Rutas: `/admin/**` exige `ROLE_ADMIN`; `/favorites/**`, `/profile/**` exigen sesión; el resto es público. `/login`, `/register`.

- [ ] **Step 1: Escribir las pruebas que fallan.** Unitarias (Mockito) en `UserServiceImplTest`: `registerNormalizesEmailToLowerCaseTrimmed()` (`"  Ana@X.com "` se guarda como `"ana@x.com"`); `registerRejectsDuplicateEmailIgnoringCase()` (lanza `ValidationException` con `"El correo ya está registrado."`); `registerRejectsShortPassword()` (menos de 8 caracteres → `ValidationException`); `registerStoresBCryptHashNotRawPassword()`; `ensureAdminDoesNothingIfAdminExists()`. De seguridad en `SecurityIT`: `anonymousCanOpenHomeAndSearch()` (GET `/` y `/search?q=x` ≠ 401/302 a login); `anonymousCannotOpenAdmin()` (GET `/admin` → redirige a `/login`); `registeredUserGets403OnAdmin()` (con `@WithMockUser(roles="USER")`); `postWithoutCsrfIsRejected()` (POST `/register` sin token → 403).
- [ ] **Step 2: Run test to verify it fails.** Run: `./mvnw -q test -Dtest='UserServiceImplTest,SecurityIT'`. Expected: FAIL.
- [ ] **Step 3: Implementar** `UserServiceImpl` (normaliza email con `trim().toLowerCase()`, valida con el repositorio, hash con `PasswordEncoder` BCrypt), `SecurityConfig` (form login, CSRF activo, cookies `HttpOnly` y `SameSite=Lax`), `SampUserDetailsService` (autoridad = `user.getRoleName()`; usuario deshabilitado no entra), `AdminBootstrap`, `AuthController` y plantillas con mensajes de error en español.
- [ ] **Step 4: Run test to verify it passes.** Mismo comando. Expected: `Failures: 0`.
- [ ] **Step 5: Commit.** `git add src && git commit -m "feat: registro, login y administrador inicial"`.

### Task 5: Catálogo y búsqueda por texto o GTIN

**Files:**
- Create: `src/main/java/com/samp/catalog/entities/Product.java`, `Brand.java`, `Category.java`, `ProductIdentifier.java`, `ProductAttribute.java`
- Create: `src/main/java/com/samp/catalog/repositories/ProductRepository.java`, `BrandRepository.java`, `CategoryRepository.java`
- Create: `src/main/java/com/samp/catalog/services/ICatalogService.java`, `CatalogServiceImpl.java`
- Test: `src/test/java/com/samp/catalog/entities/ProductEncapsulationTest.java`, `src/test/java/com/samp/catalog/services/CatalogServiceIT.java`

**Interfaces:**
- Produces:
  - `Product`: `Long id`, `String name`, `Brand brand`, `Category category`, `String modelRef`; `List<ProductIdentifier> getIdentifiers()` devuelve una **copia no modificable**; `void addIdentifier(String type, String value)`; `List<ProductAttribute> getAttributes()` (copia) y `void addAttribute(String name, String value)`.
  - `ProductRepository extends JpaRepository<Product, Long> { Page<Product> searchByText(String query, Pageable pageable); Optional<Product> findByIdentifierValue(String value); }` (JPQL con parámetro, `lower(name) like lower(concat('%', :query, '%'))`, nombre o marca o `modelRef`).
  - `ICatalogService { Page<Product> searchByText(String query, Pageable pageable); Optional<Product> findByGtin(String gtin); Product save(Product product); }`

- [ ] **Step 1: Escribir las pruebas que fallan.** **[Pilar: Encapsulamiento]** `ProductEncapsulationTest`: `getIdentifiersReturnsUnmodifiableCopy()` (`getIdentifiers().add(...)` lanza `UnsupportedOperationException`); `saveRejectsBlankName()` (`ValidationException`). `CatalogServiceIT`: `searchByTextFindsByNameBrandAndModelRef()`; `findByGtinReturnsProduct()` con `"3017620422003"`; **(Review Focus 1)** `searchWithBlankOrSpecialCharsReturnsEmptyPage()` para `""`, `"   "`, `"%"`, `"'"`, `"\""`, `"😀"` → `Page` vacía, sin excepción.
- [ ] **Step 2: Run test to verify it fails.** Run: `./mvnw -q test -Dtest='ProductEncapsulationTest,CatalogServiceIT'`. Expected: FAIL.
- [ ] **Step 3: Implementar** entidades (atributos privados, constructor vacío + completo, accesores a mano), repositorios y `CatalogServiceImpl` (texto vacío o en blanco → `Page.empty(pageable)` antes de tocar la BD).
- [ ] **Step 4: Run test to verify it passes.** Mismo comando. Expected: `Failures: 0`.
- [ ] **Step 5: Commit.** `git add src && git commit -m "feat: catálogo y búsqueda por texto o GTIN"`.

### Task 6: Tiendas y políticas de envío [Pilar: Abstracción + Polimorfismo]

**Files:**
- Create: `src/main/java/com/samp/stores/entities/Store.java`, `StoreProduct.java`, `DataKind.java`, `ShippingPolicyType.java`, `MatchMethod.java`
- Create (tipos compartidos que usan las Tareas 8 y 9): `src/main/java/com/samp/offers/entities/AvailabilityStatus.java`, `src/main/java/com/samp/connectors/entities/ConnectorProduct.java`
- Create: `src/main/java/com/samp/stores/repositories/StoreRepository.java`, `StoreProductRepository.java`
- Create: `src/main/java/com/samp/shipping/entities/ShippingPolicy.java`, `FreeShipping.java`, `FlatRateShipping.java`, `UnknownShipping.java`
- Create: `src/main/java/com/samp/shipping/services/ShippingPolicyFactory.java`
- Create: `src/main/resources/db/seed/V100__seed_demo_stores.sql`
- Test: `src/test/java/com/samp/shipping/ShippingPolicyTest.java`

**Interfaces:**
- Produces:
  - `enum DataKind { REAL, DEMO }`, `enum ShippingPolicyType { FREE, FLAT_RATE, UNKNOWN }`, `enum MatchMethod { GTIN, BRAND_REFERENCE, ATTRIBUTES, NONE }` (`NONE` = ninguna coincidencia con un producto existente; la ingestión crea un producto canónico nuevo y lo enlaza).
  - `enum AvailabilityStatus { AVAILABLE, OUT_OF_STOCK, UNKNOWN }` y `ConnectorProduct` con la definición de la Tarea 9 (clase con campos `private final`, constructor completo y getters).
  - `Store` con los campos de la migración V3 y getters/setters. `StoreProduct`: `Long id`, `Store store`, `String externalId`, `String title`, `String brandName`, `String modelRef`, `String gtin`, `String sourceUrl`, `String imageUrl`, `Map<String,String> attributes`, `Product product` (nullable), `MatchMethod matchMethod`, `Product similarToProduct` (nullable).
  - `StoreRepository { Optional<Store> findByCode(String code); List<Store> findByActiveTrue(); }`, `StoreProductRepository { Optional<StoreProduct> findByStoreAndExternalId(Store store, String externalId); List<StoreProduct> findByStore(Store store); }`.
  - `public abstract class ShippingPolicy { public abstract BigDecimal calculateShipping(BigDecimal listedPrice, String destinationCountry); }`; `FreeShipping` devuelve `new BigDecimal("0.00")`; `FlatRateShipping(BigDecimal amount)` devuelve `amount`; `UnknownShipping` devuelve `null`.
  - `public class ShippingPolicyFactory { public ShippingPolicy forStore(Store store); }` (`FLAT_RATE` sin `flatShippingAmount` → `UnknownShipping`).
  - Semilla `dev` (tabla `stores`): `open-food-facts-prices` (REAL, `openfoodfacts.org`, EUR, `UNKNOWN`); `dummyjson` (DEMO, `dummyjson.com`, USD, `FREE`, `tax_included=true`); `fakestoreapi` (DEMO, `fakestoreapi.com`, USD, `FLAT_RATE` 4.99, `tax_included=false`, `tax_rate=0.19`); `platzi` (DEMO, `api.escuelajs.co`, USD, `UNKNOWN`); `sample-store` (DEMO, `sample.samp.local`, USD, `FREE`, `tax_included=true`).

- [ ] **Step 1: Escribir las pruebas que fallan** en `ShippingPolicyTest`. **[Pilar: Polimorfismo]** `sameCallDifferentPolicies()`: con `List<ShippingPolicy>` de `new FreeShipping()`, `new FlatRateShipping(new BigDecimal("4.99"))`, `new UnknownShipping()`, y la llamada `calculateShipping(new BigDecimal("100.00"), "CO")` → `0.00`, `4.99`, `null`. **[Pilar: Abstracción]** `factoryReturnsSubtypeBehindAbstractType()`: tiendas con `FREE`, `FLAT_RATE`+`4.99` y `FLAT_RATE` sin monto dan las clases `FreeShipping`, `FlatRateShipping`, `UnknownShipping`, asignadas a una variable de tipo `ShippingPolicy`. `unknownShippingIsNullNotZero()`.
- [ ] **Step 2: Run test to verify it fails.** Run: `./mvnw -q test -Dtest=ShippingPolicyTest`. Expected: FAIL.
- [ ] **Step 3: Implementar** entidades, repositorios, las cuatro clases de envío (`@Override` en cada `calculateShipping`) y la semilla.
- [ ] **Step 4: Run test to verify it passes.** Mismo comando. Expected: `Tests run: 3, Failures: 0`.
- [ ] **Step 5: Commit.** `git add src && git commit -m "feat: tiendas y políticas de envío polimórficas"`.

### Task 7: Monedas y tasas de cambio

**Files:**
- Create: `src/main/java/com/samp/currency/entities/ExchangeRate.java`
- Create: `src/main/java/com/samp/currency/repositories/ExchangeRateRepository.java`
- Create: `src/main/java/com/samp/currency/services/ICurrencyService.java`, `CurrencyServiceImpl.java`, `ExchangeRateClient.java`, `OpenErApiClient.java`, `FrankfurterClient.java`, `FallbackExchangeRateClient.java`
- Test: `src/test/java/com/samp/currency/CurrencyServiceImplTest.java`, `src/test/java/com/samp/currency/OpenErApiClientTest.java`

**Interfaces:**
- Produces:
  - `ExchangeRate`: `String baseCurrency` (siempre `USD`), `String targetCurrency`, `BigDecimal rate`, `Instant fetchedAt`, `String source`.
  - `public interface ExchangeRateClient { Map<String, BigDecimal> fetchRates(String baseCurrency); String sourceName(); }`; `OpenErApiClient` lee `rates` del JSON; `FrankfurterClient` lee `rates`; `FallbackExchangeRateClient` prueba la primera y, ante excepción, la segunda.
  - `public interface ICurrencyService { Optional<BigDecimal> convert(BigDecimal amount, String from, String to); Optional<ExchangeRate> findRate(String targetCurrency); void refreshRates(); }`

- [ ] **Step 1: Escribir las pruebas que fallan.** `CurrencyServiceImplTest` (Mockito sobre el repositorio, con tasas USD→COP `4000.000000` y USD→EUR `0.910000`): `convertSameCurrencyReturnsSameAmount()` (`100.00 USD→USD` = `100.00`); `convertUsdToCop()` (`100.00` → `400000.00`); `convertEurToCopCrossesThroughUsd()` (`91.00 EUR→COP` = `400000.00`, redondeo `HALF_UP`, escala 2); **(Review Focus 3)** `convertWithoutRateReturnsEmpty()` (`USD→XYZ` → `Optional.empty()`, no cero). `OpenErApiClientTest` con `com.sun.net.httpserver.HttpServer` local sirviendo `{"result":"success","rates":{"USD":1,"COP":4000.5}}`: `fetchRatesParsesRates()` devuelve `COP` = `4000.5`; `httpErrorThrowsConnectorLikeException()` con respuesta 500 lanza `IllegalStateException`. Prueba de `FallbackExchangeRateClient`: si el primero lanza, usa el segundo.
- [ ] **Step 2: Run test to verify it fails.** Run: `./mvnw -q test -Dtest='CurrencyServiceImplTest,OpenErApiClientTest'`. Expected: FAIL.
- [ ] **Step 3: Implementar.** Tasa cruzada = `rate(USD→to) / rate(USD→from)` con escala 6 y `HALF_UP`; `refreshRates()` guarda cada tasa con `fetchedAt` y fuente; si el cliente falla, conserva las tasas guardadas. `convert` devuelve `Optional.empty()` si falta alguna tasa. `HttpClient` con timeout de conexión y lectura de 5 s.
- [ ] **Step 4: Run test to verify it passes.** Mismo comando. Expected: `Failures: 0`.
- [ ] **Step 5: Commit.** `git add src && git commit -m "feat: conversión de monedas con tasas fechadas"`.

### Task 8: Ofertas, historial y costo estimado [Pilar: Encapsulamiento]

**Files:**
- Create: `src/main/java/com/samp/offers/entities/Offer.java`
- Create: `src/main/java/com/samp/history/entities/PriceObservation.java`
- Create: `src/main/java/com/samp/offers/repositories/OfferRepository.java`, `src/main/java/com/samp/history/repositories/PriceObservationRepository.java`
- Create: `src/main/java/com/samp/offers/services/IOfferService.java`, `OfferServiceImpl.java`, `CostCalculator.java`
- Test: `src/test/java/com/samp/offers/CostCalculatorTest.java`, `src/test/java/com/samp/offers/OfferServiceIT.java`

**Interfaces:**
- Consumes: `StoreProduct`, `Store`, `ShippingPolicyFactory`, `AvailabilityStatus`, `ConnectorProduct` (Tarea 6); `ICurrencyService` (Tarea 7).
- Produces:
  - `Offer`: `Long id`, `StoreProduct storeProduct`, `BigDecimal listedPrice`, `String currencyCode`, `BigDecimal shippingAmount` (nullable), `BigDecimal taxAmount` (nullable), `BigDecimal convertedPrice` (nullable), `BigDecimal estimatedTotal` (nullable), `AvailabilityStatus availabilityStatus`, `Instant observedAt`, `String sourceUrl`, `boolean current`. `setListedPrice` lanza `ValidationException` si el precio es `null` o `<= 0`.
  - `PriceObservation`: `Long id`, `StoreProduct storeProduct`, `BigDecimal price`, `String currencyCode`, `Instant observedAt`.
  - `public final class CostCalculator { public static BigDecimal tax(BigDecimal listedPrice, Boolean taxIncluded, BigDecimal taxRate); public static BigDecimal estimatedTotal(BigDecimal convertedPrice, BigDecimal convertedShipping, BigDecimal convertedTax); }`
  - `public interface IOfferService { Offer upsertCurrentOffer(StoreProduct storeProduct, ConnectorProduct data); List<Offer> findCurrentByProduct(Long productId); List<PriceObservation> historyFor(Long storeProductId); }`

- [ ] **Step 1: Escribir las pruebas que fallan.** `CostCalculatorTest`: `taxIncludedMeansZeroKnown()` (`tax(100.00, true, null)` = `0.00`); `taxFromRate()` (`tax(100.00, false, 0.19)` = `19.00`); `taxUnknownIsNull()` (`tax(100.00, null, null)` = `null`); `estimatedTotalSumsWhenAllKnown()` (`100.00, 5.00, 19.00` → `124.00`); `estimatedTotalNullWhenShippingUnknown()` (`100.00, null, 19.00` → `null`); `estimatedTotalNullWhenTaxUnknown()`. `OfferServiceIT`: `upsertTwiceSamePriceKeepsOneCurrentOfferAndOneObservation()`; `priceChangeUpdatesCurrentOfferAndAddsObservation()` (100.00 → 90.00: una oferta vigente con `90.00`, dos observaciones); `unknownShippingStaysNullInOffer()` (tienda `UNKNOWN`: `getShippingAmount()` = `null`, `getEstimatedTotal()` = `null`); **(Review Focus 3)** `missingRateLeavesConvertedPriceAndTotalNull()` (moneda sin tasa: `getConvertedPrice()` = `null`, total `null`, `getListedPrice()` intacto); `offerRejectsNonPositivePrice()` para `0`, `-1` y `null`.
- [ ] **Step 2: Run test to verify it fails.** Run: `./mvnw -q test -Dtest='CostCalculatorTest,OfferServiceIT'`. Expected: FAIL.
- [ ] **Step 3: Implementar.** `upsertCurrentOffer`: busca la oferta vigente de `storeProduct`; la actualiza o crea; envío = el del conector si no es `null`, si no `ShippingPolicyFactory.forStore(...).calculateShipping(...)`; impuestos con `CostCalculator.tax`; convierte precio, envío e impuestos a USD con `ICurrencyService`; inserta `PriceObservation` solo si no hay observación previa o cambió precio o moneda. Todo en una transacción.
- [ ] **Step 4: Run test to verify it passes.** Mismo comando. Expected: `Failures: 0`.
- [ ] **Step 5: Commit.** `git add src && git commit -m "feat: ofertas con historial y costo estimado"`.

### Task 9: Contrato de conectores [Pilar: Abstracción + Herencia]

**Files:**
- Create: `src/main/java/com/samp/connectors/entities/ConnectorHealth.java`, `ConnectorException.java`, `ConnectorExecution.java` (`ConnectorProduct` ya existe desde la Tarea 6)
- Create: `src/main/java/com/samp/connectors/services/StoreConnector.java`, `AbstractStoreConnector.java`, `SampleStoreConnector.java`, `ConnectorRegistry.java`, `IConnectorExecutionService.java`, `ConnectorExecutionServiceImpl.java`
- Create: `src/main/java/com/samp/connectors/repositories/ConnectorExecutionRepository.java`
- Create: `src/main/resources/connectors/sample-store.json`
- Test: `src/test/java/com/samp/connectors/AbstractStoreConnectorTest.java`, `src/test/java/com/samp/connectors/SampleStoreConnectorTest.java`

**Interfaces:**
- Produces:
  - `ConnectorProduct` (clase con campos `private final` y getters): `String externalId`, `String title`, `String brandName`, `String modelRef`, `String gtin`, `BigDecimal listedPrice`, `String currencyCode`, `BigDecimal shippingAmount` (nullable), `BigDecimal taxAmount` (nullable), `AvailabilityStatus availabilityStatus`, `String sourceUrl`, `String imageUrl`, `Instant observedAt`, `Map<String,String> attributes`.
  - `public class ConnectorException extends RuntimeException { public enum Reason { TIMEOUT, HTTP_ERROR, INVALID_DATA } ; public ConnectorException(Reason reason, String message); public Reason getReason(); }`.
  - `public interface StoreConnector { String storeCode(); List<ConnectorProduct> search(String query); Optional<ConnectorProduct> getProduct(String externalId); ConnectorHealth health(); }`.
  - `public abstract class AbstractStoreConnector implements StoreConnector`: constructor `(String storeCode, HttpClient httpClient, String baseUrl, Duration timeout)`; `protected String httpGet(String url)` (2 reintentos con espera `200 ms * 2^n`; timeout → `Reason.TIMEOUT`; estado distinto de 2xx → `Reason.HTTP_ERROR`); `protected abstract String searchUrl(String query); protected abstract String productUrl(String externalId); protected abstract List<ConnectorProduct> parseSearch(String body); protected abstract Optional<ConnectorProduct> parseProduct(String body);` y `search`/`getProduct`/`health` implementados aquí como método plantilla (`@Override`). Una publicación con datos inválidos (precio `null`, `<= 0` o no numérico) se omite y se cuenta en un contador `skippedCount()`.
  - `SampleStoreConnector implements StoreConnector`: lee `classpath:connectors/sample-store.json` (≥ 8 productos, incluidos 3 con marca y referencia que coinciden con productos de `dummyjson` y 2 con GTIN real conocido, p. ej. `3017620422003`).
  - `ConnectorRegistry` (bean): `List<StoreConnector> all()`, `Optional<StoreConnector> byStoreCode(String code)`.
  - `ConnectorExecution`: `Long id`, `String storeCode`, `Instant startedAt`, `Instant finishedAt`, `int resultCount`, `String status` (`OK`/`FAILED`), `String errorMessage`. `IConnectorExecutionService { void record(String storeCode, Instant startedAt, Instant finishedAt, int resultCount, String errorMessage); List<ConnectorExecution> recent(int limit); }`.

- [ ] **Step 1: Escribir las pruebas que fallan.** `AbstractStoreConnectorTest` con un conector de prueba (subclase anónima) y `HttpServer` local: **[Pilar: Herencia]** `subclassReusesBaseStateAndHttp()` (`storeCode()` viene de la base); `retriesTwiceOnHttp500ThenThrowsHttpError()` (3 peticiones recibidas, `Reason.HTTP_ERROR`); `timeoutThrowsTimeoutReason()` (servidor que tarda más que `Duration.ofMillis(300)`); **(Review Focus 2)** `invalidPriceItemsAreSkippedNotFatal()` (respuesta con 3 productos: precios `10.00`, `null` y `-5` → `search` devuelve 1, `skippedCount()` = 2). `SampleStoreConnectorTest`: **[Pilar: Abstracción]** `searchServiceWorksWithAnyStoreConnectorImplementation()` — llama la búsqueda a través de una variable `StoreConnector` con la muestra y comprueba `search("phone")` no vacío; `getProductUnknownIdReturnsEmpty()`; `healthIsOk()`.
- [ ] **Step 2: Run test to verify it fails.** Run: `./mvnw -q test -Dtest='AbstractStoreConnectorTest,SampleStoreConnectorTest'`. Expected: FAIL.
- [ ] **Step 3: Implementar** las clases anteriores. El `HttpClient` es un bean con `connectTimeout(5s)`; el `User-Agent` es `samp-university-project/0.1` sin datos personales.
- [ ] **Step 4: Run test to verify it passes.** Mismo comando. Expected: `Failures: 0`.
- [ ] **Step 5: Commit.** `git add src && git commit -m "feat: contrato StoreConnector y conector de muestra"`.

### Task 10: Conectores reales y de demostración

**Files:**
- Create: `src/main/java/com/samp/connectors/services/OpenFoodFactsConnector.java`, `DummyJsonConnector.java`, `FakeStoreConnector.java`, `PlatziConnector.java`
- Create: `src/test/resources/fixtures/dummyjson-search.json`, `fakestore-products.json`, `platzi-products.json`, `off-search.json`, `off-prices.json`
- Test: `src/test/java/com/samp/connectors/RealConnectorsTest.java`

**Interfaces:**
- Consumes: `AbstractStoreConnector`, `ConnectorProduct`, `ConnectorException` (Tarea 9).
- Produces: cuatro subclases con constructor `(HttpClient httpClient, String baseUrl)` y `storeCode()` = `open-food-facts-prices`, `dummyjson`, `fakestoreapi`, `platzi`. URL base por defecto desde `application.yml` (`samp.connectors.<code>.base-url`). Mapeos:
  - `DummyJsonConnector`: `/products/search?q={q}`; `price`→`listedPrice`, `USD`, `brand`, `sku`→`modelRef`, `availabilityStatus` (`In Stock`→`AVAILABLE`, `Out of Stock`→`OUT_OF_STOCK`), `id`→`externalId`, `sourceUrl` = `https://dummyjson.com/products/{id}`.
  - `FakeStoreConnector`: `/products` (filtra por título en memoria), `USD`, sin marca ni stock (`UNKNOWN`).
  - `PlatziConnector`: `/api/v1/products?title={q}`, `USD`, sin marca.
  - `OpenFoodFactsConnector`: busca productos con `world.openfoodfacts.org/cgi/search.pl?search_terms={q}&json=1&page_size=5` y para cada `code` pide `prices.openfoodfacts.org/api/v1/prices?product_code={code}&size=5`; `externalId` = `{code}:{location_id}`, `gtin` = `code`, `price`/`currency` de cada observación, `title` = nombre del producto, `brandName` = primera marca, `observedAt` = fecha de la observación, `sourceUrl` = `https://prices.openfoodfacts.org/prices/{id}`.

- [ ] **Step 1: Verificar los endpoints con una petición real** y guardar 1 respuesta recortada de cada fuente como fixture (para `off-search.json` confirmar primero que el endpoint de búsqueda responde con `products[].code`; si no, elegir el endpoint de búsqueda vigente de Open Food Facts y anotarlo en el conector).
- [ ] **Step 2: Escribir las pruebas que fallan** en `RealConnectorsTest` con `HttpServer` local que sirve los fixtures: `dummyJsonMapsPriceBrandAndStock()`; `fakeStoreMapsPriceInUsdWithUnknownStock()`; `platziMapsTitleAndPrice()`; `openFoodFactsMapsOneOfferPerLocationWithGtin()` (el mismo GTIN con 2 ubicaciones da 2 `ConnectorProduct` con `externalId` distintos y el mismo `gtin`); **(Review Focus 2)** `connectorSkipsItemsWithNullOrNegativePrice()` por cada conector con un fixture que mezcla precios válidos y `null`/negativos; `connectorServerErrorThrowsHttpError()`.
- [ ] **Step 3: Run test to verify it fails.** Run: `./mvnw -q test -Dtest=RealConnectorsTest`. Expected: FAIL.
- [ ] **Step 4: Implementar** las cuatro subclases rellenando los métodos abstractos con Jackson (`ObjectMapper.readTree`), sin duplicar la lógica HTTP.
- [ ] **Step 5: Run test to verify it passes.** Mismo comando. Expected: `Failures: 0`.
- [ ] **Step 6: Commit.** `git add src && git commit -m "feat: conectores Open Food Facts, DummyJSON, FakeStore y Platzi"`.

### Task 11: Emparejamiento e ingestión [Pilar: Polimorfismo]

**Files:**
- Create: `src/main/java/com/samp/comparison/services/ProductMatcher.java`, `GtinMatcher.java`, `BrandReferenceMatcher.java`, `AttributeMatcher.java`, `TextSimilarity.java`
- Create: `src/main/java/com/samp/comparison/services/IMatchingService.java`, `MatchingServiceImpl.java`, `MatchOutcome.java`
- Create: `src/main/java/com/samp/offers/services/IIngestionService.java`, `IngestionServiceImpl.java`, `IngestionReport.java`
- Test: `src/test/java/com/samp/comparison/MatchersTest.java`, `src/test/java/com/samp/comparison/MatchingServiceIT.java`, `src/test/java/com/samp/offers/IngestionServiceIT.java`

**Interfaces:**
- Consumes: `StoreProduct`, `MatchMethod` (Tarea 6); `Product`, `ICatalogService` (Tarea 5); `IOfferService`, `ConnectorProduct` (Tareas 8, 9).
- Produces:
  - `public interface ProductMatcher { MatchMethod method(); boolean matches(StoreProduct storeProduct, Product product); }`. `GtinMatcher`: `gtin` no vacío y igual a algún identificador de `product`. `BrandReferenceMatcher`: marca igual sin distinguir mayúsculas y `modelRef` igual tras normalizar (quitar espacios y guiones, mayúsculas); `false` si alguno falta. `AttributeMatcher`: misma marca y al menos 2 atributos de variante en común (`capacity`, `size`, `color`, `memory`, `generation`, `condition`, `region`) todos iguales. Si algún atributo de variante compartido **difiere**, ningún matcher de nivel 2 o 3 puede devolver `true`.
  - `TextSimilarity { static double jaccard(String a, String b) }` (tokens en minúsculas).
  - `MatchOutcome { MatchMethod getMethod(); Product getProduct(); Product getSimilarTo(); }`.
  - `public interface IMatchingService { MatchOutcome match(StoreProduct storeProduct); }`: prueba los matchers en el orden GTIN → marca+referencia → atributos; si ninguno acierta, `NONE` y, si el mejor `jaccard` contra algún producto es `>= 0.6`, `getSimilarTo()` lo devuelve.
  - `public interface IIngestionService { IngestionReport ingest(String storeCode, List<ConnectorProduct> products); IngestionReport refreshStore(String storeCode); }`. `ingest`: por cada producto, upsert de `StoreProduct` por (`store`, `externalId`), `match`; con `NONE` crea un `Product` canónico nuevo a partir de la publicación y lo enlaza; luego `IOfferService.upsertCurrentOffer`. `refreshStore` llama `getProduct` por cada `StoreProduct` de la tienda. `IngestionReport { int created(); int updated(); int skipped(); }`.

- [ ] **Step 1: Escribir las pruebas que fallan.** `MatchersTest`: **[Pilar: Polimorfismo]** `sameCallDifferentMatchers()` con `List<ProductMatcher>` ejecutando `matches(sp, product)` sobre un mismo par y comprobando cada resultado; `gtinMatchesExactValue()` (`"3017620422003"` = `"3017620422003"`, distinto en un dígito → `false`); `brandReferenceIgnoresCaseSpacesAndHyphens()` (`"Apple"`/`" MQ-1 "` vs `"apple"`/`"mq1"` → `true`); `differentColorBlocksEquivalence()` (misma marca y referencia, `color` `black` vs `white` → `false` en `BrandReferenceMatcher` y `AttributeMatcher`); `blankGtinNeverMatches()`. `MatchingServiceIT`: `titleSimilarityOnlyGivesSimilarNotMatch()` (títulos con `jaccard >= 0.6` sin identificadores: método `NONE`, `getSimilarTo()` no `null`, `getProduct()` `null`); `gtinWinsOverOtherMatchers()`. `IngestionServiceIT`: `sameGtinFromTwoStoresLinksToOneProduct()`; `reingestingSameDataCreatesNoDuplicates()` (dos ingestiones → un `StoreProduct`, una oferta vigente); `gtinNoLongerMatchingUnlinksProduct()`; `unmatchedPublicationCreatesCanonicalProduct()`.
- [ ] **Step 2: Run test to verify it fails.** Run: `./mvnw -q test -Dtest='MatchersTest,MatchingServiceIT,IngestionServiceIT'`. Expected: FAIL.
- [ ] **Step 3: Implementar** matchers (`@Override` en cada uno), `TextSimilarity`, `MatchingServiceImpl` (recibe `List<ProductMatcher>` ordenada por `method()`), `IngestionServiceImpl` (transaccional por producto, un fallo no aborta el lote: se cuenta en `skipped`).
- [ ] **Step 4: Run test to verify it passes.** Mismo comando. Expected: `Failures: 0`.
- [ ] **Step 5: Commit.** `git add src && git commit -m "feat: matchers polimórficos e ingestión"`.

### Task 12: Búsqueda en vivo y páginas públicas

**Files:**
- Create: `src/main/java/com/samp/search/services/ISearchService.java`, `SearchServiceImpl.java`, `SearchResult.java`, `SearchItem.java`
- Create: `src/main/java/com/samp/search/controllers/HomeController.java`, `SearchController.java`
- Create: `src/main/resources/templates/index.html`, `search/results.html`, `stores/list.html`, `src/main/resources/static/css/app.css`
- Test: `src/test/java/com/samp/search/SearchServiceImplTest.java`, `src/test/java/com/samp/search/SearchControllerIT.java`

**Interfaces:**
- Consumes: `ConnectorRegistry`, `IConnectorExecutionService` (Tarea 9); `IIngestionService` (Tarea 11); `ICatalogService` (Tarea 5); `StoreRepository` (Tarea 6).
- Produces:
  - `public interface ISearchService { SearchResult search(String query, Pageable pageable); }`.
  - `SearchResult { List<SearchItem> getItems(); List<String> getFailedStores(); Instant getSearchedAt(); String getMessage(); }`. `SearchItem { Long getProductId(); String getTitle(); String getBrandName(); int getOfferCount(); BigDecimal getLowestConvertedPrice(); String getImageUrl(); }`.
  - Rutas: `GET /` (buscador y tiendas), `GET /search?q=&page=`, `GET /stores`.

- [ ] **Step 1: Escribir las pruebas que fallan.** `SearchServiceImplTest` (Mockito, conectores falsos que implementan `StoreConnector`): `searchQueriesActiveConnectorsInParallel()` (dos conectores que duermen 300 ms cada uno terminan en menos de 550 ms en total); `failingConnectorDoesNotStopOthers()` (uno lanza `ConnectorException`, el otro devuelve 1 producto → 1 ítem y `getFailedStores()` contiene el código fallido); **(Review Focus 5)** `allConnectorsFailingStillReturnsStoredData()` (con datos ya guardados devuelve esos ítems; sin datos devuelve lista vacía y `getMessage()` = `"No se pudo consultar ninguna tienda. Mostrando datos guardados."`); `connectorTimeoutIsApplied()` (conector que tarda 10 s se corta a los 8 s); `executionsAreRecordedOkAndFailed()`. **(Review Focus 1)** `blankQueryReturnsEmptyWithMessageWithoutCallingConnectors()`. `SearchControllerIT`: `searchWithoutLoginReturns200()`; `blankQueryReturns200WithMessage()`; `specialCharsQueryReturns200()` para `%`, `'`, `<script>`; `htmlInQueryIsEscaped()` (el `<script>` aparece escapado en el HTML); `homeListsStoresWithDataKind()` (la página `/stores` muestra "Demostración" para las tiendas `DEMO`).
- [ ] **Step 2: Run test to verify it fails.** Run: `./mvnw -q test -Dtest='SearchServiceImplTest,SearchControllerIT'`. Expected: FAIL.
- [ ] **Step 3: Implementar.** `SearchServiceImpl`: valida la consulta; `CompletableFuture.supplyAsync` por conector activo con `orTimeout(8, SECONDS)`; guarda con `IIngestionService.ingest`; registra cada ejecución; arma el resultado leyendo de la BD. Plantillas con `th:text` (nunca `th:utext`), estados de carga/vacío/error y diseño responsive.
- [ ] **Step 4: Run test to verify it passes.** Mismo comando. Expected: `Failures: 0`.
- [ ] **Step 5: Commit.** `git add src && git commit -m "feat: búsqueda en vivo y páginas públicas"`.

### Task 13: Comparación, detalle y enlaces externos

**Files:**
- Create: `src/main/java/com/samp/comparison/services/IComparisonService.java`, `ComparisonServiceImpl.java`, `ComparisonResult.java`, `ComparisonRow.java`
- Create: `src/main/java/com/samp/common/ExternalLinkValidator.java`
- Create: `src/main/java/com/samp/comparison/controllers/ProductController.java`
- Create: `src/main/resources/templates/products/detail.html`
- Test: `src/test/java/com/samp/comparison/ComparisonServiceImplTest.java`, `src/test/java/com/samp/common/ExternalLinkValidatorTest.java`, `src/test/java/com/samp/comparison/ProductControllerIT.java`

**Interfaces:**
- Consumes: `IOfferService.findCurrentByProduct`, `historyFor` (Tarea 8); `StoreProductRepository` (Tarea 6).
- Produces:
  - `ComparisonRow { Offer getOffer(); boolean isTotalComplete(); }`. `ComparisonResult { List<ComparisonRow> getRows(); LabelKind getLabelKind(); Long getLabeledOfferId(); List<StoreProduct> getSimilarItems(); }` con `enum LabelKind { CHEAPEST, LOWEST_PUBLISHED_PRICE, NONE }`.
  - `public interface IComparisonService { ComparisonResult compare(Long productId); }`. Orden: primero los totales completos de menor a mayor `estimatedTotal`; luego los incompletos por `convertedPrice` ascendente con `null` al final. Etiqueta `CHEAPEST` en `rows[0]` solo si **todas** las filas son completas; si alguna es incompleta, `LOWEST_PUBLISHED_PRICE` en la fila de menor `convertedPrice` no nulo; sin ofertas, `NONE`.
  - `public final class ExternalLinkValidator { public static boolean isSafe(String url, String storeDomain); }`.
  - Ruta `GET /products/{id}` y `GET /go/{offerId}` (valida y redirige a `sourceUrl`; si no es seguro, 400).

- [ ] **Step 1: Escribir las pruebas que fallan.** `ComparisonServiceImplTest`: `ordersCompleteTotalsFirstThenIncompleteByConvertedPrice()` (A `105.00`, B `99.00` completas; C incompleta con `convertedPrice` `90.00` → filas B, A, C); `labelIsLowestPublishedPriceWhenAnyIncomplete()` (etiqueta sobre la oferta C); `labelIsCheapestOnlyWhenAllComplete()` (solo A y B → `CHEAPEST` sobre B); `nullConvertedPriceGoesLast()`; `noOffersGivesNoneLabel()`; `similarItemsAreSeparatedFromRows()`. `ExternalLinkValidatorTest` con dominio `dummyjson.com`: `https://dummyjson.com/products/1` → `true`; `https://www.dummyjson.com/x` → `true`; `javascript:alert(1)` → `false`; `https://evil.com/x` → `false`; `https://dummyjson.com.evil.com/x` → `false`; `ftp://dummyjson.com/x` → `false`; `null` → `false`. `ProductControllerIT`: `detailShowsStoreCurrencyObservedAtAndIncompleteLabel()` (el HTML contiene el código de moneda, la fecha de observación y el texto `"total estimado incompleto"` para una oferta sin envío); `unknownShippingIsNotShownAsZero()`; `goWithUnsafeLinkReturns400()`.
- [ ] **Step 2: Run test to verify it fails.** Run: `./mvnw -q test -Dtest='ComparisonServiceImplTest,ExternalLinkValidatorTest,ProductControllerIT'`. Expected: FAIL.
- [ ] **Step 3: Implementar.** `isSafe`: esquema `http`/`https`, host igual al dominio o terminado en `"." + dominio`. La plantilla explica el criterio de orden y rotula los datos `DEMO`.
- [ ] **Step 4: Run test to verify it passes.** Mismo comando. Expected: `Failures: 0`.
- [ ] **Step 5: Commit.** `git add src && git commit -m "feat: comparación de ofertas y enlaces externos seguros"`.

### Task 14: Favoritos

**Files:**
- Create: `src/main/java/com/samp/favorites/entities/Favorite.java`
- Create: `src/main/java/com/samp/favorites/repositories/FavoriteRepository.java`
- Create: `src/main/java/com/samp/favorites/services/IFavoriteService.java`, `FavoriteServiceImpl.java`
- Create: `src/main/java/com/samp/favorites/controllers/FavoriteController.java`, `src/main/resources/templates/favorites/list.html`
- Test: `src/test/java/com/samp/favorites/FavoriteServiceIT.java`, `src/test/java/com/samp/favorites/FavoriteControllerIT.java`

**Interfaces:**
- Produces: `public interface IFavoriteService { void add(Long userId, Long productId); void remove(Long userId, Long productId); List<Product> list(Long userId); }`. Rutas (con sesión): `GET /favorites`, `POST /favorites/{productId}`, `POST /favorites/{productId}/remove`. El `userId` sale siempre de la sesión, nunca de un parámetro.

- [ ] **Step 1: Escribir las pruebas que fallan.** `FavoriteServiceIT`: `addIsIdempotent()`; `listReturnsOnlyOwnFavorites()` (usuario A y B con favoritos distintos); `removeOnlyAffectsOwnFavorite()` (A llama `remove` sobre el producto favorito de B y el favorito de B sigue); `addUnknownProductThrowsNotFound()`. `FavoriteControllerIT`: `anonymousIsRedirectedToLogin()`; `userSeesOnlyOwnFavoritesInHtml()`; `userCannotRemoveAnotherUsersFavoriteViaUrl()`.
- [ ] **Step 2: Run test to verify it fails.** Run: `./mvnw -q test -Dtest='FavoriteServiceIT,FavoriteControllerIT'`. Expected: FAIL.
- [ ] **Step 3: Implementar** con restricción única (`user_id`, `product_id`) ya definida en la migración V5.
- [ ] **Step 4: Run test to verify it passes.** Mismo comando. Expected: `Failures: 0`.
- [ ] **Step 5: Commit.** `git add src && git commit -m "feat: favoritos por usuario"`.

### Task 15: Panel de administración

**Files:**
- Create: `src/main/java/com/samp/administration/services/IAdminService.java`, `AdminServiceImpl.java`
- Create: `src/main/java/com/samp/administration/controllers/AdminController.java`
- Create: `src/main/resources/templates/admin/dashboard.html`, `admin/stores.html`, `admin/executions.html`, `admin/users.html`, `admin/catalog.html`
- Test: `src/test/java/com/samp/administration/AdminServiceImplTest.java`, `src/test/java/com/samp/administration/AdminControllerIT.java`

**Interfaces:**
- Consumes: `StoreRepository` (Tarea 6), `IIngestionService.refreshStore` (Tarea 11), `IConnectorExecutionService.recent` (Tarea 9), `UserRepository` (Tarea 3), repositorios de catálogo (Tarea 5).
- Produces: `public interface IAdminService { void setStoreActive(String storeCode, boolean active); IngestionReport refreshStore(String storeCode); List<ConnectorExecution> recentExecutions(int limit); void setUserEnabled(Long userId, boolean enabled); Brand createBrand(String name); Category createCategory(String name); }`. Rutas bajo `/admin`: `/admin`, `/admin/stores`, `/admin/executions`, `/admin/users`, `/admin/catalog`. Todas con `ROLE_ADMIN`.

- [ ] **Step 1: Escribir las pruebas que fallan.** `AdminServiceImplTest`: `deactivatedStoreIsExcludedFromActiveStores()`; `adminCannotDisableOwnAccount()` (`ValidationException`); `refreshStoreDelegatesToIngestion()`; `createBrandRejectsDuplicateName()`. `AdminControllerIT`: `adminCanOpenDashboard()` (`@WithMockUser(roles="ADMIN")` → 200); `registeredUserGets403()`; `anonymousIsRedirectedToLogin()`; `postWithoutCsrfIsRejected()`.
- [ ] **Step 2: Run test to verify it fails.** Run: `./mvnw -q test -Dtest='AdminServiceImplTest,AdminControllerIT'`. Expected: FAIL.
- [ ] **Step 3: Implementar.** Mostrar solo datos necesarios de usuarios (email y estado); la acción de refrescar corre síncrona con el mismo servicio de ingestión que usa la búsqueda.
- [ ] **Step 4: Run test to verify it passes.** Mismo comando. Expected: `Failures: 0`.
- [ ] **Step 5: Commit.** `git add src && git commit -m "feat: panel de administración básico"`.

### Task 16: Alertas y preferencias (si alcanza el tiempo)

**Files:**
- Create: `src/main/java/com/samp/notifications/entities/PriceAlert.java`, `Notification.java`
- Create: `src/main/java/com/samp/notifications/repositories/PriceAlertRepository.java`, `NotificationRepository.java`
- Create: `src/main/java/com/samp/notifications/services/IAlertService.java`, `AlertServiceImpl.java`
- Create: `src/main/java/com/samp/identity/controllers/ProfileController.java`, `src/main/resources/templates/profile/edit.html`, `notifications/list.html`
- Test: `src/test/java/com/samp/notifications/AlertServiceIT.java`

**Interfaces:**
- Produces: `public interface IAlertService { PriceAlert create(Long userId, Long productId, BigDecimal thresholdUsd); void evaluateAfterIngestion(Long productId); List<Notification> listFor(Long userId); }`. `evaluateAfterIngestion` se llama al final de `IIngestionService.ingest`. Umbral absoluto en USD; la alerta se dispara si algún `estimatedTotal` completo es `<=` al umbral; notificación solo dentro de la web.

- [ ] **Step 1: Escribir las pruebas que fallan.** `alertFiresWhenCompleteTotalBelowThreshold()` (umbral `100.00`, total `99.00` → 1 notificación); `alertDoesNotFireOnIncompleteTotal()` (total `null` → ninguna); `alertDoesNotFireTwiceForSameOffer()`; `userSeesOnlyOwnNotifications()`; `createRejectsNonPositiveThreshold()`; `profileUpdateStoresPreferredCountryAndCurrency()`.
- [ ] **Step 2: Run test to verify it fails.** Run: `./mvnw -q test -Dtest=AlertServiceIT`. Expected: FAIL.
- [ ] **Step 3: Implementar** servicio, controlador de perfil (país y moneda preferidos; la visualización usa `preferredCurrency` o `COP`) y plantillas.
- [ ] **Step 4: Run test to verify it passes.** Mismo comando. Expected: `Failures: 0`.
- [ ] **Step 5: Commit.** `git add src && git commit -m "feat: alertas de precio y preferencias"`.

### Task 17: Rate limiting y caché (si alcanza el tiempo)

**Files:**
- Modify: `pom.xml` (Bucket4j y Caffeine, versiones fijas)
- Create: `src/main/java/com/samp/config/RateLimitFilter.java`, `CacheConfig.java`
- Modify: `src/main/java/com/samp/currency/services/CurrencyServiceImpl.java:findRate` (añadir `@Cacheable`), `src/main/java/com/samp/search/services/SearchServiceImpl.java` (caché de 60 s por consulta normalizada)
- Test: `src/test/java/com/samp/config/RateLimitFilterIT.java`

**Interfaces:**
- Produces: `RateLimitFilter` por IP: `POST /login` máximo 5 por minuto, `GET /search` máximo 30 por minuto; excedido → 429 con mensaje en español.

- [ ] **Step 1: Escribir las pruebas que fallan.** `sixthLoginAttemptInAMinuteReturns429()`; `thirtyFirstSearchInAMinuteReturns429()`; `otherIpIsNotAffected()`; `cachedSearchDoesNotCallConnectorsTwice()` (misma consulta dos veces → los conectores falsos reciben 1 llamada); `cacheKeyIsCaseAndSpaceInsensitive()` (`"Phone "` y `"phone"`).
- [ ] **Step 2: Run test to verify it fails.** Run: `./mvnw -q test -Dtest=RateLimitFilterIT`. Expected: FAIL.
- [ ] **Step 3: Implementar** el filtro con un `Bucket` por IP y clave de ruta, y el caché con Caffeine (expiración 60 s en búsquedas, 1 h en tasas).
- [ ] **Step 4: Run test to verify it passes.** Mismo comando. Expected: `Failures: 0`. Luego `./mvnw -q test` completo: `Failures: 0`.
- [ ] **Step 5: Commit.** `git add pom.xml src && git commit -m "feat: rate limiting y caché"`.

### Task 18: Defensa de pilares, documentación y despliegue

**Files:**
- Create: `docs/pilares.md`, `README.md`, `docs/architecture/architecture.md`, `docs/database/erd.md`, `docs/database/data-dictionary.md`, `docs/deployment/guide.md`
- Create: `Dockerfile`, `.dockerignore`

**Interfaces:**
- Consumes: las pruebas **[Pilar]** de las Tareas 3, 5, 6, 9 y 11.

- [ ] **Step 1: Correr toda la suite.** Run: `./mvnw -q test`. Expected: `Failures: 0, Errors: 0`.
- [ ] **Step 2: Escribir `docs/pilares.md`** con, por cada pilar, una tabla: clase que se señala (`ruta:línea`), una línea explicable, la prueba repetible (nombre de clase y método de la tarea correspondiente) y el comando exacto `./mvnw -q test -Dtest=<Clase>#<método>`; más una operación recorrida de principio a fin (`GET /search?q=nutella` → `SearchController` → `SearchServiceImpl` → `StoreConnector` → `IngestionServiceImpl` → `OfferServiceImpl` → repositorio → `ComparisonServiceImpl`). Incluir la plantilla de defensa "Me basé en ___ del repositorio. Cambié ___ porque el requisito pide ___. Probé con ___, esperaba ___ y obtuve ___".
- [ ] **Step 3: Escribir `README.md`** (requisitos: JDK 21, Docker; instalación; variables de entorno con `.env.example`; ejecución con perfil `dev`; pruebas) y los demás documentos (ERD en Mermaid basado en las migraciones; diccionario de datos con tablas, columnas, tipos, restricciones e índices).
- [ ] **Step 4: Crear `Dockerfile`** multi-etapa (`eclipse-temurin:21-jdk` para compilar con `./mvnw -q -DskipTests package`, `eclipse-temurin:21-jre` para ejecutar) con `HEALTHCHECK` sobre `/actuator/health`. Run: `docker build -t samp:local .` y `docker run --rm -p 8080:8080 --env-file .env samp:local`. Expected: `/actuator/health` responde `UP`.
- [ ] **Step 5: Desplegar si alcanza el tiempo.** Guía en `docs/deployment/guide.md`: Neon (PostgreSQL), Render (Docker), variables de entorno, HTTPS, aviso de suspensión por inactividad del plan gratuito.
- [ ] **Step 6: Commit.** `git add . && git commit -m "docs: defensa de pilares, README y despliegue"`.
