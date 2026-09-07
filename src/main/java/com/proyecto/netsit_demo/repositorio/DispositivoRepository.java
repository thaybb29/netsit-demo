package com.proyecto.netsit_demo.repositorio;

import com.proyecto.netsit_demo.modelo.Dispositivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DispositivoRepository extends JpaRepository<Dispositivo, Long> {

    boolean existsByIp(String ip);

    Optional<Dispositivo> findByIp(String ip);
}