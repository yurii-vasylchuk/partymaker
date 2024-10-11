package org.yvasylchuk.bursdag.game.core.stages;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.yvasylchuk.bursdag.common.dto.AsyncAction;
import org.yvasylchuk.bursdag.common.dto.BursdagPrincipal;
import org.yvasylchuk.bursdag.game.core.Game;
import org.yvasylchuk.bursdag.game.core.GameContext;
import org.yvasylchuk.bursdag.game.core.Player;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Collections.emptyMap;

@Slf4j
@Entity
@DiscriminatorValue("CONTEST")
public class ContestStage extends Stage<ContestStage.Context> {
    private static void assertPlayerIsNotReady(BursdagPrincipal player, ContestStage.Context ctx) {
        if (ctx.playersReadiness.get(player.getId())) {
            log.error("Player already marked as ready;");
            throw new IllegalArgumentException("User marked as ready");
        }
    }

    @Override
    public void act(BursdagPrincipal player, AsyncAction absAction) {
        switch (absAction) {
            case AsyncAction.PlayerReadyAction action -> handlePlayerReadyAction(player, action);
            case AsyncAction.PlacementsSetAction action -> handlePlacementAction(player, action);
            case AsyncAction.NextTaskAction action -> handleNextTaskAction(player, action);
            case AsyncAction.PrevTaskAction action -> handlePrevTaskAction(player, action);

            default -> {
                String errorMsg = "Action %s is not supported by %s"
                        .formatted(absAction, this.getClass());
                log.warn(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }
        }
    }

    private void handlePrevTaskAction(BursdagPrincipal player, AsyncAction.PrevTaskAction action) {
        doInContext(ctx -> {
            if (ctx.currentTaskIdx <= 0) {
                throw new IllegalStateException("Can't go to previous task - already on first one");
            }

            ctx.currentTaskIdx--;
            if (ContestState.RANKING == ctx.state) {
                ctx.state = ContestState.TASK;
            }
        });
    }

    private void handleNextTaskAction(BursdagPrincipal player, AsyncAction.NextTaskAction action) {
        doInContext(ctx -> {
            if (ctx.currentTaskIdx < (ctx.tasksDistribution.size() - 1)) {
                ctx.currentTaskIdx++;
            } else if (ctx.currentTaskIdx == (ctx.tasksDistribution.size() - 1)) {
                ctx.state = ContestState.RANKING;
                ctx.currentTaskIdx++;
            } else {
                throw new IllegalStateException("Invalid current task idx: bigger then distributed tasks count");
            }
        });
    }

    private void handlePlacementAction(BursdagPrincipal player, AsyncAction.PlacementsSetAction action) {
        doInContext(ctx -> {
            assertPlayerIsNotReady(player, ctx);

            if (ctx.state != ContestState.RANKING) {
                log.warn("Adding places available only if context state is {}, but current is {}",
                         ContestState.RANKING,
                         ctx.state);
                throw new IllegalStateException("Illegal contest state is %s".formatted(ContestState.RANKING));
            }

            Set<Integer> playersIds = ctx.players.stream().map(Player::getUserId).collect(Collectors.toSet());

            Optional<Integer> notPresentPlayer = action.getPlacements()
                                                       .values()
                                                       .stream()
                                                       .filter(Objects::nonNull)
                                                       .filter(playerId -> !playersIds.contains(playerId))
                                                       .findFirst();

            if (notPresentPlayer.isPresent()) {
                log.warn("Invalid placement: player with id {} is not present in game", notPresentPlayer.get());
                throw new IllegalArgumentException("Player with provided id is not found");
            }

            boolean isAllPositionsCorrect = action.getPlacements()
                                                  .keySet()
                                                  .stream()
                                                  .allMatch(position -> position > 0 && position <= action.getPlacements()
                                                                                                          .size());

            if (!isAllPositionsCorrect) {
                String msg = "Placements should contain only consecutive numbers starting from 1 (inclusive)";
                log.warn(msg);
                throw new IllegalArgumentException(msg);
            }

            ctx.placements.put(player.getId(), action.getPlacements());
        });
    }

    private void handlePlayerReadyAction(BursdagPrincipal player, AsyncAction.PlayerReadyAction action) {
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

    private boolean isPlayerFillAll(ContestStage.Context ctx, BursdagPrincipal player) {
        Map<Integer, Integer> userPlacements = ctx.placements.getOrDefault(player.getId(), emptyMap());

        return userPlacements.size() == ctx.winningPlaces && userPlacements.values()
                                                                           .stream()
                                                                           .allMatch(Objects::nonNull);
    }

    @Override
    public boolean isReadyToComplete() {
        return runInContext(ctx -> ctx.playersReadiness.size() == ctx.players.size() &&
                ctx.playersReadiness.values().stream().allMatch(isReady -> isReady));
    }

    @Override
    public Map<Integer, Integer> calculateScores() {
        return runInContext(ctx -> {
            Map<Integer, Integer> combinedPlacements = new HashMap<>();
            Map<Integer, Integer> results = new HashMap<>();

            ctx.placements.values()
                          .forEach(playerPlacements -> playerPlacements.forEach((place, playerId) -> combinedPlacements.merge(
                                  playerId,
                                  place != null ? place : 0,
                                  Integer::sum)));

            List<Integer> winners = combinedPlacements.entrySet()
                                                      .stream()
                                                      .sorted(Comparator.comparingInt(Map.Entry::getValue))
                                                      .limit(ctx.scoresPerPlace.size())
                                                      .map(Map.Entry::getKey)
                                                      .toList();

            for (int i = 0; i < winners.size(); i++) {
                results.put(winners.get(i), ctx.scoresPerPlace.get(i));
            }

            return results;
        });
    }

    @Override
    public StageReadiness calculateStageReadiness() {
        return runInContext(context -> new StageReadiness(
                context.players.size(),
                (int) context.playersReadiness.values().stream().filter(isReady -> isReady).count()
        ));
    }

    @SneakyThrows
    @Override
    public Context loadContext() {
        return Game.MAPPER.readValue(this.getRawContext(), ContestStage.Context.class);
    }

    @Override
    public void initializeStage(GameContext gameCtx) {
        doInContext(ctx -> {
            List<Player> players = gameCtx.getPlayers();

            ctx.players = players;

            ctx.playersReadiness = players.stream().collect(Collectors.toMap(
                    Player::getUserId,
                    ignored -> false));

            ctx.currentTaskIdx = 0;
            ctx.state = ContestState.TASK;

            switch (ctx.distributionType) {
                case RANDOM -> distributeTasksRandomly(ctx);
                case PREDEFINED -> assertTasksAreDistributed(ctx);
                default ->
                        throw new IllegalStateException("Unhandled distribution type%s".formatted(ctx.distributionType));
            }

            Map<Integer, Integer> initialScores = emptyMap();

            ctx.placements = players.stream().collect(Collectors.toMap(
                    Player::getUserId,
                    ignored -> initialScores
                                                                      ));
        });
    }

    private void assertTasksAreDistributed(Context ctx) {
        Set<Integer> usersWithTasks = ctx.tasksDistribution.stream()
                                                           .flatMap(utl -> utl.usersIds.stream())
                                                           .collect(Collectors.toSet());
        List<Integer> allUsers = ctx.players.stream().map(Player::getUserId).toList();

        if (allUsers.size() != usersWithTasks.size() || !usersWithTasks.containsAll(allUsers)) {
            throw new IllegalStateException("Not all users have distributed tasks");
        }
    }

    private void distributeTasksRandomly(Context ctx) {
        List<Player> players = ctx.players;

        List<Integer> tasksIndexes = IntStream.range(0, ctx.availableTasks.size())
                                              .boxed()
                                              .collect(Collectors.toCollection(LinkedList::new));
        List<Integer> usersIds = players.stream()
                                        .map(Player::getUserId)
                                        .collect(Collectors.toCollection(LinkedList::new));

        Collections.shuffle(usersIds);
        Collections.shuffle(tasksIndexes);

        ctx.tasksDistribution = new ArrayList<>();
        for (int i = 0; i <= (usersIds.size() / ctx.usersPerTask); i++) {
            List<Integer> taskUsersIds = new ArrayList<>();
            for (int j = 0; j < ctx.usersPerTask; j++) {
                int userIdx = j + (i * ctx.usersPerTask);
                if (userIdx == usersIds.size()) {
                    break;
                }

                taskUsersIds.add(usersIds.get(userIdx));
            }

            if (!taskUsersIds.isEmpty()) {
                ctx.tasksDistribution.add(new UserTaskLink(taskUsersIds, tasksIndexes.get(i)));
            }
        }
    }

    private enum ContestTaskDistribution {
        RANDOM, PREDEFINED
    }

    private enum ContestState {
        TASK, RANKING
    }

    @Getter
    @Setter
    @Builder
    @Jacksonized
    @AllArgsConstructor
    public static class Context {
        // Dynamic data
        private Map<Integer, Map<Integer, Integer>> placements;
        private ContestState state;

        // Data, might be calculated on initialization (or provided)
        // Key - userId, value - taskIdx
        private List<UserTaskLink> tasksDistribution;
        private Integer currentTaskIdx;

        // Provided data
        private List<ContestTask> availableTasks;
        private ContestTaskDistribution distributionType;
        private Boolean isTaskDetailsPrivate;
        private Integer usersPerTask;
        private Integer winningPlaces;
        private List<Integer> scoresPerPlace;

        // Common data
        private List<Player> players;
        private Map<Integer, Boolean> playersReadiness;
    }

    public record UserTaskLink(List<Integer> usersIds, Integer taskIdx) {
    }

    @Getter
    @Setter
    @Builder
    @Jacksonized
    @AllArgsConstructor
    private static class ContestTask {
        private String title;
        private String description;
    }
}
