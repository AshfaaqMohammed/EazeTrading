package com.eaze.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtTokenValidator extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(JwtTokenValidator.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(JwtConstant.JWT_HEADER);

        // No Authorization header, or not a Bearer token -> don't authenticate,
        // let the authorization rules decide (public endpoints like /auth/** proceed).
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = header.substring(BEARER_PREFIX.length()).trim();

        try {
            Claims claims = jwtProvider.parseClaims(jwt);

            String email = String.valueOf(claims.get("email"));
            String authorities = String.valueOf(claims.get("authorities"));

            List<GrantedAuthority> authorityList = AuthorityUtils.commaSeparatedStringToAuthorityList(authorities);

            Authentication auth = new UsernamePasswordAuthenticationToken(email, null, authorityList);
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (Exception e) {
            // Expired / tampered / malformed token: do NOT authenticate, but do NOT
            // hard-fail the request here. A stale token must not block public endpoints
            // (e.g. /auth/login after logout). Protected endpoints still get denied by
            // the authorization rules because no authentication was set.
            LOG.warn("Invalid JWT: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
