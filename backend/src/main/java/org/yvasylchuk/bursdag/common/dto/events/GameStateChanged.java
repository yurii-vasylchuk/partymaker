package org.yvasylchuk.bursdag.common.dto.events;

import lombok.Getter;
import lombok.ToString;
import org.yvasylchuk.bursdag.game.core.Game;
import org.yvasylchuk.bursdag.game.core.GameContext;
import org.yvasylchuk.bursdag.game.core.Player;
import org.yvasylchuk.bursdag.game.core.stages.Stage;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

@Getter
@ToString
public final class GameStateChanged extends AsyncEvent {
    private final String name;
    private final Game.Status status;
    private final List<Player> players;
    private final StageDescriptor<?> currentStage;
    private final Map<Integer, Integer> scores;
    private final Map<Integer, Integer> lastStageScores;


    public GameStateChanged(Game game) {
        super(Type.GAME_STATE_CHANGED);
        this.name = game.getName();
        this.status = game.getStatus();

        GameContext ctx = game.loadContext();
        this.players = ctx.getPlayers() == null ? emptyList() : ctx.getPlayers();
        this.scores = ctx.getGameScores() == null ? emptyMap() : ctx.getGameScores();
        this.lastStageScores = ctx.getLastStageScores();

        Stage<?> currentStage = game.getCurrentStage();
        if (currentStage != null) {
            this.currentStage = new StageDescriptor<>(currentStage);
        } else {
            this.currentStage = null;
        }

    }

}
