package org.yvasylchuk.partymaker.game.dto;

import lombok.Getter;
import lombok.ToString;
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal;
import org.yvasylchuk.partymaker.common.dto.events.AsyncEvent;
import org.yvasylchuk.partymaker.game.core.Party;
import org.yvasylchuk.partymaker.game.core.PartyMember;

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
        this.supportedVariableOptions = party.getContext().supportedOptions().stream().map(Enum::name).toList();

        Party.PartyContext ctx = party.getContext();
        this.players = ctx.members() == null ? emptySet() : ctx.members();
        this.gamesCount = party.getGames().size();
        this.scores = ctx.partyScores() == null ? emptyMap() : ctx.partyScores();
        this.lastStageScores = ctx.lastStageScores();

        Optional<GameDescriptor<?>> descriptor = party.currentGame()
                                                      .map(game -> new GameDescriptor<>(game, principal));
        if (descriptor.isPresent()) {
            this.currentGame = descriptor.get();
        } else {
            currentGame = null;
        }
    }

}
