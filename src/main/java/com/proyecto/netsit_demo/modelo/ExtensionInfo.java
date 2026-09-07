package com.proyecto.netsit_demo.modelo;

public class ExtensionInfo {
    private String extension;
    private String nombre;
    private String estado;

    public ExtensionInfo(String extension, String nombre, String estado) {
        this.extension = extension;
        this.nombre = nombre;
        this.estado = estado;
    }
    public String getExtension() { return extension; }
    public String getNombre() { return nombre; }
    public String getEstado() { return estado; }
}