package com.eaze.idempotency;

import com.eaze.config.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Idempotency filter (client-generated key, Design A).
 *
 * Guards state-changing money endpoints. A client sends "Idempotency-Key"; the filter
 * ensures the operation executes AT MOST ONCE even if the request is duplicated by a
 * double-submit or a network retry. State lives in Redis (works across instances).
 *
 * Lenient: a request WITHOUT the header is processed normally (opt-in protection).
 *
 * Must wrap the response (ContentCachingResponseWrapper) so the successful response body
 * can be captured and REPLAYED to later duplicates — a servlet response is otherwise
 * write-only and cannot be re-read in afterCompletion.
 */
@Component
public class IdempotencyFilter extends OncePerRequestFilter {
    private static final String HEADER = "Idempotency-key";
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String[]> GAURDED = List.of(
            new String[]{"POST", "/api/orders/pay"},
            new String[]{"PUT",  "/api/wallet/*/transfer"},
            new String[]{"POST", "/api/withdrawal/*"},
            new String[]{"PUT",  "/api/wallet/deposit"}
    );
    private final IdempotencyService idempotencyService;
    private final JwtProvider jwtProvider;

    public IdempotencyFilter(IdempotencyService idempotencyService, JwtProvider jwtProvider) {
        this.idempotencyService = idempotencyService;
        this.jwtProvider = jwtProvider;
    }

    private boolean isGuarded(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        for (String[] g : GAURDED) {
            if (g[0].equals(method) && pathMatcher.match(g[1], path)){
                return true;
            }
        }
        return false;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!isGuarded((request))) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = request.getHeader(HEADER);
        if (key == null || key.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = resolveEmail(request);
        if (email == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String scopedKey =  email + ":" + request.getMethod() + ":" + request.getRequestURI() + ":" + key;
        IdempotencyRecord existing = idempotencyService.find(scopedKey);
        if (existing != null && existing.getState() == IdempotencyRecord.State.DONE) {
            writeStored(response, existing);
            return;
        }

        if (existing != null && existing.getState() == IdempotencyRecord.State.IN_PROGRESS) {
            writeConflict(response);
            return;
        }

        if (!idempotencyService.tryBegin(scopedKey)) {
            writeConflict(response);
            return;
        }

        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        boolean completed = false;
        try {
            filterChain.doFilter(request, wrapper);

            int status = wrapper.getStatus();
            String body = new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8);

            if (status >= 200 && status < 300) {
                idempotencyService.complete(scopedKey, status, body);
            } else {
                idempotencyService.release(scopedKey);
            }
            completed = true;
        } finally {
            if (!completed) {
                idempotencyService.release(scopedKey);
            }
            wrapper.copyBodyToResponse();
        }
    }

    private String resolveEmail(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null) {
            return null;
        }
        try {
            return jwtProvider.getEmailFromToken(auth);
        } catch (Exception e) {
            return null;
        }
    }

    private void writeStored(HttpServletResponse response, IdempotencyRecord record) throws IOException {
        response.setStatus(record.getHttpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Idempotent-Replayed", "true");  // signals the client this is a replay
        if (record.getBody() != null) {
            response.getWriter().write(record.getBody());
        }
    }

    private void writeConflict(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.CONFLICT.value());  // 409
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"error\":\"A request with this Idempotency-Key is already being processed or has an in-flight duplicate.\"}");
    }
}
