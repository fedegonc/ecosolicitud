package com.ecosolicitud.shared;

import java.util.function.Supplier;

// Un mismo dato pedido varias veces dentro de un pedido HTTP (la organización
// activa la piden el controller, el listado, el menú y los avisos) se consulta
// una sola vez. El almacén vive lo que vive el pedido; sin almacén (fuera de
// un pedido) se calcula siempre. Un null no se guarda.
public final class Memo {

    public interface Almacen {
        Object leer(String clave);

        void guardar(String clave, Object valor);

        void borrar(String clave);
    }

    private Memo() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T obtener(Almacen almacen, String clave, Supplier<T> calculo) {
        if (almacen == null) {
            return calculo.get();
        }
        var guardado = almacen.leer(clave);
        if (guardado != null) {
            return (T) guardado;
        }
        var valor = calculo.get();
        if (valor != null) {
            almacen.guardar(clave, valor);
        }
        return valor;
    }

    // después de escribir el dato, para que el resto del pedido lea el nuevo
    public static void olvidar(Almacen almacen, String clave) {
        if (almacen != null) {
            almacen.borrar(clave);
        }
    }
}
