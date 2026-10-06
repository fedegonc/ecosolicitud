package com.ecosolicitud.solicitud.interno;

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
    static String intentar(Supplier<Resultado> accion, String codigoOk, long id,
            String destino, RedirectAttributes redir) {
        Resultado resultado;
        try {
            resultado = accion.get();
        } catch (OptimisticLockingFailureException | UnexpectedRollbackException e) {
            resultado = Resultado.CONFLICTO;
        }
        return responder(resultado, codigoOk, id, destino, redir);
    }

    static String responder(Resultado resultado, String codigoOk, long id, String destino,
            RedirectAttributes redir) {
        switch (resultado) {
            case OK -> {
                redir.addFlashAttribute("avisoCodigo", codigoOk);
                redir.addFlashAttribute("avisoId", id);
            }
            case INVALIDA, CONFLICTO -> {
                redir.addFlashAttribute("alertaCodigo",
                        resultado == Resultado.INVALIDA ? "accion.invalida" : "accion.conflicto");
                redir.addFlashAttribute("alertaId", id);
            }
            case NO_ENCONTRADA ->
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            case SIN_PERMISO ->
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return "redirect:" + destino;
    }
}
