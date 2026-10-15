package com.ecosolicitud.solicitud.interno;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.ecosolicitud.solicitud.Resultado;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

final class Respuestas {

    private Respuestas() {
    }

    // La versión puede cambiar entre el findById y el flush dentro de la TX:
    // ahí el servicio no puede devolver CONFLICTO (la TX ya quedó rollback-only),
    // la excepción cruza el proxy transaccional y se responde acá.
    static Resultado intentar(Supplier<Resultado> accion) {
        try {
            return accion.get();
        } catch (OptimisticLockingFailureException | UnexpectedRollbackException e) {
            return Resultado.CONFLICTO;
        }
    }

    // POST clásico: post-redirect-get con el resultado en atributos flash
    static String intentar(Supplier<Resultado> accion, String codigoOk, long id,
            String destino, RedirectAttributes redir) {
        return responder(intentar(accion), codigoOk, id, destino, redir);
    }

    static String responder(Resultado resultado, String codigoOk, long id, String destino,
            RedirectAttributes redir) {
        avisar(resultado, codigoOk, id, redir::addFlashAttribute);
        return "redirect:" + destino;
    }

    // el aviso del resultado como atributos: flash para el redirect,
    // de modelo cuando la respuesta son fragmentos para htmx
    static void avisar(Resultado resultado, String codigoOk, long id,
            BiConsumer<String, Object> poner) {
        switch (resultado) {
            case OK -> {
                poner.accept("avisoCodigo", codigoOk);
                poner.accept("avisoId", id);
            }
            case INVALIDA, CONFLICTO -> {
                poner.accept("alertaCodigo",
                        resultado == Resultado.INVALIDA ? "accion.invalida" : "accion.conflicto");
                poner.accept("alertaId", id);
            }
            case NO_ENCONTRADA ->
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            case SIN_PERMISO ->
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
