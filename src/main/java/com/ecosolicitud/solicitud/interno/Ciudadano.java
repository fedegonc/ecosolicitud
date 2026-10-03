package com.ecosolicitud.solicitud.interno;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ciudadanos")
@Getter
@Setter
public class Ciudadano {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 80)
    private String nombre;
}
