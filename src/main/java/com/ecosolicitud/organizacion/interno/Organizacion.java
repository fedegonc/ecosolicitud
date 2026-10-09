package com.ecosolicitud.organizacion.interno;

import java.util.ArrayList;
import java.util.List;

import com.ecosolicitud.shared.Ubicacion;
import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Material;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "organizaciones")
@Getter
public class Organizacion {

    // el patrón vive acá (la regla es del dominio); PerfilForm lo referencia
    static final String PATRON_TELEFONO =
            "^(?=(?:\\D*\\d){8,})\\+?[0-9(][0-9 ()\\-.]{4,28}[0-9]$";

    @Id
    private String id;

    private String nombre;

    @Enumerated(EnumType.STRING)
    private Ciudad ciudad;

    @NotEmpty
    @BatchSize(size = 50)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "organizacion_materiales",
            joinColumns = @JoinColumn(name = "organizacion_id"),
            inverseJoinColumns = @JoinColumn(name = "material_id"))
    @OrderColumn(name = "orden")
    private List<Material> materiales = new ArrayList<>();

    @NotBlank(message = "{perfil.error.horario}")
    @Size(max = 80, message = "{perfil.error.horario}")
    @Column(nullable = false, length = 80)
    private String horario;

    @NotBlank(message = "{perfil.error.telefono}")
    @Size(max = 30, message = "{perfil.error.telefono}")
    @Pattern(regexp = PATRON_TELEFONO, message = "{perfil.error.telefono}")
    @Column(nullable = false, length = 30)
    private String telefono;

    // ambas null o ambas presentes: un centro puede no tener ubicación cargada
    private Double latitud;
    private Double longitud;

    @Version
    private long version;

    protected Organizacion() {
    }

    public Organizacion(String id, String nombre, Ciudad ciudad,
            List<Material> materiales, String horario, String telefono, Ubicacion ubicacion) {
        this.id = id;
        this.nombre = nombre;
        this.ciudad = ciudad;
        this.materiales = new ArrayList<>(materiales);
        this.horario = horario;
        this.telefono = telefono;
        if (ubicacion != null) {
            this.latitud = ubicacion.latitud();
            this.longitud = ubicacion.longitud();
        }
    }

    public Ubicacion ubicacion() {
        return latitud == null || longitud == null ? null : new Ubicacion(latitud, longitud);
    }

    public void actualizar(List<Material> materiales, String horario, String telefono,
            Ubicacion ubicacion) {
        this.materiales = new ArrayList<>(materiales);
        this.horario = horario;
        this.telefono = telefono;
        this.latitud = ubicacion == null ? null : ubicacion.latitud();
        this.longitud = ubicacion == null ? null : ubicacion.longitud();
    }
}
