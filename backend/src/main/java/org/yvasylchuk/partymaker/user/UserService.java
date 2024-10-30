package org.yvasylchuk.partymaker.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal;
import org.yvasylchuk.partymaker.exception.PartymakerException;
import org.yvasylchuk.partymaker.user.command.CreateUserCommand;
import org.yvasylchuk.partymaker.user.dto.UserDto;

import java.util.List;
import java.util.Optional;

import static org.yvasylchuk.partymaker.exception.PartymakerException.Kind.GENERIC_CLIENT;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public Optional<PartymakerPrincipal> findUserByToken(String authToken) {
        return userRepository.findUserByToken(authToken).map(e -> new PartymakerPrincipal(e.getId(),
                                                                                          e.getRoles(),
                                                                                          e.getUsername()));
    }

    public UserDto createUser(CreateUserCommand command) {
        User user = this.userRepository.save(new User(
                null,
                command.username(),
                List.of("USER"),
                command.token()
        ));

        return UserDto.of(user);
    }

    public UserDto findById(String id) {
        return userRepository.findById(id)
                             .map(UserDto::of)
                             .orElseThrow(() -> new PartymakerException(GENERIC_CLIENT, "User is not found"));
    }
}
