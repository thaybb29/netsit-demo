package com.proyecto.netsit_demo.controlador;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proyecto.netsit_demo.modelo.UmbralesTrafico;
import com.proyecto.netsit_demo.repositorio.ConfigTrafficRepository;;

@RestController
@RequestMapping("/api/network/configuracion")
public class ConfigTrafficController {

    private static final long CONFIG_ID = 1L;
    private static final double DEFAULT_ADVERTENCIA = 5.0;
    private static final double DEFAULT_CRITICO = 10.0;

    private final ConfigTrafficRepository repository;

    public ConfigTrafficController(ConfigTrafficRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<UmbralesTrafico> obtenerConfiguracion() {
        return ResponseEntity.ok(obtenerOCrearConfiguracion());
    }

    @PutMapping
    public ResponseEntity<?> guardarConfiguracion(@RequestBody Map<String, Object> body) {
        try {
            double advertencia = Double.parseDouble(String.valueOf(body.get("umbralAdvertencia")));
            double critico = Double.parseDouble(String.valueOf(body.get("umbralCritico")));

            if (!Double.isFinite(advertencia) || !Double.isFinite(critico) || advertencia < 0 || critico < 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Los umbrales deben ser números mayores o iguales a 0."));
            }

            if (critico < advertencia) {
                return ResponseEntity.badRequest().body(Map.of("error", "El umbral crítico debe ser mayor o igual al de advertencia."));
            }

            UmbralesTrafico config = obtenerOCrearConfiguracion();
            config.setUmbralAdvertencia(advertencia);
            config.setUmbralCritico(critico);
            config = repository.save(config);

            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "No se pudo guardar la configuración: " + e.getMessage()));
        }
    }

    private UmbralesTrafico obtenerOCrearConfiguracion() {
        return repository.findById(CONFIG_ID).orElseGet(() ->
                repository.save(new UmbralesTrafico(CONFIG_ID, DEFAULT_ADVERTENCIA, DEFAULT_CRITICO)));
    }
}
