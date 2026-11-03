package com.ecosolicitud.solicitud;

import java.time.Instant;
import java.util.List;
import java.util.stream.LongStream;

import com.ecosolicitud.organizacion.OrganizacionService;
import com.ecosolicitud.shared.Actor;
import com.ecosolicitud.shared.Rol;
import com.ecosolicitud.solicitud.interno.Aviso;
import com.ecosolicitud.solicitud.interno.AvisoRepository;
import com.ecosolicitud.solicitud.interno.SolicitudRepository;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AvisoServiceTest {

    private final AvisoRepository avisos = mock(AvisoRepository.class);
    private final SolicitudRepository solicitudes = mock(SolicitudRepository.class);
    private final OrganizacionService organizaciones = mock(OrganizacionService.class);
    private final AvisoService servicio = new AvisoService(avisos, solicitudes, organizaciones);
    private final Actor vecino = new Actor(Rol.CIUDADANO, "vecino", "Vecino", null);

    @Test
    void unaLecturaYUnaActualizacionPorPaginaSinRecargarParaLasColumnas() {
        var pedido = PageRequest.of(0, 12);
        var lista = LongStream.rangeClosed(1, 12).mapToObj(id -> {
            var aviso = new Aviso("vecino", null, TipoAviso.ACEPTADA, "Centro", Instant.now());
            ReflectionTestUtils.setField(aviso, "id", id);
            return aviso;
        }).toList();
        when(avisos.findByDestinoOrderByCreadaEnDescIdDesc("vecino", pedido))
                .thenReturn(new PageImpl<>(lista, pedido, 1000));

        var pagina = servicio.avisosPara(vecino, 1);
        var columnas = servicio.kanbanPara(pagina.getContent());

        assertThat(pagina.getContent()).hasSize(12).allMatch(a -> !a.leida());
        assertThat(pagina.getTotalElements()).isEqualTo(1000);
        assertThat(columnas.getFirst().avisos()).hasSize(12);
        assertThat(columnas.getLast().avisos()).isEmpty();
        verify(avisos).findByDestinoOrderByCreadaEnDescIdDesc("vecino", pedido);
        verify(avisos).marcarLeidas("vecino", LongStream.rangeClosed(1, 12).boxed().toList());
        verifyNoMoreInteractions(avisos);
        verifyNoInteractions(solicitudes, organizaciones);
    }

    @Test
    void noEscribeNiConsultaSolicitudesSiLaPaginaEstaVacia() {
        var pedido = PageRequest.of(0, 12);
        when(avisos.findByDestinoOrderByCreadaEnDescIdDesc("vecino", pedido))
                .thenReturn(Page.empty(pedido));
        assertThat(servicio.avisosPara(vecino, 1)).isEmpty();
        verify(avisos).findByDestinoOrderByCreadaEnDescIdDesc("vecino", pedido);
        verifyNoMoreInteractions(avisos);
        verifyNoInteractions(solicitudes, organizaciones);
    }

    @Test
    void noActualizaLosAvisosQueYaEstanLeidos() {
        var pedido = PageRequest.of(0, 12);
        var aviso = new Aviso("vecino", null, TipoAviso.ACEPTADA, "Centro", Instant.now());
        ReflectionTestUtils.setField(aviso, "id", 1L);
        aviso.marcarLeida();
        when(avisos.findByDestinoOrderByCreadaEnDescIdDesc("vecino", pedido))
                .thenReturn(new PageImpl<>(List.of(aviso), pedido, 1));
        assertThat(servicio.avisosPara(vecino, 1).getContent()).allMatch(AvisoInfo::leida);
        verify(avisos).findByDestinoOrderByCreadaEnDescIdDesc("vecino", pedido);
        verifyNoMoreInteractions(avisos);
    }
}
