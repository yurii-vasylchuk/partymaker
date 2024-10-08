package org.yvasylchuk.bursdag.game.core.stages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import lombok.extern.slf4j.Slf4j;
import org.yvasylchuk.bursdag.common.dto.AsyncAction;
import org.yvasylchuk.bursdag.common.dto.AsyncAction.ChoseWinnerAction;
import org.yvasylchuk.bursdag.common.dto.AsyncAction.NextMatchAction;
import org.yvasylchuk.bursdag.common.dto.AsyncAction.VoteAction;
import org.yvasylchuk.bursdag.common.dto.BursdagPrincipal;
import org.yvasylchuk.bursdag.game.core.Game;
import org.yvasylchuk.bursdag.game.core.GameContext;
import org.yvasylchuk.bursdag.game.core.Player;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

@Slf4j
@Entity
@DiscriminatorValue("TOURNAMENT")
public class TournamentStage extends Stage<TournamentStage.Context> {
    private static List<Match> prepareNextRound(TournamentStage.Context ctx) {
        Integer nextRoundId = ctx.currentRound + 1;

        List<Integer> competitors;
        if (ctx.currentRound == -1) {
            competitors = ctx.players.stream().map(Player::getUserId).collect(
                    ArrayList::new,
                    ArrayList::add,
                    ArrayList::addAll
                                                                             );

        } else {
            int scanningRoundId = ctx.currentRound;
            competitors = new ArrayList<>();
            boolean searchWinners = true;
            while (competitors.size() < 2 && scanningRoundId >= 0) {
                int finalScanningRoundId = scanningRoundId;

                if (searchWinners) {
                    ctx.matches.stream()
                               .filter(m -> m.roundId == finalScanningRoundId)
                               .map(Match::getWinnerId)
                               .filter(pid -> !ctx.winners.contains(pid))
                               .forEach(competitors::add);
                    searchWinners = false;
                } else {
                    ctx.matches.stream()
                               .filter(m -> m.roundId == finalScanningRoundId)
                               .flatMap(m -> m.getCompetitors().stream())
                               .filter(pid -> !ctx.winners.contains(pid))
                               .forEach(competitors::add);

                    searchWinners = true;
                    scanningRoundId--;
                }
            }
        }

        if (competitors.isEmpty()) {
            String msg = "Can't find any competitor for next round";
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        if (competitors.size() == 1) {
            return List.of(Match.builder()
                                .roundId(nextRoundId)
                                .competitors(competitors)
                                .winnerId(competitors.getFirst())
                                .build());
        }
        if (ctx.matches == null) {
            ctx.matches = new ArrayList<>();
        }

        Collections.shuffle(competitors);
        Set<MatchTemplate> takenTasks = new HashSet<>(ctx.matches);
        Set<MatchTemplate> availableTasks =
                ctx.matchTemplates.stream()
                                  .filter(task -> takenTasks.stream().noneMatch(t -> Objects.equals(t.tid, task.tid)))
                                  .collect(HashSet::new,
                                           HashSet::add,
                                           HashSet::addAll);

        List<Match> result = new ArrayList<>();

        for (int i = 0; i < competitors.size() / 2; i++) {
            List<MatchType> applicableTypes = new ArrayList<>();
            applicableTypes.add(MatchType.SAME_FOR_ALL);
            if (competitors.size() % 2 == 1 && i == (competitors.size() / 2) - 1) {
                applicableTypes.add(MatchType.DISTINCT_3);
            } else {
                applicableTypes.add(MatchType.DISTINCT_2);
            }

            MatchTemplate selectedTask = availableTasks.stream()
                                                       .filter(t -> applicableTypes.contains(t.type))
                                                       .min(Comparator.comparingInt(t -> t.type.ordinal()))
                                                       .orElseThrow(() -> new IllegalStateException("Not enough tasks"));
            availableTasks.remove(selectedTask);
            result.add(Match.builder()
                            .competitors(competitors.stream().skip(i * 2).limit(2).collect(
                                    ArrayList::new,
                                    ArrayList::add,
                                    ArrayList::addAll
                                                                                          ))
                            .tid(selectedTask.tid)
                            .roundId(nextRoundId)
                            .type(selectedTask.type)
                            .competitorsTasks(selectedTask.competitorsTasks)
                            .build());
        }

        if (competitors.size() % 2 == 1) {
            result.getLast().competitors.add(competitors.getLast());
        }

        return result;
    }

    @Override
    public void act(BursdagPrincipal player, AsyncAction abstractAction) {
        switch (abstractAction) {
            case VoteAction action -> handleVoteAction(action, player);
            case ChoseWinnerAction action -> handleChoseWinnerAction(action, player);
            case NextMatchAction action -> handleNextMatchAction(action, player);
            default -> {
                String errorMsg = "Action %s is not supported by %s"
                        .formatted(abstractAction, this.getClass());
                log.warn(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }
        }
    }

    private void handleNextMatchAction(NextMatchAction action, BursdagPrincipal player) {
        if (!player.getRoles().contains("ADMIN")) {
            String msg = "Only admin can switch matches";
            log.warn(msg);
            throw new IllegalArgumentException(msg);
        }

        doInContext(ctx -> {
            Match currentMatch = ctx.matches.get(ctx.currentMatchIdx);
            if (currentMatch.winnerId == null) {
                String msg = "Can't go to the next match: winner isn't set for current";
                log.error(msg);
                throw new IllegalStateException(msg);
            }

            if (ctx.currentMatchIdx < (ctx.matches.size() - 1)) {
                ctx.currentMatchIdx++;
                return;
            }

            List<List<Match>> rounds = ctx.matches.stream()
                                                  .collect(Collectors.groupingBy(Match::getRoundId))
                                                  .entrySet()
                                                  .stream()
                                                  .sorted(Comparator.comparingInt(Map.Entry::getKey))
                                                  .map(Map.Entry::getValue)
                                                  .toList();

            List<Integer> roundWinners = rounds.getLast()
                                               .stream()
                                               .map(Match::getWinnerId)
                                               .toList();
            if (roundWinners.size() > 1) {
                ctx.matches.addAll(prepareNextRound(ctx));
                ctx.currentMatchIdx++;
                ctx.currentRound++;
                return;
            }

            ctx.winners.add(roundWinners.getFirst());

            if (ctx.winners.size() == ctx.scoresPerPlace.size()) {
                //TODO: Finish tournament somehow
                return;
            }

            List<Integer> roundLosers = rounds.getLast()
                                              .stream()
                                              .flatMap(m -> m.competitors.stream())
                                              .filter(c -> !ctx.winners.contains(c))
                                              .toList();

            if (roundLosers.size() == 1) {
                ctx.winners.add(roundLosers.getFirst());
            }

            if (ctx.winners.size() == ctx.scoresPerPlace.size()) {
                //TODO: Finish tournament somehow
                return;
            }

            ctx.matches.addAll(prepareNextRound(ctx));
            ctx.currentMatchIdx++;
            ctx.currentRound++;
        });
    }

    private void handleChoseWinnerAction(ChoseWinnerAction action, BursdagPrincipal player) {
        if (!player.getRoles().contains("ADMIN")) {
            String msg = "Only admin can choose winner for match";
            log.warn(msg);
            throw new IllegalArgumentException(msg);
        }

        doInContext(ctx -> {
            Match currentMatch = ctx.currentMatch();

            if (!currentMatch.competitors.contains(action.getWinnerId())) {
                String msg = "Can't find competitor with id %d".formatted(action.getWinnerId());
                log.warn(msg);
                throw new IllegalArgumentException(msg);
            }

            currentMatch.winnerId = action.getWinnerId();
        });
    }

    private void handleVoteAction(VoteAction action, BursdagPrincipal player) {
        doInContext(ctx -> {
            if (ctx.winDecider != WinDecider.VOTING) {
                log.error("Voting is not applicable for current tournament: WinDecider is configured to {}",
                          ctx.winDecider);
                throw new IllegalStateException("Voting is not applicable for current tournament");
            }

            Match currentMatch = ctx.currentMatch();
            if (!currentMatch.competitors.contains(action.getVotedPlayer())) {
                String msg = "Can't find competitor with id %d".formatted(action.getVotedPlayer());
                log.warn(msg);
                throw new IllegalArgumentException(msg);
            }

            Integer previousVote = ctx.votes.put(player.getId(), action.getVotedPlayer());

            if (previousVote == null) {
                log.debug("Player {} voted for {}", player.getId(), action.getVotedPlayer());
            } else {
                log.debug("Player {} changed vote from {} to {}",
                          player.getId(),
                          previousVote,
                          action.getVotedPlayer());
            }
        });
    }

    @Override
    public boolean isReadyToComplete() {
        StageReadiness readiness = calculateStageReadiness();
        return readiness.total() != null && Objects.equals(readiness.total(), readiness.ready());
    }

    @Override
    public Map<Integer, Integer> calculateScores() {
        return runInContext(ctx -> ctx.winners == null ?
                emptyMap() :
                ctx.winners.stream().collect(Collectors.toMap(
                        Function.identity(),
                        winnerId -> ctx.scoresPerPlace.get(ctx.winners.indexOf(winnerId))
                                                             )));
    }

    @Override
    public StageReadiness calculateStageReadiness() {
        return runInContext(ctx -> new StageReadiness(
                ctx.scoresPerPlace.size(),
                ctx.winners.size()
        ));
    }

    @SneakyThrows
    @Override
    public Context loadContext() {
        return Game.MAPPER.readValue(getRawContext(), TournamentStage.Context.class);
    }

    @Override
    public void initializeStage(GameContext gameCtx) {
        doInContext(ctx -> {
            ctx.players = gameCtx.getPlayers();
            ctx.winners = emptyList();
            ctx.currentRound = -1;
            Collections.shuffle(ctx.matchTemplates);
            AtomicInteger tid = new AtomicInteger(0);
            ctx.matchTemplates.forEach(template -> template.tid = tid.incrementAndGet());

            ctx.matches = prepareNextRound(ctx);
            ctx.currentRound++;
            ctx.currentMatchIdx = 0;
        });
    }

    public enum WinDecider {
        GAME_MASTER,
        VOTING,
    }

    public enum CompetitorTaskType {
        IMAGE, TEXT
    }

    public enum MatchType {
        DISTINCT_2,
        DISTINCT_3,
        SAME_FOR_ALL,
    }

    @Getter
    @Setter
    @Builder
    @Jacksonized
    @AllArgsConstructor
    public static class Context {
        // Dynamic
        private List<Integer> winners;
        private List<Match> matches;
        private int currentRound;
        private int currentMatchIdx;
        // VoterID -> CompetitorID
        private Map<Integer, Integer> votes;

        // Provided
        private List<Integer> scoresPerPlace;
        private List<MatchTemplate> matchTemplates;
        private WinDecider winDecider;

        // Common data
        private List<Player> players;

        @JsonIgnore
        public Integer getPlacesCount() {
            return Math.min(scoresPerPlace.size(), players.size());
        }

        @JsonIgnore
        public Match currentMatch() {
            return matches.get(currentMatchIdx);
        }
    }

    @Getter
    @Setter
    @SuperBuilder
    @Jacksonized
    public static class Match extends MatchTemplate {
        private Integer roundId;
        private Integer winnerId;
        private List<Integer> competitors;
    }

    @Getter
    @Setter
    @SuperBuilder
    @Jacksonized
    @AllArgsConstructor
    public static class MatchTemplate {
        // Template ID
        protected Integer tid;
        protected MatchType type;
        protected List<CompetitorTask> competitorsTasks;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
    @JsonSubTypes(failOnRepeatedNames = true,
            value = {
                    @JsonSubTypes.Type(value = CompetitorTask.ImageCompetitorTask.class, name = "IMAGE"),
                    @JsonSubTypes.Type(value = CompetitorTask.TextCompetitorTask.class, name = "TEXT")
            })
    public abstract static sealed class CompetitorTask {
        public abstract CompetitorTaskType getType();

        @Getter
        @Builder
        @Jacksonized
        @RequiredArgsConstructor
        public static final class ImageCompetitorTask extends CompetitorTask {
            private final CompetitorTaskType type = CompetitorTaskType.IMAGE;
            private final String path;
        }

        @Getter
        @Builder
        @Jacksonized
        @RequiredArgsConstructor
        public static final class TextCompetitorTask extends CompetitorTask {
            private final CompetitorTaskType type = CompetitorTaskType.TEXT;
            private final String text;
        }
    }
}
