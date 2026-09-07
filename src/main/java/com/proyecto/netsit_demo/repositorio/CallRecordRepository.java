package com.proyecto.netsit_demo.repositorio;

import com.proyecto.netsit_demo.modelo.CallRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallRecordRepository extends JpaRepository<CallRecord, Long> {
}