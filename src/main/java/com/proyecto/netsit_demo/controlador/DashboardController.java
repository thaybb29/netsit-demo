package com.proyecto.netsit_demo.controlador;

import com.proyecto.netsit_demo.modelo.CallInfo;
import com.proyecto.netsit_demo.servicio.AsteriskMonitorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collection;

@Controller
public class DashboardController {

    private final AsteriskMonitorService monitorService;

    public DashboardController(AsteriskMonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("llamadasActivas", monitorService.getLlamadasActivas());
        return "dashboard"; // src/main/resources/templates/dashboard.html
    }

    @GetMapping("/api/calls/active")
    @ResponseBody
    public Collection<CallInfo> llamadasActivasJson() {
        return monitorService.getLlamadasActivas();
    }
}