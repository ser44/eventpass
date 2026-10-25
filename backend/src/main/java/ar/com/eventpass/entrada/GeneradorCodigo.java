package ar.com.eventpass.entrada;

import java.security.SecureRandom;

/**
 * Genera códigos de entrada con formato EVP-XXXX-XXXX-XXXX usando un alfabeto
 * sin caracteres ambiguos (sin 0/O, 1/I/L). Es lo que se codifica en el QR.
 */
public final class GeneradorCodigo {

    private static final String ALFABETO = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private GeneradorCodigo() {
    }

    public static String generar() {
        return "EVP-" + grupo() + "-" + grupo() + "-" + grupo();
    }

    private static String grupo() {
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }
}
