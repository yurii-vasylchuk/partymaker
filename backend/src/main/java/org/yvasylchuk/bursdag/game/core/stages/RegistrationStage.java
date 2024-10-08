package org.yvasylchuk.bursdag.game.core.stages;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.yvasylchuk.bursdag.common.dto.AsyncAction;
import org.yvasylchuk.bursdag.common.dto.AsyncAction.ChoseAvatarAction;
import org.yvasylchuk.bursdag.common.dto.AsyncAction.ChoseNicknameAction;
import org.yvasylchuk.bursdag.common.dto.AsyncAction.ChoseTraitsAction;
import org.yvasylchuk.bursdag.common.dto.AsyncAction.PlayerReadyAction;
import org.yvasylchuk.bursdag.common.dto.BursdagPrincipal;
import org.yvasylchuk.bursdag.game.core.Game;
import org.yvasylchuk.bursdag.game.core.GameContext;
import org.yvasylchuk.bursdag.game.core.Player;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

@Slf4j
@Entity
@DiscriminatorValue("REGISTRATION")
public final class RegistrationStage extends Stage<RegistrationStage.Context> {
    private static void assertPlayerIsNotReady(BursdagPrincipal player, Context ctx) {
        if (ctx.playersReadiness.get(player.getId())) {
            log.error("Player already marked as ready; Can't change avatar");
            throw new IllegalArgumentException("User marked as ready");
        }
    }

    @SneakyThrows
    @Override
    public Context loadContext() {
        return Game.MAPPER.readValue(getRawContext(), Context.class);
    }

    @Override
    public boolean isReadyToComplete() {
        return runInContext(ctx -> ctx.playersReadiness
                .values()
                .stream()
                .allMatch(ready -> ready));
    }

    @Override
    public void initializeStage(GameContext gameCtx) {
        List<Player> players = getGame().loadContext().getPlayers();

        doInContext(ctx -> {
            ctx.playersIds = players.stream().map(Player::getUserId).toList();
            ctx.playersReadiness = players.stream().collect(Collectors.toMap(Player::getUserId, (id) -> false));
        });
    }

    @Override
    public void finalizeStage(GameContext gameCtx) {
        doInContext(ctx -> gameCtx.getPlayers().forEach(gp -> {
            gp.setNickname(ctx.chosenNicknames.get(gp.getUserId()));
            gp.setChosenAvatar(ctx.chosenAvatars.get(gp.getUserId()));
            gp.setChosenTraits(ctx.chosenTraits.get(gp.getUserId()));
        }));

    }

    @Override
    public Map<Integer, Integer> calculateScores() {
        boolean readyToGoNext = this.isReadyToComplete();
        return runInContext(ctx -> ctx.playersIds
                .stream()
                .collect(Collectors.toMap(identity(),
                                          (ignored) -> readyToGoNext ? 1 : 0)));
    }

    @Override
    public void act(BursdagPrincipal player, AsyncAction absAction) {
        switch (absAction) {
            case ChoseAvatarAction action -> handleChoseAvatarAction(player, action);
            case ChoseNicknameAction action -> handleChoseNicknameAction(player, action);
            case ChoseTraitsAction action -> handleChoseTraitsAction(player, action);
            case PlayerReadyAction action -> handlePlayerReadyAction(player, action);
            default -> {
                String errorMsg = "Action %s is not supported by %s"
                        .formatted(absAction, this.getClass());
                throw new IllegalArgumentException(errorMsg);
            }
        }
    }

    private void handlePlayerReadyAction(BursdagPrincipal player, PlayerReadyAction action) {
        doInContext(ctx -> {
            assertPlayerIsNotReady(player, ctx);

            boolean isPlayerFillAll = isPlayerFillAll(player);
            if (isPlayerFillAll) {
                ctx.playersReadiness.put(player.getId(), true);
            } else {
                String msg = "Player haven't fill all necessary data";
                log.warn(msg);
                throw new IllegalStateException(msg);
            }
        });
    }

    private boolean isPlayerFillAll(BursdagPrincipal player) {
        return runInContext(ctx -> !Strings.isBlank(ctx.chosenAvatars.get(player.getId())) &&
                                    !Strings.isBlank(ctx.chosenNicknames.get(player.getId())) &&
                                    ctx.chosenTraits.get(player.getId())
                                                    .values()
                                                    .stream()
                                                    .filter(Objects::nonNull)
                                                    .count() == ctx.availableTraits.size()
                           );
    }

    @Override
    public StageReadiness calculateStageReadiness() {
        return runInContext(ctx -> new StageReadiness(
                ctx.playersIds.size(),
                (int) ctx.playersReadiness.values().stream().filter(isReady -> isReady).count()
        ));
    }

    private void handleChoseTraitsAction(BursdagPrincipal player, ChoseTraitsAction action) {
        doInContext(ctx -> {
            assertPlayerIsNotReady(player, ctx);

            Set<String> categories = action.getTraits().keySet();
            if (!ctx.availableTraits.keySet().containsAll(categories)) {
                log.error("Some of provided categories are not in available ones; provided: [{}], available: [{}]",
                          categories,
                          ctx.availableTraits.keySet());

                throw new IllegalArgumentException("Invalid trait category");
            }

            for (String category : categories) {
                String chosenTrait = action.getTraits().get(category);
                if (!ctx.availableTraits.get(category).contains(chosenTrait)) {
                    log.error("Trait '{}' is not in available traits for category '{}':[{}]",
                              chosenTrait,
                              category,
                              ctx.availableTraits.get(category));

                    throw new IllegalArgumentException("Invalid trait");
                }
            }

            if (ctx.chosenTraits == null) {
                ctx.chosenTraits = new HashMap<>();
            }

            Map<String, String> userChosenTraits = ctx.chosenTraits.computeIfAbsent(player.getId(),
                                                                                    (ignored) -> new HashMap<>());
            userChosenTraits.putAll(action.getTraits());
        });
    }

    private void handleChoseNicknameAction(BursdagPrincipal player, ChoseNicknameAction action) {
        doInContext(ctx -> {
            assertPlayerIsNotReady(player, ctx);

            if (ctx.chosenNicknames == null) {
                ctx.chosenNicknames = new HashMap<>();
            }

            if (ctx.chosenNicknames.values().stream().anyMatch(chosen -> Objects.equals(chosen,
                                                                                        action.getNickname()))) {
                log.error("Nickname {} is already chosen", action.getNickname());
                throw new IllegalArgumentException("Nickname already chosen");
            }

            ctx.chosenNicknames.put(player.getId(), action.getNickname());
        });
    }

    private void handleChoseAvatarAction(BursdagPrincipal player, ChoseAvatarAction action) {
        doInContext(ctx -> {
            assertPlayerIsNotReady(player, ctx);

            if (ctx.availableAvatars.stream().noneMatch(av -> av.equals(action.getAvatar()))) {
                log.error("Invalid avatar: {}", action.getAvatar());
                throw new IllegalArgumentException("Invalid avatar - not present or already chosen");
            }

            if (ctx.chosenAvatars == null) {
                ctx.chosenAvatars = new HashMap<>();
            }

            if (ctx.chosenAvatars.containsValue(action.getAvatar())) {
                log.error("Avatar {} is already chosen", action.getAvatar());
                throw new IllegalArgumentException("Avatar is already chosen");
            }

            ctx.chosenAvatars.put(player.getId(), action.getAvatar());
        });
    }

    @Getter
    @Setter
    @Builder
    @Jacksonized
    @AllArgsConstructor
    public static class Context {
        private List<Integer> playersIds;

        private Map<Integer, String> chosenAvatars;
        private Map<Integer, Map<String, String>> chosenTraits;
        private Map<Integer, String> chosenNicknames;
        private Map<Integer, Boolean> playersReadiness;

        private List<String> availableAvatars;
        private Map<String, List<String>> availableTraits;
    }
}
