package com.proyecto.netsit_demo.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "dispositivos",
    indexes = {
        @Index(name = "idx_dispositivo_ip", columnList = "ip"),
        @Index(name = "idx_dispositivo_mac", columnList = "mac")
    }
)
public class Dispositivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nombre;

    @Column(nullable = false, length = 45)
    private String ip;

    @Column(length = 50)
    private String mac;

    @Column(length = 100)
    private String hostname;

    @Column(length = 150)
    private String fabricante;

    @Column(nullable = false, length = 20)
    private String estado = "INACTIVO";

    @Column(nullable = false, length = 20)
    private String estadoAutorizacion = "DESCONOCIDO";

    private LocalDateTime ultimaConexion;

    public Dispositivo() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getMac() { return mac; }
    public void setMac(String mac) { this.mac = mac; }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public String getFabricante() { return fabricante; }
    public void setFabricante(String fabricante) { this.fabricante = fabricante; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getEstadoAutorizacion() { return estadoAutorizacion; }
    public void setEstadoAutorizacion(String estadoAutorizacion) { this.estadoAutorizacion = estadoAutorizacion; }

    public LocalDateTime getUltimaConexion() { return ultimaConexion; }
    public void setUltimaConexion(LocalDateTime ultimaConexion) { this.ultimaConexion = ultimaConexion; }
}