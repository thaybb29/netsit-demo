package com.proyecto.netsit_demo.repositorio;

import com.proyecto.netsit_demo.modelo.Qos;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface QosRepository extends JpaRepository<Qos, Long> {

    List<Qos> findByMeasuredAtAfterOrderByMeasuredAtDesc(
            LocalDateTime date
    );

    List<Qos> findByHostOrderByMeasuredAtDesc(
            String host
    );
}