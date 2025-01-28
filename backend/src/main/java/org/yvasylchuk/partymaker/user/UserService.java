package org.yvasylchuk.partymaker.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.yvasylchuk.partymaker.exception.PartymakerException;
import org.yvasylchuk.partymaker.user.command.AuthenticateByAuthTokenCommand;
import org.yvasylchuk.partymaker.user.command.CreateParticipantUserCommand;
import org.yvasylchuk.partymaker.user.command.CreateRegularUserCommand;
import org.yvasylchuk.partymaker.user.core.User;
import org.yvasylchuk.partymaker.user.dao.UserRepository;
import org.yvasylchuk.partymaker.user.command.AuthenticateByEmailAndPasswordCommand;
import org.yvasylchuk.partymaker.user.dto.UserDto;

import java.util.List;

import static org.yvasylchuk.partymaker.exception.PartymakerException.Kind.ACCESS_DENIED;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserDto createUser(CreateParticipantUserCommand command) {
        User user = userRepository.save(User.createParticipantUser(command));

        return UserDto.of(user);
    }

    public UserDto register(CreateRegularUserCommand command) {
        User user = User.createRegularUser(command);

        user = userRepository.save(user);

        return UserDto.of(user);
    }

    public UserDto findUserByAuthToken(AuthenticateByAuthTokenCommand command) {
        User user = userRepository.findUserByToken(command.authToken())
                                  .orElseThrow(() -> new PartymakerException(ACCESS_DENIED, "Invalid token"));

        return UserDto.of(user);

    }

    public UserDto findUserByEmailAndPassword(AuthenticateByEmailAndPasswordCommand command) {
        User user = userRepository.findUserByEmail(command.email())
                                  .orElseThrow(() -> new PartymakerException(ACCESS_DENIED, "Invalid credentials"));

        if (!passwordEncoder.matches(command.password(), user.getPassword())) {
            throw new PartymakerException(ACCESS_DENIED, "Invalid credentials");
        }

        return UserDto.of(user);
    }

    public List<UserDto> findByIdIn(Iterable<String> usersIds) {
        return userRepository.findAllById(usersIds).stream().map(UserDto::of).toList();
    }

    public boolean isUserExists(String id) {
        return userRepository.existsById(id);
    }
}
