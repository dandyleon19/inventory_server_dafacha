# Inventory Server Dafacha

Backend del sistema de inventario Dafacha (retail / tiendas pequeñas). Repo propio, base de datos propia, despliegue propio.
No depende de `salonspa_*`. Su frontend es `inventory-app-dafacha` (Nuxt 3).

**Stack:** Spring Boot 4.1 · Java 21 · WebFlux · R2DBC + PostgreSQL · Flyway · Spring Security (JWT) · MapStruct · Lombok.

## Hoja de ruta (una etapa a la vez)

| Etapa | Contenido | Estado |
|-------|-----------|--------|
| 0 | Base: tenant (`companies`), usuarios, JWT + refresh tokens, bootstrap, manejo de errores | hecho |
| 1 | Catálogo: categorías y productos (SKU, código de barras, unidad, **costo**, precio, margen, stock mínimo). Sin stock todavía | hecho (sin probar) |
| 2 | Almacenes/sedes con **stock por ubicación**, movimientos (audit log), transferencias y **valorización** (cantidad × costo) | hecho (sin probar) |
| 3 | Proveedores + **órdenes de compra** (recepción alimenta stock y costo) | hecho (sin probar) |
| 3b | **Lotes y vencimientos** (FEFO), alertas y dashboard | hecho (sin probar) |
| 8 | **Usuarios y roles** (roles fijos, acceso por almacén) y **auditoría** | hecho (sin probar) |
| Después | Reportes, motivo en salidas, conversión de unidad de compra, importar productos, códigos de barras | sin priorizar |

## Puesta en marcha local

1. Crear la base de datos Postgres `inventory_dafacha` (por defecto `postgres`/`root` en `localhost:5432`).
2. Variables opcionales: `DB_URL`, `JDBC_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`
   (ver `src/main/resources/application.properties`). El `JWT_SECRET` por defecto es **solo para desarrollo**.
3. `./mvnw spring-boot:run`. Levanta en el puerto fijo **8081** (`server.port`; `salonspa_server_v2` usa 8080).
   Flyway aplica las migraciones al arrancar.
4. Inicializar el sistema (solo funciona si no hay usuarios): `POST /api/auth/bootstrap` con
   `{ "company": { "name": "..." }, "admin": { "firstName", "lastName", "email", "password" } }`.

## API del catálogo (etapa 1)

Todo está acotado a la empresa del usuario autenticado. Lectura para cualquier usuario; escritura solo `ADMIN_USER` / `SUPER_ADMIN`.

- `GET /api/products?page&size&search&categoryId&isActive` · `GET|PUT|DELETE /api/products/{id}` · `POST /api/products`
- `GET /api/product-categories?page&size&search` · `GET|PUT|DELETE /api/product-categories/{id}` · `POST /api/product-categories`
- SKU, categoría y código de barras son **opcionales por defecto**; cada empresa puede volverlos obligatorios en su configuración (ver abajo). Cuando se informan, el SKU es único por empresa (sin distinguir mayúsculas) y el código de barras también. Una categoría con productos no se puede eliminar (409).
- `marginAmount` / `marginPercent` se calculan en la respuesta (margen sobre el precio de venta).

## Empresa y configuración

La empresa es siempre la del usuario autenticado (no hay id en la ruta). Lectura para cualquier usuario; escritura solo `ADMIN_USER` / `SUPER_ADMIN`.

- `GET|PUT /api/company` — nombre, razón social, RUC (único), dirección fiscal, teléfono.
- `GET|PUT /api/company/settings` — `requireProductSku`, `requireProductCategory`, `requireProductBarcode` (todos `false` por defecto). Sin fila guardada se devuelven los valores por defecto; la primera vez que se guarda se crea la fila (upsert).
- Las reglas se validan en el backend al crear y al editar productos (400 si falta un campo obligatorio) y no alteran los productos ya existentes. Para agregar una regla nueva: columna en `company_settings` (migración), campo en `CompanySettings`, y su validación en el servicio correspondiente.

## Almacenes y stock (etapa 2)

Todo acotado a la empresa del usuario. Cada empresa tiene un "Almacén principal" desde el bootstrap (y la migración `V4` lo crea para empresas existentes).

- `GET|POST /api/warehouses` · `GET|PUT|DELETE /api/warehouses/{id}` — escritura solo admins. Un almacén con movimientos no se elimina (409): se desactiva.
- `GET /api/stock?page&size&search&productId&warehouseId&isActive&lowStockOnly` — productos con su stock (suma de todos los almacenes, o solo el de `warehouseId`), valor y bandera `lowStock` (hay mínimo > 0 y stock ≤ mínimo).
- `GET /api/stock/valuation` — valor total y por almacén (**stock × costo actual del catálogo**; el costo promedio ponderado llega con las compras, etapa 3).
- `GET /api/stock/movements?page&size&productId&warehouseId&type` — historial (kardex), más reciente primero.
- `POST /api/stock/entries` · `/exits` · `/transfers` — cualquier usuario. `POST /api/stock/adjustments` (conteo físico: fija el stock al valor contado y registra la diferencia) — solo admins.
- Tipos de movimiento: `ENTRY`, `EXIT`, `ADJUSTMENT`, `TRANSFER_IN`, `TRANSFER_OUT`. Cada movimiento guarda cantidad con signo, **saldo resultante** y una **copia del costo** del momento. Las dos patas de una transferencia comparten `transferRef`.
- Concurrencia: el cambio de stock es un **`UPDATE` condicional atómico** (`… WHERE cantidad + delta >= 0 … RETURNING`, precedido de un `INSERT … ON CONFLICT DO NOTHING` que garantiza que la fila exista) que rechaza el resultado negativo; el bloqueo de fila del `UPDATE` serializa las operaciones simultáneas sobre el mismo stock, así que no pueden sobrevender. Las transferencias corren en una transacción (si el origen no alcanza, no se acredita nada).
- Setting `allowNegativeStock` (apagado por defecto): permite que una salida o transferencia deje el stock bajo cero.
- Productos en unidad, caja, paquete o docena solo aceptan cantidades enteras. Un producto con movimientos no se elimina (409): se desactiva.
- `unit_cost` y `balance_after` son fotos del momento: no se recalculan si cambia el costo del producto.

## Proveedores y compras (etapa 3)

Todo acotado a la empresa del usuario. Lectura para cualquier usuario; escritura de proveedores y de órdenes solo admins, **excepto recibir mercadería, que puede hacerlo cualquier usuario**.

- `GET|POST /api/suppliers` · `GET|PUT|DELETE /api/suppliers/{id}` — nombre y RUC únicos por empresa. Un proveedor con órdenes no se elimina (409): se desactiva.
- `GET /api/purchase-orders?page&size&search&status&supplierId` (lista con `lineCount`, sin líneas) · `GET /api/purchase-orders/{id}` (detalle con líneas, recibido y pendiente).
- `POST /api/purchase-orders` crea un **borrador** (numeración atómica por empresa: `OC-000001`) · `PUT /{id}` y `DELETE /{id}` solo sobre borradores.
- `POST /{id}/order` (borrador → pedida) · `POST /{id}/receive` · `POST /{id}/cancel`.
- Estados: `DRAFT → ORDERED → PARTIALLY_RECEIVED → RECEIVED`, o `CANCELLED`. Cancelar una orden parcialmente recibida conserva el stock ya recibido.
- Líneas: cantidad y costo van **siempre en la unidad del producto**; el formulario deja pedir por unidades o por paquetes (+ sueltas) y dar el precio por unidad, por paquete o como total, y calcula las unidades y el costo por unidad. Cómo se pidió se guarda como texto informativo (`packaging`). Un producto **puede repetirse** en la misma orden (por ejemplo un paquete a un precio y unas unidades extra a otro): cada línea se recibe por separado y entra con su propio costo.
- **Recibir** (`{ "receivedDate"?, "items": [{ "lineId", "quantity" }] }`, parcial permitido, no más de lo pendiente). `receivedDate` (`yyyy-MM-dd`) es el día que llegó la mercadería: hoy por defecto, no puede ser futuro ni anterior al día del pedido, y es la fecha con la que quedan los movimientos (un día anterior se registra a mediodía). Todo en una transacción y por cada línea:
  1. recalcula el costo del producto por **promedio ponderado** con el stock que tiene en *todos* los almacenes: `(stock × costo actual + recibido × costo de compra) / (stock + recibido)`; sin stock (≤ 0) el costo pasa a ser el de la compra;
  2. suma el stock al almacén de destino de la orden;
  3. registra un movimiento `PURCHASE` (con el costo de compra y la referencia a la orden);
  4. actualiza lo recibido de la línea.
- `GET /api/stock/movements?purchaseOrderId=` lista las recepciones de una orden.
- Límite conocido: dos recepciones simultáneas del mismo producto pueden promediar sobre un stock levemente desactualizado; el efecto es mínimo y el stock siempre queda correcto.

## Costo real y valor del inventario

El valor del inventario es **real**, no stock × un costo promedio: cada compra o ingreso que trae stock a un almacén se guarda como una **capa de costo** (`stock_cost_layers`: producto, almacén, costo unitario real, cantidad que queda, y el lote al que pertenece si el producto controla vencimiento). El valor es la suma de `cantidad × costo` de las capas, por almacén y en total (`GET /api/stock/valuation`, `GET /api/dashboard`, y la columna *Valor real* de Inventario).

- **Entran capas**: recepciones de órdenes (al costo de la compra), ingresos manuales (`unitCost` del request; sin él, el costo actual del producto), ajustes hacia arriba (costo actual del producto) y transferencias (llegan con el costo que tenían donde salieron).
- **Salen capas** (salidas, transferencias, ajustes hacia abajo): cada unidad que sale se lleva el costo de la capa de la que viene, y el movimiento queda registrado con ese costo real (`unit_cost` del kardex). Una salida que toca varias compras genera un movimiento por compra.
- **Quién elige qué sale** lo decide la configuración de la empresa `costMethod`. `FIFO` (en la interfaz «Automático», el valor por defecto): no se pregunta nada; en un producto con vencimiento sale el lote que vence primero y en uno sin vencimiento la compra más antigua (dentro de un lote, también la más antigua). `MANUAL` («Lo elijo yo»): en cada salida/transferencia se elige el lote (`lots`, producto con vencimiento) o la compra (`layers: [{ "layerId", "quantity" }]`, producto sin vencimiento; `GET /api/stock/cost-layers?productId&warehouseId` las lista). En modo automático el formulario ofrece «Elegir manualmente» para una salida puntual. El servidor acepta `lots`/`layers` en cualquier modo: la configuración solo decide si el formulario los pide. El conteo físico de un producto con vencimiento siempre se hace lote por lote.
- **Stock negativo**: las unidades que ninguna capa cubre salen al costo actual del producto y no consumen nada; cuando después entra mercadería, las primeras unidades solo cubren ese faltante (no crean capa).
- **Migración V8**: el stock que ya existía entra como una capa por producto/almacén (y por lote) **al costo actual del catálogo**: desde ahí todo es exacto.
- El `costPrice` del catálogo sigue siendo el **costo promedio ponderado** (se actualiza al recibir órdenes) y se usa para el margen y como costo por defecto; ya no se usa para valorizar el inventario.
- Límites conocidos: los ingresos manuales no actualizan el costo promedio del catálogo (solo las recepciones de órdenes); en `MANUAL`, los productos con vencimiento no eligen compra dentro del lote.

## Vencimientos, alertas y dashboard

- **Producto con `tracksExpiry`**: su stock se lleva por **lote** (almacén + producto + fecha de vencimiento + número de lote opcional) en `stock_lots`. `stock_levels` sigue siendo el total; los lotes suman ese total, salvo el stock que ya existía antes de activar el control (queda "sin lote" y se consume al final).
- **Entran lotes**: `POST /api/stock/entries` (con `expiryDate` y `lotNumber` opcional, obligatorio si el producto controla vencimiento y no puede estar en el pasado), recepciones de compra (`expiryDate`/`lotNumber` por línea de `POST /api/purchase-orders/{id}/receive`) y ajustes por conteo **hacia arriba**. Un mismo lote (misma fecha y número) se acumula.
- **El usuario puede elegir de qué lote sale**: `POST /api/stock/exits` y `POST /api/stock/transfers` aceptan `lots: [{ "lotId", "quantity" }]` (un `lotId` nulo = las unidades sin fecha de vencimiento). Deben sumar la cantidad, no repetir un lote y no pasarse de lo que tiene cada uno (409 si el lote ya no alcanza). El formulario muestra los lotes del almacén con su fecha y lo disponible, y propone el que vence antes; se puede cambiar cada cantidad. Si no se envía `lots`, sale por FEFO como antes.
- **El conteo físico de un producto con vencimiento se hace lote por lote**: `POST /api/stock/adjustments` acepta `lotCounts: [{ "lotId", "countedQuantity" }]` (lo contado en cada lote que el sistema conoce; `lotId` nulo = unidades sin fecha) y `newLots: [{ "expiryDate", "lotNumber", "quantity" }]` (lotes encontrados que el sistema no tenía). `countedQuantity` debe ser la suma de ambos. La diferencia de cada lote se ajusta **en ese lote** (sube o baja) y deja un movimiento `ADJUSTMENT` por lote; lo que no cambió no genera movimiento. Sin `lotCounts` (producto sin vencimiento) el ajuste es el de siempre sobre el total.
- **Salen por FEFO** (el que vence antes, primero; es lo que pasa si no se elige): salidas, la pata de salida de transferencias y ajustes hacia abajo. Una salida que abarca varios lotes se registra como **un movimiento por lote** (cada uno con su lote y fecha). Las transferencias llevan cada lote al destino con la misma fecha y número.
- Todo pasa por `StockLedger` (un solo lugar donde cambia el stock), dentro de la transacción; los lotes que se leen para consumir se bloquean (`FOR UPDATE`).
- `GET /api/stock/lots?page&size&productId&warehouseId&scope=ALL|EXPIRED|EXPIRING|ATTENTION` — lotes con stock, el que vence antes primero, con `daysToExpiry` (negativo = vencido). `EXPIRING` y `ATTENTION` usan la ventana de la empresa.
- **Configuración** (`/api/company/settings`): `expiryAlertDays` (1–365, por defecto 30) y `purchaseArrivalAlertDays` (1–90, por defecto 7).
- `GET /api/dashboard` — valor del inventario, contadores de alertas y listas cortas (5):
  stock bajo (productos con mínimo > 0 y stock ≤ mínimo), lotes vencidos / por vencer, órdenes de compra abiertas que se esperan dentro de la ventana o ya atrasadas, últimos movimientos y valor por almacén. Alimenta el dashboard y la campana de alertas.
- Límite conocido: los productos que ya tenían stock antes de activar el control de vencimiento no tienen fechas para ese stock; el control aplica a lo que entra a partir de ese momento.

## Usuarios, roles y auditoría (etapa 8)

**Roles fijos** (`Role`), cada uno con permisos predefinidos. Leer es libre para todo usuario autenticado (un `VIEWER` ve todo); escribir depende del rol (la matriz vive en `SecurityConfig`):

| | ADMIN_USER / SUPER_ADMIN | WAREHOUSE_USER | PURCHASING_USER | VIEWER |
|---|:-:|:-:|:-:|:-:|
| Usuarios, auditoría, empresa y configuración | x | | | |
| Catálogo (productos, categorías) y almacenes | x | | | |
| Ajustes por conteo | x | | | |
| Ingresos, salidas, transferencias | x | x | | |
| Proveedores y órdenes de compra (crear, editar, pedir, cancelar) | x | | x | |
| Recibir mercadería | x | x | x | |

La migración `V7` convierte los `STAFF_USER` existentes en `WAREHOUSE_USER` (mismos permisos que tenían). `SUPER_ADMIN` (el dueño de la cuenta) se crea solo en el bootstrap y no se asigna por la API.

**El rol, el estado y los almacenes se leen de la base de datos en cada petición** (`UserAccessResolver`, con un caché de 10 s que se invalida al editar un usuario), no del token: el token solo identifica a la persona. Así, desactivar a alguien o cambiarle el rol aplica de inmediato aunque su token (24 h) siga vigente.

**Acceso por almacén**: un usuario que no es admin puede quedar limitado a algunos almacenes (tabla `user_warehouses`). Sin filas = todos. Para un usuario limitado:
- ingresos, salidas, ajustes, transferencias (ambos extremos) y órdenes de compra de otro almacén responden **403**;
- inventario, valorización, lotes, movimientos, el dashboard y las órdenes de compra solo suman/muestran sus almacenes (una orden de otro almacén responde 404, como si no existiera);
- `GET /api/warehouses` solo devuelve los suyos, así los selectores del frontend ya salen filtrados.
Los admins nunca están limitados (lo guardado se ignora).

**Usuarios** (`/api/users`, solo admins, de la empresa del que llama):
- `GET /api/users?page&size&search&role&isActive` · `GET /api/users/{id}` · `POST /api/users` · `PUT /api/users/{id}` · `PUT /api/users/{id}/password`.
- Body de crear/editar: `firstName`, `lastName`, `email`, `role`, `isActive`, `warehouseIds` (vacío = todos) y, solo al crear, `password` (mínimo 8). El correo es único (sin distinguir mayúsculas).
- No se eliminan (los movimientos y la auditoría los referencian): se desactivan. Desactivar o restablecer la contraseña **cierra todas sus sesiones** (revoca los refresh tokens).
- Reglas de seguridad: nadie puede cambiar su propio rol ni desactivarse; `SUPER_ADMIN` no se asigna, no cambia de rol ni se desactiva, y solo otro super admin puede editarlo. Se eliminó `POST /api/auth/register`; se usa `POST /api/users`.

**Auditoría** (`GET /api/audit?page&size&entityType&action&userId&search&from&to`, solo admins, solo lectura):
- Cada entrada guarda quién (nombre copiado al momento), qué (`action`: `CREATE`, `UPDATE`, `DELETE`, `ORDER`, `RECEIVE`, `CANCEL`, `PASSWORD_RESET`, `LOGIN`), sobre qué (`entityType`: producto, categoría, proveedor, almacén, orden de compra, usuario, empresa, configuración, sesión), cuál (`entityLabel`) y el detalle: una línea por campo (`Precio de venta: 4.5 → 5`). Una edición sin cambios no deja entrada.
- Se registra en los servicios (`AuditRecorder`), no en los controllers. Si escribir la entrada falla, se registra en el log y la operación sigue. Los movimientos de stock ya son su propio historial (kardex) y no se duplican aquí.
- `from` / `to` son días (`yyyy-MM-dd`), ambos inclusivos. Sin claves foráneas a propósito: el rastro sobrevive a lo que describe.

## Convenciones

- Hexagonal: `domain` (modelos, puertos, excepciones) → `application` (servicios, DTOs) → `infrastructure` (controllers, adapters R2DBC, seguridad).
  Casos de uso y adapters de repositorio son clases planas cableadas en `config/BeanConfig`.
- Multi-tenant por `company_id` en cada tabla de datos; el acceso cruzado entre empresas responde 404 (`SecurityHelper.requireSameCompany`).
- Migraciones Flyway en `src/main/resources/db/migration` (`V{n}__descripcion.sql`).
- Código en inglés; **los mensajes que ve el usuario (errores de la API, validaciones y el detalle de la auditoría) van en español**, porque el frontend los muestra tal cual. Al agregar un mensaje nuevo, escríbelo en español.
- **Tras agregar campos a un modelo o entidad, hacer un clean rebuild** (`./mvnw clean compile`): los `*MapperImpl` de MapStruct pueden quedar viejos con un build incremental y los campos nuevos llegan nulos sin error.
