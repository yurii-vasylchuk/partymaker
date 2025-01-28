package org.yvasylchuk.partymaker.user.core;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.yvasylchuk.partymaker.config.AppContextProvider;
import org.yvasylchuk.partymaker.user.command.CreateParticipantUserCommand;
import org.yvasylchuk.partymaker.user.command.CreateRegularUserCommand;

import java.util.List;

@Getter
@Document(collection = "users")
@AllArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @PersistenceCreator)
public class User {
    @Id
    private String id;
    @Indexed
    private String email;
    private String username;
    private String password;
    private List<PartymakerRole> roles;

    public static User createRegularUser(CreateRegularUserCommand command) {
        PasswordEncoder passwordEncoder = AppContextProvider.getBean(PasswordEncoder.class);

        return new User(null,
                        command.email(),
                        command.username(),
                        passwordEncoder.encode(command.password()),
                        List.of(PartymakerRole.PARTICIPANT, PartymakerRole.REGULAR));
    }

    public static User createParticipantUser(CreateParticipantUserCommand command) {
        return new User(null,
                        null,
                        command.username(),
                        null,
                        List.of(PartymakerRole.PARTICIPANT));
    }
}
