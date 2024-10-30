package org.yvasylchuk.partymaker.game.core;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.PersistenceCreator;
import org.yvasylchuk.partymaker.common.dto.OutgoingMessage;
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal;
import org.yvasylchuk.partymaker.game.dto.AsyncAction;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Getter
@SuperBuilder
public class Contest extends Game<Contest.Context> {
    @PersistenceCreator
    protected Contest(Integer order, String name, String description, GameType type, Context context) {
        super(order, name, description, type, context);
    }

    public static Contest create(Integer order, String name, String description, List<String> tasks) {
        Context context = new Context(
                tasks.stream().map(MatchTask::new).toList(),
                Collections.emptyList()
        );
        return new Contest(order, name, description, GameType.CONTEST, context);
    }

    @Override
    void act(PartymakerPrincipal actor, AsyncAction action, Consumer<OutgoingMessage<?>> eventsCollector) {

    }

    @Override
    void initialize(Party.PartyContext partyContext) {

    }

    @Override
    void finalize(Party.PartyContext partyContext) {

    }

    @Override
    public StageReadiness calculateStageReadiness() {
        return null;
    }

    @Override
    public Map<String, Integer> calculateScores() {
        return Map.of();
    }

    public record Context(List<MatchTask> tasks,
                          List<PartyMember> members) implements DistillableForPrincipleContext<Context> {
        @Override
        public Context getDistilledContext(PartymakerPrincipal principal) {
            return new Context(this.tasks, this.members.stream()
                                                       .filter(m -> m.getId().equals(principal.id()))
                                                       .toList());
        }
    }

    public record MatchTask(String markdown) {
    }

    public record Match(MatchTask task,
                        String competitor) {
    }

}
