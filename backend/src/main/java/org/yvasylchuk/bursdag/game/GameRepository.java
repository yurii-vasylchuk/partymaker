package org.yvasylchuk.bursdag.game;

import org.springframework.data.jpa.repository.JpaRepository;
import org.yvasylchuk.bursdag.game.core.Game;

public interface GameRepository extends JpaRepository<Game, Integer> {
}
