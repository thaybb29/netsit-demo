package com.proyecto.netsit_demo.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.proyecto.netsit_demo.modelo.UmbralesTrafico;

public interface ConfigTrafficRepository extends JpaRepository<UmbralesTrafico, Long> {
}
