package com.proyecto.netsit_demo.modelo;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "actividad_logs")
public class ActividadLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String usuario;

    @Column(nullable = false, length = 30)
    private String accion;

    @Column(nullable = false, length = 255)
    private String ip;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false, length = 500)
    private String descripcion;

    public ActividadLog() {
    }

    public ActividadLog(
            String usuario,
            String accion,
            String ip,
            LocalDateTime fechaHora,
            String descripcion) {

        this.usuario = usuario;
        this.accion = accion;
        this.ip = ip;
        this.fechaHora = fechaHora;
        this.descripcion = descripcion;
    }

    public Long getId() {
        return id;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getAccion() {
        return accion;
    }

    public String getIp() {
        return ip;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}