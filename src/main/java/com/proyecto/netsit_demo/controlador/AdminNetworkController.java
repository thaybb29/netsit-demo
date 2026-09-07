package com.proyecto.netsit_demo.controlador;

import com.proyecto.netsit_demo.servicio.ActividadLogService;
import com.proyecto.netsit_demo.servicio.NetworkService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AdminNetworkController {

    private final NetworkService networkService;
    private final ActividadLogService actividadLogService;

    @GetMapping("/admin/dashboard")
    public String dashboard() {
        return "admin";
    }

    @PostMapping("/api/diagnostico/ping")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> ping(
            @RequestParam("ipAddress") String ipAddress,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        if (ipAddress == null || ipAddress.trim().isEmpty()) {

            response.put("success", false);
            response.put("error", true);
            response.put(
                    "output",
                    "Debe introducir una IP o dominio."
            );

            return ResponseEntity.badRequest().body(response);
        }

        String host = ipAddress.trim();

        String usuario = "Desconocido";

        if (authentication != null) {

            String nombreUsuario = authentication.getName();

            if (nombreUsuario != null &&
                    !nombreUsuario.trim().isEmpty()) {

                usuario = nombreUsuario.trim();
            }
        }

        try {
            String output =
                    networkService.executePing(host);

            response.put("success", true);
            response.put("error", false);
            response.put("host", host);
            response.put("output", output);

            actividadLogService.registrar(
                    usuario,
                    "PING",
                    host,
                    "Ping ejecutado correctamente"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            String mensajeError = e.getMessage();

            if (mensajeError == null ||
                    mensajeError.trim().isEmpty()) {

                mensajeError = "Error desconocido";
            }

            actividadLogService.registrar(
                    usuario,
                    "PING",
                    host,
                    "Error al ejecutar Ping: " + mensajeError
            );

            response.put("success", false);
            response.put("error", true);
            response.put(
                    "output",
                    "Error ejecutando ping:\n\n"
                            + mensajeError
            );

            return ResponseEntity
                    .internalServerError()
                    .body(response);
        }
    }

    @PostMapping("/api/diagnostico/portscan")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> portScan(
            @RequestParam("ipAddressScan") String ipAddressScan,
            @RequestParam(
                    value = "portRange",
                    required = false
            ) String portRange,
            Authentication authentication) {

        if (ipAddressScan == null ||
                ipAddressScan.trim().isEmpty()) {

            Map<String, Object> error =
                    new HashMap<>();

            error.put("success", false);
            error.put("error", true);
            error.put(
                    "output",
                    "Debe introducir una IP o dominio."
            );

            return ResponseEntity
                    .badRequest()
                    .body(error);
        }

        String host = ipAddressScan.trim();

        String rango;

        if (portRange == null ||
                portRange.trim().isEmpty()) {

            rango = "1-1024";

        } else {

            rango = portRange.trim();
        }

        String usuario = "Desconocido";

        if (authentication != null) {

            String nombreUsuario = authentication.getName();

            if (nombreUsuario != null &&
                    !nombreUsuario.trim().isEmpty()) {

                usuario = nombreUsuario.trim();
            }
        }

        try {
            Map<String, Object> result =
                    networkService.scanPorts(
                            host,
                            rango
                    );

            if (Boolean.TRUE.equals(
                    result.get("error"))) {

                actividadLogService.registrar(
                        usuario,
                        "ESCANEO",
                        host,
                        "Error en escaneo de puertos. "
                                + "Rango: " + rango
                );

                return ResponseEntity
                        .badRequest()
                        .body(result);
            }

            actividadLogService.registrar(
                    usuario,
                    "ESCANEO",
                    host,
                    "Escaneo de puertos ejecutado. "
                            + "Rango: " + rango
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {

            String mensajeError = e.getMessage();

            if (mensajeError == null ||
                    mensajeError.trim().isEmpty()) {

                mensajeError = "Error desconocido";
            }

            actividadLogService.registrar(
                    usuario,
                    "ESCANEO",
                    host,
                    "Error ejecutando escaneo. "
                            + "Rango: " + rango
            );

            Map<String, Object> error =
                    new HashMap<>();

            error.put("success", false);
            error.put("error", true);
            error.put(
                    "output",
                    "Error ejecutando Nmap:\n\n"
                            + mensajeError
            );

            return ResponseEntity
                    .internalServerError()
                    .body(error);
        }
    }

    @GetMapping({"/", "/admin"})
    public String panel() {
        return "admin";
    }
}