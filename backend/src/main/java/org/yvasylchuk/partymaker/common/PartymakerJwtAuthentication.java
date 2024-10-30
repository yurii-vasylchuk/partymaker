package org.yvasylchuk.partymaker.common;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal;

import java.util.Collection;

@RequiredArgsConstructor
public class PartymakerJwtAuthentication implements Authentication {
    private final PartymakerPrincipal principal;
    private final String rawJws;
    private boolean isAuthenticated = true;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return principal.roles().stream().map(SimpleGrantedAuthority::new).toList();
    }

    @Override
    public Object getCredentials() {
        return rawJws;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public PartymakerPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.isAuthenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        return principal.username();
    }
}
