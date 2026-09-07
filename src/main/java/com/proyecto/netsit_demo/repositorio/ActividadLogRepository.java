package com.proyecto.netsit_demo.repositorio;

import com.proyecto.netsit_demo.modelo.ActividadLog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActividadLogRepository
        extends JpaRepository<ActividadLog, Long> {

    Page<ActividadLog> findAllByOrderByFechaHoraDesc(
            Pageable pageable
    );

    Page<ActividadLog> findByAccionIgnoreCaseOrderByFechaHoraDesc(
            String accion,
            Pageable pageable
    );

    Page<ActividadLog> findByUsuarioContainingIgnoreCaseOrderByFechaHoraDesc(
            String usuario,
            Pageable pageable
    );

    Page<ActividadLog> findByAccionIgnoreCaseAndUsuarioContainingIgnoreCaseOrderByFechaHoraDesc(
            String accion,
            String usuario,
            Pageable pageable
    );
}