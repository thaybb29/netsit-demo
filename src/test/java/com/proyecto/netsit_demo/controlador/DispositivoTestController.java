package com.proyecto.netsit_demo.controlador;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.proyecto.netsit_demo.repositorio.DispositivoRepository;
import com.proyecto.netsit_demo.servicio.PythonNetworkClientService;

@WebMvcTest(DispositivoController.class)
@AutoConfigureMockMvc(addFilters = false)
class DispositivoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DispositivoRepository dispositivoRepository;

    @MockitoBean
    private PythonNetworkClientService pythonClientService;

    @Test
    void debeListarDispositivos() throws Exception {

        when(dispositivoRepository.findAll())
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/dispositivos"))
                .andExpect(status().isOk());
    }
}