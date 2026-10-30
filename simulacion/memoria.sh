#!/usr/bin/env bash
# Escenarios de memoria para grabar en video (asciinema / OBS).
# Uso: bash simulacion/memoria.sh
set -u
cd "$(dirname "$0")/.."
JAR=$(ls target/ecosolicitud-*.jar 2>/dev/null | head -1)
pausa() { echo; read -r -p "   [Enter para seguir] " _; }
titulo() { printf '\n========== %s ==========\n\n' "$1"; }

[ -z "$JAR" ] && { echo "No hay jar: corré mvn package -DskipTests primero"; exit 1; }

titulo "Escenario 1: heap imposible (-Xmx32m)"
echo "La JVM ni siquiera puede crear el contexto de Spring."
echo "java -Xmx32m -jar $JAR (base en memoria)"
pausa
timeout 60 java -Xmx32m -jar "$JAR" \
    --spring.datasource.url=jdbc:h2:mem:v1 --spring.jpa.hibernate.ddl-auto=create-drop \
    2>&1 | grep -iE "OutOfMemoryError|GC overhead|Started|failed" | head -4
echo "→ OutOfMemoryError visible en el log: muerte en el arranque."
pausa

titulo "Escenario 2: heap justo (-Xmx64m)"
echo "Arranca, pero queda sin margen para atender pedidos."
pausa
timeout 60 java -Xmx64m -jar "$JAR" --server.port=8099 \
    --spring.datasource.url=jdbc:h2:mem:v2 --spring.jpa.hibernate.ddl-auto=create-drop &
PID=$!
sleep 14
echo "La app respondió: $(curl -s -o /dev/null -w '%{http_code}' http://localhost:8099/ 2>/dev/null)"
kill $PID 2>/dev/null
echo "→ Arranca pero con ~0 MB de margen: bajo carga cae por GC overhead."
pausa

titulo "Escenario 3: heap dimensionado (-Xmx350m, como el plan de Render)"
echo "Mismo binario, memoria acorde: arranque normal y margen para tráfico."
pausa
timeout 60 java -Xmx350m -jar "$JAR" --server.port=8099 \
    --spring.datasource.url=jdbc:h2:mem:v3 --spring.jpa.hibernate.ddl-auto=create-drop &
PID=$!
sleep 14
echo "La app respondió: $(curl -s -o /dev/null -w '%{http_code}' http://localhost:8099/ 2>/dev/null)"
kill $PID 2>/dev/null
echo "→ En Render: JAVA_TOOL_OPTIONS=-Xmx350m y el contenedor deja de reiniciarse."
echo
echo "Fin de la grabación."
