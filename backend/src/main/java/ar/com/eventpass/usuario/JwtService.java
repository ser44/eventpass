package ar.com.eventpass.usuario;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;

/** Emite tokens JWT firmados con HS256 (clave simétrica desde JWT_SECRET). Expiran a las 8 h. */
@Service
public class JwtService {

    private static final Duration EXPIRACION = Duration.ofHours(8);

    private final JwtEncoder encoder;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        SecretKey clave = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(clave));
    }

    public String generar(Usuario u) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject(u.getId().toString())
            .issuedAt(ahora)
            .expiresAt(ahora.plus(EXPIRACION))
            .claim("email", u.getEmail())
            .claim("nombre", u.getNombre())
            .claim("rol", u.getRol().name())
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
