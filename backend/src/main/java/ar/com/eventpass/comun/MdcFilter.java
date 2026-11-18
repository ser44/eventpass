package ar.com.eventpass.comun;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Carga userId en el MDC (ya autenticado el JWT) y deja una línea de acceso
 * por request con la duración, para correlacionar con traceId en Loki. Se
 * engancha explícitamente después del filtro de autenticación (ver
 * SecurityConfig) para que el usuario ya esté resuelto en el contexto.
 */
public class MdcFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("ar.com.eventpass.acceso");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long inicio = System.currentTimeMillis();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            MDC.put("userId", jwt.getSubject());
        }
        try {
            chain.doFilter(request, response);
        } finally {
            long duracionMs = System.currentTimeMillis() - inicio;
            MDC.put("duracionMs", String.valueOf(duracionMs));
            log.info("{} {} -> {} ({} ms)", request.getMethod(), request.getRequestURI(),
                response.getStatus(), duracionMs);
            MDC.remove("userId");
            MDC.remove("duracionMs");
        }
    }
}
