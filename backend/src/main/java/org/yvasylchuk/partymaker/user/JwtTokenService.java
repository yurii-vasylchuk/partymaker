package org.yvasylchuk.partymaker.user;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.List;

@Service
public class JwtTokenService {
    private static final String ROLES_CLAIM = "roles";
    private static final String USERNAME_CLAIM = "username";

    private final SecretKey secretKey;

    public JwtTokenService(@Value("${bursdag.jwt.secretKey}") String secretKeyStr) {
        this.secretKey = Keys.hmacShaKeyFor((Decoders.BASE64.decode(secretKeyStr)));
    }

    public PartymakerPrincipal extractUser(String rawToken) {
        JwtParser parser = Jwts.parser()
                               .verifyWith(secretKey)
                               .build();

        Jws<Claims> claims = parser.parseSignedClaims(rawToken);

        String id = claims.getPayload().getSubject();
        String rolesStr = claims.getPayload().get(ROLES_CLAIM, String.class);
        List<String> roles = Arrays.asList(rolesStr.split(","));
        String username = claims.getPayload().get(USERNAME_CLAIM, String.class);

        return new PartymakerPrincipal(id, roles, username);
    }

    public String generateJwt(PartymakerPrincipal user) {
        return Jwts.builder()
                   .signWith(secretKey)
                   .subject(user.id())
                   .claim(ROLES_CLAIM, String.join(",", user.roles()))
                   .claim(USERNAME_CLAIM, user.username())
                   .compact();
    }
}
