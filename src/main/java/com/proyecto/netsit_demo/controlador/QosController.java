package com.proyecto.netsit_demo.controlador;

import com.proyecto.netsit_demo.modelo.Qos;
import com.proyecto.netsit_demo.servicio.QosService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/network/qos")
public class QosController {

    private final QosService qosService;

    public QosController(QosService qosService) {
        this.qosService = qosService;
    }

    @PostMapping("/measure")
    public ResponseEntity<?> measureQos(
            @RequestBody QosRequest request) {

        try {

            if (request == null) {
                return ResponseEntity
                        .badRequest()
                        .body(errorResponse(
                                "No se recibieron datos para la medición."
                        ));
            }

            String ip = request.getIp();

            int packets = request.getPackets();

            if (ip == null || ip.trim().isEmpty()) {
                return ResponseEntity
                        .badRequest()
                        .body(errorResponse(
                                "Debe introducir una IP o dominio."
                        ));
            }

            Qos qos = qosService.measure(
                    ip.trim(),
                    packets
            );

            return ResponseEntity.ok(
                    convertToMap(qos)
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(errorResponse(e.getMessage()));

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse(
                            "No fue posible realizar la medición QoS."
                    ));
        }
    }

    /**
     * Obtiene el historial de las últimas horas.
     *
     * GET:
     * /api/network/qos/history?hours=24
     */
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @RequestParam(defaultValue = "24") int hours) {

        try {

            List<Qos> history =
                    qosService.getHistory(hours);

            List<Map<String, Object>> response =
                    history.stream()
                            .map(this::convertToMap)
                            .toList();

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse(
                            "No fue posible obtener el historial QoS."
                    ));
        }
    }

    @GetMapping("/history/host")
    public ResponseEntity<?> getHistoryByHost(
            @RequestParam String host) {

        try {

            List<Qos> history =
                    qosService.getHistoryByHost(host);

            List<Map<String, Object>> response =
                    history.stream()
                            .map(this::convertToMap)
                            .toList();

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse(
                            "No fue posible obtener el historial del host."
                    ));
        }
    }

    private Map<String, Object> convertToMap(Qos qos) {

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "id",
                qos.getId()
        );

        /*
         * El HTML utiliza targetIp.
         */
        data.put(
                "targetIp",
                qos.getHost()
        );

        data.put(
                "latencyMs",
                qos.getLatencyMs()
        );

        data.put(
                "jitterMs",
                qos.getJitterMs()
        );

        data.put(
                "packetLossPercent",
                qos.getPacketLossPercent()
        );

        data.put(
                "mosScore",
                qos.getMosScore()
        );

        data.put(
                "qualityStatus",
                qos.getQualityStatus()
        );

        data.put(
                "timestamp",
                qos.getMeasuredAt()
        );

        data.put(
                "packetsSent",
                qos.getPacketsSent()
        );

        data.put(
                "packetsReceived",
                qos.getPacketsReceived()
        );

        data.put(
                "host",
                qos.getHost()
        );

        data.put(
                "measuredAt",
                qos.getMeasuredAt()
        );

        return data;
    }

    private Map<String, Object> errorResponse(
            String message) {

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "error",
                message
        );

        return response;
    }

    public static class QosRequest {

        private String ip;

        private int packets = 10;

        public QosRequest() {
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public int getPackets() {
            return packets;
        }

        public void setPackets(int packets) {
            this.packets = packets;
        }
    }
}