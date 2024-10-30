package org.yvasylchuk.partymaker.game.dto;

import lombok.Getter;
import lombok.ToString;
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal;
import org.yvasylchuk.partymaker.game.core.DistillableForPrincipleContext;
import org.yvasylchuk.partymaker.game.core.Game;

import java.util.Map;

@Getter
@ToString
public class GameDescriptor<CTX extends DistillableForPrincipleContext<CTX>> {
    private final String name;
    private final String description;
    private final Integer order;
    private final String type;
    private final Game.StageReadiness readiness;
    private final Map<String, Integer> scores;

    private final CTX ctx;

    public GameDescriptor(Game<CTX> game, PartymakerPrincipal principal) {
        this.name = game.getName();
        this.description = game.getDescription();
        this.order = game.getOrder();
        this.type = game.getType().name();
        this.readiness = game.calculateStageReadiness();
        this.scores = game.calculateScores();

        this.ctx = game.getContext().getDistilledContext(principal);
    }


}
