> **REEMPLAZADO por `docs/fuente-de-verdad.md` (2026-11-17).** Este diseño se hizo sobre la v1.0 de la spec y quedó desactualizado: la v2.0 y las decisiones nuevas (Supabase, GitHub Actions, scraping híbrido, IA con Gemini) mandan. Se conserva solo como historial.

# SAMP — Diseño de implementación (Programación II)

Fecha: 2026-11-17 · Entrega: 2026-11-30 · Equipo: 3 personas
Fuentes: `Especificacion_Tecnica_SAMP.pdf` (consigna oficial), `guia-poo-prog2-01.pdf` y repo `amilkarhernandez/prog2-01` (estilo del profesor, rama `master`, revisión `7d69392`). Copias de los dos PDF en `docs/referencias/`.

**Precedencia:** este diseño complementa la spec. Los RF-01 a RF-20, los RNF y la sec. 15.1 de la spec mandan sobre cualquier cosa de este documento; este documento solo fija cómo se cubre el MVP, el estilo y las decisiones que la spec dejó abiertas (sec. 20 de la spec).

## 1. Intención y por qué

SAMP es una aplicación web que busca productos en varias tiendas, normaliza sus datos, identifica productos equivalentes y compara precios con transparencia (moneda, fecha de consulta, costos desconocidos).

La spec es la consigna oficial. **Lo que se evalúa, sobre todo, es el uso de los cuatro pilares de POO** (encapsulamiento, herencia, abstracción, polimorfismo) con la lógica y estructura del repo del profesor. Por eso Spring Boot es solo la infraestructura: los pilares deben ser fáciles de ver, explicar y probar en el dominio.

## 2. Objetivos, no objetivos, anti-objetivos

**Objetivos**
- Una ruta completa y demostrable: buscar → consultar conectores → normalizar → guardar → emparejar → comparar → mostrar.
- Cada pilar con una clase concreta que se pueda señalar, una línea que se pueda explicar y una prueba repetible.
- Conectores a APIs reales gratuitas, tantos como sean viables, con etiquetado honesto de qué es dato real y qué es catálogo de demostración.
- Cumplir los criterios críticos de la sec. 15.1 de la spec.

**No objetivos**
- Cobertura de todas las tiendas del mundo, checkout o pagos dentro de SAMP.
- Scraping de sitios que lo prohíban, evasión de CAPTCHA o de controles de acceso.
- Similitud semántica (nivel 5 de equivalencia): descartada.
- OpenAPI/Springdoc y microservicios.
- SLA de disponibilidad (el hosting gratuito es solo demostración).

**Anti-objetivos (fracaso aunque "funcione")**
- Pilares escondidos dentro de la magia de Spring, imposibles de señalar en la defensa.
- Mostrar un costo desconocido como 0, o llamar "el más barato" a algo con costos faltantes.
- Presentar datos de APIs de demostración como precios reales de mercado.
- Secretos en Git.

## 3. Restricciones

- Java 21, Spring Boot 3.x con versión exacta fijada en el `pom.xml`, Maven, PostgreSQL, Flyway, Spring Data JPA, Spring Security, Thymeleaf, Jakarta Validation, JUnit 5 y Mockito. Docker y despliegue en Render + Neon al final, si alcanza el tiempo.
- **Estilo del profesor:**
  - Paquetes `entities`, `repositories` y `services` dentro de cada módulo.
  - Nombres de clases y métodos en inglés (`createStudent`, `findOne`, `listAll`); mensajes de usuario en español.
  - Tipos explícitos; orden atributos → constructores → getters/setters.
  - `@Override` siempre que se implemente o sobrescriba.
  - Interfaz `IXxxService` + `XxxServiceImpl`.
  - Constantes `private static final`.
  - Sin `var`, `record` ni Lombok. Sin streams complicados donde un `for` mejorado con `.equals()` baste.
- Dinero siempre como `BigDecimal`/`NUMERIC`, nunca `double`. Fechas en UTC (`timestamptz`).
- Desconocido = `null`/estado explícito, nunca cero.
- Conectores: timeouts, reintentos con backoff, nunca registrar secretos, respetar términos de uso.
- Entrega el 30 de noviembre: quedan 13 días con 3 personas.

## 4. Enfoques considerados

| Enfoque | Resumen | Costo |
|---|---|---|
| **A (elegido)** | Spring normal. Repositorios como interfaces de Spring Data; servicios `IXxxService` + `Impl`; entidades con getters/setters escritos a mano; pilares en el dominio | Rápido y cumple la spec. Riesgo: la magia de Spring esconde código, se mitiga con `docs/pilares.md` |
| B | Núcleo POO en Java puro + capa Spring fina | Pilares muy limpios, pero duplica el mapeo dominio↔JPA; demasiado trabajo en 13 días |
| C | Repositorios escritos a mano con listas en memoria, como el repo | Más parecido al profesor, pero incumple la spec (PostgreSQL, JPA, Flyway) |

El usuario eligió **A**.

## 5. Arquitectura y estructura de paquetes

Monolito modular. Paquete base `com.samp`. Cada módulo de la spec contiene `entities`, `repositories` y `services` (más `controllers` y `dto` donde haga falta, siempre separados de los tres paquetes del profesor).

```
com.samp
  SampApplication            // equivale al Main del profesor
  common, config, security
  identity   catalog   stores   connectors   search
  offers     comparison   currency   shipping
  history    favorites    notifications   administration   analytics
```

Flujo (igual que en el repo): `controller` → `service` (valida y aplica reglas) → `repository` (guarda y recupera) → `entities`.

## 6. Mapa de los cuatro pilares

| Pilar | Dónde | Cómo se ve | Prueba repetible |
|---|---|---|---|
| **Encapsulamiento** | Todas las entidades (`Product`, `Offer`, `StoreProduct`, `ExchangeRate`, …) | Atributos `private`, constructor vacío + completo, getters/setters; el servicio valida antes de guardar; ninguna lista interna se expone para modificarse desde fuera | Test: no se puede crear una `Offer` con precio negativo; la lista de ofertas devuelta es una copia |
| **Herencia** | `User` → `RegisteredUser`, `AdminUser` (JPA `SINGLE_TABLE`, `super(...)` como `Student extends Person`). `AbstractStoreConnector` → conectores concretos | Datos comunes en el padre, específicos en el hijo; relación "es un" justificable | Test: `AdminUser` hereda email/nombre de `User` y agrega sus permisos |
| **Abstracción** | Interfaz `StoreConnector` (`search`, `getProduct`, `health`); `AbstractStoreConnector` con estado común (código de tienda, timeouts, cliente HTTP) y `abstract normalize(...)`; clase abstracta `ShippingPolicy` con `abstract BigDecimal calculateShipping(...)` (análoga a `Pay.calculate_amount()`); interfaces `IXxxService` | Contrato visible; `Main`/servicios dependen de la interfaz, no de la implementación | Test: el servicio de búsqueda funciona con un conector falso que implementa la interfaz |
| **Polimorfismo** | `List<StoreConnector>` ejecutada con la misma llamada; interfaz `ProductMatcher` con `GtinMatcher`, `BrandReferenceMatcher`, `AttributeMatcher`; `ShippingPolicy` con `FreeShipping`, `FlatRateShipping`, `UnknownShipping`; `User.canManageStores()` sobrescrito | Una llamada, comportamiento distinto según el objeto real; `@Override` en todas; sobrecarga con los constructores de las entidades | Test: la misma llamada `calculateShipping()` sobre tres políticas da tres resultados definidos (como los límites 100.000 / 120.000 de `Pay`) |

Entregable de defensa: `docs/pilares.md`, con una línea explicable y una prueba repetible por pilar, y una operación recorrida desde el controlador hasta el repositorio.

## 7. Conectores

Probados el 2026-11-17 desde este equipo:

| Fuente | Estado | Qué es | Uso |
|---|---|---|---|
| Open Food Facts Prices (`prices.openfoodfacts.org/api/v1`) + producto (`world.openfoodfacts.org/api/v2`) | 200, sin clave | **Precios reales** colaborativos con GTIN, tienda/lugar y moneda | Conector principal; permite comparar el mismo GTIN entre varias ubicaciones |
| DummyJSON (`dummyjson.com`) | 200, sin clave | Catálogo de demostración (electrónica, etc.) con marca, stock y envío | Tienda de demostración |
| Fake Store API (`fakestoreapi.com`) | 200, sin clave | Catálogo de demostración | Tienda de demostración |
| Platzi Fake Store (`api.escuelajs.co`) | 200, sin clave | Catálogo de demostración | Tienda de demostración |
| `SampleStoreConnector` (JSON local) | n/a | Productos con solapamiento deliberado con otras fuentes | Garantiza una demo de equivalencia por marca+referencia y atributos, y sirve para pruebas |
| MercadoLibre (`api.mercadolibre.com`) | **403 sin credenciales** | Real | No se usa por defecto. Opcional con credenciales en variables de entorno |
| eBay Browse / Best Buy | Requieren clave de desarrollador | Reales | Opcionales; se activan solo si hay clave en variables de entorno y quedan desactivados por defecto |

Reglas:
- Cada tienda guarda una marca `data_kind` (`REAL` o `DEMO`) y la interfaz la muestra. Los tres catálogos de demostración nunca se presentan como precios de mercado.
- Los catálogos de demostración probablemente no comparten productos entre sí. La demo de comparación real se apoya en Open Food Facts (mismo GTIN) y en `SampleStoreConnector`.
- Cada ejecución registra inicio, fin, conector, cantidad de resultados, errores y estado (`connector_executions`). Un conector caído no impide mostrar resultados de los demás.
- Se usa `java.net.http.HttpClient`.

## 8. Monedas

- Fuente de tasas: `open.er-api.com/v6/latest/USD` (gratuita, sin clave, 160+ monedas con COP, actualización diaria). Respaldo: `api.frankfurter.dev`, que **no incluye COP**, así que solo sirve como alternativa parcial.
- Las tasas se guardan en `exchange_rates` con fecha y fuente. Si la fuente no responde se usa la última tasa guardada, indicando su fecha.
- Moneda de referencia del sistema: USD (como propone la spec). La preferencia de visualización por defecto es COP, configurable por el usuario. Estas dos decisiones son del diseñador.
- La conversión es informativa y se rotula así.

## 9. Equivalencia y costo

- Orden de coincidencia: (1) GTIN/EAN/UPC exacto, (2) marca + referencia de fabricante, (3) atributos estructurados, (4) similitud textual solo para generar candidatos. Todo lo que no llegue a 1–3 se muestra como **"similar"**, no como "el mismo producto".
- Variantes que afectan precio o utilidad (capacidad, color, condición, región, contenido) impiden la equivalencia.
- `estimated_total` = precio convertido + envío conocido + impuestos conocidos. Si falta un componente, `estimated_total` es `null` y se etiqueta **"total estimado incompleto"** o **"menor precio publicado"**.

### 9.1 Envío, impuestos y orden de la comparación

- Ninguna de las APIs del MVP entrega impuestos de forma fiable y el envío casi nunca. Por eso `stores` guarda su configuración: `shipping_policy_type` (`FREE`, `FLAT_RATE`, `UNKNOWN`), `flat_shipping_amount` (nullable), `tax_included` (nullable) y `tax_rate` (nullable). El servicio elige la subclase de `ShippingPolicy` según `shipping_policy_type`; esa es la fábrica que da el polimorfismo.
- Los datos de demostración siembran tiendas con configuraciones distintas (una con envío gratis, una con tarifa fija, una con envío desconocido), para que la comparación muestre totales completos e incompletos.
- Destino de envío: país fijo por defecto (`CO`) en el MVP; la preferencia por usuario es RF-16 (opcional).
- Orden de la lista: primero las ofertas con total completo, de menor a mayor `estimated_total`; después las incompletas, por precio publicado convertido. La etiqueta **"más barato"** solo aparece si todas las ofertas del grupo tienen total completo; si no, la mejor oferta se rotula "menor precio publicado". La interfaz explica el criterio.

### 9.2 Cuándo se empareja y qué se guarda

- El emparejamiento corre **al ingerir** (después de normalizar cada resultado), no al consultar.
- `products` es el producto global canónico. `store_products` lleva `product_id` (nullable), `match_method` (`GTIN`, `BRAND_REFERENCE`, `ATTRIBUTES`, `NONE`) y `similar_to_product_id` (nullable, para candidatos "similares" que no llegaron a los niveles 1–3).
- En cada ingestión se recalcula. Si una coincidencia por GTIN deja de cumplirse, se desvincula y se conserva el cambio en el historial de la publicación.
- Unicidad: `store_products` único por (`store_id`, `external_id`).

### 9.3 Cómo funciona una búsqueda

1. La búsqueda pública consulta **en paralelo** a todos los conectores activos, con timeout por conector, y guarda (upsert) lo que devuelven.
2. Responde desde la base local: lo recién guardado más lo que ya había de las tiendas que fallaron o expiraron, marcando la fecha de observación de cada oferta.
3. Un conector que falla no impide mostrar los demás (criterio 8); la ejecución queda en `connector_executions`.
4. El botón manual del panel de administración vuelve a consultar con `getProduct` todos los `store_products` activos de las tiendas elegidas. La búsqueda y la actualización usan el mismo servicio de ingestión, así que las dos crean o actualizan la oferta vigente sin borrar historial (criterio 6).

## 10. Seguridad y operación

- BCrypt, CSRF, validación en servidor, consultas JPA parametrizadas, cookies `HttpOnly`/`SameSite`/`Secure` en producción.
- Permisos comprobados en el servidor; favoritos y alertas solo accesibles por su dueño.
- **Primer administrador:** variables de entorno `SAMP_ADMIN_EMAIL` y `SAMP_ADMIN_PASSWORD`. Si no existe un administrador al arrancar, se crea con esos valores. Nunca van en Git; `.env.example` solo lleva los nombres.
- **Rate limiting** (Bucket4j) en login y búsqueda, y **caché** (Caffeine) en búsquedas y tasas: entran en las últimas fases y son pequeños. Si el tiempo aprieta, se recorta el caché antes que cualquier pilar.
- Redirecciones externas validadas: solo `http`/`https` hacia el dominio de la tienda.
- Actualización de ofertas: botón manual en el panel de administración. `@Scheduled` queda configurable y apagado por defecto, porque Render gratuito suspende la app por inactividad.

## 11. Alcance

**MVP** (RF de la spec): 01 registro/login, 02 búsqueda por texto, 03 búsqueda por GTIN, 04 catálogo, 05 tiendas, 06 conectores, 07 normalización, 08 equivalencia, 09 ofertas, 10 comparación, 11 costo estimado, 12 monedas, 13 historial, 14 favoritos, 17 administración básica.

**Si alcanza el tiempo:** 15 alertas (umbral absoluto, notificación dentro de la web, evaluada al actualizar ofertas), 16 preferencias, 18 monitoreo, 19 auditoría, rate limiting, caché, Docker y despliegue Render + Neon.

**Fuera:** RF-20 se cumple de forma mínima (enlace externo validado); idiomas: español, con claves de mensajes preparadas para inglés.

## 12. Criterios de aceptación del MVP

1. La búsqueda pública funciona sin iniciar sesión.
2. Un usuario registrado solo ve y modifica sus propios favoritos.
3. Un visitante no accede al panel de administración ni a endpoints protegidos.
4. Dos publicaciones se agrupan solo cuando cumplen las reglas 1–3; las demás se muestran como "similares".
5. Cada oferta muestra tienda, moneda y fecha de observación.
6. Actualizar una oferta crea o actualiza la vigente sin borrar el historial.
7. Envío o impuestos desconocidos nunca se convierten en cero y marcan el total como incompleto.
8. Si una tienda falla, se siguen mostrando los resultados de las demás.
9. Cada pilar tiene su prueba repetible (sec. 6) y todas pasan.
10. Las migraciones Flyway se ejecutan en una base limpia.

## 13. Pruebas

JUnit 5 + Mockito para reglas aisladas (costo, equivalencia, conversión, `ShippingPolicy`). Pruebas de integración con PostgreSQL para repositorios y migraciones. Pruebas de seguridad para acceso cruzado a favoritos y rutas de administración. Pruebas de conectores con respuestas grabadas (timeouts, datos incompletos). La prueba de cada pilar de la sec. 6 es obligatoria.

## 14. Documentación mínima

`README.md`, `docs/pilares.md`, `docs/architecture/architecture.md`, `docs/database/erd.md` y `data-dictionary.md`, `docs/deployment/guide.md`. El resto de la lista de la sec. 16 de la spec queda para el final, si sobra tiempo.

## 15. Plan por días y reparto (propuesta)

| Días | Foco |
|---|---|
| 17–19 nov | Proyecto Maven, perfiles, Flyway V1–V3, entidades base y jerarquía de usuarios, seguridad y login |
| 20–23 nov | Catálogo y búsqueda, contrato `StoreConnector`, primeros dos conectores, normalización |
| 24–26 nov | Matchers, ofertas, monedas, comparación, historial |
| 27–28 nov | Favoritos, panel de administración, alertas, rate limiting, caché |
| 29 nov | Pruebas, `docs/pilares.md`, documentación, despliegue si alcanza |
| 30 nov | Entrega |

Reparto sugerido por persona: **A** identity, favorites, notifications, administration; **B** catalog, search, comparison (matchers); **C** connectors, stores, offers, currency, shipping, history. La configuración de seguridad, la clase `SampApplication` y la documentación son compartidas.

## 16. Quién decidió qué

**Decidido por el usuario:** enfoque A; la spec es la consigna oficial; los pilares son lo que más se evalúa; conectores reales tantos como se puedan; el administrador inicial se crea con variables de entorno; usar rate limiting y caché "todo lo necesario"; entrega el 30 de noviembre con 3 personas.

**Decidido por el diseñador (marcado como propuesta):** el conjunto exacto de conectores y el etiquetado `data_kind`; `open.er-api.com` como fuente de tasas; USD como referencia y COP como visualización por defecto; alertas solo dentro de la web; descartar similitud semántica; actualización manual en vez de `@Scheduled` por defecto; `HttpClient`; sin OpenAPI; el mapa de clases de la sec. 6; el reparto por persona y el calendario; la configuración de envío e impuestos por tienda y la regla de orden (sec. 9.1); emparejar al ingerir y las columnas de `store_products` (sec. 9.2); búsqueda en vivo en paralelo con respaldo en la base local (sec. 9.3).

## 17. Pendiente para quien construya

- Entorno (verificado en la máquina de desarrollo del usuario el 2026-11-17): Docker 29.9.2 está instalado; Java es 1.8 (hay que instalar JDK 21); no hay Maven ni `psql` (se usa Maven Wrapper `mvnw`). Las pruebas de integración con PostgreSQL usan Testcontainers si cada integrante tiene Docker; si no, PostgreSQL local con un perfil `test`.
- Versión exacta de Spring Boot 3.x y de las dependencias.
- Detalles de columnas de `offers`, `price_history` y `connector_executions` (la sec. 8.4 de la spec fija los campos conceptuales).
- Los diagramas de la spec (secs. 6.2, 7, 8.2, 14.2 y 14.3) no se pudieron leer; el ERD se rehará en `docs/database/erd.md`.
- Mapeo de los campos de cada API a `Offer` (el de Open Food Facts es el más complejo: ubicación como tienda, cantidad y moneda por observación).
- Reglas exactas de unicidad para evitar duplicar `store_products` entre ejecuciones.
