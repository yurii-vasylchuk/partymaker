package org.yvasylchuk.partymaker.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.yvasylchuk.partymaker.common.PartymakerJwtAuthentication;
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal;
import org.yvasylchuk.partymaker.user.JwtTokenService;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null) {
                log.info("Authorization header is empty, skipping authorization");
                //NOT Authenticated
                SecurityContextHolder.getContext().setAuthentication(null);
                filterChain.doFilter(request, response);
                return;
            }

            String rawToken = authHeader.substring(BEARER_PREFIX.length());

            PartymakerPrincipal user = jwtService.extractUser(rawToken);

            log.info("Authenticated: {} {}; User: {}", request.getMethod(), request.getRequestURI(), user);

            SecurityContextHolder.getContext().setAuthentication(
                    new PartymakerJwtAuthentication(
                            user,
                            rawToken
                    ));

        } catch (Exception e) {
            log.error("Failed to authenticate request.", e);
        }

        filterChain.doFilter(request, response);
    }
}
