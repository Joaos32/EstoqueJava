package br.com.estoqueti.util;

import java.net.InetAddress;

public final class WorkstationUtils {

    private WorkstationUtils() {
    }

    public static String resolveStationIdentifier() {
        String computerName = System.getenv("COMPUTERNAME");
        if (computerName != null && !computerName.isBlank()) {
            return computerName;
        }

        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception exception) {
            return "ESTACAO_DESKTOP";
        }
    }
}
