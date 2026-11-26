# Evidencia de pruebas de carga

Resultados de las campañas de carga ejecutadas contra el sistema, conservados
como evidencia del TCC. Cada archivo `.resumen.json` resume una corrida
(inicio/fin UTC, usuarios, iteraciones, rampa, pausa, muestras, errores y
estadísticas de JMeter); el `.jtl` es el volcado crudo de muestras (todos
pesan menos de 1 MB) y el `.statistics.json` es el total del reporte HTML de
JMeter (`pct2ResTime` = percentil 95). `resumen.csv` consolida las cuatro
tandas.

## Mediciones contra Render (plan gratuito) — Tabla B.3 del TCC

Carpeta `medicion-C-20261102/`, ejecutadas el **2026-11-02** (UTC) contra
`https://ecosolicitud.onrender.com` (HTTPS, puerto 443), perfil de
navegación: 8 páginas GET por recorrido, cookies por usuario, assertions de
HTTP 200 + `<main`.

Plan JMeter usado: `carga-10-usuarios-demo.jmx` (raíz de esta carpeta).
Parámetros comunes: **rampa 15 s, pausa 500 ms, 30 iteraciones previstas**
(8 páginas × usuarios × iteraciones = muestras esperadas). Los usuarios se
pasan por `-Jusuarios`; los valores usados fueron 10, 15, 20 y 20.

| Archivo | Usuarios | Muestras | Errores | p50 | p95 | Nota |
|---|---:|---:|---:|---:|---:|---|
| `render-escalon-10-20261102-184938Z.*` | 10 | 2.440/2.440 | 0 | 1,00 s | 2,00 s | corrida completa |
| `render-escalon-15-20261102-185851Z.*` | 15 | 3.660/3.660 | 0 | 1,60 s | 2,59 s | corrida completa |
| `render-escalon-20-20261102-191013Z.*` | 20 | 403/4.800 | 0 | 2,30 s | 5,01 s | detenida por p95 > 3 s sostenido |
| `render-escalon-20-20261102-191340Z.*` | 20 | 4.008/4.800 | 0 | 2,30 s | 3,96 s | detenida al llegar a 600 s |

La tabla del TCC (10 / 15 / 20 personas → p95 2,0 / 2,6 / 4,0 s, 0 errores)
corresponde a la primera corrida de 10, la de 15 y la **segunda** corrida de
20 (`...191340Z`, la que figura en `resumen.csv` como "Tanda 4"). La primera
corrida de 20 (`...191013Z`) se cortó antes por el criterio de latencia y se
conserva como evidencia de la variabilidad entre corridas.

## Prueba local con escalones (Tabla B.2 del TCC)

**No se encontraron los archivos de la corrida local con escalones de
1, 10, 20, 40 y 80 usuarios** (mediana ~0,86 s, p95 ~2,3 s, ~49 páginas/s
con 80 usuarios) en `testing/`, `scratch/`, `target/` ni en el resto del
directorio de trabajo al 2026-11-25. La Tabla B.2 no tiene respaldo de
archivos en esta carpeta; si la evidencia existe en otra máquina o se
perdió, corresponde regenerarla o corregir la tabla.

Existe una corrida local distinta, del **2026-11-06**
(`testing/resultados/local-20261106-6QaREj/`, no versionada): navegación con
escalones de 5, 10 y 15 sesiones sobre la app real con perfil prod, 0,15 CPU
y 512 MiB, que se cortó en 15 por p95 > 5 s. No corresponde a la Tabla B.2
(es otro rango de usuarios y otra configuración) y no se copió aquí.
