package org.yvasylchuk.partymaker.game.core;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal;
import org.yvasylchuk.partymaker.exception.PartymakerException;
import org.yvasylchuk.partymaker.game.core.command.AddPartyGameCommand;

import java.util.function.Supplier;

import static org.yvasylchuk.partymaker.exception.PartymakerException.Kind.INTERNAL;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AbstractGameFactory {
    public static final AbstractGameFactory instance = new AbstractGameFactory();

    public Supplier<? extends Game<?>> getFactory(AddPartyGameCommand command,
                                                  Party party,
                                                  PartymakerPrincipal principal) {
        return switch (command.gameType()) {
            case CONTEST -> () -> this.createContest(command, party, principal);
            default -> throw new PartymakerException(INTERNAL, "Unknown game type %s".formatted(command.gameType()));
        };
    }

    private Contest createContest(AddPartyGameCommand command, Party party, PartymakerPrincipal principal) {
        return Contest.create(party.getGames().size(),
                              command.name(),
                              command.description(),
                              command.tasks());
    }
}
