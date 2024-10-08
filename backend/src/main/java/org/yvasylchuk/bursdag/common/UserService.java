package org.yvasylchuk.bursdag.common;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yvasylchuk.bursdag.common.dto.BursdagPrincipal;
import org.yvasylchuk.bursdag.dao.repository.UserRepository;
import org.yvasylchuk.bursdag.game.core.Game;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public Optional<BursdagPrincipal> findUserByToken(String authToken) {
        return userRepository.findUserByToken(authToken).map(e -> {
            Game game = e.getGame();
            return new BursdagPrincipal(e.getId(),
                                        e.getRoles(),
                                        e.getUsername(),
                                        game != null ? game.getId() : null);
        });
    }

}
