package com.ecosolicitud.solicitud.interno;

import com.ecosolicitud.solicitud.Resultado;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

final class Respuestas {

    private Respuestas() {
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
