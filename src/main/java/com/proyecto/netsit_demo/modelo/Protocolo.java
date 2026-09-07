package com.proyecto.netsit_demo.modelo;


public class Protocolo {
    private double httpMbps;
    private double voipMbps;
    private double streamingMbps;
    private double otherMbps;
    private double otherDnsMbps;
    private double otherNtpMbps;
    private double otherUnclassifiedMbps;

    public Protocolo() {
        this.httpMbps = 0.0;
        this.voipMbps = 0.0;
        this.streamingMbps = 0.0;
        this.otherMbps = 0.0;
        this.otherDnsMbps = 0.0;
        this.otherNtpMbps = 0.0;
        this.otherUnclassifiedMbps = 0.0;
    }

    public double getHttpMbps() {
        return httpMbps;
    }

    public void setHttpMbps(double httpMbps) {
        this.httpMbps = httpMbps;
    }

    public double getVoipMbps() {
        return voipMbps;
    }

    public void setVoipMbps(double voipMbps) {
        this.voipMbps = voipMbps;
    }

    public double getStreamingMbps() {
        return streamingMbps;
    }

    public void setStreamingMbps(double streamingMbps) {
        this.streamingMbps = streamingMbps;
    }

    public double getOtherMbps() {
        return otherMbps;
    }

    public void setOtherMbps(double otherMbps) {
        this.otherMbps = otherMbps;
    }

    public double getOtherDnsMbps() { return otherDnsMbps; }
    public void setOtherDnsMbps(double otherDnsMbps) { this.otherDnsMbps = otherDnsMbps; }
    public double getOtherNtpMbps() { return otherNtpMbps; }
    public void setOtherNtpMbps(double otherNtpMbps) { this.otherNtpMbps = otherNtpMbps; }
    public double getOtherUnclassifiedMbps() { return otherUnclassifiedMbps; }
    public void setOtherUnclassifiedMbps(double otherUnclassifiedMbps) { this.otherUnclassifiedMbps = otherUnclassifiedMbps; }

    @Override
    public String toString() {
        return String.format("HTTP: %.2f MB/s, VoIP: %.2f MB/s, Streaming: %.2f MB/s, Otros: %.2f MB/s",
                httpMbps, voipMbps, streamingMbps, otherMbps);
    }
}
