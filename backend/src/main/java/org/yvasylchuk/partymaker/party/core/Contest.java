package org.yvasylchuk.partymaker.party.core;

import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.PersistenceCreator;
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal;
import org.yvasylchuk.partymaker.exception.PartymakerException;
import org.yvasylchuk.partymaker.party.core.command.TasksDistribution;
import org.yvasylchuk.partymaker.party.core.command.UpsertPartyGameCommand.UpsertContestCommand;
import org.yvasylchuk.partymaker.party.dto.management.ContestTaskDto;
import org.yvasylchuk.partymaker.party.dto.process.OutgoingMessage;
import org.yvasylchuk.partymaker.party.dto.process.PartyAction;

import java.util.*;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static org.yvasylchuk.partymaker.exception.PartymakerException.Kind.GENERIC_CLIENT;
import static org.yvasylchuk.partymaker.exception.PartymakerException.Kind.INVALID_CONFIGURATION;
import static org.yvasylchuk.partymaker.party.core.command.TasksDistribution.PREDEFINED;
import static org.yvasylchuk.partymaker.party.core.command.TasksDistribution.RANDOM;

@Slf4j
@Getter
@SuperBuilder
public class Contest extends Game<Contest.Context> {
    @PersistenceCreator
    protected Contest(Integer order,
                      String name,
                      String description,
                      GameType type,
                      Context context) {
        super(order, name, description, type, context);
    }

    public static Contest create(Integer order,
                                 UpsertContestCommand command) {

        Context context = new Context(
                new ArrayList<>(),
                command.getScorePerPlace(),
                command.getTasksDistribution(),
                new ArrayList<>(),
                new ArrayList<>(),
                new HashMap<>(),
                ContestStage.CONFIGURED,
                -1
        );

        for (ContestTaskDto dto : command.getTasks()) {
            MatchTask task = new MatchTask(dto.task());
            context.tasks.add(task);

            if (command.getTasksDistribution() == PREDEFINED) {
                context.matches.add(new Match(task, dto.participantId()));
            }
        }

        return new Contest(order, command.getName(), command.getDescription(), GameType.CONTEST, context);
    }

    @Override
    void act(PartymakerPrincipal actor, PartyAction action, Consumer<OutgoingMessage<?>> eventsCollector) {
        switch (action) {
            case PartyAction.PlayerReadyAction playerReadyAction ->
                    handlePlayerReadyAction(actor, playerReadyAction, eventsCollector);
            case PartyAction.ContestVotingAction votingAction ->
                    handleContestVotingAction(actor, votingAction, eventsCollector);
            case PartyAction.NextMatchAction nextMatchAction ->
                    handleNextMatchAction(actor, nextMatchAction, eventsCollector);
            case PartyAction.PrevMatchAction prevMatchAction ->
                    handlePrevMatchAction(actor, prevMatchAction, eventsCollector);
            case PartyAction.StartVotingAction startVotingAction ->
                    handleStartVotingAction(actor, startVotingAction, eventsCollector);
            default -> log.info("Contest ignores {} actions. Details: {}", action.getType(), action);
        }
    }

    private void handleStartVotingAction(PartymakerPrincipal actor, PartyAction.StartVotingAction action, Consumer<OutgoingMessage<?>> eventsCollector) {
        if (context.stage != ContestStage.CONTEST) {
            throw new PartymakerException(GENERIC_CLIENT,
                                          "'%s' is not available: wrong contest stage; Allowed stage: '%s', current stage: '%s'",
                                          action.getType(),
                                          ContestStage.CONTEST,
                                          context.stage);
        }

        if (context.currentMatch < (context.matches.size() - 1)) {
            log.warn("Unable to start voting: not all matches are played; Current match is {}, total matches: {}",
                     context.currentMatch + 1,
                     context.matches.size());

            throw new PartymakerException(
                    GENERIC_CLIENT,
                    "Unable to start voting: not all matches are played; Current match is %d, total matches: %d",
                    context.currentMatch + 1,
                    context.matches.size());
        }

        context.stage = ContestStage.VOTING;
    }

    private void handlePrevMatchAction(PartymakerPrincipal actor, PartyAction.PrevMatchAction action, Consumer<OutgoingMessage<?>> eventsCollector) {
        if (context.stage != ContestStage.CONTEST) {
            throw new PartymakerException(GENERIC_CLIENT,
                                          "'%s' is not available: wrong contest stage; Allowed stage: '%s', current stage: '%s'",
                                          action.getType(),
                                          ContestStage.CONTEST,
                                          context.stage);
        }

        if (context.currentMatch <= 0) {
            throw new PartymakerException(GENERIC_CLIENT,
                                          "Can't go to previous match: current match is already first");
        }

        context.currentMatch--;
    }

    private void handleNextMatchAction(PartymakerPrincipal actor, PartyAction.NextMatchAction action, Consumer<OutgoingMessage<?>> eventsCollector) {
        if (context.stage != ContestStage.CONTEST) {
            throw new PartymakerException(GENERIC_CLIENT,
                                          "'%s' is not available: wrong contest stage; Allowed stage: '%s', current stage: '%s'",
                                          action.getType(),
                                          ContestStage.CONTEST,
                                          context.stage);
        }

        if (context.currentMatch >= (context.matches.size() - 1)) {
            throw new PartymakerException(GENERIC_CLIENT,
                                          "Can't go to next match: current match is already last");
        }

        context.currentMatch++;
    }

    private void handleContestVotingAction(PartymakerPrincipal actor, PartyAction.ContestVotingAction votingAction, Consumer<OutgoingMessage<?>> eventsCollector) {
        if (context.stage != ContestStage.VOTING) {
            throw new PartymakerException(GENERIC_CLIENT,
                                          "'%s' is not available: wrong contest stage; Allowed stage: '%s', current stage: '%s'",
                                          votingAction.getType(),
                                          ContestStage.VOTING,
                                          context.stage);
        }

        if (votingAction.getPlaces().stream().anyMatch(Objects::isNull)) {
            throw new PartymakerException(GENERIC_CLIENT, "Invalid vote: should not be null");
        }

        if (votingAction.getPlaces().stream().anyMatch(actor.id()::equals)) {
            throw new PartymakerException(GENERIC_CLIENT, "You can not vote for yourself");
        }

        Optional<String> invalidVote = votingAction.getPlaces()
                                                   .stream()
                                                   .filter(vote -> context.members.stream()
                                                                                  .map(PartyMember::getId)
                                                                                  .noneMatch(vote::equals))
                                                   .findAny();

        if (invalidVote.isPresent()) {
            throw new PartymakerException(GENERIC_CLIENT, "Invalid vote: %s is not a member", invalidVote.get());
        }

        context.votes.put(actor.id(), votingAction.getPlaces());
    }

    private void handlePlayerReadyAction(PartymakerPrincipal actor, PartyAction.PlayerReadyAction action, Consumer<OutgoingMessage<?>> eventsCollector) {
        String playerId = actor.id();

        int playerVotesCount = context.votes.getOrDefault(playerId, emptyList()).size();
        int expectedVotesCount = context.scoresPerPlace.size();

        if (playerVotesCount < expectedVotesCount) {
            throw new PartymakerException(GENERIC_CLIENT,
                                          "Player haven't fully voted: %d of %d",
                                          playerVotesCount,
                                          expectedVotesCount);
        }

        //TODO: Implement readiness list with making voting unavailable or remove this method
    }

    @Override
    void initializeGame(Party.PartyContext partyContext) {
        List<PartyMember> members = new ArrayList<>(partyContext.getMembers());
        Collections.shuffle(members);
        context.setMembers(members);

        if (context.matchDistribution == RANDOM) {
            if (members.size() > context.tasks.size()) {
                throw new PartymakerException(
                        INVALID_CONFIGURATION,
                        "Invalid contest %s configuration: members more than tasks",
                        name);
            }


            Iterator<MatchTask> matchTaskIterator = context.tasks.iterator();
            ArrayList<Match> matches = context.members.stream()
                                                      .map(m -> new Match(matchTaskIterator.next(), m.getId()))
                                                      .collect(ArrayList::new,
                                                               List::add,
                                                               List::addAll);
            Collections.shuffle(matches);

            context.setMatches(matches);
        } else {
            if (context.matches == null ||
                members.stream()
                       .map(PartyMember::getId)
                       .anyMatch(mid -> context.matches.stream().noneMatch(m -> m.competitor.equals(mid)))) {
                throw new PartymakerException(
                        INVALID_CONFIGURATION,
                        "Invalid contest matches configuration: match distribution is %s, but some competitor(s) doesn't configured match",
                        PREDEFINED
                );
            }
        }

        context.stage = ContestStage.CONTEST;
        context.currentMatch = 0;
    }

    @Override
    void finalizeGame() {
        if (context.stage != ContestStage.VOTING) {
            log.warn(
                    "Unable to finalize contest: finalizing is only available for contests in status {}, but current status is {}",
                    ContestStage.VOTING,
                    context.stage);
            throw new PartymakerException(
                    GENERIC_CLIENT,
                    "Unable to finalize contest: finalizing is only available for contests in status %s, but current status is %s",
                    ContestStage.VOTING,
                    context.stage);
        }

        int expectedVotesCount = Math.min(context.scoresPerPlace.size(), context.members.size() - 1);
        for (List<String> playerVotes : context.votes.values()) {
            int playerVotesCount = playerVotes.size();

            if (playerVotesCount < expectedVotesCount) {
                throw new PartymakerException(GENERIC_CLIENT,
                                              "Player haven't fully voted: %d of %d",
                                              playerVotesCount,
                                              expectedVotesCount);
            }
        }

        context.stage = ContestStage.FINISHED;
    }

    @Override
    public PlayersReadiness calculateStageReadiness() {
        return new PlayersReadiness(
                (int) context.votes.values()
                                   .stream()
                                   .filter(votes -> votes.size() >= Math.min(context.scoresPerPlace.size(), context.members.size() - 1))
                                   .count(),
                context.members.size(),
                context.stage.order,
                ContestStage.values().length
        );
    }

    @Override
    public Map<String, Integer> calculateScores() {
        HashMap<String, Integer> votesByMember = new HashMap<>();
        List<Integer> scoresPerPlace = context.scoresPerPlace;


        context.votes.forEach((voter, memberVotes) -> {
            for (int i = 0; i < memberVotes.size(); i++) {
                final int place = i;

                if (scoresPerPlace.size() <= place) {
                    log.warn("Too much votes: {}, scores per place count: {}, voter {}, contest name: {}",
                             memberVotes.size(), scoresPerPlace.size(), voter, name);
                    break;
                }

                votesByMember.compute(memberVotes.get(i),
                              (ignored, existentVotesCnt) ->( existentVotesCnt == null ? 0: existentVotesCnt) + scoresPerPlace.size() - place);
            }
        });

        SortedMap<Integer, List<String>> membersByVotes = new TreeMap<>(Comparator.reverseOrder());
        votesByMember.forEach((memberId, votes) -> {
            membersByVotes.compute(votes, (ignored, members) -> {
                if (members == null) {
                    members = new ArrayList<>();
                }

                members.add(memberId);

                return members;
            });
        });

        Map<String, Integer> scores = new HashMap<>();

        Iterator<Integer> scoresPerPlaceIter = scoresPerPlace.iterator();
        membersByVotes.values().forEach(members -> {
            if (!scoresPerPlaceIter.hasNext()) {
                return;
            }

            Integer nextScores = scoresPerPlaceIter.next();
            for (String memberId : members) {
                scores.put(memberId, nextScores);
            }
        });

        return scores;
    }

    @Override
    public List<Party.PartyConfigurationViolation> validateConfiguration(GameValidationContext validationContext) {
        List<Party.PartyConfigurationViolation> violations = super.validateConfiguration(validationContext);

        if (validationContext.allPartyUsers().size() > context.tasks.size()) {
            violations.add(new Party.PartyConfigurationViolation(
                    "tasks",
                    "Invalid contest '%s' configuration: possible members more than tasks. Members - %d, Contest tasks - %d"
                            .formatted(name, validationContext.allPartyUsers().size(), context.tasks.size())
            ));
        }

        if (context.matchDistribution == PREDEFINED && (
                context.matches == null ||
                validationContext.allPartyUsers()
                                 .stream()
                                 .anyMatch(mid -> context.matches.stream().noneMatch(m -> m.competitor.equals(mid))))) {
            violations.add(new Party.PartyConfigurationViolation(
                    "tasks",
                    "Invalid contest '%s' configuration: match distribution is '%s', but tasks were configured not for all possible competitors"
                            .formatted(name, PREDEFINED)
            ));
        }

        return violations;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Context implements DistillableForPrincipleContext<Context> {
        // Set during creation
        private List<MatchTask> tasks;
        private List<Integer> scoresPerPlace;
        private TasksDistribution matchDistribution;

        // Set during initialization
        private List<PartyMember> members;
        private List<Match> matches;

        // Dynamic
        @With
        private Map<String, List<String>> votes;
        private ContestStage stage;
        private int currentMatch;


        @Override
        public Context getDistilledContext(PartymakerPrincipal principal) {
            return this.withVotes(Map.of(principal.id(), votes.getOrDefault(principal.id(), emptyList())));
        }
    }

    public record MatchTask(String markdown) {
    }

    public record Match(MatchTask task,
                        String competitor) {
    }

    @Getter
    @RequiredArgsConstructor
    public enum ContestStage {
        CONFIGURED(1),
        CONTEST(2),
        VOTING(3),
        FINISHED(4);

        private final int order;
    }

}
