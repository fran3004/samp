-- V4: Datos base de Colombia, roles, las 5 tiendas objetivo, conectores, reglas de compatibilidad y configuración inicial

-- Monedas
INSERT INTO currencies (code, name, decimals) VALUES
    ('USD', 'Dólar estadounidense', 2),
    ('COP', 'Peso colombiano', 0),
    ('EUR', 'Euro', 2)
ON CONFLICT (code) DO NOTHING;

-- Idiomas
INSERT INTO languages (code, name) VALUES
    ('es-CO', 'Español (Colombia)'),
    ('en-US', 'English (US)')
ON CONFLICT (code) DO NOTHING;

-- Países
INSERT INTO countries (code, name, default_currency) VALUES
    ('CO', 'Colombia', 'COP'),
    ('US', 'Estados Unidos', 'USD'),
    ('CN', 'China', 'USD')
ON CONFLICT (code) DO NOTHING;

-- Configuración por país
INSERT INTO country_settings (country_code, default_currency, default_language, import_tax_pct, timezone) VALUES
    ('CO', 'COP', 'es-CO', 19.00, 'America/Bogota'),
    ('US', 'USD', 'en-US', 0.00, 'America/New_York')
ON CONFLICT (country_code) DO NOTHING;

-- Tasas de cambio iniciales fechadas
INSERT INTO exchange_rates (base_currency, quote_currency, rate, source, fetched_at) VALUES
    ('USD', 'COP', 4250.000000, 'TRM_REFERENCIA_INICIAL', NOW()),
    ('COP', 'USD', 0.000235, 'TRM_REFERENCIA_INICIAL', NOW()),
    ('EUR', 'USD', 1.080000, 'ECB_REFERENCIA_INICIAL', NOW()),
    ('EUR', 'COP', 4590.000000, 'TRM_REFERENCIA_INICIAL', NOW());

-- Roles
INSERT INTO roles (name) VALUES
    ('VISITOR'),
    ('REGISTERED'),
    ('ADMIN')
ON CONFLICT (name) DO NOTHING;

-- Las 5 tiendas objetivo para Colombia (respaldo DEMO listo; scraping permitido se intenta primero)
INSERT INTO stores (code, country_code, name, base_url, default_currency, reliability_score, active, data_kind, shipping_policy_type, flat_shipping_amount, tax_included, tax_rate) VALUES
    ('MERCADOLIBRE_CO', 'CO', 'Mercado Libre Colombia', 'https://www.mercadolibre.com.co', 'COP', 0.95, TRUE, 'DEMO', 'CALCULATED', NULL, TRUE, 19.00),
    ('AMAZON_CO',       'US', 'Amazon (Envío a Colombia)', 'https://www.amazon.com', 'USD', 0.96, TRUE, 'DEMO', 'FREE_OVER_THRESHOLD', NULL, FALSE, 19.00),
    ('TEMU_CO',         'CN', 'Temu Colombia', 'https://www.temu.com/co', 'COP', 0.82, TRUE, 'DEMO', 'FREE_STANDARD', 0.00, TRUE, 0.00),
    ('FALABELLA_CO',    'CO', 'Falabella Colombia', 'https://www.falabella.com.co', 'COP', 0.93, TRUE, 'DEMO', 'STORE_PICKUP_OR_PAID', NULL, TRUE, 19.00),
    ('ALIEXPRESS_CO',   'CN', 'AliExpress Choice', 'https://es.aliexpress.com', 'USD', 0.85, TRUE, 'DEMO', 'CHOICE_OR_CALCULATED', NULL, FALSE, 19.00)
ON CONFLICT (code) DO NOTHING;

-- Conectores por tienda (prioridad 1 = scraping respetando robots.txt, prioridad 2 = respaldo DEMO)
INSERT INTO store_connectors (store_id, connector_type, phase, enabled, rate_limit_per_min, kind, priority, evidence_url, ttl_minutes, config_json)
SELECT s.id,
       CASE s.code
           WHEN 'MERCADOLIBRE_CO' THEN 'MercadoLibreScraperConnector'
           WHEN 'AMAZON_CO'       THEN 'AmazonScraperConnector'
           WHEN 'TEMU_CO'         THEN 'TemuScraperConnector'
           WHEN 'FALABELLA_CO'    THEN 'FalabellaScraperConnector'
           WHEN 'ALIEXPRESS_CO'   THEN 'AliExpressScraperConnector'
       END,
       1, TRUE, 15, 'SCRAPER', 1, s.base_url || '/robots.txt', 1440,
       jsonb_build_object('searchUrl', s.base_url, 'respectRobots', true)
FROM stores s
WHERE s.code IN ('MERCADOLIBRE_CO', 'AMAZON_CO', 'TEMU_CO', 'FALABELLA_CO', 'ALIEXPRESS_CO');

INSERT INTO store_connectors (store_id, connector_type, phase, enabled, rate_limit_per_min, kind, priority, evidence_url, ttl_minutes, config_json)
SELECT s.id, 'CatalogFallbackConnector', 1, TRUE, 120, 'FALLBACK', 2, 'internal://samp-demo-catalog', 1440,
       jsonb_build_object('mode', 'DEMO_FALLBACK', 'storeCode', s.code)
FROM stores s
WHERE s.code IN ('MERCADOLIBRE_CO', 'AMAZON_CO', 'TEMU_CO', 'FALABELLA_CO', 'ALIEXPRESS_CO');

-- Opciones de envío de referencia hacia Colombia
INSERT INTO shipping_options (store_id, destination_country, cost, currency, min_days, max_days)
SELECT s.id, 'CO',
       CASE s.code
           WHEN 'MERCADOLIBRE_CO' THEN NULL
           WHEN 'AMAZON_CO'       THEN 0.00
           WHEN 'TEMU_CO'         THEN 0.00
           WHEN 'FALABELLA_CO'    THEN 0.00
           WHEN 'ALIEXPRESS_CO'   THEN 0.00
       END,
       s.default_currency,
       CASE s.code
           WHEN 'MERCADOLIBRE_CO' THEN 1
           WHEN 'AMAZON_CO'       THEN 5
           WHEN 'TEMU_CO'         THEN 7
           WHEN 'FALABELLA_CO'    THEN 1
           WHEN 'ALIEXPRESS_CO'   THEN 10
       END,
       CASE s.code
           WHEN 'MERCADOLIBRE_CO' THEN 2
           WHEN 'AMAZON_CO'       THEN 10
           WHEN 'TEMU_CO'         THEN 14
           WHEN 'FALABELLA_CO'    THEN 4
           WHEN 'ALIEXPRESS_CO'   THEN 15
       END
FROM stores s;

-- Configuración del sistema
INSERT INTO system_settings (key, value) VALUES
    ('samp.ai.provider', 'mock'),
    ('samp.ai.model', 'gemini-2.5-flash'),
    ('samp.default.currency', 'COP'),
    ('samp.default.locale', 'es-CO'),
    ('samp.scraper.cron', '0 */6 * * *')
ON CONFLICT (key) DO NOTHING;

-- Reglas de compatibilidad (generales + hardware/PC)
INSERT INTO compat_rules (code, category, severity, description) VALUES
    ('BUDGET_LIMIT',       'GENERAL', 'WARNING', 'El total estimado de la configuración supera el presupuesto indicado.'),
    ('UNKNOWN_SHIPPING',   'GENERAL', 'WARNING', 'Uno o más artículos tienen costo de envío desconocido; el total es incompleto.'),
    ('CPU_MB_SOCKET',      'PC',      'ERROR',   'El socket del procesador debe coincidir con el socket de la placa base.'),
    ('RAM_MB_TYPE',        'PC',      'ERROR',   'El tipo de memoria RAM (DDR4/DDR5) debe ser compatible con la placa base.'),
    ('CASE_MB_FORMFACTOR', 'PC',      'ERROR',   'El factor de forma de la placa base debe caber en el gabinete.'),
    ('GPU_CASE_LENGTH',    'PC',      'ERROR',   'La longitud de la tarjeta gráfica excede el espacio máximo del gabinete.'),
    ('COOLER_CASE_HEIGHT', 'PC',      'ERROR',   'La altura del disipador excede el máximo permitido por el gabinete.'),
    ('PSU_POWER_MARGIN',   'PC',      'WARNING', 'La potencia de la fuente debe ofrecer margen suficiente sobre el TDP total.'),
    ('STORAGE_SLOTS',      'PC',      'WARNING', 'Verificar disponibilidad de ranuras M.2/SATA para las unidades elegidas.'),
    ('BALANCE_PERFORMANCE','PC',      'WARNING', 'Se recomienda equilibrar el rendimiento entre CPU y GPU para evitar cuello de botella.')
ON CONFLICT (code) DO NOTHING;
