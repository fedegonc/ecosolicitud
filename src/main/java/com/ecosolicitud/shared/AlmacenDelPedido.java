package com.ecosolicitud.shared;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

// El almacén de Memo para el pedido HTTP en curso: los atributos del request.
// Fuera de un pedido (arranque, tareas) no hay almacén y Memo calcula siempre.
public final class AlmacenDelPedido {

    private static final String PREFIJO = "memo:";

    private AlmacenDelPedido() {
    }

    public static Memo.Almacen actual() {
        var atributos = RequestContextHolder.getRequestAttributes();
        if (atributos == null) {
            return null;
        }
        return new Memo.Almacen() {
            @Override
            public Object leer(String clave) {
                return atributos.getAttribute(PREFIJO + clave, RequestAttributes.SCOPE_REQUEST);
            }

            @Override
            public void guardar(String clave, Object valor) {
                atributos.setAttribute(PREFIJO + clave, valor, RequestAttributes.SCOPE_REQUEST);
            }

            @Override
            public void borrar(String clave) {
                atributos.removeAttribute(PREFIJO + clave, RequestAttributes.SCOPE_REQUEST);
            }
        };
    }

    // todo lo que empiece con el prefijo: reemplazar un dataset entero invalida
    // claves que no se conocen una por una
    public static void olvidarTodo(String prefijo) {
        var atributos = RequestContextHolder.getRequestAttributes();
        if (atributos == null) {
            return;
        }
        for (var nombre : atributos.getAttributeNames(RequestAttributes.SCOPE_REQUEST)) {
            if (nombre.startsWith(PREFIJO + prefijo)) {
                atributos.removeAttribute(nombre, RequestAttributes.SCOPE_REQUEST);
            }
        }
    }
}
