package com.ecosolicitud.organizacion.interno;

import java.util.ArrayList;
import java.util.List;

import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Material;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;
import lombok.Setter;

@Entity
@Table(name = "organizaciones")
@Getter
@Setter
public class Organizacion {

    @Id
    private String id;

    private String nombre;

    @Enumerated(EnumType.STRING)
    private Ciudad ciudad;

    @BatchSize(size = 50)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "organizacion_materiales",
            joinColumns = @JoinColumn(name = "organizacion_id"))
    @OrderColumn(name = "orden")
    @Enumerated(EnumType.STRING)
    @Column(name = "material")
    private List<Material> materiales = new ArrayList<>();

    private String horario;

    @Column(nullable = false, length = 30)
    private String telefono;

    @Version
    private long version;
}
