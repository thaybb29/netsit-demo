package com.proyecto.netsit_demo.controlador;

import com.proyecto.netsit_demo.modelo.Dispositivo;
import com.proyecto.netsit_demo.repositorio.DispositivoRepository;
import com.proyecto.netsit_demo.servicio.PythonNetworkClientService;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dispositivos")
public class DispositivoController {

    private final PythonNetworkClientService pythonClientService;
    private final DispositivoRepository dispositivoRepository;

    public DispositivoController(
            PythonNetworkClientService pythonClientService,
            DispositivoRepository dispositivoRepository) {

        this.pythonClientService = pythonClientService;
        this.dispositivoRepository = dispositivoRepository;
    }

    @GetMapping
    public ResponseEntity<List<Dispositivo>> listarDispositivos() {

        List<Dispositivo> dispositivos =
                dispositivoRepository.findAll();

        return ResponseEntity.ok(dispositivos);
    }

    @PostMapping
    public ResponseEntity<?> registrarDispositivo(
            @RequestBody Dispositivo dispositivo) {

        try {

            if (dispositivo.getIp() == null ||
                dispositivo.getIp().trim().isEmpty()) {

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "La dirección IP es obligatoria."
                        ));
            }

            if (dispositivoRepository.existsByIp(dispositivo.getIp())) {

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "Ya existe un dispositivo registrado con esa IP."
                        ));
            }

            if (dispositivo.getEstado() == null ||
                dispositivo.getEstado().trim().isEmpty()) {

                dispositivo.setEstado("INACTIVO");
            }

            if (dispositivo.getEstadoAutorizacion() == null ||
                dispositivo.getEstadoAutorizacion().trim().isEmpty()) {

                dispositivo.setEstadoAutorizacion("DESCONOCIDO");
            }

            Dispositivo nuevoDispositivo =
                    dispositivoRepository.save(dispositivo);

            return ResponseEntity.ok(nuevoDispositivo);

        } catch (Exception e) {

            return ResponseEntity.status(500)
                    .body(Map.of(
                            "error",
                            e.getMessage() != null
                                    ? e.getMessage()
                                    : "Error interno del servidor."
                    ));
        }
    }

    @PutMapping("/{id}/autorizacion")
    public ResponseEntity<?> cambiarAutorizacion(
            @PathVariable Long id,
            @RequestBody Map<String, String> datos) {

        try {

            Dispositivo dispositivo =
                    dispositivoRepository.findById(id)
                            .orElse(null);

            if (dispositivo == null) {

                return ResponseEntity.notFound().build();
            }

            String estado = datos.get("estado");

            if (estado == null ||
                (
                    !estado.equals("AUTORIZADO") &&
                    !estado.equals("NO_AUTORIZADO") &&
                    !estado.equals("DESCONOCIDO")
                )) {

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "Estado de autorización inválido."
                        ));
            }

            dispositivo.setEstadoAutorizacion(estado);

            Dispositivo actualizado =
                    dispositivoRepository.save(dispositivo);

            return ResponseEntity.ok(actualizado);

        } catch (Exception e) {

            return ResponseEntity.status(500)
                    .body(Map.of(
                            "error",
                            e.getMessage() != null
                                    ? e.getMessage()
                                    : "Error interno del servidor."
                    ));
        }
    }

    @PostMapping("/escanear")
    public ResponseEntity<?> escanearRed(
            @RequestBody Map<String, String> payload) {

        try {

            String subnet = payload.get("subnet");

            if (subnet == null ||
                subnet.trim().isEmpty()) {

                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "La subred es obligatoria."
                        ));
            }

            JsonNode resultado =
                    pythonClientService.escanearRed(subnet);

            return ResponseEntity.ok(resultado);

        } catch (Exception e) {

            return ResponseEntity.status(500)
                    .body(Map.of(
                            "error",
                            e.getMessage() != null
                                    ? e.getMessage()
                                    : "Error al escanear la red."
                    ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarDispositivo(
            @PathVariable Long id) {

        try {

            if (!dispositivoRepository.existsById(id)) {

                return ResponseEntity.notFound().build();
            }

            dispositivoRepository.deleteById(id);

            return ResponseEntity.ok(
                    Map.of(
                            "mensaje",
                            "Dispositivo eliminado correctamente."
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.status(500)
                    .body(Map.of(
                            "error",
                            e.getMessage() != null
                                    ? e.getMessage()
                                    : "Error al eliminar el dispositivo."
                    ));
        }
    }
}
