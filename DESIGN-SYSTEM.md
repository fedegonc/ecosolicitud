# Sistema de diseno

## Una hoja propia

`src/main/resources/static/css/app.css` es la unica hoja CSS propia.
`app.html` la carga una vez; no agregar `@import`, hojas paralelas ni
bloques `<style>` en plantillas. El CSS de Leaflet es de terceros y se
carga solo con el mapa. Las proporciones calculadas de los graficos no
son nuevas reglas visuales.

La hoja contiene tokens/base, armazon, componentes, patrones, variantes
demo y herramientas dev. Las reglas responsive se conservan junto al
bloque que modifican para mantener la cascada. No hay una segunda capa
de sobreescrituras en otro archivo.

No se oculta la complejidad minificando para cumplir un conteo de lineas:
se eliminan duplicaciones y se comparten componentes. Un cambio en el
orden de las reglas requiere comprobar ambas vistas y movil.

## Direccion visual

- Verde de marca para identidad y navegacion, no como fondo dominante.
- Navegacion verde muy clara con texto oscuro, foco verde y estado activo
  marcado tambien con peso y una linea lateral.
- Accion principal naranja con texto oscuro: contraste aproximado 4.84:1.
  No usar texto blanco sobre este naranja. El acceso a nueva solicitud
  comparte estos tokens con los botones principales.
- Acciones secundarias con contorno verde; peligro con contorno rojo.
- Bordes continuos y estados deshabilitados visibles, sin cambiar radios,
  tipografia, fotografias ni mensajes de producto en este bloque.
- Los colores suaves de avisos usan texto oscuro, no naranja claro.

## Catalogo compacto

`/dev/design-system`, solo con `dev` y sin `prod`, muestra cuatro
desplegables nativos: tokens, componentes, patrones y reglas.
Se abre una categoria por vez, incluso sin JavaScript en navegadores
que admiten `details[name]`. Las muestras no guardan datos.

- Tokens: colores y medidas leidos de las variables reales, no una paleta nueva.
- Componentes: acciones, campos, seleccion y sus estados pertinentes.
- Patrones: estados de negocio, ficha, avisos, mensajes y kanban.
- Reglas: accesibilidad, vocabulario y conectividad.

El kanban reutiliza `secciones/avisos :: kanban(columnas)`, con las mismas
clases `.kanban`, `.kanban-col` y `.tarjeta` de las pantallas reales.
La muestra tiene una columna con un aviso ficticio y otra vacia. La ficha
y la linea de tiempo tambien son fragmentos compartidos, no copias.

Los casos extensos y la inspeccion estructural siguen en `/dev/layout-system`.
El catalogo no incluye un iframe ni consulta servicios externos.

## Reglas de uso

- Colores por proposito y tamanos desde los tokens existentes.
- Una tarea principal por pantalla; no confundir tarjetas informativas con botones.
- Etiquetas visibles, foco de teclado, texto e icono para los estados.
- Estados del dominio: pendiente, en curso, completada, rechazada y cancelada.
- Vocabulario: vecino, centro y solicitud; traducciones equivalentes en portugues.
- Sin conexion no se confirma un envio: no existe una cola offline de escrituras.
- Ante un resultado incierto, verificar antes de reenviar.
- La carga y los errores se muestran donde corresponde; no todos los
  componentes necesitan todos los estados.

## Verificacion

Ejecutar `mvn verify` sin un arranque Maven simultaneo en el mismo directorio.
Las pruebas comprueban las traducciones, los fragmentos, la ausencia de
escrituras y la exclusion de produccion.

Para la revision visual usar una base H2 temporal como indica `AGENTS.md`.
Comprobar vecino y centro, 390 px, teclado, movimiento reducido y reflujo
al ampliar el contenido. No usar la base persistente para automatizaciones.
