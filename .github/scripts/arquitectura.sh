#!/usr/bin/env bash
# Reglas de arquitectura que se verifican solas.
# Corren en CI (push/PR) y local: bash .github/scripts/arquitectura.sh
# Cada regla existe porque ya se violó una vez o porque el costo de violarla es alto.
set -uo pipefail
cd "$(dirname "$0")/../.."

fallas=0
fallo() { echo "::error::$1"; fallas=1; }
# Módulos = subdirectorios del paquete base; la lista se deriva para que un
# módulo nuevo quede cubierto por el gate sin tocar este script.
mapfile -t modulos < <(find src/main/java/com/ecosolicitud -mindepth 1 -maxdepth 1 -type d -exec basename {} \; | sort)

# 1. Ningún módulo importa la raíz: el gradiente va de específico a general.
if grep -rqE "^import com\.ecosolicitud\.[A-Z]" src/main/java/com/ecosolicitud/*/ 2>/dev/null; then
    fallo "un módulo importa la raíz (com.ecosolicitud.X): el gradiente se invirtió"
    grep -rnE "^import com\.ecosolicitud\.[A-Z]" src/main/java/com/ecosolicitud/*/ | grep -v "/interno/"
fi

# 2. shared es hoja: si importa un módulo, dejó de ser la capa general.
otros_shared=$(printf '%s\n' "${modulos[@]}" | grep -v '^shared$' | paste -sd'|')
if grep -rqE "^import com\.ecosolicitud\.($otros_shared)\." src/main/java/com/ecosolicitud/shared/ 2>/dev/null; then
    fallo "shared importa un módulo: dejó de ser hoja"
fi

# 3. Nadie importa el interno de OTRO módulo (Modulith lo verifica; acá se ve el detalle).
for m in "${modulos[@]}"; do
    otros=$(printf '%s\n' "${modulos[@]}" | grep -v "^$m$" | paste -sd'|')
    ajenos=$(grep -rlE "^import com\.ecosolicitud\.($otros)\.interno\." \
            "src/main/java/com/ecosolicitud/$m/" 2>/dev/null || true)
    if [ -n "$ajenos" ]; then
        fallo "import de interno ajeno desde $m: $ajenos"
    fi
done

# 4. Ningún archivo pasa de 200 líneas: el god class se ve antes de que duela.
grandes=$(find src/main/java -name '*.java' -exec wc -l {} + | awk '$2 != "total" && $1 > 200 {print $2" ("$1" líneas)"}')
if [ -n "$grandes" ]; then
    fallo "archivo con más de 200 líneas: $grandes"
fi

# 5. Ninguna interfaz con una sola implementación (salvo repositorios de Spring Data).
for i in $(grep -rl "^public interface" src/main/java --include='*.java' | grep -v Repository || true); do
    nombre=$(basename "$i" .java)
    n=$(grep -rl "implements $nombre\b" src/main/java --include='*.java' 2>/dev/null | wc -l)
    if [ "$n" -le 1 ]; then
        fallo "interfaz con $n implementación: $nombre (abstracción antes del segundo uso)"
    fi
done

# 6. La vista no decide transiciones: la máquina de estados vive en el dominio.
if grep -rqE "estado\.name\(\) *==|estado *== *'" src/main/resources/templates/ 2>/dev/null; then
    fallo "un template decide transiciones: la máquina de estados volvió a la vista"
    grep -rnE "estado\.name\(\) *==|estado *== *'" src/main/resources/templates/
fi

# 7. La capa de componentes solo usa roles: un color literal o de paleta ahí
#    rompe el intercambio de tema (el tema reescribe roles, no componentes).
capa=$(awk '/^:root \{/{en=1} en && /^\}/{en=0; next} !en' src/main/resources/static/css/app.css)
literales=$(printf '%s' "$capa" | grep -nE "#[0-9a-fA-F]{3,6}|var\(--(ocre|oliva|oliva-oscuro|oliva-activo|ardosia|ardosia-texto|teja|teja-texto|crema|papel|papel-claro|tinta)\)" || true)
if [ -n "$literales" ]; then
    fallo "la capa de componentes usa color literal o de paleta: el tema deja de ser intercambiable"
    printf '%s\n' "$literales" | head -5
fi

if [ "$fallas" -eq 0 ]; then
    echo "arquitectura: OK — fronteras, tamaño, abstracciones y vistas en regla"
fi
exit "$fallas"
