package com.proyecto.netsit_demo.controlador;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proyecto.netsit_demo.servicio.NetworkTrafficService;

@RestController
@RequestMapping("/api/network")
public class NetworkTrafficController {

    private static final Logger logger = LoggerFactory.getLogger(NetworkTrafficController.class);

    private final NetworkTrafficService service;

    public NetworkTrafficController(NetworkTrafficService service) {
        this.service = service;
    }

    @GetMapping("/data")
    public ResponseEntity<NetworkTrafficService.TrafficData> getCurrentData() {
        return ResponseEntity.ok(service.getCurrentTrafficData());
    }

    @PostMapping("/start")
    public ResponseEntity<String> startCapturing() {
        try {
            service.startCapturing();
            return ResponseEntity.ok("Captura de tráfico iniciada");
        } catch (Exception e) {
            logger.error("Error al iniciar la captura de tráfico", e);
            return ResponseEntity.internalServerError()
                    .body("Error al iniciar la captura: " + e.getMessage());
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<String> stopCapturing() {
        service.stopCapturing();
        return ResponseEntity.ok("Captura de tráfico detenida");
    }

    @PostMapping("/reset")
    public ResponseEntity<String> resetCounter() {
        service.resetCounter();
        return ResponseEntity.ok("Contador reiniciado");
    }

    @GetMapping("/interfaces")
    public ResponseEntity<List<String>> listInterfaces() {
        return ResponseEntity.ok(service.listInterfaces());
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "capturando", service.isCapturing()
        ));
    }
}
