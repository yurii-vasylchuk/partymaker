package org.yvasylchuk.partymaker.party.core;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.yvasylchuk.partymaker.exception.PartymakerException;
import org.yvasylchuk.partymaker.party.core.command.UpsertPartyGameCommand;
import org.yvasylchuk.partymaker.party.core.command.UpsertPartyGameCommand.UpsertContestCommand;

import java.util.function.Supplier;

import static org.yvasylchuk.partymaker.exception.PartymakerException.Kind.INTERNAL;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GameAbstractFactory {
    public static final GameAbstractFactory instance = new GameAbstractFactory();

    public Supplier<? extends Game<?>> getFactory(UpsertPartyGameCommand command,
                                                  Party party) {
        return switch (command) {
            case UpsertContestCommand cmd -> () -> this.createContest(cmd, party);
//            case GUESS_WHO -> new GuessWhoFactory(command, party, principal);
//            case TOURNAMENT -> new TournamentFactory(command, party, principal);
            default -> throw new PartymakerException(INTERNAL,
                                                     "Unknown game type %s",
                                                     command.getGameType());
        };
    }

    private Contest createContest(UpsertContestCommand cmd, Party party) {
        return Contest.create(
                cmd.getGameIdx() == null ? party.getGames().size() : cmd.getGameIdx() + 1,
                cmd);
    }
}
