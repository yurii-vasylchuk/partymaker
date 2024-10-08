package org.yvasylchuk.bursdag.game.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.yvasylchuk.bursdag.common.dto.AsyncAction;
import org.yvasylchuk.bursdag.common.dto.BursdagPrincipal;
import org.yvasylchuk.bursdag.common.dto.OutgoingMessage;
import org.yvasylchuk.bursdag.common.dto.events.GameStateChanged;
import org.yvasylchuk.bursdag.dao.entity.UserEntity;
import org.yvasylchuk.bursdag.game.core.stages.Stage;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Table(name = "games")
@Entity
@Getter
@Setter(value = AccessLevel.PROTECTED)
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Game {
    public static final ObjectMapper MAPPER = new ObjectMapper();
    @Transient
    private final List<OutgoingMessage<?>> events = new ArrayList<>();
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "context")
    @Getter(AccessLevel.PROTECTED)
    private String rawContext;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "game", orphanRemoval = true)
    private List<UserEntity> players;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "game", orphanRemoval = true)
    @OrderColumn(name = "stage_order")
    private List<Stage<?>> stages;
    @OneToOne
    @JoinColumn(name = "current_stage", referencedColumnName = "id")
    private Stage<?> currentStage;

    @SneakyThrows
    public GameContext loadContext() {
        return MAPPER.readValue(rawContext, GameContext.class);
    }

    @SneakyThrows
    public void updateContext(GameContext ctx) {
        rawContext = MAPPER.writeValueAsString(ctx);
    }

    public void join(BursdagPrincipal player) {
        if (players.stream()
                   .noneMatch(e -> Objects.equals(e.getId(), player.getId()))) {
            log.error("Player is not a participant; {}", player);
        }

        doInContext(ctx -> {
            if (ctx.getPlayers() == null) {
                ctx.setPlayers(new ArrayList<>());
            }

            if (ctx.getPlayers().stream().anyMatch(p -> Objects.equals(p.getUserId(), player.getId()))) {
                log.warn("Player {} already joined", player);
                return;
            }

            ctx.getPlayers()
               .add(Player.builder()
                          .userId(player.getId())
                          .username(player.getUsername())
                          .build());
        });

        onGameChanged();
    }

    private void onGameChanged() {
        events.add(new OutgoingMessage<>(
                "/topic/game",
                new GameStateChanged(this)
        ));
    }

    public void completeStage() {
        if (status != Status.ON_STAGE) {
            final String message = "Can't complete stage: current game status is %s, but %s status is required"
                    .formatted(status, Status.ON_STAGE);
            log.warn(message);
            throw new IllegalStateException(message);
        }

        if (!currentStage.isReadyToComplete()) {
            throw new IllegalStateException("Stage is not ready to go next");
        }

        doInContext(ctx -> {
            currentStage.finalizeStage(ctx);

            Map<Integer, Integer> scores = currentStage.calculateScores();
            Set<Integer> allPlayersIds = ctx.getPlayers().stream().map(Player::getUserId).collect(Collectors.toSet());
            if (!allPlayersIds.containsAll(scores.keySet())) {
                log.error(
                        "Stage {} (#{}) calculated scores wrongly: some playerId(s) from scores is not found in game players list",
                        currentStage.getName(),
                        currentStage.getId());
                throw new IllegalStateException("Stage provided wrong scores");
            }

            ctx.setLastStageScores(scores);
            scores.forEach((playerId, score) -> ctx.getGameScores().merge(playerId, score, Integer::sum));
        });

        status = Status.STAGE_COMPLETED;

        onGameChanged();
    }


    public void goToNextStage() {
        if (status != Status.STAGE_COMPLETED && status != Status.NEW) {
            log.error("Can't go to next stage because game in status: {}", status);
            throw new IllegalStateException("Illegal game status");
        }

        int currentStageOrder = currentStage != null ? currentStage.getOrder() : -1;

        if (currentStageOrder == -1) {
            initializeGame();
        }

        Optional<Stage<?>> nextStage = stages.stream()
                                             .filter(Objects::nonNull)
                                             .filter(s -> s.getOrder() > currentStageOrder)
                                             .min(Comparator.comparingInt(Stage::getOrder));

        if (nextStage.isPresent()) {
            currentStage = nextStage.get();
            status = Status.ON_STAGE;
            doInContext(currentStage::initializeStage);
        } else {
            currentStage = null;
            status = Status.FINALIZED;
            finalizeGame();
        }

        onGameChanged();
    }

    private void initializeGame() {
        doInContext(ctx -> ctx.setGameScores(ctx.getPlayers()
                                                .stream()
                                                .map(Player::getUserId)
                                                .collect(Collectors.toMap(Function.identity(),
                                                                          (ignored) -> 0))));
    }

    private void finalizeGame() {
        status = Status.FINALIZED;
    }

    public boolean hasEvents() {
        return !this.events.isEmpty();
    }

    private <T> T runInContext(Function<GameContext, T> operation) {
        GameContext context = loadContext();
        T result = operation.apply(context);
        updateContext(context);
        return result;
    }

    private void doInContext(Consumer<GameContext> operation) {
        GameContext context = loadContext();
        operation.accept(context);
        updateContext(context);
    }

    public void adminAct(BursdagPrincipal actor, AsyncAction action) {
        if (currentStage == null) {
            throw new IllegalStateException("Game haven't started yet");
        }

        boolean invalidPlayer = players.stream()
                                       .noneMatch(p -> Objects.equals(p.getId(), actor.getId()) && actor.getRoles()
                                                                                                        .contains(
                                                                                                                "ADMIN"));
        if (invalidPlayer) {
            throw new IllegalArgumentException(
                    "Logged in user is not related for current game or don't have admin authorities");
        }

        currentStage.act(actor, action);

        onGameChanged();
    }

    public void act(BursdagPrincipal player, AsyncAction action) {
        if (currentStage == null) {
            throw new IllegalStateException("Game haven't started yet");
        }

        boolean invalidPlayer = runInContext(ctx -> ctx.getPlayers()
                                                       .stream()
                                                       .noneMatch(p -> Objects.equals(p.getUserId(), player.getId())));

        if (invalidPlayer) {
            throw new IllegalArgumentException("Logged in user is absent in a game's players list");
        }

        currentStage.act(player, action);

        onGameChanged();
    }

    public enum Status {
        NEW, ON_STAGE, STAGE_COMPLETED, FINALIZED
    }
}
