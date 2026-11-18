package ar.com.eventpass.comun;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Seguridad stateless con JWT HS256 (resource server). Las rutas van sin el
 * prefijo /api/v1 porque ese es el context-path del servlet.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${app.jwt.secret}")
    private String secret;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                // Scrape interno (Prometheus entra directo por la red de Docker); nginx
                // bloquea /actuator para que no salga al público (§6.4).
                .requestMatchers("/actuator/prometheus").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login").permitAll()
                .requestMatchers("/hola").permitAll()
                // Documentación viva de la API (§6.3): OpenAPI JSON + Swagger UI, públicos.
                .requestMatchers("/docs", "/docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                // Las stats NO son públicas (regla específica ANTES del permitAll del catálogo).
                .requestMatchers(HttpMethod.GET, "/events/*/stats").authenticated()
                // Catálogo y detalle de eventos: públicos (solo GET). "/events/**" cubre
                // /events y /events/{id}.
                .requestMatchers(HttpMethod.GET, "/events", "/events/**").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(convertidorJwt())))
            .addFilterAfter(new MdcFilter(), BearerTokenAuthenticationFilter.class)
            .exceptionHandling(e -> e.accessDeniedHandler(accesoDenegadoHandler()));
        return http.build();
    }

    /** WARN de negocio (§9.1) sin cambiar la respuesta 403 por defecto de Spring Security. */
    private AccessDeniedHandler accesoDenegadoHandler() {
        return (request, response, ex) -> {
            try (var c = MDC.putCloseable("motivo", "ACCESO_DENEGADO")) {
                log.warn("Acceso denegado: {} {}", request.getMethod(), request.getRequestURI());
            }
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        };
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey clave = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Mapea el claim `rol` del token a una autoridad ROLE_* para @PreAuthorize/hasRole. */
    private JwtAuthenticationConverter convertidorJwt() {
        JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
        conv.setJwtGrantedAuthoritiesConverter(jwt -> {
            String rol = jwt.getClaimAsString("rol");
            return rol == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + rol));
        });
        return conv;
    }
}
