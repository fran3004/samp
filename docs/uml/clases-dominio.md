# SAMP — Diagramas de clases de dominio

Paquetes por módulo (v2.0): `web`, `service`, `domain`, `repository`, `dto`. Los diagramas muestran `domain` y los servicios clave. Interfaz = `<<interface>>`, clase abstracta = `<<abstract>>`.

## 1. Conectores (herencia, abstracción, polimorfismo)

```mermaid
classDiagram
    direction TB
    class StoreConnector {
        <<interface>>
        +storeCode() String
        +capabilities() ConnectorCapabilities
        +search(SearchQuery q, ConnectorContext ctx) List~RawOffer~
        +fetchByIdentifier(ProductIdentifier id, ConnectorContext ctx) Optional~RawOffer~
        +health() HealthStatus
    }
    class AbstractStoreConnector {
        <<abstract>>
        -RateLimiter rateLimiter
        -int maxRetries
        +search(SearchQuery q, ConnectorContext ctx) List~RawOffer~
        #fetch(SearchQuery q) String
        #parse(String payload) List~RawOffer~
        -validate(RawOffer o) boolean
    }
    class ApiStoreConnector {
        <<abstract>>
        -String baseUrl
        -CredentialsRef credentials
        #fetch(SearchQuery q) String
    }
    class ScrapingStoreConnector {
        <<abstract>>
        -ScrapeConfig config
        -RobotsPolicy robots
        #fetch(SearchQuery q) String
        #selectors() ScrapeConfig
    }
    class FeedStoreConnector {
        -String feedUrl
        #fetch(SearchQuery q) String
        #parse(String payload) List~RawOffer~
    }
    class CatalogFallbackConnector {
        -ProductRepository repo
        #fetch(SearchQuery q) String
    }
    class EbayConnector {
        #parse(String payload) List~RawOffer~
    }
    class MercadoLibreConnector {
        #parse(String payload) List~RawOffer~
    }
    class ShopScraperConnector {
        #parse(String payload) List~RawOffer~
        #selectors() ScrapeConfig
    }
    class ConnectorSelector {
        -SelectionPolicy policy
        +orderedFor(Store s) List~StoreConnector~
    }
    class SelectionPolicy {
        <<interface>>
        +rank(List~ConnectorState~ c) List~ConnectorState~
    }
    class SuccessRatePolicy {
        +rank(List~ConnectorState~ c) List~ConnectorState~
    }
    class ConnectorRegistry {
        -Map~String,StoreConnector~ byCode
        +get(String code) StoreConnector
    }
    class ConnectorState {
        -int priority
        -double successRate
        -CircuitState circuit
        -int consecutiveFailures
        +isAvailable() boolean
        +recordSuccess()
        +recordFailure()
    }
    class RawOffer {
        <<value object>>
        +externalId String
        +title String
        +price BigDecimal
        +currency String
        +imageUrls List~String~
        +attributes Map~String,String~
    }
    class SearchQuery {
        <<value object>>
    }
    class ConnectorContext {
        <<value object>>
    }
    class ConnectorCapabilities {
        <<value object>>
    }
    class HealthStatus {
        <<value object>>
    }

    StoreConnector <|.. AbstractStoreConnector
    AbstractStoreConnector <|-- ApiStoreConnector
    AbstractStoreConnector <|-- ScrapingStoreConnector
    AbstractStoreConnector <|-- FeedStoreConnector
    AbstractStoreConnector <|-- CatalogFallbackConnector
    ApiStoreConnector <|-- EbayConnector
    ApiStoreConnector <|-- MercadoLibreConnector
    ScrapingStoreConnector <|-- ShopScraperConnector
    SelectionPolicy <|.. SuccessRatePolicy
    ConnectorSelector --> SelectionPolicy : usa
    ConnectorSelector --> ConnectorRegistry : consulta
    ConnectorSelector --> ConnectorState : ordena
    StoreConnector ..> RawOffer : produce
    StoreConnector ..> SearchQuery
    StoreConnector ..> ConnectorContext
    StoreConnector ..> HealthStatus
    StoreConnector ..> ConnectorCapabilities
```

## 2. Ingesta y normalización (perfil `scraper`)

```mermaid
classDiagram
    direction LR
    class ScrapeRunner {
        <<CommandLineRunner perfil scraper>>
        +run(String[] args)
    }
    class ScrapeOrchestrator {
        -ConnectorSelector selector
        -OfferNormalizer normalizer
        -DataQualityValidator validator
        -ProductMatcher matcher
        +runAll(Trigger t)
        +runStore(Store s)
        -execute(StoreConnector c) ConnectorExecution
    }
    class ConnectorExecution {
        -Status status
        -int created
        -int updated
        -int skipped
        -double completenessPct
        +finish(Status s)
    }
    class OfferNormalizer {
        +normalize(RawOffer r, Store s) StoreProduct
        -toMoney(RawOffer r) Money
    }
    class DataQualityValidator {
        +check(RawOffer r) List~QualityIssue~
        +completeness(RawOffer r) double
    }
    class ProductMatcher {
        -List~MatchStrategy~ strategies
        +match(StoreProduct sp) MatchResult
    }
    class MatchStrategy {
        <<interface>>
        +level() int
        +tryMatch(StoreProduct sp) Optional~MatchResult~
    }
    class GtinMatch
    class MpnMatch
    class AttributeMatch
    class TextSimilarityMatch
    class EmbeddingMatch
    class PriceCalculator {
        +finalPrice(Money price, Money shipping, Money tax) Money
    }
    class CurrencyConverter {
        +convert(Money m, String target) Money
    }

    ScrapeRunner --> ScrapeOrchestrator
    ScrapeOrchestrator --> ConnectorExecution : crea
    ScrapeOrchestrator --> OfferNormalizer
    ScrapeOrchestrator --> DataQualityValidator
    ScrapeOrchestrator --> ProductMatcher
    ScrapeOrchestrator --> PriceCalculator
    ProductMatcher o-- MatchStrategy
    MatchStrategy <|.. GtinMatch
    MatchStrategy <|.. MpnMatch
    MatchStrategy <|.. AttributeMatch
    MatchStrategy <|.. TextSimilarityMatch
    MatchStrategy <|.. EmbeddingMatch
    PriceCalculator --> CurrencyConverter
```

Flujo: `selector.orderedFor(store)` → `connector.search()` → `validator.check()` (incidencias en `data_quality_issues`) → `normalizer.normalize()` → `matcher.match()` → `PriceCalculator` (Pf = Pp + Pe + Pi, envío desconocido nunca es cero) → persistir `offers` y `price_history`. Si el conector falla, `ConnectorState.recordFailure()` y se prueba el siguiente.

## 3. Dominio base (encapsulamiento)

```mermaid
classDiagram
    direction TB
    class Money {
        <<value object inmutable>>
        -BigDecimal amount
        -String currency
        +plus(Money o) Money
        +convertTo(String cur, ExchangeRate r) Money
        +isZero() boolean
    }
    class User {
        -Long id
        -String email
        -String passwordHash
        -boolean enabled
        -Set~Role~ roles
        +hasRole(String name) boolean
        +disable()
    }
    class Role {
        -String name
    }
    class Brand {
        -String name
    }
    class Category {
        -String name
        -Category parent
    }
    class Product {
        -Long id
        -String name
        -Set~ProductIdentifier~ identifiers
        -List~ProductAttribute~ attributes
        -List~String~ imageUrls
        +isEquivalentTo(Product p) boolean
        +bestOffer() Optional~Offer~
    }
    class ProductIdentifier {
        -IdType type
        -String value
    }
    class Store {
        -String name
        -DataKind dataKind
        -ShippingPolicy shippingPolicy
        -TaxPolicy taxPolicy
        +shippingFor(Product p) Optional~Money~
    }
    class StoreProduct {
        -String externalId
        -String url
        -int matchLevel
        -double matchConfidence
        -boolean active
        +linkTo(Product p, int level)
        +currentOffer() Offer
    }
    class Offer {
        -Money price
        -Money shipping
        -Money tax
        -boolean shippingKnown
        -boolean current
        -Instant checkedAt
        +finalPrice() Money
        +isStale(Duration ttl) boolean
        +supersededBy(Offer o)
    }
    class PriceHistory {
        -Money price
        -Instant recordedAt
    }
    class Favorite
    class PriceAlert {
        -Money target
        -boolean notified
        +shouldTrigger(Offer o) boolean
        +markNotified()
    }

    User "1" --> "*" Role
    User "1" --> "*" Favorite
    User "1" --> "*" PriceAlert
    Favorite --> Product
    PriceAlert --> Product
    Brand "1" --> "*" Product
    Category "1" --> "*" Product
    Product "1" --> "*" ProductIdentifier
    Product "1" --> "*" StoreProduct
    Store "1" --> "*" StoreProduct
    StoreProduct "1" --> "*" Offer
    Offer "1" --> "*" PriceHistory
    Offer *-- Money
    PriceHistory *-- Money
```

## 4. IA y armador de PC

```mermaid
classDiagram
    direction TB
    class AiProvider {
        <<interface>>
        +id() String
        +chat(AiRequest r) AiResponse
        +stream(AiRequest r) Flux~String~
        +embed(String text) float[]
        +supportsVision() boolean
        +limits() ProviderLimits
    }
    class GeminiProvider
    class ClaudeProvider
    class OpenAiCompatibleProvider
    class MockProvider
    class AiOrchestrator {
        -AiProvider provider
        -AiProvider fallback
        -ToolRegistry tools
        -OutputValidator validator
        -int maxToolCalls
        +chat(ChatRequest r) ChatResponse
    }
    class AiGuardrails {
        +screen(String input) GuardResult
        +redact(String text) String
    }
    class OutputValidator {
        +validate(String answer, List~ToolResult~ evidence) ValidationResult
    }
    class ToolRegistry {
        -Map~String,AiTool~ tools
        +get(String name) AiTool
    }
    class AiTool {
        <<interface>>
        +name() String
        +execute(JsonNode args) ToolResult
    }
    class SearchProductsTool
    class CompareOffersTool
    class PriceHistoryTool
    class BuyOrWaitTool
    class ValidateCompatibilityTool
    class CalculateTotalTool
    class RagService {
        +retrieve(String query, int k) List~Chunk~
    }
    class PcBuildService {
        -List~CompatibilityRule~ rules
        +validate(PcBuild b) List~RuleResult~
        +repair(PcBuild b) PcBuild
    }
    class CompatibilityRule {
        <<interface>>
        +code() String
        +severity() Severity
        +check(PcBuild b) RuleResult
    }
    class SocketRule
    class MemoryTypeRule
    class FormFactorRule
    class GpuLengthRule
    class CoolerHeightRule
    class PowerRule
    class BuyOrWaitAdvisor {
        +advise(Product p) Advice
    }

    AiProvider <|.. GeminiProvider
    AiProvider <|.. ClaudeProvider
    AiProvider <|.. OpenAiCompatibleProvider
    AiProvider <|.. MockProvider
    AiOrchestrator --> AiProvider
    AiOrchestrator --> AiGuardrails
    AiOrchestrator --> ToolRegistry
    AiOrchestrator --> OutputValidator
    AiOrchestrator --> RagService
    ToolRegistry o-- AiTool
    AiTool <|.. SearchProductsTool
    AiTool <|.. CompareOffersTool
    AiTool <|.. PriceHistoryTool
    AiTool <|.. BuyOrWaitTool
    AiTool <|.. ValidateCompatibilityTool
    AiTool <|.. CalculateTotalTool
    ValidateCompatibilityTool --> PcBuildService
    BuyOrWaitTool --> BuyOrWaitAdvisor
    PcBuildService o-- CompatibilityRule
    CompatibilityRule <|.. SocketRule
    CompatibilityRule <|.. MemoryTypeRule
    CompatibilityRule <|.. FormFactorRule
    CompatibilityRule <|.. GpuLengthRule
    CompatibilityRule <|.. CoolerHeightRule
    CompatibilityRule <|.. PowerRule
```

Las 10 herramientas de la v2.0 (`buscar_productos`, `obtener_producto`, `comparar_ofertas`, `historial_precios`, `prediccion_compra`, `listar_tiendas`, `buscar_componentes`, `validar_compatibilidad`, `calcular_total`, `consultar_ayuda`) implementan `AiTool`; el diagrama muestra seis. Las 10 reglas de compatibilidad implementan `CompatibilityRule`; el diagrama muestra seis (faltan conectores, almacenamiento, chipset/BIOS y equilibrio de rendimiento). Todas son de solo lectura.

## Trazabilidad con los 4 pilares POO

| Pilar | Dónde se demuestra |
|---|---|
| **Abstracción** | `StoreConnector`, `AiProvider`, `AiTool`, `CompatibilityRule`, `MatchStrategy`, `SelectionPolicy`: contratos que ocultan cómo se obtiene o valida cada dato. |
| **Encapsulamiento** | `Money` inmutable, `Offer.finalPrice()` y `Offer.isStale()`, `ConnectorState.recordFailure()`, `PriceAlert.markNotified()`: atributos privados y comportamiento en la clase. |
| **Herencia** | `AbstractStoreConnector` → `ApiStoreConnector` → `EbayConnector` / `MercadoLibreConnector`; `ScrapingStoreConnector` → `ShopScraperConnector`. |
| **Polimorfismo** | `ConnectorSelector` y `ScrapeOrchestrator` tratan cualquier `StoreConnector` igual; `AiOrchestrator` usa cualquier `AiProvider` (Gemini, Claude, Mock); `ProductMatcher` recorre `MatchStrategy`; `PcBuildService` aplica todas las `CompatibilityRule`. |

## Trazabilidad con los requisitos

| Requisito | Tablas | Clases |
|---|---|---|
| RF-06 recopilación automática | `store_connectors`, `connector_executions`, `raw_offers`, `offers`, `data_quality_issues` | `ScrapeRunner`, `ScrapeOrchestrator`, `StoreConnector` |
| RF-13 gestión de tiendas y conectores | `stores`, `store_connectors`, `connector_logs` | `ConnectorSelector`, `ConnectorState` |
| RF-19 asistente de IA | `ai_conversations`, `ai_messages`, `ai_tool_calls`, `ai_validation_results` | `AiOrchestrator`, `OutputValidator`, `AiTool` |
| RF-20 armador de PC | `component_specs`, `component_compat_rules`, `pc_builds`, `pc_build_items`, `pc_build_validations` | `PcBuildService`, `CompatibilityRule` |
| RF-21 búsqueda por imagen | `image_queries`, `product_embeddings` | `AiProvider.supportsVision`, `EmbeddingMatch` |
| RF-22 comprar o esperar | `price_history`, `benchmark_scores` | `BuyOrWaitAdvisor`, `BuyOrWaitTool` |
| RF-24 cuotas y costos | `ai_usage`, `ai_messages` | `AiGuardrails`, `ProviderLimits` |
| RF-25 configuraciones y alertas masivas | `pc_builds`, `price_alerts.pc_build_id` | `PriceAlert`, `PcBuildService` |
| RF-26 proveedor intercambiable | `system_settings` | `AiProvider`, `AiOrchestrator` |
