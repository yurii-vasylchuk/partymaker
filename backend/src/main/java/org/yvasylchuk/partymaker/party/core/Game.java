package org.yvasylchuk.partymaker.party.core;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal;
import org.yvasylchuk.partymaker.party.dto.process.PartyAction;
import org.yvasylchuk.partymaker.party.dto.process.OutgoingMessage;

import java.util.ArrayList;
import java.util.List;
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

    abstract void act(PartymakerPrincipal actor, PartyAction action, Consumer<OutgoingMessage<?>> eventsCollector);

    abstract void initializeGame(Party.PartyContext partyContext);

    abstract void finalizeGame();

    public abstract PlayersReadiness calculateStageReadiness();

    public abstract Map<String, Integer> calculateScores();

    public List<Party.PartyConfigurationViolation> validateConfiguration(GameValidationContext validationContext) {
        List<Party.PartyConfigurationViolation> violations = new ArrayList<>();

        if (name == null || name.isBlank()) {
            violations.add(new Party.PartyConfigurationViolation(
                    "name",
                    "name should be not null nor empty"
            ));
        }

        return violations;
    }

    public enum GameType {
        CONTEST,
        GUESS_WHO,
        TOURNAMENT
    }

    //TODO: Improve with list of ready/unready player or something like that
    public record PlayersReadiness(int inStageReadiness, int total, int currentStage, int totalStages) {
        public boolean isStageDone() {
            return inStageReadiness == total;
        }
        public boolean isGameDone() {
            return currentStage == totalStages && inStageReadiness == total;
        }
    }

    public record GameValidationContext(
            List<String> allPartyUsers
    ) {
        public GameValidationContext(Party party) {
            this(party.getParticipants().stream().map(PartyParticipant::id).toList());
        }
    }
}
