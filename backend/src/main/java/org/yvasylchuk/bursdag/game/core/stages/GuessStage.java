package org.yvasylchuk.bursdag.game.core.stages;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.yvasylchuk.bursdag.common.dto.AsyncAction;
import org.yvasylchuk.bursdag.common.dto.AsyncAction.GuessesChangedAction;
import org.yvasylchuk.bursdag.common.dto.AsyncAction.PlayerReadyAction;
import org.yvasylchuk.bursdag.common.dto.BursdagPrincipal;
import org.yvasylchuk.bursdag.game.core.Game;
import org.yvasylchuk.bursdag.game.core.GameContext;
import org.yvasylchuk.bursdag.game.core.Player;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;

@Slf4j
@Entity
@DiscriminatorValue("GUESS")
public final class GuessStage extends Stage<GuessStage.Context> {

    private static void assertPlayerIsNotReady(BursdagPrincipal player, GuessStage.Context ctx) {
        if (ctx.playersReadiness.get(player.getId())) {
            log.error("Player already marked as ready;");
            throw new IllegalArgumentException("User marked as ready");
        }
    }

    @Override
    public void act(BursdagPrincipal player, AsyncAction absAction) {
        switch (absAction) {
            case PlayerReadyAction action -> handlePlayerReadyAction(player, action);
            case GuessesChangedAction action -> handlePLayerChangedGuessesAction(player, action);
            default -> {
                String errorMsg = "Action %s is not supported by %s"
                        .formatted(absAction, this.getClass());
                log.warn(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }
        }
    }

    private void handlePLayerChangedGuessesAction(BursdagPrincipal player, GuessesChangedAction action) {
        doInContext(ctx -> {
            assertPlayerIsNotReady(player, ctx);

            Set<Integer> validIds = ctx.players.stream().map(Player::getUserId).collect(Collectors.toSet());
            action.getGuesses().forEach((key, value) -> {
                if (!validIds.contains(key) || !validIds.contains(value)) {
                    log.warn("Illegal guess - there are no player with provided id = {}:{}", key, value);
                    throw new IllegalArgumentException("Invalid player id in guess");
                }
            });

            ctx.getGuesses().put(player.getId(), action.getGuesses());
        });
    }

    private void handlePlayerReadyAction(BursdagPrincipal player, PlayerReadyAction action) {
        doInContext(ctx -> {
            assertPlayerIsNotReady(player, ctx);

            boolean isPlayerFillAll = isPlayerFillAll(ctx, player);
            if (isPlayerFillAll) {
                ctx.playersReadiness.put(player.getId(), true);
            } else {
                String msg = "Player haven't fill all necessary data";
                log.warn(msg);
                throw new IllegalStateException(msg);
            }
        });
    }

    private boolean isPlayerFillAll(Context ctx, BursdagPrincipal player) {
        Map<Integer, Integer> guesses = ctx.guesses.getOrDefault(player.getId(), emptyMap());
        return guesses.size() == ctx.players.size();
    }

    @Override
    public boolean isReadyToComplete() {
        return runInContext(ctx -> ctx.playersReadiness.size() == ctx.players.size() &&
                ctx.playersReadiness.values().stream().allMatch(isReady -> isReady));
    }

    @Override
    public Map<Integer, Integer> calculateScores() {
        // Simplified version - gave scores only to ones who were best and worst guessed by others
        return runInContext(ctx -> {

            Map<Integer, Integer> results = ctx.players.stream().collect(Collectors.toMap(
                    Player::getUserId,
                    p -> (int) ctx.guesses.values()
                                          .stream()
                                          .map(guesses -> Objects.equals(guesses.get(p.getUserId()), p.getUserId()))
                                          .filter(isCorrect -> isCorrect)
                                          .count()
                                                                                         ));

            List<Integer> playersOrderedByGuesses = results.entrySet()
                                                           .stream()
                                                           .sorted(Comparator.comparingInt(Map.Entry::getValue))
                                                           .map(Map.Entry::getKey)
                                                           .toList();

            return Map.of(
                    playersOrderedByGuesses.getFirst(), 5,
                    playersOrderedByGuesses.getLast(), 5
                         );
        });
    }

    @Override
    public StageReadiness calculateStageReadiness() {
        return runInContext(context -> new StageReadiness(
                context.players.size(),
                (int) context.playersReadiness.values().stream().filter(isReady -> isReady).count()
        ));
    }

    @Override
    @SneakyThrows
    public Context loadContext() {
        return Game.MAPPER.readValue(getRawContext(), Context.class);
    }

    @Override
    public void initializeStage(GameContext gameCtx) {
        doInContext(ctx -> {
            List<Player> players = gameCtx.getPlayers();
            ctx.players = players;
            ctx.playersReadiness = players.stream().collect(Collectors.toMap(
                    Player::getUserId,
                    ignored -> false
                                                                            ));
            ctx.guesses = players.stream().collect(Collectors.toMap(
                    Player::getUserId,
                    (ignored) -> emptyMap()
                                                                   ));
        });
    }

    @Getter
    @Setter
    @Builder
    @Jacksonized
    @AllArgsConstructor
    public static class Context {
        //[guesserId: [actualPlayerId: guessedPlayerId]]
        private Map<Integer, Map<Integer, Integer>> guesses;
        private List<Player> players;
        private Map<Integer, Boolean> playersReadiness;
    }
}
