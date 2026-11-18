package com.gestion.alquileres.seguridad;

import com.gestion.alquileres.comun.Contexto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ContextFilter.class);

    static final String PREFIJO = "/api";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = pathSinContextPath(request);
        return !(path.equals(PREFIJO) || path.startsWith(PREFIJO + "/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(ContextoHolder.HEADER);
        log.info("HTTP Request: {} {} | X-Context-Id: {}", request.getMethod(), request.getRequestURI(), header);
        if (header == null || header.isBlank()) {
            log.warn("Petición rechazada: falta el header {} en {} {}", ContextoHolder.HEADER, request.getMethod(), request.getRequestURI());
            rechazar(response, "El header X-Context-Id es obligatorio");
            return;
        }

        final Contexto contexto;
        try {
            contexto = Contexto.valueOf(header.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warn("Petición rechazada: X-Context-Id inválido '{}' en {} {}", header, request.getMethod(), request.getRequestURI());
            rechazar(response, "X-Context-Id no es un contexto válido");
            return;
        }

        log.info("Contexto resuelto: {} para {} {}", contexto, request.getMethod(), request.getRequestURI());
        request.setAttribute(ContextoHolder.ATRIBUTO, contexto);
        ContextoHolder.set(contexto);
        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            log.error("Excepción no controlada procesando {} {} con contexto {}", request.getMethod(), request.getRequestURI(), contexto, ex);
            throw ex;
        } finally {
            ContextoHolder.clear();
        }
    }

    private static String pathSinContextPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }

    private static void rechazar(HttpServletResponse response, String mensaje) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"error\":\"" + mensaje + "\",\"permitidos\":[\"REAL_ESTATE\",\"HOME\",\"PERSONAL\"]}");
    }
}
