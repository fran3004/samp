# SAMP — ER de base de datos (52 tablas)

Fuente: `docs/referencias/SAMP-UML-Base-de-Datos.svg` (46 tablas, v2.0) + 6 tablas nuevas y columnas añadidas (ver `docs/fuente-de-verdad.md`).
Motor: PostgreSQL (Supabase) con `pgvector`. Moneda: `numeric(14,2)`. Vectores: `vector(768)`.
Convención: `PK` clave primaria, `FK` clave foránea, `UK` única. **NUEVO** marca lo añadido sobre la v2.0.

## 0. Vista general (solo entidades y relaciones)

```mermaid
erDiagram
    users ||--o{ user_roles : tiene
    roles ||--o{ user_roles : asigna
    users ||--o| user_preferences : configura
    users ||--o{ sessions : abre
    countries ||--o{ users : reside
    languages ||--o{ users : habla
    currencies ||--o{ user_preferences : prefiere

    brands ||--o{ products : fabrica
    categories ||--o{ products : clasifica
    categories ||--o{ categories : padre
    products ||--o{ product_attributes : tiene
    products ||--o{ product_identifiers : identifica
    products ||--o{ product_images : muestra

    countries ||--o{ stores : ubica
    currencies ||--o{ stores : moneda_defecto
    currencies ||--o{ countries : moneda_defecto
    countries ||--o{ shipping_options : destino
    stores ||--o{ store_connectors : usa
    store_connectors ||--o{ connector_executions : ejecuta
    connector_executions ||--o{ connector_logs : registra
    searches |o--o{ connector_executions : dispara

    stores ||--o{ store_products : publica
    products |o--o{ store_products : equivale
    products |o--o{ store_products : similar_a
    store_products ||--o{ offers : ofrece
    connector_executions ||--o{ offers : origina
    currencies ||--o{ offers : moneda
    exchange_rates ||--o{ offers : tasa_usada
    offers ||--o{ price_history : historiza
    store_products ||--o{ price_history : ancla
    exchange_rates ||--o{ price_history : tasa_usada
    stores ||--o{ shipping_options : envia
    currencies ||--o{ shipping_options : moneda

    connector_executions ||--o{ raw_offers : captura
    store_products |o--o{ raw_offers : origen
    store_products ||--o{ store_product_attributes : describe
    store_products ||--o{ store_product_images : ilustra
    connector_executions ||--o{ data_quality_issues : detecta
    store_products |o--o{ data_quality_issues : afecta

    users |o--o{ searches : realiza
    searches ||--o{ search_filters : filtra
    searches ||--o{ search_results : devuelve
    store_products ||--o{ search_results : aparece

    users ||--o{ favorites : marca
    products ||--o{ favorites : es_favorito
    users ||--o{ price_alerts : crea
    products ||--o{ price_alerts : vigila
    pc_builds |o--o{ price_alerts : vigila_build
    users ||--o{ notifications : recibe
    price_alerts |o--o{ notifications : genera

    currencies ||--o{ exchange_rates : base
    currencies ||--o{ exchange_rates : cotizada
    countries ||--o| country_settings : configura
    languages ||--o{ country_settings : idioma
    currencies ||--o{ country_settings : moneda

    users |o--o{ audit_logs : actua
    users ||--o{ system_settings : actualiza

    users |o--o{ ai_conversations : conversa
    ai_conversations ||--o{ ai_messages : contiene
    ai_messages ||--o{ ai_tool_calls : invoca
    ai_messages ||--o| ai_validation_results : valida
    users |o--o{ ai_usage : consume
    languages ||--o{ knowledge_documents : idioma
    knowledge_documents ||--o{ knowledge_chunks : divide
    products ||--o{ product_embeddings : vectoriza
    users |o--o{ image_queries : consulta
    searches |o--o{ image_queries : origina
    ai_conversations |o--o{ image_queries : en
    products ||--o| component_specs : especifica
    products ||--o{ benchmark_scores : mide
    users ||--o{ pc_builds : arma
    ai_conversations |o--o{ pc_builds : asesora
    pc_builds ||--o{ pc_build_items : incluye
    products ||--o{ pc_build_items : pieza
    offers |o--o{ pc_build_items : precio_guardado
    pc_builds ||--o{ pc_build_validations : valida
    component_compat_rules ||--o{ pc_build_validations : aplica

    system_metrics
```

## A. Usuarios y seguridad (5)

```mermaid
erDiagram
    users {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar display_name
        char country_code FK
        varchar locale FK
        boolean enabled
        timestamp deleted_at
    }
    roles {
        int id PK
        varchar name UK "VISITOR, REGISTERED, ADMIN"
    }
    user_roles {
        bigint user_id PK, FK
        int role_id PK, FK
    }
    user_preferences {
        bigint user_id PK, FK
        char currency_code FK
        varchar locale
    }
    sessions {
        uuid id PK
        bigint user_id FK "nullable"
        timestamp created_at
        timestamp expires_at
    }
    users ||--o{ user_roles : tiene
    roles ||--o{ user_roles : asigna
    users ||--o| user_preferences : configura
    users ||--o{ sessions : abre
```

## B. Catálogo (6)

```mermaid
erDiagram
    brands {
        bigint id PK
        varchar name UK
    }
    categories {
        bigint id PK
        bigint parent_id FK "autorreferencia"
        varchar name
    }
    products {
        bigint id PK
        bigint brand_id FK "nullable"
        bigint category_id FK
        varchar name
        varchar model
    }
    product_attributes {
        bigint id PK
        bigint product_id FK
        varchar name "UK con product_id"
        varchar value
    }
    product_identifiers {
        bigint id PK
        bigint product_id FK
        varchar type "GTIN, EAN, UPC, ISBN, MPN"
        varchar value "UK con type"
    }
    product_images {
        bigint id PK
        bigint product_id FK
        varchar url
        int position
    }
    brands ||--o{ products : fabrica
    categories ||--o{ products : clasifica
    categories ||--o{ categories : padre
    products ||--o{ product_attributes : tiene
    products ||--o{ product_identifiers : identifica
    products ||--o{ product_images : muestra
```

## C. Tiendas y conectores (5) — con selección automática y respaldo

```mermaid
erDiagram
    countries {
        char code PK "ISO 3166"
        varchar name
        char default_currency FK
    }
    stores {
        bigint id PK
        char country_code FK
        varchar name
        varchar base_url
        char default_currency FK
        numeric reliability_score
        boolean active
        varchar data_kind "NUEVO REAL o DEMO"
        varchar shipping_policy_type "NUEVO"
        numeric flat_shipping_amount "NUEVO"
        boolean tax_included "NUEVO"
        numeric tax_rate "NUEVO"
    }
    store_connectors {
        bigint id PK
        bigint store_id FK
        varchar connector_type "clase concreta"
        smallint phase
        boolean enabled
        int rate_limit_per_min
        varchar credentials_ref
        varchar kind "NUEVO API, SCRAPER, FEED"
        int priority "NUEVO menor = primero"
        numeric success_rate "NUEVO 0 a 1"
        int consecutive_failures "NUEVO"
        timestamp last_success_at "NUEVO"
        varchar circuit_state "NUEVO CLOSED, OPEN, HALF_OPEN"
        jsonb config_json "NUEVO selectores o endpoints"
        varchar evidence_url "NUEVO permiso de uso"
        timestamp robots_checked_at "NUEVO"
        int ttl_minutes "NUEVO frescura"
        int daily_call_budget "NUEVO"
    }
    connector_executions {
        bigint id PK
        bigint connector_id FK
        bigint search_id FK "NUEVO nullable"
        timestamp started_at
        timestamp finished_at
        varchar status
        int items_fetched
        int error_count
        varchar trigger "NUEVO SCHEDULED, MANUAL, LIVE"
        varchar runner "NUEVO ACTIONS, RENDER, LOCAL"
        int items_created "NUEVO"
        int items_updated "NUEVO"
        int items_skipped "NUEVO"
        numeric completeness_pct "NUEVO"
        text error_message "NUEVO"
    }
    connector_logs {
        bigint id PK
        bigint execution_id FK
        varchar level
        text message
        timestamp logged_at
    }
    countries ||--o{ stores : ubica
    currencies ||--o{ countries : moneda_defecto
    stores ||--o{ store_connectors : usa
    store_connectors ||--o{ connector_executions : ejecuta
    connector_executions ||--o{ connector_logs : registra
```

## D. Ofertas, precios e ingesta (4 + 4 nuevas)

```mermaid
erDiagram
    store_products {
        bigint id PK
        bigint store_id FK
        bigint product_id FK "nullable hasta emparejar"
        bigint similar_to_product_id FK "NUEVO"
        varchar external_id "UK con store_id"
        varchar title
        varchar url
        smallint match_level
        numeric match_confidence
        varchar match_method "NUEVO"
        varchar brand_raw "NUEVO"
        varchar model_raw "NUEVO"
        varchar gtin_raw "NUEVO"
        timestamp first_seen_at "NUEVO"
        timestamp last_seen_at "NUEVO"
        boolean active "NUEVO"
    }
    offers {
        bigint id PK
        bigint store_product_id FK
        bigint execution_id FK "NUEVO"
        numeric price_amount
        char currency_code FK
        numeric shipping_amount "nullable"
        boolean shipping_known
        numeric tax_amount
        boolean tax_known "NUEVO"
        numeric final_price_ref
        bigint exchange_rate_id FK
        varchar availability
        varchar source_url "NUEVO"
        timestamp checked_at
        boolean is_current "UK parcial"
    }
    price_history {
        bigint id PK
        bigint offer_id FK
        bigint store_product_id FK "NUEVO"
        numeric price
        char currency
        numeric shipping
        numeric final_price_ref "NUEVO"
        bigint exchange_rate_id FK "NUEVO"
        varchar availability "NUEVO"
        timestamp recorded_at
    }
    shipping_options {
        bigint id PK
        bigint store_id FK
        char destination_country FK
        numeric cost "nullable"
        char currency FK
        int min_days
        int max_days
    }
    raw_offers {
        bigint id PK
        bigint execution_id FK
        bigint store_product_id FK "nullable"
        jsonb payload_json
        varchar parse_status "OK, PARTIAL, FAILED"
        timestamp fetched_at
    }
    store_product_attributes {
        bigint id PK
        bigint store_product_id FK
        varchar name
        varchar value
    }
    store_product_images {
        bigint id PK
        bigint store_product_id FK
        varchar url "solo URL"
        int position
        varchar source
        timestamp checked_at
    }
    data_quality_issues {
        bigint id PK
        bigint execution_id FK
        bigint store_product_id FK "nullable"
        varchar field
        varchar issue_type "MISSING, INVALID, OUTLIER"
        boolean resolved
    }
    store_products ||--o{ offers : ofrece
    offers ||--o{ price_history : historiza
    store_products ||--o{ price_history : ancla
    store_products ||--o{ store_product_attributes : describe
    store_products ||--o{ store_product_images : ilustra
    store_products |o--o{ raw_offers : origen
    store_products |o--o{ data_quality_issues : afecta
    stores ||--o{ shipping_options : envia
    countries ||--o{ shipping_options : destino
```

## E. Búsquedas (3)

```mermaid
erDiagram
    searches {
        bigint id PK
        bigint user_id FK "nullable"
        varchar query_text
        varchar search_type "TEXT, BARCODE, IMAGE"
        timestamp created_at
    }
    search_filters {
        bigint id PK
        bigint search_id FK
        varchar name
        varchar value
    }
    search_results {
        bigint search_id PK, FK
        bigint store_product_id PK, FK
        int rank
        numeric score
    }
    searches ||--o{ search_filters : filtra
    searches ||--o{ search_results : devuelve
```

## F. Seguimiento (3)

```mermaid
erDiagram
    favorites {
        bigint user_id PK, FK
        bigint product_id PK, FK
        timestamp created_at
    }
    price_alerts {
        bigint id PK
        bigint user_id FK
        bigint product_id FK "nullable"
        bigint pc_build_id FK "NUEVO nullable"
        numeric target_price
        char currency
        boolean notified
    }
    notifications {
        bigint id PK
        bigint user_id FK
        bigint alert_id FK "nullable"
        varchar message
        boolean read
        timestamp created_at
    }
    price_alerts ||--o{ notifications : genera
```

## G. Internacionalización (4)

```mermaid
erDiagram
    currencies {
        char code PK
        varchar name
        smallint decimals
    }
    exchange_rates {
        bigint id PK
        char base_currency FK
        char quote_currency FK
        numeric rate
        varchar source
        timestamp fetched_at
    }
    languages {
        varchar code PK
        varchar name
    }
    country_settings {
        char country_code PK, FK
        char default_currency FK
        varchar default_language FK
        numeric import_tax_pct
        varchar timezone
    }
    currencies ||--o{ exchange_rates : base
    currencies ||--o{ exchange_rates : cotizada
    currencies ||--o{ country_settings : moneda
    languages ||--o{ country_settings : idioma
```

## H. Administración (3)

```mermaid
erDiagram
    audit_logs {
        bigint id PK
        bigint user_id FK "nullable"
        varchar action
        jsonb detail_json
        timestamp created_at
    }
    system_settings {
        varchar key PK
        text value "incluye samp.ai.provider"
        bigint updated_by FK
        timestamp updated_at
    }
    system_metrics {
        bigint id PK
        varchar name
        numeric value
        timestamp recorded_at
    }
```

## I. IA y armador de PC (13 + 2 nuevas)

```mermaid
erDiagram
    ai_conversations {
        uuid id PK
        bigint user_id FK "nullable"
        varchar session_key
        varchar provider
        varchar prompt_version
        boolean consent_given "NUEVO"
        timestamp deleted_at "NUEVO"
        timestamp expires_at
    }
    ai_messages {
        bigint id PK
        uuid conversation_id FK
        varchar role "USER, ASSISTANT, TOOL"
        text content
        int tokens
        int latency_ms
        numeric cost
    }
    ai_tool_calls {
        bigint id PK
        bigint message_id FK
        varchar tool_name
        jsonb arguments_json
        boolean success
    }
    ai_validation_results {
        bigint id PK
        bigint message_id FK "NUEVO"
        boolean passed
        jsonb issues_json
        int regenerations
    }
    ai_usage {
        bigint id PK
        bigint user_id FK "nullable"
        varchar session_key
        date usage_date
        int requests
        int tokens
        int image_requests
    }
    knowledge_documents {
        bigint id PK
        varchar title
        char language_code FK
    }
    knowledge_chunks {
        bigint id PK
        bigint document_id FK
        varchar model_id
        text content
        vector embedding "vector(768)"
    }
    product_embeddings {
        bigint id PK
        bigint product_id FK "UK con model_id"
        varchar model_id
        varchar content_hash
        vector embedding "vector(768)"
    }
    image_queries {
        bigint id PK
        bigint user_id FK "nullable"
        bigint search_id FK
        uuid conversation_id FK "NUEVO"
        varchar image_hash
        varchar barcode
        varchar provider "NUEVO"
        numeric confidence "NUEVO"
        jsonb result_json
        timestamp expires_at
    }
    component_specs {
        bigint product_id PK, FK
        varchar socket
        varchar memory_type
        int tdp
        int length_mm
        int height_mm
        varchar form_factor
        jsonb extra_json
    }
    component_compat_rules {
        int id PK
        varchar code UK
        varchar severity "ERROR, WARNING"
        text description
    }
    benchmark_scores {
        bigint id PK
        bigint product_id FK
        varchar benchmark
        numeric score
    }
    pc_builds {
        bigint id PK
        bigint user_id FK
        uuid conversation_id FK "NUEVO nullable"
        numeric budget
        char currency
        varchar purpose
        varchar status "NUEVO"
        numeric total_final_ref "NUEVO"
    }
    pc_build_items {
        bigint id PK
        bigint build_id FK
        bigint product_id FK
        bigint offer_id FK "nullable"
        numeric price_at_save
        int quantity
    }
    pc_build_validations {
        bigint id PK
        bigint build_id FK "NUEVO"
        int rule_id FK
        varchar severity
        boolean passed
        text message
    }
    ai_conversations ||--o{ ai_messages : contiene
    ai_messages ||--o{ ai_tool_calls : invoca
    ai_messages ||--o| ai_validation_results : valida
    knowledge_documents ||--o{ knowledge_chunks : divide
    ai_conversations |o--o{ image_queries : en
    ai_conversations |o--o{ pc_builds : asesora
    pc_builds ||--o{ pc_build_items : incluye
    pc_builds ||--o{ pc_build_validations : valida
    component_compat_rules ||--o{ pc_build_validations : aplica
    languages ||--o{ knowledge_documents : idioma
```

## Notas de diseño
- **`offers` es append-only con `is_current`**: una fila vigente por `store_product` (índice único parcial); las anteriores quedan como historial. `price_history` guarda además un registro compacto con tasa y disponibilidad.
- **Imágenes**: solo URLs (`product_images` para el producto canónico, `store_product_images` por publicación).
- **Selección de conector**: el orquestador ordena `store_connectors` por `priority` y `success_rate`, salta los de `circuit_state = OPEN` y pasa al siguiente si falla.
- **Frescura**: una oferta con `checked_at` anterior a `ttl_minutes` del conector se trata como desactualizada.
- **Vectores**: `vector(768)` con un único modelo de embeddings por índice; cambiar de modelo exige reindexar (`content_hash`).
