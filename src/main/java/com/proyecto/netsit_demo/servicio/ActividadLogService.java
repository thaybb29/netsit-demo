package com.proyecto.netsit_demo.servicio;

import com.proyecto.netsit_demo.modelo.ActividadLog;
import com.proyecto.netsit_demo.repositorio.ActividadLogRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ActividadLogService {

    private final ActividadLogRepository actividadLogRepository;

    public ActividadLogService(
            ActividadLogRepository actividadLogRepository) {

        this.actividadLogRepository =
                actividadLogRepository;
    }

    public void registrar(
            String usuario,
            String accion,
            String ip,
            String descripcion) {

        if (usuario == null || usuario.trim().isEmpty()) {
            usuario = "Desconocido";
        }

        if (accion == null || accion.trim().isEmpty()) {
            accion = "OTRO";
        }

        if (ip == null || ip.trim().isEmpty()) {
            ip = "-";
        }

        if (descripcion == null ||
                descripcion.trim().isEmpty()) {

            descripcion = "Actividad registrada";
        }

        ActividadLog log =
                new ActividadLog(
                        usuario.trim(),
                        accion.trim().toUpperCase(),
                        ip.trim(),
                        LocalDateTime.now(),
                        descripcion.trim()
                );

        actividadLogRepository.save(log);
    }

    public Page<ActividadLog> obtenerLogs(
            int page,
            int size,
            String filtro,
            String usuario) {

        if (page < 0) {
            page = 0;
        }

        if (size < 1) {
            size = 8;
        }

        // Máximo 100 para evitar consultas excesivas.
        if (size > 100) {
            size = 100;
        }

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "fechaHora"
                        )
                );

        boolean tieneFiltro =
                filtro != null &&
                !filtro.trim().isEmpty() &&
                !filtro.equalsIgnoreCase("Todos");

        boolean tieneUsuario =
                usuario != null &&
                !usuario.trim().isEmpty();

        if (tieneFiltro && tieneUsuario) {

            return actividadLogRepository
                    .findByAccionIgnoreCaseAndUsuarioContainingIgnoreCaseOrderByFechaHoraDesc(
                            normalizarAccion(filtro),
                            usuario == null ? "" : usuario.trim(),
                            pageable
                    );
        }

        if (tieneFiltro) {

            return actividadLogRepository
                    .findByAccionIgnoreCaseOrderByFechaHoraDesc(
                            normalizarAccion(filtro),
                            pageable
                    );
        }

        if (tieneUsuario) {

            return actividadLogRepository
                    .findByUsuarioContainingIgnoreCaseOrderByFechaHoraDesc(
                            usuario == null ? "" : usuario.trim(),
                            pageable
                    );
        }

        return actividadLogRepository
                .findAllByOrderByFechaHoraDesc(pageable);
    }

    private String normalizarAccion(String filtro) {

        if (filtro == null) {
            return "";
        }

        if (filtro.equalsIgnoreCase("Escaneos")) {
            return "ESCANEO";
        }

        if (filtro.equalsIgnoreCase("Escaneo")) {
            return "ESCANEO";
        }

        if (filtro.equalsIgnoreCase("Ping")) {
            return "PING";
        }

        return filtro.trim().toUpperCase();
    }
}
