package org.yvasylchuk.bursdag.game;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yvasylchuk.bursdag.common.BursdagUserDetailsService.BursdagUserDetails;
import org.yvasylchuk.bursdag.common.OutgoingMessageHandler;
import org.yvasylchuk.bursdag.common.dto.AsyncAction;
import org.yvasylchuk.bursdag.common.dto.BursdagPrincipal;
import org.yvasylchuk.bursdag.common.dto.events.GameStateChanged;
import org.yvasylchuk.bursdag.game.core.Game;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class GameService {
    private final GameRepository gameRepository;
    private final OutgoingMessageHandler outgoingMessageHandler;

    public void goToNextStage(BursdagUserDetails admin) {
        Game game = gameRepository.findById(admin.getGameId()).orElseThrow();

        game.goToNextStage();

        gameRepository.save(game);

        if (game.hasEvents()) {
            outgoingMessageHandler.sendMessages(game.getEvents());
        }
    }

    public GameStateChanged loadCurrentState(BursdagPrincipal user) {
        Game game = gameRepository.findById(user.getGameId()).orElseThrow();
        return new GameStateChanged(game);
    }

    public void join(BursdagPrincipal player) {
        Game game = gameRepository.findById(player.getGameId()).orElseThrow();

        game.join(player);

        gameRepository.save(game);

        if (game.hasEvents()) {
            outgoingMessageHandler.sendMessages(game.getEvents());
        }
    }

    public void act(BursdagPrincipal player, AsyncAction action) {
        Game game = gameRepository.findById(player.getGameId()).orElseThrow();

        game.act(player, action);

        gameRepository.save(game);

        if (game.hasEvents()) {
            outgoingMessageHandler.sendMessages(game.getEvents());
        }
    }

    public void adminAct(BursdagPrincipal player, AsyncAction action) {
        Game game = gameRepository.findById(player.getGameId()).orElseThrow();

        game.adminAct(player, action);

        gameRepository.save(game);

        if (game.hasEvents()) {
            outgoingMessageHandler.sendMessages(game.getEvents());
        }
    }

    public void completeStage(BursdagUserDetails admin) {
        Game game = gameRepository.findById(admin.getGameId()).orElseThrow();

        game.completeStage();

        gameRepository.save(game);

        if (game.hasEvents()) {
            outgoingMessageHandler.sendMessages(game.getEvents());
        }
    }
}
