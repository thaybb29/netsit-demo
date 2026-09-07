package com.proyecto.netsit_demo.controlador;

import com.proyecto.netsit_demo.modelo.CallInfo;
import com.proyecto.netsit_demo.modelo.CallRecord;
import com.proyecto.netsit_demo.modelo.ExtensionInfo;
import com.proyecto.netsit_demo.repositorio.CallRecordRepository;
import com.proyecto.netsit_demo.servicio.AsteriskMonitorService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/voip")
public class VoipController {

    private final AsteriskMonitorService monitorService;
    private final CallRecordRepository callRecordRepository;

    public VoipController(AsteriskMonitorService monitorService,
                           CallRecordRepository callRecordRepository) {
        this.monitorService = monitorService;
        this.callRecordRepository = callRecordRepository;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        List<ExtensionInfo> extensiones = monitorService.getExtensiones();
        long enLinea = extensiones.stream().filter(e -> "EN_LINEA".equals(e.getEstado())).count();

        Map<String, Object> resp = new HashMap<>();
        resp.put("conectado", monitorService.isConectado());
        resp.put("extensionesTotales", extensiones.size());
        resp.put("extensionesEnLinea", enLinea);
        resp.put("extensionesFueraDeLinea", extensiones.size() - enLinea);
        resp.put("llamadasActivas", monitorService.getLlamadasActivas().size());
        return resp;
    }

    @GetMapping("/extensions")
    public List<ExtensionInfo> extensions() {
        return monitorService.getExtensiones();
    }

    @GetMapping("/calls/active")
    public Collection<CallInfo> activeCalls() {
        return monitorService.getLlamadasActivas();
    }

    @GetMapping("/cdr")
    public List<CallRecord> cdr() {
        return callRecordRepository.findAll(
            PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "fechaHora"))
        ).getContent();
    }
}