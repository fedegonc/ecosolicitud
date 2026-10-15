package com.ecosolicitud;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// Días de recolección domiciliaria por zona, tal como figuran en el imán
// de la Intendencia de Rivera. Livramento y las localidades secundarias
// no publican días por barrio: no tienen zonas.
final class Recoleccion {

    enum Turno {
        MANANA, NOCHE
    }

    record Zona(int numero, Set<DayOfWeek> comun, Turno turnoComun,
            DayOfWeek reciclables, Turno turnoReciclables) {
    }

    // el próximo paso del camión a partir de hoy (hoy incluido)
    record Paso(DayOfWeek dia, Turno turno, boolean hoy) {
    }

    private static final Set<DayOfWeek> LMJVS = EnumSet.of(MONDAY, TUESDAY, THURSDAY, FRIDAY, SATURDAY);
    private static final Set<DayOfWeek> LMIJVS = EnumSet.of(MONDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY);
    private static final Set<DayOfWeek> MJS = EnumSet.of(TUESDAY, THURSDAY, SATURDAY);
    private static final Set<DayOfWeek> LMV = EnumSet.of(MONDAY, WEDNESDAY, FRIDAY);

    private static final List<Zona> RIVERA = List.of(
            new Zona(1, LMJVS, Turno.NOCHE, WEDNESDAY, Turno.NOCHE),
            new Zona(2, LMIJVS, Turno.NOCHE, TUESDAY, Turno.NOCHE),
            new Zona(3, MJS, Turno.NOCHE, WEDNESDAY, Turno.NOCHE),
            new Zona(4, LMV, Turno.NOCHE, TUESDAY, Turno.MANANA),
            new Zona(5, LMV, Turno.NOCHE, THURSDAY, Turno.NOCHE),
            new Zona(6, LMV, Turno.MANANA, TUESDAY, Turno.MANANA),
            new Zona(7, MJS, Turno.MANANA, MONDAY, Turno.MANANA),
            new Zona(8, MJS, Turno.MANANA, WEDNESDAY, Turno.MANANA),
            new Zona(9, LMV, Turno.MANANA, THURSDAY, Turno.MANANA),
            new Zona(10, MJS, Turno.NOCHE, MONDAY, Turno.NOCHE),
            new Zona(11, MJS, Turno.MANANA, FRIDAY, Turno.MANANA),
            new Zona(12, LMV, Turno.MANANA, THURSDAY, Turno.MANANA));

    private Recoleccion() {
    }

    static List<Zona> zonas(Localidad localidad) {
        return localidad == Localidad.RIVERA ? RIVERA : List.of();
    }

    static Optional<Zona> zona(Localidad localidad, Integer numero) {
        return zonas(localidad).stream().filter(z -> numero != null && z.numero() == numero).findFirst();
    }

    static Paso comun(Zona zona, DayOfWeek hoy) {
        var dia = hoy;
        while (!zona.comun().contains(dia)) {
            dia = dia.plus(1);
        }
        return new Paso(dia, zona.turnoComun(), dia == hoy);
    }

    static Paso reciclables(Zona zona, DayOfWeek hoy) {
        return new Paso(zona.reciclables(), zona.turnoReciclables(), zona.reciclables() == hoy);
    }
}
