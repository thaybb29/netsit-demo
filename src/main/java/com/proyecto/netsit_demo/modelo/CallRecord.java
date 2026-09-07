package com.proyecto.netsit_demo.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "call_records")
public class CallRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String origen;
    private String destino;
    private LocalDateTime fechaHora;
    private Integer duracionSegundos;
    private String estado; // ACTIVA, FINALIZADA

    public CallRecord() {}

    public CallRecord(String origen, String destino, LocalDateTime fechaHora,
                       Integer duracionSegundos, String estado) {
        this.origen = origen;
        this.destino = destino;
        this.fechaHora = fechaHora;
        this.duracionSegundos = duracionSegundos;
        this.estado = estado;
    }

    // Getters y setters
    public Long getId() { return id; }
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public Integer getDuracionSegundos() { return duracionSegundos; }
    public void setDuracionSegundos(Integer duracionSegundos) { this.duracionSegundos = duracionSegundos; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}