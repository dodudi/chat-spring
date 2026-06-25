package kr.it.rudy.chat.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String X_REQUEST_ID = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        MDC.put(TRACE_ID_KEY, traceId);
        response.setHeader(X_REQUEST_ID, traceId);

        long start = System.currentTimeMillis();
        try {
            log.info("[REQUEST] {} {}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            log.info("[RESPONSE] {} {} {}ms", request.getMethod(), request.getRequestURI(), elapsed);
            MDC.remove(TRACE_ID_KEY);
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String requestId = request.getHeader(X_REQUEST_ID);
        return (requestId != null && !requestId.isBlank()) ? requestId : UUID.randomUUID().toString();
    }
}
