package org.yvasylchuk.bursdag.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.yvasylchuk.bursdag.dao.entity.UserEntity;
import org.yvasylchuk.bursdag.dao.repository.UserRepository;

import java.util.List;

@Service
@Primary
@RequiredArgsConstructor
public class BursdagUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        int id = Integer.parseInt(username);
        UserEntity user = userRepository.findById(id)
                                        .orElseThrow(() -> new UsernameNotFoundException("User with id %d is not found".formatted(
                                                id)));

        return new BursdagUserDetails(user);
    }

    @Getter
    @ToString(of = {"username", "authorities", "gameId"})
    @AllArgsConstructor
    public static class BursdagUserDetails implements UserDetails {
        private final String username;
        private final String password;
        private final Integer gameId;
        private final List<? extends GrantedAuthority> authorities;

        public BursdagUserDetails(UserEntity entity) {
            username = "%d".formatted(entity.getId());
            password = entity.getToken();
            gameId = entity.getGame().getId();
            authorities = entity.getRoles().stream().map(SimpleGrantedAuthority::new).toList();
        }
    }
}
