package com.proyecto.netsit_demo.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class NetworkTrafficServiceTest {

    @Test
    void debeObtenerDatosDeTraficoIniciales() {

        NetworkTrafficService service = new NetworkTrafficService();

        NetworkTrafficService.TrafficData datos =
                service.getCurrentTrafficData();

        assertNotNull(datos);
        assertEquals(0.0, datos.getMbps());
        assertNotNull(datos.getProtocolo());
        assertFalse(datos.isActivo());
    }
}