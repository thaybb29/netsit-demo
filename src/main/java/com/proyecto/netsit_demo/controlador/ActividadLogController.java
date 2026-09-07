package com.proyecto.netsit_demo.controlador;

import com.proyecto.netsit_demo.modelo.ActividadLog;
import com.proyecto.netsit_demo.servicio.ActividadLogService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/diagnostico")
public class ActividadLogController {

    private final ActividadLogService actividadLogService;

    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> obtenerLogs(

            @RequestParam(
                    defaultValue = "0"
            )
            int page,

            @RequestParam(
                    defaultValue = "8"
            )
            int size,

            @RequestParam(
                    defaultValue = "Todos"
            )
            String filtro,

            @RequestParam(
                    defaultValue = ""
            )
            String usuario,

            Authentication authentication) {

        Page<ActividadLog> resultado =
                actividadLogService.obtenerLogs(
                        page,
                        size,
                        filtro,
                        usuario
                );

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "content",
                resultado.getContent()
        );

        response.put(
                "number",
                resultado.getNumber()
        );

        response.put(
                "size",
                resultado.getSize()
        );

        response.put(
                "totalElements",
                resultado.getTotalElements()
        );

        response.put(
                "totalPages",
                resultado.getTotalPages()
        );

        response.put(
                "first",
                resultado.isFirst()
        );

        response.put(
                "last",
                resultado.isLast()
        );

        if (authentication != null) {

            response.put(
                    "usuarioActual",
                    authentication.getName()
            );
        }

        return ResponseEntity.ok(response);
    }
}
