#!/usr/bin/env bash
# Reglas de arquitectura que se verifican solas.
# Corren en CI (push/PR) y local: bash .github/scripts/arquitectura.sh
# Cada regla existe porque ya se violó una vez o porque el costo de violarla es alto.
set -uo pipefail
cd "$(dirname "$0")/../.."

fallas=0
fallo() { echo "::error::$1"; fallas=1; }
modulos=(shared organizacion solicitud demo)

# 1. Ningún módulo importa la raíz: el gradiente va de específico a general.
if grep -rqE "^import com\.ecosolicitud\.[A-Z]" "src/main/java/com/ecosolicitud/${modulos[0]}/" \
        src/main/java/com/ecosolicitud/organizacion/ src/main/java/com/ecosolicitud/solicitud/ \
        src/main/java/com/ecosolicitud/demo/ 2>/dev/null; then
    fallo "un módulo importa la raíz (com.ecosolicitud.X): el gradiente se invirtió"
    grep -rnE "^import com\.ecosolicitud\.[A-Z]" src/main/java/com/ecosolicitud/*/ | grep -v "/interno/"
fi

# 2. shared es hoja: si importa un módulo, dejó de ser la capa general.
if grep -rqE "^import com\.ecosolicitud\.(organizacion|solicitud|demo)" src/main/java/com/ecosolicitud/shared/ 2>/dev/null; then
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

if [ "$fallas" -eq 0 ]; then
    echo "arquitectura: OK — fronteras, tamaño, abstracciones y vistas en regla"
fi
exit "$fallas"
