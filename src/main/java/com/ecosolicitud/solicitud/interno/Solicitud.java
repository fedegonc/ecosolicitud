package com.ecosolicitud.solicitud.interno;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.ecosolicitud.shared.Ciudad;
import com.ecosolicitud.shared.Material;
import com.ecosolicitud.solicitud.Estado;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "solicitudes")
@Getter
@Setter
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String ciudadanoId;
    @Enumerated(EnumType.STRING)
    private Ciudad ciudad;
    private String direccion;
    private String referencia;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "solicitud_materiales", joinColumns = @JoinColumn(name = "solicitud_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "material")
    private List<Material> materiales = new ArrayList<>();
    private String organizacionId;
    private String nota;
    @Enumerated(EnumType.STRING)
    private Estado estado;
    private Instant creadaEn;
    private Instant finalizadaEn;
    @Version
    private long version;
}
