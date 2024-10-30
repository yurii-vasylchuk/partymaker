package org.yvasylchuk.partymaker.game.core;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.yvasylchuk.partymaker.common.dto.OutgoingMessage;
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal;
import org.yvasylchuk.partymaker.game.dto.AsyncAction;

import java.util.Map;
import java.util.function.Consumer;

@Getter
@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Game<CTX extends DistillableForPrincipleContext<CTX>> {

    protected Integer order;

    protected String name;
    protected String description;
    protected GameType type;

    protected CTX context;

    abstract void act(PartymakerPrincipal actor, AsyncAction action, Consumer<OutgoingMessage<?>> eventsCollector);

    abstract void initialize(Party.PartyContext partyContext);

    abstract void finalize(Party.PartyContext partyContext);

    public abstract StageReadiness calculateStageReadiness();

    public abstract Map<String, Integer> calculateScores();

    public enum GameType {
        CONTEST,
        GUESS_WHO,
        TOURNAMENT
    }

    public record StageReadiness(int ready, int total) {
    }
}
