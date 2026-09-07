package com.proyecto.netsit_demo.servicio;

import com.proyecto.netsit_demo.modelo.CallInfo;
import com.proyecto.netsit_demo.modelo.CallRecord;
import com.proyecto.netsit_demo.modelo.ExtensionInfo;
import com.proyecto.netsit_demo.repositorio.CallRecordRepository;

import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionState;
import org.asteriskjava.manager.ManagerEventListener;
import org.asteriskjava.manager.action.CommandAction;
import org.asteriskjava.manager.event.HangupEvent;
import org.asteriskjava.manager.event.ManagerEvent;
import org.asteriskjava.manager.event.NewChannelEvent;
import org.asteriskjava.manager.response.CommandResponse;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AsteriskMonitorService implements ManagerEventListener {

    private final Map<String, CallInfo> llamadasActivas =
            new ConcurrentHashMap<>();

    private final CallRecordRepository callRecordRepository;
    private final ManagerConnection connection;

    /*
     * Formato habitual de Asterisk:
     *
     * Endpoint:  1001/1001                 Not in use    0 of inf
     * Endpoint:  1002/1002                 Unavailable   0 of inf
     * Endpoint:  1003/1003                 In use        1 of inf
     *
     * Capturamos:
     *   1 -> extensión
     *   2 -> estado
     */
    private static final Pattern ENDPOINT_PATTERN = Pattern.compile(
            "^\\s*Endpoint:\\s+(\\S+)(?:\\s+)(.+?)\\s+\\d+\\s+of\\s+.*$",
            Pattern.CASE_INSENSITIVE
    );

    public AsteriskMonitorService(
            ManagerConnection connection,
            CallRecordRepository callRecordRepository) {

        this.connection = connection;
        this.callRecordRepository = callRecordRepository;

        connection.addEventListener(this);
    }

    // ============================================================
    // EVENTOS AMI
    // ============================================================

    @Override
    public void onManagerEvent(ManagerEvent event) {

        /*
         * Cuando comienza un canal, registramos la llamada activa.
         */
        if (event instanceof NewChannelEvent nc) {

            String uniqueId = nc.getUniqueId();

            if (uniqueId == null || uniqueId.isBlank()) {
                return;
            }

            String canal = nc.getChannel();
            String numeroOrigen = nc.getCallerIdNum();

            if (numeroOrigen == null || numeroOrigen.isBlank()) {
                numeroOrigen = "Desconocido";
            }

            llamadasActivas.put(
                    uniqueId,
                    new CallInfo(
                            canal,
                            numeroOrigen,
                            LocalDateTime.now()
                    )
            );
        }

        /*
         * Cuando termina un canal, quitamos la llamada de las activas
         * y guardamos el registro en el CDR.
         */
        else if (event instanceof HangupEvent hu) {

            String uniqueId = hu.getUniqueId();

            if (uniqueId == null || uniqueId.isBlank()) {
                return;
            }

            CallInfo info = llamadasActivas.remove(uniqueId);

            if (info == null) {
                return;
            }

            long duracion = ChronoUnit.SECONDS.between(
                    info.getInicio(),
                    LocalDateTime.now()
            );

            if (duracion < 0) {
                duracion = 0;
            }

            String destino = obtenerDestinoDesdeCanal(hu.getChannel());

            CallRecord record = new CallRecord(
                    info.getNumeroOrigen(),
                    destino,
                    info.getInicio(),
                    (int) duracion,
                    "FINALIZADA"
            );

            callRecordRepository.save(record);
        }
    }

    public Collection<CallInfo> getLlamadasActivas() {
        return llamadasActivas.values();
    }

    public boolean isConectado() {
        return connection != null
                && connection.getState() == ManagerConnectionState.CONNECTED;
    }
    
    public List<ExtensionInfo> getExtensiones() {

        List<ExtensionInfo> resultado = new ArrayList<>();

        if (!isConectado()) {
            return resultado;
        }

        try {

            CommandAction action =
                    new CommandAction("pjsip show endpoints");

            CommandResponse response =
                    (CommandResponse) connection.sendAction(action, 5000);

            if (response == null) {
                throw new IllegalStateException(
                        "Asterisk no devolvió respuesta para 'pjsip show endpoints'"
                );
            }

            Collection<String> lineas = response.getResult();

            if (lineas == null || lineas.isEmpty()) {
                return resultado;
            }

            for (String linea : lineas) {

                if (linea == null || linea.isBlank()) {
                    continue;
                }

                ExtensionInfo extension = analizarEndpoint(linea);

                if (extension != null) {
                    resultado.add(extension);
                }
            }

            return resultado;

        } catch (Exception e) {

            /*
             * Antes este error se ocultaba y el frontend recibía [].
             * Ahora se registra claramente para poder saber qué ocurre.
             */
            System.err.println(
                    "ERROR obteniendo extensiones desde Asterisk AMI:"
            );

            e.printStackTrace();

            return resultado;
        }
    }

    private ExtensionInfo analizarEndpoint(String linea) {

        Matcher matcher = ENDPOINT_PATTERN.matcher(linea);

        if (!matcher.matches()) {
            return null;
        }

        String identificador = matcher.group(1).trim();
        String estadoAsterisk = matcher.group(2).trim();

        String extension = identificador;

        if (identificador.contains("/")) {
            extension = identificador.substring(
                    0,
                    identificador.indexOf("/")
            );
        }

        if (extension.isBlank()) {
            return null;
        }

        boolean enLinea = estaDisponible(estadoAsterisk);

        String estado = enLinea
                ? "EN_LINEA"
                : "FUERA_DE_LINEA";

        return new ExtensionInfo(
                extension,
                extension,
                estado
        );
    }

    private boolean estaDisponible(String estado) {

        if (estado == null) {
            return false;
        }

        String estadoNormalizado =
                estado.trim().toLowerCase();

        /*
         * Estados que consideramos disponibles.
         */
        if (estadoNormalizado.contains("avail")) {
            return true;
        }

        if (estadoNormalizado.contains("in use")) {
            return true;
        }

        if (estadoNormalizado.contains("ringing")) {
            return true;
        }

        if (estadoNormalizado.contains("busy")) {
            return true;
        }

        if (estadoNormalizado.contains("not in use")) {
            return true;
        }

        return false;
    }

    private String obtenerDestinoDesdeCanal(String canal) {

        if (canal == null || canal.isBlank()) {
            return "Desconocido";
        }

        if (canal.startsWith("PJSIP/")) {

            String valor = canal.substring("PJSIP/".length());

            int separador = valor.indexOf("-");

            if (separador > 0) {
                valor = valor.substring(0, separador);
            }

            if (!valor.isBlank()) {
                return valor;
            }
        }

        return canal;
    }
}