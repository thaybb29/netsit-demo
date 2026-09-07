package com.proyecto.netsit_demo.servicio;

import com.proyecto.netsit_demo.modelo.Qos;
import com.proyecto.netsit_demo.repositorio.QosRepository;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QosService {

    private final QosRepository qosRepository;

    private static final Pattern LATENCY_PATTERN = Pattern.compile(
            "(?:time|tiempo)[=<]\\s*(\\d+(?:[\\.,]\\d+)?)\\s*ms",
            Pattern.CASE_INSENSITIVE
    );

    public QosService(QosRepository qosRepository) {
        this.qosRepository = qosRepository;
    }

    public Qos measure(String host, int packets) {

        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Debe introducir una IP o dominio."
            );
        }

        host = host.trim();

        if (packets < 1 || packets > 100) {
            throw new IllegalArgumentException(
                    "La cantidad de paquetes debe estar entre 1 y 100."
            );
        }

        List<Double> latencies = new ArrayList<>();

        int packetsReceived = 0;

        for (int i = 0; i < packets; i++) {

            Double latency = executeSinglePing(host);

            if (latency != null) {
                latencies.add(latency);
                packetsReceived++;
            }
        }

        double latencyMs = calculateAverageLatency(latencies);

        double jitterMs = calculateJitter(latencies);

        double packetLossPercent =
                ((double) (packets - packetsReceived) / packets) * 100.0;

        double mosScore = calculateMos(
                latencyMs,
                jitterMs,
                packetLossPercent
        );

        String qualityStatus = determineQuality(mosScore);

        Qos qos = new Qos(
                host,
                packets,
                packetsReceived,
                round(latencyMs),
                round(jitterMs),
                round(packetLossPercent),
                round(mosScore),
                qualityStatus,
                LocalDateTime.now()
        );

        return qosRepository.save(qos);
    }

    private Double executeSinglePing(String host) {

        boolean windows = System
                .getProperty("os.name")
                .toLowerCase()
                .contains("win");

        List<String> command = new ArrayList<>();

        if (windows) {

            command.add("C:\\Windows\\System32\\PING.EXE");
            command.add("-n");
            command.add("1");
            command.add("-w");
            command.add("1000");

        } else {

            command.add("ping");
            command.add("-c");
            command.add("1");
            command.add("-W");
            command.add("1");
        }

        command.add(host);

        Process process = null;

        try {

            ProcessBuilder processBuilder =
                    new ProcessBuilder(command);

            processBuilder.redirectErrorStream(true);

            process = processBuilder.start();

            StringBuilder output = new StringBuilder();

            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
                                         process.getInputStream(),
                                         StandardCharsets.UTF_8))) {

                String line;

                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished =
                    process.waitFor(3, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return null;
            }

            return extractLatency(output.toString());

        } catch (Exception e) {

            if (process != null) {
                process.destroyForcibly();
            }

            return null;
        }
    }

    private Double extractLatency(String output) {

        if (output == null || output.isEmpty()) {
            return null;
        }

        Matcher matcher =
                LATENCY_PATTERN.matcher(output);

        if (matcher.find()) {

            try {

                String value =
                        matcher.group(1)
                                .replace(",", ".");

                return Double.parseDouble(value);

            } catch (NumberFormatException e) {

                return null;
            }
        }

        return null;
    }

    private double calculateAverageLatency(
            List<Double> latencies) {

        if (latencies == null || latencies.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;

        for (Double latency : latencies) {
            total += latency;
        }

        return total / latencies.size();
    }

    private double calculateJitter(
            List<Double> latencies) {

        if (latencies == null || latencies.size() < 2) {
            return 0.0;
        }

        double totalDifference = 0.0;

        for (int i = 1; i < latencies.size(); i++) {

            double difference =
                    Math.abs(
                            latencies.get(i)
                                    - latencies.get(i - 1)
                    );

            totalDifference += difference;
        }

        return totalDifference /
                (latencies.size() - 1);
    }

    private double calculateMos(
            double latencyMs,
            double jitterMs,
            double packetLossPercent) {

        double latencyPenalty =
                latencyMs / 150.0;

        double jitterPenalty =
                jitterMs / 50.0;

        double lossPenalty =
                packetLossPercent / 10.0;

        double mos =
                4.5
                        - latencyPenalty
                        - jitterPenalty
                        - lossPenalty;

        if (mos > 5.0) {
            mos = 5.0;
        }

        if (mos < 1.0) {
            mos = 1.0;
        }

        return mos;
    }

    private String determineQuality(
            double mos) {

        if (mos >= 4.3) {
            return "EXCELENTE";
        }

        if (mos >= 3.8) {
            return "BUENA";
        }

        if (mos >= 3.0) {
            return "REGULAR";
        }

        if (mos >= 2.0) {
            return "MALA";
        }

        return "CRÍTICA";
    }

    public List<Qos> getHistory(int hours) {

        if (hours < 1) {
            hours = 24;
        }

        if (hours > 720) {
            hours = 720;
        }

        LocalDateTime since =
                LocalDateTime.now()
                        .minusHours(hours);

        return qosRepository
                .findByMeasuredAtAfterOrderByMeasuredAtDesc(
                        since
                );
    }

    public List<Qos> getHistoryByHost(
            String host) {

        if (host == null || host.trim().isEmpty()) {
            return List.of();
        }

        return qosRepository
                .findByHostOrderByMeasuredAtDesc(
                        host.trim()
                );
    }
    
    private double round(double value) {

        return Math.round(value * 100.0) / 100.0;
    }
}