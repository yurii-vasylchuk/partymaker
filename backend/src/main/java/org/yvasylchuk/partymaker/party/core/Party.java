package org.yvasylchuk.partymaker.party.core;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal;
import org.yvasylchuk.partymaker.exception.PartymakerException;
import org.yvasylchuk.partymaker.party.core.PartyMember.VariableOptionType;
import org.yvasylchuk.partymaker.party.core.command.AddPartyUserCommand;
import org.yvasylchuk.partymaker.party.core.command.CreatePartyCommand;
import org.yvasylchuk.partymaker.party.core.command.UpsertPartyGameCommand;
import org.yvasylchuk.partymaker.party.dto.process.OutgoingMessage;
import org.yvasylchuk.partymaker.party.dto.process.PartyAction;
import org.yvasylchuk.partymaker.party.dto.process.PartyAction.*;
import org.yvasylchuk.partymaker.party.dto.process.PartyStateChanged;

import java.util.*;
import java.util.stream.Collectors;

import static org.yvasylchuk.partymaker.exception.PartymakerException.Kind.*;

@Slf4j
@Getter
@Document(collection = "parties", language = "ru")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Party {
    private static final List<PartyAction.Type> onlyMasterAllowedActionTypes = List.of(
            PartyAction.Type.NEXT_MATCH,
            PartyAction.Type.PREV_MATCH,
            PartyAction.Type.NEXT_GAME,
            PartyAction.Type.START_PARTY,
            PartyAction.Type.START_VOTING);

    @Transient
    private final List<OutgoingMessage<?>> events = new ArrayList<>();

    @Transient
    @Setter
    private TokenGenerator tokenGenerator;

    @Id
    private String id;
    @Version
    private Long version;
    private PartyStatus status;
    @TextIndexed(weight = 3)
    private String name;
    @TextIndexed(weight = 1)
    private String description;
    private PartyContext context;
    private List<Game<?>> games;
    private Integer currentGameIdx;
    /// All users, who will be able to join party
    private List<PartyParticipant> participants;
    private String partyMaster;

    @PersistenceCreator
    protected Party(String id, PartyStatus status, String name, String description, PartyContext context, List<Game<?>> games, Integer currentGameIdx, List<PartyParticipant> participants, String partyMaster) {
        this.id = id;
        this.status = status;
        this.name = name;
        this.description = description;
        this.context = context;
        this.games = games;
        this.currentGameIdx = currentGameIdx;
        this.participants = participants;
        this.partyMaster = partyMaster;
    }

    public static Party create(CreatePartyCommand command, PartymakerPrincipal principal, TokenGenerator tokenGenerator) {

        List<VariableOptionType> supportedVariableOptions;
        try {
            supportedVariableOptions = command.variableOptions().stream().map(VariableOptionType::valueOf).toList();
        } catch (Exception e) {
            String msg = "Can't parse %s: variableOptions from [%s]".formatted(CreatePartyCommand.class.getSimpleName(),
                                                                               String.join(", ",
                                                                                           command.variableOptions()));
            log.warn(msg, e);
            throw new PartymakerException(GENERIC_CLIENT, msg, e);
        }

        List<PartyParticipant> users = new ArrayList<>();
        if (command.isMasterParticipating()) {
            users.add(new PartyParticipant(principal.id(), tokenGenerator.generateToken()));
        }

        return new Party(tokenGenerator,
                         null,
                         null,
                         PartyStatus.CREATED,
                         command.name(),
                         command.description(),
                         PartyContext.defaultContext(supportedVariableOptions),
                         new ArrayList<>(),
                         null,
                         users,
                         principal.id());
    }

    public Party upsertGame(UpsertPartyGameCommand command, PartymakerPrincipal principal) {
        if (!isMaster(principal)) {
            throw new PartymakerException(ACCESS_DENIED, "Only game masters and platform admins can add game");
        }

        if (status != PartyStatus.CREATED) {
            throw new PartymakerException(GENERIC_CLIENT,
                                          "Modifying party is only possible for parties in status %s. Current party status is %s",
                                          PartyStatus.CREATED,
                                          status);
        }
        if (command.getGameIdx() != null && (games == null || games.size() < command.getGameIdx() + 1)) {
            throw new PartymakerException(GENERIC_CLIENT,
                                          "Can't update game at idx %d: not enough games",
                                          command.getGameIdx());
        }

        Game<?> game = GameAbstractFactory.instance.getFactory(command, this).get();

        if (command.getGameIdx() != null) {
            this.games.set(command.getGameIdx(), game);
        } else {
            this.games.add(game);
        }

        this.events.add(new OutgoingMessage<>("/app/party/%s".formatted(id), new PartyStateChanged(this, principal)));

        return this;
    }

    public void act(PartymakerPrincipal actor, PartyAction partyAction) {
        log.info("Handling action {}; actor: {}, party.id: {}", partyAction, actor, id);

        if (!isAccessAllowed(actor)) {
            log.warn("User {} don't have access to party {}", actor, id);
            throw new PartymakerException(ACCESS_DENIED, "You don't have access to current party");
        }

        if (onlyMasterAllowedActionTypes.contains(partyAction.getType()) && !isMaster(actor)) {
            log.warn("{} action is allowed only for masters", partyAction.getType());
            throw new PartymakerException(ACCESS_DENIED,
                                          "You are not allowed to send %s action: you are not party master",
                                          partyAction.getType());
        }

        switch (partyAction) {
            case JoinPartyAction action:
                handleJoin(actor, action);
                break;
            case VariableOptionSetAction action:
                handleVariableOptionSet(actor, action);
                break;
            case PlayerReadyAction action:
                handlePlayerReady(actor, action);
                break;
            case NextGameAction action:
                handleNextGameAction(actor, action);
                break;
            case StartPartyAction action:
                handleStartPartyAction(actor, action);
                break;


            default:
                // do nothing, needed for compiler
        }

        currentGame().ifPresent(game -> game.act(actor, partyAction, events::add));

        this.events.add(new OutgoingMessage<>("/topic/party/%s".formatted(id), new PartyStateChanged(this, actor)));
    }

    private void handleStartPartyAction(PartymakerPrincipal actor, StartPartyAction action) {
        if (status != PartyStatus.CREATED) {
            log.warn("Can't start party: party already in {} status", status);
            throw new PartymakerException(GENERIC_CLIENT, "Can't start party: party already in %s status", status);
        }

        status = PartyStatus.INITIALIZING;
    }

    private void handleNextGameAction(PartymakerPrincipal principal, NextGameAction action) {
        if (!List.of(PartyStatus.INITIALIZING, PartyStatus.IN_PROGRESS).contains(this.status)) {
            throw new PartymakerException(GENERIC_CLIENT,
                                          "Can't handle %s command: Party in invalid status: %s",
                                          action.getType(),
                                          this.status);
        }

        if (games.isEmpty()) {
            throw new PartymakerException(INVALID_CONFIGURATION,
                                          "Invalid party [%s] configuration: no games configured",
                                          id);
        }

        if (currentGameIdx == null) {
            if (status == PartyStatus.IN_PROGRESS) {
                log.warn("Invalid party status: status is {}, but currentGameIdx is null", status);
            }

            currentGameIdx = 0;
            games.get(currentGameIdx).initializeGame(context);
            status = PartyStatus.IN_PROGRESS;
        } else {
            Game<?> currentGame = games.get(currentGameIdx);

            if (!currentGame.calculateStageReadiness().isStageDone()) {
                throw new PartymakerException(GENERIC_CLIENT, "Can't finish stage: not everybody is ready");
            }

            currentGame.finalizeGame();
            Map<String, Integer> scores = currentGame.calculateScores();
            context.lastStageScores = scores;

            for (Map.Entry<String, Integer> entry : scores.entrySet()) {
                String playerId = entry.getKey();
                Integer gameScore = entry.getValue();

                context.partyScores.compute(playerId,
                                            (ignored, partyScore) -> (partyScore == null ? 0 : partyScore) + gameScore);
            }

            if (currentGameIdx < games.size() - 1) {
                currentGameIdx++;
                games.get(currentGameIdx).initializeGame(context);
            } else {
                finalizeParty();
            }
        }


    }

    private void handlePlayerReady(PartymakerPrincipal actor, PlayerReadyAction action) {
        if (status != PartyStatus.INITIALIZING) {
            throw new PartymakerException(GENERIC_CLIENT,
                                          "Can't handle %s command: Party in invalid status: %s",
                                          action.getType(),
                                          status);
        }

        if (context.members.stream().noneMatch(pm -> Objects.equals(pm.getId(), actor.id()))) {
            throw new PartymakerException(ACCESS_DENIED, "User %s is not participating %s party", actor.id(), id);
        }
    }

    private void handleVariableOptionSet(PartymakerPrincipal actor, VariableOptionSetAction action) {
        try {
            VariableOptionType.valueOf(action.getOptionType());
        } catch (IllegalArgumentException e) {
            log.warn("User %s sent invalid action: Unsupported variable option type".formatted(actor), e);

            throw new PartymakerException(GENERIC_CLIENT,
                                          "Unknown variable option type %s, supported: [%s]",
                                          action.getOptionType(),
                                          Arrays.stream(VariableOptionType.values())
                                                .map(Enum::name)
                                                .collect(Collectors.joining(", ")));
        }

        PartyMember member = context.members.stream()
                                            .filter(pm -> pm.getId().equals(actor.id()))
                                            .findFirst()
                                            .orElseThrow(() -> {
                                                log.warn("User tried to set variable option, but didn't join the party");
                                                return new PartymakerException(ACCESS_DENIED,
                                                                               "User is not joined party");
                                            });

        log.info("Setting variable option {}; value: {}; actor: {}", action.getOptionType(), action.getValue(), actor);
        member.getVariableOptions().put(VariableOptionType.valueOf(action.getOptionType()), action.getValue());
    }

    private void handleJoin(PartymakerPrincipal actor, JoinPartyAction action) {
        if (status != PartyStatus.INITIALIZING) {
            log.warn("User {} are trying to join {} party; Join allowed only when party is on {} stage",
                     actor,
                     status,
                     PartyStatus.INITIALIZING);
            throw new PartymakerException(ACCESS_DENIED,
                                          "Join is only available when party is on %s status",
                                          PartyStatus.INITIALIZING);
        }

        if (participants.stream().map(PartyParticipant::id).noneMatch(id -> id.equals(actor.id()))) {
            log.warn("User {} tried to join the party {} without invite", actor, id);
            throw new PartymakerException(ACCESS_DENIED, "User is not added to current party");
        }

        if (context.members.stream().anyMatch(pm -> pm.getId().equals(actor.id()))) {
            log.warn("User {} already joined the party {}", actor, id);
            throw new PartymakerException(GENERIC_CLIENT, "User is already joined");
        }

        context.members.add(PartyMember.builder()
                                       .id(actor.id())
                                       .name(actor.username())
                                       .variableOptions(new HashMap<>())
                                       .build());
    }

    public boolean hasEvents() {
        return !events.isEmpty();
    }

    public Optional<Game<?>> currentGame() {
        return Optional.ofNullable(currentGameIdx).map(games::get);
    }

    public boolean isMaster(PartymakerPrincipal actor) {
        return actor.roles().contains("ADMIN") || Objects.equals(actor.id(), partyMaster);
    }

    public boolean isMember(PartymakerPrincipal actor) {
        if (isPartyStarted()) {
            return context.members != null &&
                   context.members.stream().anyMatch(m -> Objects.equals(m.getId(), actor.id()));
        } else {
            return participants != null && participants.stream().anyMatch(u -> u.id().equals(actor.id()));
        }
    }

    public boolean isPartyStarted() {
        return PartyStatus.STARTED_STATUSES.contains(status);
    }

    public boolean isAccessAllowed(PartymakerPrincipal principal) {
        return isMaster(principal) || isMember(principal);
    }

    public void addUser(AddPartyUserCommand.AddExistentPartyUserCommand command, PartymakerPrincipal principal) {
        if (!isMaster(principal)) {
            throw new PartymakerException(ACCESS_DENIED, "Only party master can modify party");
        }

        this.participants.add(new PartyParticipant(command.getId(), tokenGenerator.generateToken()));

        this.events.add(new OutgoingMessage<>("/app/party/%s".formatted(id), new PartyStateChanged(this, principal)));
    }

    public List<PartyConfigurationViolation> validate() {
        List<PartyConfigurationViolation> violations = new ArrayList<>();

        if (name == null || name.isBlank()) {
            violations.add(new PartyConfigurationViolation("name",
                                                           "Invalid party configuration: name is required property and shouldn't be null or blank"));
        }

        //TODO: Validate party level

        if (games != null) {
            Game.GameValidationContext validationContext = new Game.GameValidationContext(this);

            for (int i = 0; i < games.size(); i++) {
                Game<?> game = games.get(i);
                final int idx = i;
                violations.addAll(game.validateConfiguration(validationContext)
                                      .stream()
                                      .map(v -> v.withPathPrefix("games[%d].".formatted(idx)))
                                      .toList());
            }
        }

        return violations;
    }

    private void finalizeParty() {
        status = PartyStatus.FINISHED;
    }

    public enum PartyStatus {
        CREATED, // Admin, PartyMaster can edit party
        INITIALIZING, // Users can join party and fill PartyMember profile
        IN_PROGRESS, // Party started - Party's games rolls
        FINISHED; // Party closed

        public static final List<PartyStatus> STARTED_STATUSES = List.of(IN_PROGRESS, FINISHED);
    }

    @EqualsAndHashCode
    @ToString
    @Setter(AccessLevel.PRIVATE)
    @AllArgsConstructor
    public static final class PartyContext implements DistillableForPrincipleContext<PartyContext> {
        private Set<PartyMember> members;
        private Map<String, Integer> partyScores;
        private Map<String, Integer> lastStageScores;
        private List<VariableOptionType> supportedOptions;

        public static PartyContext defaultContext(List<VariableOptionType> supportedOptions) {
            return new PartyContext(new HashSet<>(), new HashMap<>(), new HashMap<>(), supportedOptions);
        }

        public Set<PartyMember> getMembers() {
            return new HashSet<>(members);
        }

        public Map<String, Integer> getPartyScores() {
            return new HashMap<>(partyScores);
        }

        public Map<String, Integer> getLastStageScores() {
            return new HashMap<>(lastStageScores);
        }

        public List<VariableOptionType> getSupportedOptions() {
            return new ArrayList<>(supportedOptions);
        }

        @Override
        public PartyContext getDistilledContext(PartymakerPrincipal principal) {
            return this;
        }
    }

    /// @param path    path to invalid field, something that makes sense to end user, not the actual path
    /// @param message text describing why specified field is invalid
    public record PartyConfigurationViolation(String path, String message) {
        public PartyConfigurationViolation withPathPrefix(String prefix) {
            return new PartyConfigurationViolation(prefix + path, message);
        }
    }
}
