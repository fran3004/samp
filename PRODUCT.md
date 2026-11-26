# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Stack

Decidido en la especificación del proyecto (`docs/referencias/Especificacion_Tecnica_SAMP.pdf`) y en el diseño aprobado (`docs/superpowers/specs/2026-11-17-samp-design.md`): Java 21 + Spring Boot 3.x con vistas renderizadas por Thymeleaf, HTML5, CSS3 y JavaScript sin framework de frontend. El diseño visual debe poder expresarse como plantillas Thymeleaf con CSS propio.

## Users

Comprador en Colombia, muchas veces desde el celular, que busca un producto y quiere saber dónde sale más barato. Ve los precios en COP por defecto (el sistema los guarda en USD y convierte). El sitio es de acceso público para buscar y comparar; las cuentas se usan para favoritos, alertas y preferencias. Hay además un administrador que gestiona tiendas, conectores y usuarios. Para la entrega del curso (30 de noviembre de 2026), los evaluadores también verán el sitio en una demostración.

## Product Purpose

SAMP (Sistema Automatizado de Mejores Precios) es una aplicación web que busca productos en varias tiendas en línea, normaliza sus datos, identifica productos equivalentes y compara precios con transparencia: moneda, fecha de consulta, envío e impuestos conocidos y disponibilidad. El éxito es que una persona pueda buscar un producto y entender de inmediato dónde conviene comprar, y por qué puede confiar en esa comparación.

## Positioning

SAMP no oculta lo que no sabe. Un costo de envío o de impuestos desconocido se muestra como desconocido, nunca como cero, y nunca se llama "el más barato" a una oferta con costos faltantes (se rotula "menor precio publicado" o "total estimado incompleto"). Cada oferta muestra su tienda, moneda original, fecha de observación y si el dato es real o de demostración. Dos publicaciones solo se llaman "el mismo producto" cuando hay una regla verificable; si no, se muestran como "similares".

## Operating Context

Una persona escribe el nombre o el código de barras (GTIN) de un producto. SAMP consulta en paralelo a varias tiendas, guarda lo que devuelven y muestra la comparación ordenada por total estimado. Las ofertas guardan historial de precios. Un usuario registrado guarda favoritos; el administrador revisa ejecuciones de conectores y activa o desactiva tiendas. Fuentes del MVP: Open Food Facts Prices (precios reales con GTIN), tres catálogos de demostración (DummyJSON, Fake Store, Platzi) y un conector de muestra local. Las monedas se convierten con tasas fechadas.

## Capabilities and Constraints

- Búsqueda pública por texto y por GTIN, sin iniciar sesión.
- Comparación de ofertas con precio publicado, envío, impuestos conocidos, total estimado y enlace externo seguro a la tienda. No hay checkout ni pagos dentro de SAMP.
- Cada tienda está marcada como real o de demostración y la interfaz debe mostrarlo.
- Moneda de referencia USD; visualización por defecto COP; idioma inicial español, con claves preparadas para inglés.
- Favoritos, alertas de precio y preferencias para usuarios registrados; panel de administración para tiendas, conectores, usuarios y catálogo.
- Las vistas son plantillas del servidor; los estados de carga, vacío, error y fallo parcial de tiendas deben existir.
- Decisión abierta: qué idioma y moneda se muestran a un visitante que aún no ha elegido preferencias (hoy: español y COP).

## Brand Commitments

El nombre es **SAMP**. El equipo tiene un logo (imagen enviada por el usuario en el chat el 2026-11-17; el archivo aún no está en el proyecto). Es una marca en forma de «S» plegada en azul brillante (aprox. `#0B7BF2`) con un pliegue azul más profundo (aprox. `#1447C0`) que termina en turquesa (aprox. `#22E3D2`), junto al texto «SAMP» en mayúsculas anchas de color azul marino (aprox. `#0F1E2E`) y la leyenda «SISTEMA AUTOMÁTICO DE MEJORES PRECIOS» en gris azulado sobre fondo gris muy claro (aprox. `#F4F6F9`). Los colores del sitio pueden derivarse de él; los valores son estimados a ojo y deben confirmarse con el archivo. Nota: el logo dice «Automático» y la especificación dice «Automatizado»; falta decidir cuál es el nombre oficial. No hay otro requisito visual fijado.

Preferencia visual confirmada por el usuario (2026-11-17): SAMP debe verse y sentirse como una **tienda en línea moderna y confiable**, aunque no vende productos propios sino que reúne las ofertas de todas las tiendas. Debe poder sentarse al lado de **Apple y Nike** en calidad de acabado: mucho aire, fotografía de producto grande, animaciones **sutiles y rápidas**, detalles cuidados y señales de confianza (tienda, moneda, fecha y tipo de dato siempre visibles). Esta preferencia sustituye a las direcciones temáticas exploradas (billetes, cartera de boletos, mapa de metro, etc.).

## Evidence on Hand

- Documentos del proyecto: `docs/referencias/Especificacion_Tecnica_SAMP.pdf`, `docs/superpowers/specs/2026-11-17-samp-design.md`, `docs/superpowers/plans/2026-11-17-samp-implementation.md`.
- Datos reales disponibles vía Open Food Facts Prices (por ejemplo, el GTIN 3017620422003 devuelve precios por ubicación) y datos de demostración de las tres APIs gratuitas.
- No hay clientes, testimonios, reseñas, prensa, métricas de uso ni fotografías propias. No se deben inventar.

## Product Principles

1. Transparencia antes que atractivo: tienda, moneda, fecha y tipo de dato (real o demostración) siempre visibles junto a cada precio.
2. Lo desconocido se muestra como desconocido, nunca como cero ni como ventaja.
3. Equivalencia con reglas verificables; lo que no se puede confirmar es "similar".
4. El buscador es lo primero, y la confianza en el resultado se demuestra en cada resultado, no en un texto aparte.
5. Pensado primero para el celular de un comprador en Colombia.

## Accessibility & Inclusion

Diseño responsive (celular, tableta y escritorio), uso completo con teclado, foco visible, mensajes claros en español y estados de carga, vacío y error. No hay un estándar formal exigido más allá de la spec del proyecto.
