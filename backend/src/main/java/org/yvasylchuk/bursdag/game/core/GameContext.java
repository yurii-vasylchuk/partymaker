package org.yvasylchuk.bursdag.game.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@Jacksonized
@AllArgsConstructor
public class GameContext {
    private List<Player> players;
    private Map<Integer, Integer> gameScores;
    private Map<Integer, Integer> lastStageScores;
}
