package com.proyecto.netsit_demo.servicio;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.annotation.PreDestroy;

import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PacketListener;
import org.pcap4j.core.Pcaps;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.proyecto.netsit_demo.modelo.Protocolo;

@Service
public class NetworkTrafficService {

    private static final Logger logger = LoggerFactory.getLogger(NetworkTrafficService.class);

    private final AtomicLong totalBytes = new AtomicLong(0);
    private final AtomicBoolean isCapturing = new AtomicBoolean(false);
    private final List<PcapHandle> activeHandles = new CopyOnWriteArrayList<>();
    private Thread captureThread;
    private Thread cleanupThread;

    private volatile long lastBytes = 0;
    private volatile long lastTime = System.currentTimeMillis();
    private volatile double currentSpeed = 0.0;

    private final AtomicLong httpBytes = new AtomicLong(0);
    private final AtomicLong voipBytes = new AtomicLong(0);
    private final AtomicLong streamingBytes = new AtomicLong(0);
    private final AtomicLong otherBytes = new AtomicLong(0);
    private final AtomicLong otherDnsBytes = new AtomicLong(0);
    private final AtomicLong otherNtpBytes = new AtomicLong(0);
    private final AtomicLong otherUnclassifiedBytes = new AtomicLong(0);

    private volatile long lastHttpBytes = 0;
    private volatile long lastVoipBytes = 0;
    private volatile long lastStreamingBytes = 0;
    private volatile long lastOtherBytes = 0;
    private volatile long lastOtherDnsBytes = 0;
    private volatile long lastOtherNtpBytes = 0;
    private volatile long lastOtherUnclassifiedBytes = 0;

    private volatile Protocolo currentProtocolStats = new Protocolo();

    private static class FlowStats {
        long firstSeen = System.currentTimeMillis();
        long lastSeen = firstSeen;
        long downBytes = 0;
        long upBytes = 0;
        int largePacketCount = 0;
        int totalPacketCount = 0;
        String category = null; // null = sin decidir; "STREAMING" | "HTTP"
        boolean sniChecked = false;
    }

    private final Map<String, FlowStats> flows = new ConcurrentHashMap<>();
    private static final int FLOW_MAP_MAX_SIZE = 20000;
    private static final long FLOW_IDLE_TIMEOUT_MS = 30_000;

    private static final long HEURISTIC_MIN_DURATION_MS = 8_000;
    private static final double HEURISTIC_MIN_DOWN_UP_RATIO = 15.0;
    private static final long HEURISTIC_MIN_DOWN_BYTES = 300_000;
    private static final double HEURISTIC_MIN_LARGE_PACKET_RATIO = 0.6;

    private static final boolean AUTO_DETECT_INTERFACE = true;
    private static final List<Integer> MANUAL_INTERFACE_INDEXES = List.of(); // usar solo si AUTO_DETECT_INTERFACE = false

    public List<String> listInterfaces() {
        try {
            List<PcapNetworkInterface> interfaces = Pcaps.findAllDevs();
            List<String> result = new java.util.ArrayList<>();
            for (int i = 0; i < interfaces.size(); i++) {
                PcapNetworkInterface nif = interfaces.get(i);
                result.add(i + ": " + nif.getName() + " - " + nif.getDescription());
            }
            return result;
        } catch (PcapNativeException e) {
            logger.error("No se pudieron listar las interfaces: {}", e.getMessage());
            return List.of("Error al listar interfaces: " + e.getMessage());
        }
    }

    public synchronized void startCapturing() {
        if (isCapturing.compareAndSet(false, true)) {
            resetCounters();
            startFlowCleanupTask();

            captureThread = new Thread(() -> {
                try {
                    List<PcapNetworkInterface> interfaces = Pcaps.findAllDevs();

                    if (interfaces.isEmpty()) {
                        logger.error("No se encontraron interfaces de red. ¿Está instalado Npcap/libpcap?");
                        isCapturing.set(false);
                        return;
                    }

                    List<PcapNetworkInterface> toCapture = new java.util.ArrayList<>();

                    if (AUTO_DETECT_INTERFACE) {
                        for (PcapNetworkInterface nif : interfaces) {
                            boolean hasIpv4 = nif.getAddresses().stream()
                                    .anyMatch(addr -> addr.getAddress() instanceof java.net.Inet4Address);
                            boolean isLoopback = nif.isLoopBack();
                            if (hasIpv4 && !isLoopback) {
                                toCapture.add(nif);
                            }
                        }
                        if (toCapture.isEmpty() && !interfaces.isEmpty()) {
                            // Fallback: si no encontramos ninguna "candidata", usar la primera disponible
                            toCapture.add(interfaces.get(0));
                        }
                    } else {
                        for (Integer index : MANUAL_INTERFACE_INDEXES) {
                            if (index >= 0 && index < interfaces.size()) {
                                toCapture.add(interfaces.get(index));
                            }
                        }
                    }

                    logger.info("Interfaces seleccionadas para captura: {}",
                            toCapture.stream().map(nif -> nif.getName()).toList());

                    for (PcapNetworkInterface nif : toCapture) {
                        PcapHandle handle;
                        try {
                            handle = nif.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 10);
                        } catch (PcapNativeException e) {
                            logger.error("No se pudo abrir la interfaz {} (¿faltan permisos de administrador/root?): {}",
                                    nif.getName(), e.getMessage());
                            continue;
                        }
                        activeHandles.add(handle);

                        Thread captureInterfaceThread = new Thread(() -> runCaptureLoop(handle, nif.getName()));
                        captureInterfaceThread.setDaemon(true);
                        captureInterfaceThread.start();
                    }

                    if (activeHandles.isEmpty()) {
                        logger.error("Ninguna interfaz pudo abrirse. Deteniendo captura.");
                        isCapturing.set(false);
                    }

                } catch (PcapNativeException e) {
                    logger.error("Error nativo Pcap: {}", e.getMessage(), e);
                    isCapturing.set(false);
                } catch (Exception e) {
                    logger.error("Error inesperado al iniciar captura: {}", e.getMessage(), e);
                    isCapturing.set(false);
                }
            });

            captureThread.setDaemon(true);
            captureThread.start();
        }
    }

    private void runCaptureLoop(PcapHandle handle, String interfaceName) {
        try {
            handle.loop(-1, (PacketListener) packet -> {
                if (!isCapturing.get()) {
                    try {
                        handle.breakLoop();
                    } catch (NotOpenException e) {
                        logger.debug("Interfaz ya cerrada al intentar detener: {}", e.getMessage());
                    }
                    return;
                }

                int packetSize = packet.length();
                long newTotal = totalBytes.addAndGet(packetSize);

                classifyPacket(packet, packetSize);

                long currentTime = System.currentTimeMillis();
                long timeDiff = currentTime - lastTime;

                if (timeDiff >= 1000) {
                    long bytesDiff = newTotal - lastBytes;
                    currentSpeed = (bytesDiff / (1024.0 * 1024.0)) / (timeDiff / 1000.0);

                    calculateProtocolSpeeds(timeDiff);

                    lastBytes = newTotal;
                    lastTime = currentTime;
                }
            });
        } catch (PcapNativeException | NotOpenException e) {
            logger.error("Error en captura de interfaz {}: {}", interfaceName, e.getMessage());
        } catch (InterruptedException e) {
            logger.info("Captura interrumpida en interfaz: {}", interfaceName);
            Thread.currentThread().interrupt();
        }
    }

    private void startFlowCleanupTask() {
        cleanupThread = new Thread(() -> {
            while (isCapturing.get()) {
                try {
                    Thread.sleep(10_000);
                    long now = System.currentTimeMillis();
                    flows.entrySet().removeIf(e -> (now - e.getValue().lastSeen) > FLOW_IDLE_TIMEOUT_MS);
                    if (flows.size() > FLOW_MAP_MAX_SIZE) {
                        flows.clear();
                        logger.debug("Mapa de flujos reiniciado (límite alcanzado)");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }

    private void classifyPacket(Packet packet, int size) {
        TcpPacket tcpPacket = packet.get(TcpPacket.class);
        UdpPacket udpPacket = packet.get(UdpPacket.class);

        if (tcpPacket != null) {
            classifyTcpPacket(packet, tcpPacket, size);
        } else if (udpPacket != null) {
            classifyUdpPacket(packet, udpPacket, size);
        } else {
            otherBytes.addAndGet(size);
            otherUnclassifiedBytes.addAndGet(size);
        }
    }

    private void classifyTcpPacket(Packet fullPacket, TcpPacket tcpPacket, int size) {
        int srcPort = tcpPacket.getHeader().getSrcPort().valueAsInt();
        int dstPort = tcpPacket.getHeader().getDstPort().valueAsInt();

        if (dstPort == 443 || srcPort == 443) {
            boolean isOutbound = dstPort == 443;
            String flowKey = buildFlowKey(fullPacket, srcPort, dstPort, "TCP");
            FlowStats flow = flows.computeIfAbsent(flowKey, k -> new FlowStats());

            updateFlowStats(flow, size, isOutbound);

            if (!flow.sniChecked) {
                String sni = tryExtractSni(tcpPacket);
                if (sni != null) {
                    flow.sniChecked = true;
                    flow.category = looksLikeVideoHostname(sni) ? "STREAMING" : "HTTP";
                }
            }

            if (flow.category == null) {
                evaluateHeuristic(flow);
            }

            addToCategory(flow.category, size);
            return;
        }

        if (isHttpPort(srcPort) || isHttpPort(dstPort)) {
            httpBytes.addAndGet(size);
        } else if (isStreamingPort(srcPort) || isStreamingPort(dstPort)) {
            streamingBytes.addAndGet(size);
        } else if (isKnownStreamingAppPort(srcPort) || isKnownStreamingAppPort(dstPort)) {
            streamingBytes.addAndGet(size);
        } else if (srcPort == 5060 || dstPort == 5060) {
            voipBytes.addAndGet(size);
        } else if (size > 1400) {
            streamingBytes.addAndGet(size);
        } else {
            otherBytes.addAndGet(size);
        }
    }

    private void classifyUdpPacket(Packet fullPacket, UdpPacket udpPacket, int size) {
        int srcPort = udpPacket.getHeader().getSrcPort().valueAsInt();
        int dstPort = udpPacket.getHeader().getDstPort().valueAsInt();

        if (isVoipPort(srcPort) || isVoipPort(dstPort)) {
            voipBytes.addAndGet(size);
            return;
        }
        if (size < 300 && (srcPort >= 10000 || dstPort >= 10000)) {
            voipBytes.addAndGet(size);
            return;
        }
        if (isStreamingPort(srcPort) || isStreamingPort(dstPort)) {
            streamingBytes.addAndGet(size);
            return;
        }

        if (srcPort == 443 || dstPort == 443) {
            boolean isOutbound = dstPort == 443;
            String flowKey = buildFlowKey(fullPacket, srcPort, dstPort, "UDP");
            FlowStats flow = flows.computeIfAbsent(flowKey, k -> new FlowStats());

            updateFlowStats(flow, size, isOutbound);

            if (flow.category == null) {
                evaluateHeuristic(flow);
            }

            addToCategory(flow.category, size);
            return;
        }

        if (size > 500 && size < 1500) {
            streamingBytes.addAndGet(size);
            return;
        }
        if (srcPort == 53 || dstPort == 53) {
            otherBytes.addAndGet(size);
            otherDnsBytes.addAndGet(size);
            return;
        }
        if (srcPort == 123 || dstPort == 123) {
            otherBytes.addAndGet(size);
            otherNtpBytes.addAndGet(size);
            return;
        }
        otherBytes.addAndGet(size);
        otherUnclassifiedBytes.addAndGet(size);
    }

    private void addToCategory(String category, int size) {
        if ("STREAMING".equals(category)) {
            streamingBytes.addAndGet(size);
        } else {
            httpBytes.addAndGet(size);
        }
    }

    private void updateFlowStats(FlowStats flow, int size, boolean isOutbound) {
        flow.lastSeen = System.currentTimeMillis();
        flow.totalPacketCount++;
        if (size >= 1200) {
            flow.largePacketCount++;
        }
        if (isOutbound) {
            flow.upBytes += size;
        } else {
            flow.downBytes += size;
        }
    }

    private void evaluateHeuristic(FlowStats flow) {
        long duration = flow.lastSeen - flow.firstSeen;
        if (duration < HEURISTIC_MIN_DURATION_MS) return;
        if (flow.downBytes < HEURISTIC_MIN_DOWN_BYTES) return;

        double ratio = flow.upBytes == 0 ? Double.MAX_VALUE : (double) flow.downBytes / flow.upBytes;
        double largePacketRatio = flow.totalPacketCount == 0 ? 0 :
                (double) flow.largePacketCount / flow.totalPacketCount;

        if (ratio >= HEURISTIC_MIN_DOWN_UP_RATIO && largePacketRatio >= HEURISTIC_MIN_LARGE_PACKET_RATIO) {
            flow.category = "STREAMING";
        } else {
            flow.category = "HTTP";
        }
    }

    private String buildFlowKey(Packet fullPacket, int srcPort, int dstPort, String proto) {
        IpV4Packet ipPacket = fullPacket.get(IpV4Packet.class);
        String srcIp = ipPacket != null ? ipPacket.getHeader().getSrcAddr().getHostAddress() : "?";
        String dstIp = ipPacket != null ? ipPacket.getHeader().getDstAddr().getHostAddress() : "?";
        if (srcIp.compareTo(dstIp) <= 0) {
            return proto + ":" + srcIp + ":" + srcPort + "-" + dstIp + ":" + dstPort;
        } else {
            return proto + ":" + dstIp + ":" + dstPort + "-" + srcIp + ":" + srcPort;
        }
    }
    
    private String tryExtractSni(TcpPacket tcpPacket) {
        if (tcpPacket.getPayload() == null) return null;
        byte[] data = tcpPacket.getPayload().getRawData();
        if (data == null || data.length < 43) return null;

        try {
            if ((data[0] & 0xFF) != 0x16) return null;
            if ((data[1] & 0xFF) != 0x03) return null;
            if ((data[5] & 0xFF) != 0x01) return null;

            int pos = 38;
            if (pos >= data.length) return null;

            int sessionIdLen = data[pos] & 0xFF;
            pos += 1 + sessionIdLen;
            if (pos + 2 > data.length) return null;

            int cipherSuitesLen = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
            pos += 2 + cipherSuitesLen;
            if (pos + 1 > data.length) return null;

            int compressionMethodsLen = data[pos] & 0xFF;
            pos += 1 + compressionMethodsLen;
            if (pos + 2 > data.length) return null;

            int extensionsLen = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
            pos += 2;
            int extensionsEnd = Math.min(pos + extensionsLen, data.length);

            while (pos + 4 <= extensionsEnd) {
                int extType = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
                int extLen = ((data[pos + 2] & 0xFF) << 8) | (data[pos + 3] & 0xFF);
                pos += 4;

                if (extType == 0x0000) {
                    return parseServerNameExtension(data, pos, extLen);
                }
                pos += extLen;
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    private String parseServerNameExtension(byte[] data, int offset, int len) {
        try {
            if (offset + len > data.length) return null;
            int listPos = offset + 2;
            if (listPos + 3 > data.length) return null;

            int nameType = data[listPos] & 0xFF;
            if (nameType != 0) return null;

            int nameLen = ((data[listPos + 1] & 0xFF) << 8) | (data[listPos + 2] & 0xFF);
            int nameStart = listPos + 3;
            if (nameStart + nameLen > data.length || nameLen <= 0 || nameLen > 255) return null;

            return new String(data, nameStart, nameLen, StandardCharsets.US_ASCII);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean looksLikeVideoHostname(String sni) {
        String host = sni.toLowerCase();
        return host.contains("video") || host.contains("stream")
                || host.startsWith("media") || host.contains(".cdn.")
                || host.contains("vod.") || host.contains("livestream");
    }

    private boolean isHttpPort(int port) {
        return port == 80 || port == 8080 || port == 8443 ||
                port == 3000 || port == 8000 || port == 8888;
    }

    private boolean isVoipPort(int port) {
        return port == 5060 || port == 5061 ||
                (port >= 16384 && port <= 32767) ||
                port == 3478 || port == 3479;
    }

    private boolean isStreamingPort(int port) {
        return port == 1935 || port == 554 || port == 8554 || port == 1755;
    }

    private boolean isKnownStreamingAppPort(int port) {
        return port == 4070 || port == 57621;
    }

    private void calculateProtocolSpeeds(long timeDiff) {
        long httpDiff = httpBytes.get() - lastHttpBytes;
        long voipDiff = voipBytes.get() - lastVoipBytes;
        long streamingDiff = streamingBytes.get() - lastStreamingBytes;
        long otherDiff = otherBytes.get() - lastOtherBytes;
        long otherDnsDiff = otherDnsBytes.get() - lastOtherDnsBytes;
        long otherNtpDiff = otherNtpBytes.get() - lastOtherNtpBytes;
        long otherUnclassifiedDiff = otherUnclassifiedBytes.get() - lastOtherUnclassifiedBytes;

        double timeInSeconds = timeDiff / 1000.0;

        Protocolo stats = new Protocolo();
        stats.setHttpMbps((httpDiff / (1024.0 * 1024.0)) / timeInSeconds);
        stats.setVoipMbps((voipDiff / (1024.0 * 1024.0)) / timeInSeconds);
        stats.setStreamingMbps((streamingDiff / (1024.0 * 1024.0)) / timeInSeconds);
        stats.setOtherMbps((otherDiff / (1024.0 * 1024.0)) / timeInSeconds);
        stats.setOtherDnsMbps((otherDnsDiff / (1024.0 * 1024.0)) / timeInSeconds);
        stats.setOtherNtpMbps((otherNtpDiff / (1024.0 * 1024.0)) / timeInSeconds);
        stats.setOtherUnclassifiedMbps((otherUnclassifiedDiff / (1024.0 * 1024.0)) / timeInSeconds);
        currentProtocolStats = stats;

        lastHttpBytes = httpBytes.get();
        lastVoipBytes = voipBytes.get();
        lastStreamingBytes = streamingBytes.get();
        lastOtherBytes = otherBytes.get();
        lastOtherDnsBytes = otherDnsBytes.get();
        lastOtherNtpBytes = otherNtpBytes.get();
        lastOtherUnclassifiedBytes = otherUnclassifiedBytes.get();
    }

    private void resetCounters() {
        lastBytes = 0;
        lastTime = System.currentTimeMillis();
        currentSpeed = 0.0;
        totalBytes.set(0);

        httpBytes.set(0);
        voipBytes.set(0);
        streamingBytes.set(0);
        otherBytes.set(0);
        otherDnsBytes.set(0);
        otherNtpBytes.set(0);
        otherUnclassifiedBytes.set(0);

        lastHttpBytes = 0;
        lastVoipBytes = 0;
        lastStreamingBytes = 0;
        lastOtherBytes = 0;
        lastOtherDnsBytes = 0;
        lastOtherNtpBytes = 0;
        lastOtherUnclassifiedBytes = 0;

        flows.clear();

        currentProtocolStats = new Protocolo();
    }

    public synchronized void stopCapturing() {
        if (isCapturing.compareAndSet(true, false)) {
            cleanupHandles();

            if (captureThread != null) {
                captureThread.interrupt();
                captureThread = null;
            }
            if (cleanupThread != null) {
                cleanupThread.interrupt();
                cleanupThread = null;
            }

            currentSpeed = 0.0;
            currentProtocolStats = new Protocolo();
            logger.info("Captura de tráfico detenida");
        }
    }

    public void resetCounter() {
        resetCounters();
        logger.info("Contador de tráfico reiniciado");
    }

    public boolean isCapturing() {
        return isCapturing.get();
    }

    public TrafficData getCurrentTrafficData() {
        return new TrafficData(currentSpeed, currentProtocolStats, isCapturing.get());
    }

    private void cleanupHandles() {
        for (PcapHandle handle : activeHandles) {
            try {
                handle.breakLoop();
                handle.close();
            } catch (Exception e) {
                logger.warn("Error al cerrar un handle de captura", e);
            }
        }
        activeHandles.clear();
    }

    public static class TrafficData {
        private final double mbps;
        private final Protocolo protocolo;
        private final boolean activo;

        public TrafficData(double mbps, Protocolo protocolo, boolean activo) {
            this.mbps = mbps;
            this.protocolo = protocolo;
            this.activo = activo;
        }

        public double getMbps() {
            return mbps;
        }

        public Protocolo getProtocolo() {
            return protocolo;
        }

        public boolean isActivo() {
            return activo;
        }
    }

    @PreDestroy
    public void cleanup() {
        stopCapturing();
        logger.info("Servicio NetworkTraffic detenido");
    }
}