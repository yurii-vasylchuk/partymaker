package org.yvasylchuk.bursdag.common;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yvasylchuk.bursdag.common.dto.BursdagPrincipal;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.List;

@Service
public class JwtTokenService {
    private static final String ROLES_CLAIM = "roles";
    private static final String USERNAME_CLAIM = "username";
    private static final String GAME_ID_CLAIM = "gameId";

    private final SecretKey secretKey;

    public JwtTokenService(@Value("${bursdag.jwt.secretKey}") String secretKeyStr) {
        this.secretKey = Keys.hmacShaKeyFor((Decoders.BASE64.decode(secretKeyStr)));
    }

    public BursdagPrincipal extractUser(String rawToken) {
        JwtParser parser = Jwts.parser()
                               .verifyWith(secretKey)
                               .build();

        Jws<Claims> claims = parser.parseSignedClaims(rawToken);

        int id = Integer.parseInt(claims.getPayload().getSubject());
        String rolesStr = claims.getPayload().get(ROLES_CLAIM, String.class);
        List<String> roles = Arrays.asList(rolesStr.split(","));
        String username = claims.getPayload().get(USERNAME_CLAIM, String.class);
        Integer gameId = claims.getPayload().get(GAME_ID_CLAIM, Integer.class);

        return new BursdagPrincipal(id, roles, username, gameId);
    }

    public String generateJwt(BursdagPrincipal user) {
        return Jwts.builder()
                   .signWith(secretKey)
                   .subject("%d".formatted(user.getId()))
                   .claim(ROLES_CLAIM, String.join(",", user.getRoles()))
                   .claim(USERNAME_CLAIM, user.getUsername())
                   .claim(GAME_ID_CLAIM, user.getGameId())
                   .compact();
    }
}
