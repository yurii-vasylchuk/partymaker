package org.yvasylchuk.bursdag.common.dto.events;

import lombok.Getter;
import lombok.ToString;
import org.yvasylchuk.bursdag.game.core.stages.Stage;

import java.util.Map;

@Getter
@ToString
public class StageDescriptor<CTX> {
    private final String name;
    private final String description;
    private final Integer order;
    private final String type;
    private final Stage.StageReadiness readiness;
    private final Map<Integer, Integer> scores;

    private final CTX ctx;

    public StageDescriptor(Stage<CTX> stage) {
        this.name = stage.getName();
        this.description = stage.getDescription();
        this.order = stage.getOrder();
        this.type = stage.getType().name();
        this.scores = stage.calculateScores();
        this.readiness = stage.calculateStageReadiness();

        this.ctx = stage.loadContext();
    }


}
