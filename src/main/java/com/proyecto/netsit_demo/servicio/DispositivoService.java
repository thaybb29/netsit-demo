package com.proyecto.netsit_demo.servicio;

import com.proyecto.netsit_demo.modelo.Dispositivo;
import com.proyecto.netsit_demo.repositorio.DispositivoRepository;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DispositivoService {

    private final DispositivoRepository repository;
    private final PythonNetworkClientService pythonService;

    public DispositivoService(DispositivoRepository repository, PythonNetworkClientService pythonService) {
        this.repository = repository;
        this.pythonService = pythonService;
    }

    public List<Dispositivo> obtenerTodos() {
        return repository.findAll();
    }

    @Transactional
    public List<Dispositivo> escanear(String subnet) throws Exception {
        JsonNode respuesta = pythonService.escanearRed(subnet);
        JsonNode devices = respuesta.get("devices");

        if (devices == null || !devices.isArray()) {
            throw new RuntimeException("Python no devolvió dispositivos.");
        }

        Set<String> ipsDetectadas = new HashSet<>();

        for (JsonNode deviceNode : devices) {
            String ip = texto(deviceNode, "ip");
            if (ip == null || ip.isBlank()) {
                continue;
            }

            ipsDetectadas.add(ip);

            String mac = texto(deviceNode, "mac");
            String hostname = texto(deviceNode, "hostname");
            String fabricante = texto(deviceNode, "fabricante");

            Dispositivo dispositivo = repository.findByIp(ip).orElseGet(Dispositivo::new);

            dispositivo.setIp(ip);
            dispositivo.setMac(mac);
            dispositivo.setHostname(hostname);
            dispositivo.setFabricante(fabricante);

            if (hostname != null && !hostname.isBlank()) {
                dispositivo.setNombre(hostname);
            } else {
                dispositivo.setNombre("Dispositivo " + ip);
            }

            dispositivo.setEstado("ACTIVO");
            dispositivo.setUltimaConexion(LocalDateTime.now());

            if (dispositivo.getEstadoAutorizacion() == null || dispositivo.getEstadoAutorizacion().isBlank()) {
                dispositivo.setEstadoAutorizacion("DESCONOCIDO");
            }

            repository.save(dispositivo);
        }

        List<Dispositivo> registrados = repository.findAll();

        for (Dispositivo dispositivo : registrados) {
            if (!ipsDetectadas.contains(dispositivo.getIp())) {
                dispositivo.setEstado("INACTIVO");
                repository.save(dispositivo);
            }
        }

        return repository.findAll();
    }

    public Dispositivo cambiarAutorizacion(Long id, String estado) {
        if (!estado.equals("AUTORIZADO") && !estado.equals("NO_AUTORIZADO") && !estado.equals("DESCONOCIDO")) {
            throw new IllegalArgumentException("Estado de autorización inválido.");
        }

        Dispositivo dispositivo = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dispositivo no encontrado."));

        dispositivo.setEstadoAutorizacion(estado);
        return repository.save(dispositivo);
    }

    private String texto(JsonNode node, String campo) {
        JsonNode valor = node.get(campo);
        if (valor == null || valor.isNull()) return null;
        String texto = valor.asText();
        return (texto == null || texto.isBlank()) ? null : texto;
    }
}
