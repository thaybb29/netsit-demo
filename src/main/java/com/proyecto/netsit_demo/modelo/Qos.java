package com.proyecto.netsit_demo.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "qos")
public class Qos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String host;

    @Column(nullable = false)
    private int packetsSent;

    @Column(nullable = false)
    private int packetsReceived;

    @Column(nullable = false)
    private double latencyMs;

    @Column(nullable = false)
    private double jitterMs;

    @Column(nullable = false)
    private double packetLossPercent;

    @Column(nullable = false)
    private double mosScore;

    @Column(nullable = false, length = 50)
    private String qualityStatus;

    @Column(nullable = false)
    private LocalDateTime measuredAt;

    public Qos() {
    }

    public Qos(
            String host,
            int packetsSent,
            int packetsReceived,
            double latencyMs,
            double jitterMs,
            double packetLossPercent,
            double mosScore,
            String qualityStatus,
            LocalDateTime measuredAt) {

        this.host = host;
        this.packetsSent = packetsSent;
        this.packetsReceived = packetsReceived;
        this.latencyMs = latencyMs;
        this.jitterMs = jitterMs;
        this.packetLossPercent = packetLossPercent;
        this.mosScore = mosScore;
        this.qualityStatus = qualityStatus;
        this.measuredAt = measuredAt;
    }

    // GETTERS

    public Long getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPacketsSent() {
        return packetsSent;
    }

    public int getPacketsReceived() {
        return packetsReceived;
    }

    public double getLatencyMs() {
        return latencyMs;
    }

    public double getJitterMs() {
        return jitterMs;
    }

    public double getPacketLossPercent() {
        return packetLossPercent;
    }

    public double getMosScore() {
        return mosScore;
    }

    public String getQualityStatus() {
        return qualityStatus;
    }

    public LocalDateTime getMeasuredAt() {
        return measuredAt;
    }

    // SETTERS

    public void setId(Long id) {
        this.id = id;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPacketsSent(int packetsSent) {
        this.packetsSent = packetsSent;
    }

    public void setPacketsReceived(int packetsReceived) {
        this.packetsReceived = packetsReceived;
    }

    public void setLatencyMs(double latencyMs) {
        this.latencyMs = latencyMs;
    }

    public void setJitterMs(double jitterMs) {
        this.jitterMs = jitterMs;
    }

    public void setPacketLossPercent(double packetLossPercent) {
        this.packetLossPercent = packetLossPercent;
    }

    public void setMosScore(double mosScore) {
        this.mosScore = mosScore;
    }

    public void setQualityStatus(String qualityStatus) {
        this.qualityStatus = qualityStatus;
    }

    public void setMeasuredAt(LocalDateTime measuredAt) {
        this.measuredAt = measuredAt;
    }
}