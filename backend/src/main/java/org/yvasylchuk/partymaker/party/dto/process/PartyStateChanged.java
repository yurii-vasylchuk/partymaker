package org.yvasylchuk.partymaker.party.dto.process;

import lombok.Getter;
import lombok.ToString;
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal;
import org.yvasylchuk.partymaker.party.core.Party;
import org.yvasylchuk.partymaker.party.core.PartyMember;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

@Getter
@ToString
public final class PartyStateChanged extends AsyncEvent {
    private final String name;
    private final String description;
    private final Party.PartyStatus status;
    private final List<String> supportedVariableOptions;
    private final Set<PartyMember> players;
    private final Integer gamesCount;
    private final GameDescriptor<?> currentGame;
    private final Map<String, Integer> scores;
    private final Map<String, Integer> lastStageScores;

    public PartyStateChanged(Party party, PartymakerPrincipal principal) {
        super(Type.GAME_STATE_CHANGED);
        this.name = party.getName();
        this.description = party.getDescription();
        this.status = party.getStatus();
        this.supportedVariableOptions = party.getContext().getSupportedOptions().stream().map(Enum::name).toList();

        Party.PartyContext ctx = party.getContext();
        this.players = ctx.getMembers() == null ? emptySet() : ctx.getMembers();
        this.gamesCount = party.getGames().size();
        this.scores = ctx.getPartyScores() == null ? emptyMap() : ctx.getPartyScores();
        this.lastStageScores = ctx.getLastStageScores();

        Optional<GameDescriptor<?>> descriptor = party.currentGame()
                                                      .map(game -> new GameDescriptor<>(game, principal));
        if (descriptor.isPresent()) {
            this.currentGame = descriptor.get();
        } else {
            currentGame = null;
        }
    }

}
