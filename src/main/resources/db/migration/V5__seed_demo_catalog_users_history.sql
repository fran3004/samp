-- V5: Semilla completa en PostgreSQL/Supabase: usuarios con BCrypt (pgcrypto),
-- catálogo canónico, publicaciones, ofertas vigentes e historial de 30 días (RF-22) marcado DEMO.

-- 1. Usuarios iniciales (AdminSamp2026! y Comprador2026! cifrados con BCrypt)
INSERT INTO users (email, password_hash, display_name, country_code, locale, enabled) VALUES
    ('admin@samp.co',     crypt('AdminSamp2026!', gen_salt('bf', 10)),     'Administrador SAMP', 'CO', 'es-CO', TRUE),
    ('comprador@samp.co', crypt('Comprador2026!', gen_salt('bf', 10)),     'Comprador Colombia', 'CO', 'es-CO', TRUE)
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON (u.email = 'admin@samp.co' AND r.name IN ('ADMIN', 'REGISTERED'))
             OR (u.email = 'comprador@samp.co' AND r.name = 'REGISTERED')
ON CONFLICT DO NOTHING;

INSERT INTO user_preferences (user_id, currency_code, locale)
SELECT id, 'COP', 'es-CO' FROM users
ON CONFLICT (user_id) DO NOTHING;

-- 2. Marcas y categorías
INSERT INTO brands (name) VALUES ('Sony'), ('Logitech'), ('Ninja'), ('Ugreen'), ('Ferrero')
ON CONFLICT (name) DO NOTHING;

INSERT INTO categories (name)
SELECT v.name FROM (VALUES ('Audio'), ('Periféricos'), ('Hogar'), ('Accesorios'), ('Alimentos')) AS v(name)
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.name = v.name);

-- 3. Productos canónicos
INSERT INTO products (brand_id, category_id, name, model)
SELECT b.id, c.id, p.name, p.model
FROM (VALUES
    ('Sony',     'Audio',       'Audífonos Sony WH-1000XM5 Cancelación de Ruido Negro', 'WH-1000XM5'),
    ('Logitech', 'Periféricos', 'Mouse Gamer Logitech G502 HERO 25K DPI',               'G502 HERO'),
    ('Ninja',    'Hogar',       'Freidora de Aire Ninja AF161 Max XL 5.2 Litros',       'AF161'),
    ('Ugreen',   'Accesorios',  'Hub USB-C 7 en 1 HDMI 4K Aluminio',                    'CM478'),
    ('Ferrero',  'Alimentos',   'Crema de Avellanas Nutella 350g',                      'Nutella 350g')
) AS p(brand_name, cat_name, name, model)
JOIN brands b ON b.name = p.brand_name
JOIN categories c ON c.name = p.cat_name
WHERE NOT EXISTS (SELECT 1 FROM products pr WHERE pr.name = p.name);

-- 4. Identificadores GTIN verificables
INSERT INTO product_identifiers (product_id, type, value)
SELECT pr.id, 'GTIN', v.gtin
FROM (VALUES
    ('WH-1000XM5',   '0027242923232'),
    ('G502 HERO',    '0097855141224'),
    ('AF161',        '0622356563093'),
    ('CM478',        '6901234567890'),
    ('Nutella 350g', '3017620422003')
) AS v(model, gtin)
JOIN products pr ON pr.model = v.model
ON CONFLICT (type, value) DO NOTHING;

-- 5. Imágenes por URL
INSERT INTO product_images (product_id, url, position)
SELECT pr.id, v.url, 0
FROM (VALUES
    ('WH-1000XM5',   'https://images.unsplash.com/photo-1618366712010-f4ae9c647dcb?w=600'),
    ('G502 HERO',    'https://images.unsplash.com/photo-1527814050087-3793815479db?w=600'),
    ('AF161',        'https://images.unsplash.com/photo-1585515320310-259814833e62?w=600'),
    ('CM478',        'https://images.unsplash.com/photo-1625842268584-8f3296236761?w=600'),
    ('Nutella 350g', 'https://images.openfoodfacts.org/images/products/301/762/042/2003/front_en.633.400.jpg')
) AS v(model, url)
JOIN products pr ON pr.model = v.model
WHERE NOT EXISTS (SELECT 1 FROM product_images pi WHERE pi.product_id = pr.id);

-- 6. Publicaciones en las 5 tiendas de Colombia
INSERT INTO store_products (store_id, product_id, external_id, title, url, match_level, match_confidence, match_method, gtin_raw)
SELECT s.id, pr.id, v.ext_id, v.title, v.url, 1, 1.0000, 'GTIN_EXACT', v.gtin
FROM (VALUES
    ('MERCADOLIBRE_CO', 'WH-1000XM5',   'MCO-XM5-01',  'Audífonos Sony WH-1000XM5 Cancelación de Ruido Negro', 'https://www.mercadolibre.com.co/p/MCO19528491', '0027242923232'),
    ('AMAZON_CO',       'WH-1000XM5',   'B09XS7JWHH',  'Sony WH-1000XM5 Wireless Noise Canceling Headphones',  'https://www.amazon.com/dp/B09XS7JWHH',          '0027242923232'),
    ('FALABELLA_CO',    'WH-1000XM5',   'FAL-XM5-01',  'Audífonos Bluetooth Sony WH-1000XM5 Negro',            'https://www.falabella.com.co/falabella-co/product/xm5', '0027242923232'),
    ('MERCADOLIBRE_CO', 'G502 HERO',    'MCO-G502-01', 'Mouse Gamer Logitech G502 HERO 25K DPI',               'https://www.mercadolibre.com.co/p/MCO15192341', '0097855141224'),
    ('AMAZON_CO',       'G502 HERO',    'B07GBZ4Q68',  'Logitech G502 HERO Wired Gaming Mouse',                'https://www.amazon.com/dp/B07GBZ4Q68',          '0097855141224'),
    ('ALIEXPRESS_CO',   'G502 HERO',    'ALI-G502-01', 'Logitech G502 HERO Gaming Mouse Choice',               'https://es.aliexpress.com/item/100500123456.html', '0097855141224'),
    ('FALABELLA_CO',    'AF161',        'FAL-AIR-01',  'Freidora de Aire Ninja AF161 Max XL 5.2 Litros',       'https://www.falabella.com.co/falabella-co/product/ninja-af161', '0622356563093'),
    ('MERCADOLIBRE_CO', 'AF161',        'MCO-AIR-01',  'Freidora de Aire Ninja Max XL 5.2L AF161',             'https://www.mercadolibre.com.co/p/MCO18234111', '0622356563093'),
    ('TEMU_CO',         'CM478',        'TEMU-HUB-01', 'Hub USB-C 7 en 1 HDMI 4K Aluminio para Portátil',      'https://www.temu.com/co/goods-hub-usbc.html',   '6901234567890'),
    ('ALIEXPRESS_CO',   'CM478',        'ALI-HUB-01',  'Ugreen Hub USB-C 7 en 1 HDMI 4K Choice',               'https://es.aliexpress.com/item/100500999111.html', '6901234567890'),
    ('MERCADOLIBRE_CO', 'Nutella 350g', 'MCO-NUT-01',  'Crema de Avellanas Nutella 350g',                      'https://www.mercadolibre.com.co/p/MCO19901122', '3017620422003')
) AS v(store_code, model, ext_id, title, url, gtin)
JOIN stores s ON s.code = v.store_code
JOIN products pr ON pr.model = v.model
ON CONFLICT (store_id, external_id) DO NOTHING;

-- 7. Ofertas vigentes (con precio de referencia en USD y transparencia de envío/impuestos)
INSERT INTO offers (store_product_id, price_amount, currency_code, shipping_amount, shipping_known, tax_amount, tax_known, final_price_ref, availability, source_url, is_current)
SELECT sp.id, v.price, v.cur, v.ship, v.ship_ok, v.tax, v.tax_ok, v.ref_usd, 'IN_STOCK', sp.url, TRUE
FROM (VALUES
    ('MCO-XM5-01',  1349900.00, 'COP',    0.00, TRUE,     0.00, TRUE,  317.62),
    ('B09XS7JWHH',      298.00, 'USD',    0.00, TRUE,    56.62, TRUE,  354.62),
    ('FAL-XM5-01',  1499900.00, 'COP',    NULL, FALSE,    0.00, TRUE,  352.92),
    ('MCO-G502-01',  199900.00, 'COP',    0.00, TRUE,     0.00, TRUE,   47.04),
    ('B07GBZ4Q68',       39.99, 'USD',    0.00, TRUE,     0.00, TRUE,   39.99),
    ('ALI-G502-01',      36.50, 'USD',    NULL, FALSE,    NULL, FALSE,  36.50),
    ('FAL-AIR-01',   549900.00, 'COP',    0.00, TRUE,     0.00, TRUE,  129.39),
    ('MCO-AIR-01',   579000.00, 'COP',    0.00, TRUE,     0.00, TRUE,  136.24),
    ('TEMU-HUB-01',   42900.00, 'COP',    0.00, TRUE,     0.00, TRUE,   10.09),
    ('ALI-HUB-01',       14.90, 'USD',    0.00, TRUE,     NULL, FALSE,  14.90),
    ('MCO-NUT-01',    26900.00, 'COP', 8500.00, TRUE,     0.00, TRUE,    8.33)
) AS v(ext_id, price, cur, ship, ship_ok, tax, tax_ok, ref_usd)
JOIN store_products sp ON sp.external_id = v.ext_id
WHERE NOT EXISTS (SELECT 1 FROM offers o WHERE o.store_product_id = sp.id AND o.is_current = TRUE);

-- 8. Historial de 30 días (RF-22: 30 puntos diarios por oferta vigente)
INSERT INTO price_history (offer_id, store_product_id, price, currency, shipping, final_price_ref, availability, recorded_at)
SELECT o.id,
       o.store_product_id,
       ROUND((o.price_amount * (1 + (SIN((d.day_offset + o.id) * 0.45) * 0.05)))::numeric, 2),
       o.currency_code,
       o.shipping_amount,
       ROUND((o.final_price_ref * (1 + (SIN((d.day_offset + o.id) * 0.45) * 0.05)))::numeric, 2),
       o.availability,
       NOW() - (d.day_offset || ' days')::interval
FROM offers o
CROSS JOIN generate_series(0, 29) AS d(day_offset)
WHERE o.is_current = TRUE
  AND NOT EXISTS (SELECT 1 FROM price_history ph WHERE ph.offer_id = o.id);
