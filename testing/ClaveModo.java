import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// Utilidad local: la clave no aparece en pantalla, argumentos ni historial.
class ClaveModo {

    public static void main(String[] args) {
        var consola = System.console();
        if (consola == null) {
            throw new IllegalStateException("Ejecuta desde una terminal interactiva");
        }
        var clave = consola.readPassword("Nueva clave de administrador (12 caracteres minimo): ");
        if (clave == null) {
            return;
        }
        try {
            int bytes = StandardCharsets.UTF_8.encode(CharBuffer.wrap(clave)).remaining();
            if (clave.length < 12 || bytes > 72) {
                throw new IllegalArgumentException("Usa al menos 12 caracteres y hasta 72 bytes UTF-8");
            }
            System.out.println(new BCryptPasswordEncoder().encode(CharBuffer.wrap(clave)));
        } finally {
            Arrays.fill(clave, '\0');
        }
    }
}
