package com.proyecto.netsit_demo.modelo;

import java.time.LocalDateTime;

public class CallInfo {

    private String canal;
    private String numeroOrigen;
    private LocalDateTime inicio;

    public CallInfo(String canal, String numeroOrigen, LocalDateTime inicio) {
        this.canal = canal;
        this.numeroOrigen = numeroOrigen;
        this.inicio = inicio;
    }

    public String getCanal() { return canal; }
    public String getNumeroOrigen() { return numeroOrigen; }
    public LocalDateTime getInicio() { return inicio; }
}