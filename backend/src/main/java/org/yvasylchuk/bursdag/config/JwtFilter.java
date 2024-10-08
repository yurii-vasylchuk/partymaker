package org.yvasylchuk.bursdag.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;
import org.yvasylchuk.bursdag.common.BursdagUserDetailsService;
import org.yvasylchuk.bursdag.common.JwtTokenService;
import org.yvasylchuk.bursdag.common.dto.BursdagPrincipal;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtService;
    private final UserDetailsService userDetailsService;

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

            BursdagPrincipal user = jwtService.extractUser(rawToken);

            log.info("Authenticated: {} {}; User: {}", request.getMethod(), request.getRequestURI(), user);
            List<? extends GrantedAuthority> grantedAuthorities = user.getRoles()
                                                                      .stream()
                                                                      .map(SimpleGrantedAuthority::new)
                                                                      .toList();

            BursdagUserDetailsService.BursdagUserDetails principal = new BursdagUserDetailsService.BursdagUserDetails(
                    "%d".formatted(user.getId()),
                    rawToken,
                    user.getGameId(),
                    grantedAuthorities);

            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,
                                                                                                         rawToken,
                                                                                                         grantedAuthorities));

        } catch (Exception e) {
            log.error("Failed to authenticate request.", e);
        }

        filterChain.doFilter(request, response);
    }
}
