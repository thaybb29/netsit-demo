package com.proyecto.netsit_demo.servicio;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NetworkService {

    public String executePing(String host) {

        if (host == null || host.trim().isEmpty()) {
            return "Error: debe introducir una IP o dominio.";
        }

        host = host.trim();

        List<String> command = new ArrayList<>();

        boolean windows = System
                .getProperty("os.name")
                .toLowerCase()
                .contains("win");

        if (windows) {

            command.add("C:\\Windows\\System32\\PING.EXE");
            command.add("-n");
            command.add("4");

        } else {

            command.add("ping");
            command.add("-c");
            command.add("4");
        }

        command.add(host);

        return runProcess(command);
    }

    public Map<String, Object> scanPorts(
            String host,
            String portRange) {

        Map<String, Object> response =
                new HashMap<>();

        if (host == null ||
                host.trim().isEmpty()) {

            response.put("success", false);
            response.put("error", true);
            response.put(
                    "output",
                    "Debe introducir una IP o dominio."
            );

            return response;
        }

        host = host.trim();

        if (portRange == null ||
                portRange.trim().isEmpty()) {

            portRange = "1-1024";
        }

        portRange = portRange.trim();
        
        if (!isValidPortRange(portRange)) {

            response.put("success", false);
            response.put("error", true);
            response.put(
                    "output",
                    "Rango de puertos inválido. " +
                    "Ejemplos permitidos: 80, 1-1024, 22,80,443."
            );

            return response;
        }

        try {

            String nmapPath =
                    findNmapExecutable();

            if (nmapPath == null) {

                response.put(
                        "success",
                        false
                );

                response.put(
                        "error",
                        true
                );

                response.put(
                        "output",
                        "Nmap no fue encontrado.\n\n" +
                        "Instale Nmap en Windows o " +
                        "agregue Nmap al PATH del sistema."
                );

                return response;
            }

            List<String> command =
                    new ArrayList<>();

            command.add(nmapPath);
            command.add("-Pn");
            command.add("-T4");
            command.add("-sT");
            command.add("--open");
            command.add("-p");
            command.add(portRange);
            command.add(host);

            String output =
                    runProcess(command);

            response.put(
                    "success",
                    true
            );

            response.put(
                    "error",
                    false
            );

            response.put(
                    "ip",
                    host
            );

            response.put(
                    "portRange",
                    portRange
            );

            response.put(
                    "scanner",
                    "Nmap"
            );

            response.put(
                    "output",
                    output
            );

            /*
             * Extraer puertos abiertos
             * del resultado de Nmap.
             */
            List<Map<String, String>> openPorts =
                    parseNmapPorts(output);

            response.put(
                    "openPorts",
                    openPorts
            );

            response.put(
                    "totalOpenPorts",
                    openPorts.size()
            );

            /*
             * Análisis básico de seguridad.
             */
            Map<String, Object> analysis =
                    analyzePorts(openPorts);

            response.put(
                    "securityAnalysis",
                    analysis
            );

            return response;

        } catch (Exception e) {

            response.put(
                    "success",
                    false
            );

            response.put(
                    "error",
                    true
            );

            response.put(
                    "output",
                    "Error ejecutando Nmap:\n\n" +
                    e.getMessage()
            );

            return response;
        }
    }

    private String findNmapExecutable() {

        String[] possiblePaths = {

                "C:\\Program Files\\Nmap\\nmap.exe",

                "C:\\Program Files (x86)\\Nmap\\nmap.exe",

                "nmap.exe",

                "nmap"
        };

        for (String path : possiblePaths) {

            try {

                ProcessBuilder test =
                        new ProcessBuilder(
                                path,
                                "--version"
                        );

                test.redirectErrorStream(true);

                Process process =
                        test.start();

                process.waitFor();

                if (process.exitValue() == 0) {

                    return path;
                }

            } catch (Exception ignored) {

                // Probar siguiente ubicación.
            }
        }

        return null;
    }

    private boolean isValidPortRange(
            String range) {

        if (range == null ||
                range.isBlank()) {

            return false;
        }

        return range.matches(
                "^[0-9\\-,]+$"
        );
    }

    private List<Map<String, String>> parseNmapPorts(
            String output) {

        List<Map<String, String>> ports =
                new ArrayList<>();

        if (output == null) {
            return ports;
        }

        String[] lines =
                output.split("\\R");

        for (String line : lines) {

            line = line.trim();

            if (line.matches(
                    "^\\d+/(tcp|udp)\\s+open\\s+.*"
            )) {

                String[] parts =
                        line.split("\\s+");

                if (parts.length >= 3) {

                    String portProtocol =
                            parts[0];

                    String state =
                            parts[1];

                    String service =
                            parts[2];

                    Map<String, String> port =
                            new HashMap<>();

                    port.put(
                            "port",
                            portProtocol
                    );

                    port.put(
                            "state",
                            state
                    );

                    port.put(
                            "service",
                            service
                    );

                    port.put(
                            "riskLevel",
                            getRiskLevel(
                                    portProtocol
                            )
                    );

                    ports.add(port);
                }
            }
        }

        return ports;
    }

    private Map<String, Object> analyzePorts(
            List<Map<String, String>> ports) {

        Map<String, Object> analysis =
                new HashMap<>();

        int critical = 0;
        int high = 0;
        int medium = 0;
        int low = 0;

        for (Map<String, String> port :
                ports) {

            String risk =
                    port.get("riskLevel");

            if ("CRÍTICO".equals(risk)) {

                critical++;

            } else if ("ALTO".equals(risk)) {

                high++;

            } else if ("MEDIO".equals(risk)) {

                medium++;

            } else {

                low++;
            }
        }

        String overallRisk = "BAJO";

        if (critical > 0) {

            overallRisk = "CRÍTICO";

        } else if (high > 0) {

            overallRisk = "ALTO";

        } else if (medium > 0) {

            overallRisk = "MEDIO";
        }

        analysis.put(
                "overallRisk",
                overallRisk
        );

        analysis.put(
                "criticalPorts",
                critical
        );

        analysis.put(
                "highRiskPorts",
                high
        );

        analysis.put(
                "mediumRiskPorts",
                medium
        );

        analysis.put(
                "lowRiskPorts",
                low
        );

        analysis.put(
                "totalOpenPorts",
                ports.size()
        );

        return analysis;
    }

    private String getRiskLevel(
            String portProtocol) {

        try {

            String portString =
                    portProtocol
                            .split("/")[0];

            int port =
                    Integer.parseInt(
                            portString
                    );

            return switch (port) {

                case 23 ->
                        "CRÍTICO";

                case 135, 139, 445, 3389 ->
                        "ALTO";

                case 20, 21, 25, 110,
                     143, 3306, 5432 ->
                        "MEDIO";

                default ->
                        "BAJO";
            };

        } catch (Exception e) {

            return "BAJO";
        }
    }

    private String runProcess(
            List<String> command) {

        StringBuilder output =
                new StringBuilder();

        try {

            ProcessBuilder processBuilder =
                    new ProcessBuilder(command);

            processBuilder
                    .redirectErrorStream(true);

            Process process =
                    processBuilder.start();

            try (
                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            process.getInputStream()
                                    )
                            )
            ) {

                String line;

                while (
                        (line =
                                reader.readLine())
                                != null
                ) {

                    output.append(line)
                            .append("\n");
                }
            }

            int exitCode =
                    process.waitFor();

            if (exitCode != 0) {

                output.append(
                        "\nProceso finalizado con código: "
                ).append(exitCode);
            }

        } catch (Exception e) {

            return "Error ejecutando comando:\n\n"
                    + e.getMessage();
        }

        return output.toString();
    }
}
