package org.yvasylchuk.partymaker.game.core;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.yvasylchuk.partymaker.common.dto.OutgoingMessage;
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal;
import org.yvasylchuk.partymaker.exception.PartymakerException;
import org.yvasylchuk.partymaker.game.core.PartyMember.VariableOptionType;
import org.yvasylchuk.partymaker.game.core.command.AddPartyGameCommand;
import org.yvasylchuk.partymaker.game.core.command.AddPartyUserCommand;
import org.yvasylchuk.partymaker.game.core.command.CreatePartyCommand;
import org.yvasylchuk.partymaker.game.dto.AsyncAction;
import org.yvasylchuk.partymaker.game.dto.AsyncAction.JoinPartyAction;
import org.yvasylchuk.partymaker.game.dto.AsyncAction.PlayerReadyAction;
import org.yvasylchuk.partymaker.game.dto.AsyncAction.VariableOptionSetAction;
import org.yvasylchuk.partymaker.game.dto.PartyStateChanged;

import java.util.*;
import java.util.stream.Collectors;

import static org.yvasylchuk.partymaker.exception.PartymakerException.Kind.ACCESS_DENIED;
import static org.yvasylchuk.partymaker.exception.PartymakerException.Kind.GENERIC_CLIENT;

@Slf4j
@Getter
@Document(collection = "parties")
@AllArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @PersistenceCreator)
public class Party {
    @Transient
    private final List<OutgoingMessage<?>> events = new ArrayList<>();
    @Id
    private String id;
    private PartyStatus status;
    private String name;
    private String description;
    private PartyContext context;
    private List<Game<?>> games;
    private Integer currentGameIdx;
    /// All users, who will be able to join party
    private List<String> users;
    private String partyMaster;

    public static Party create(CreatePartyCommand command, PartymakerPrincipal principal) {

        List<VariableOptionType> supportedVariableOptions;
        try {
            supportedVariableOptions = command.variableOptions()
                                              .stream()
                                              .map(VariableOptionType::valueOf)
                                              .toList();
        } catch (Exception e) {
            log.warn("Can't parse %s:variableOptions from [%s]".formatted(
                             CreatePartyCommand.class.getSimpleName(),
                             String.join(", ", command.variableOptions())),
                     e);
            throw e;
        }

        List<String> users = new ArrayList<>();
        users.add(principal.id());

        return new Party(
                null,
                PartyStatus.CREATED,
                command.name(),
                command.description(),
                PartyContext.defaultContext(supportedVariableOptions),
                new ArrayList<>(),
                -1,
                users,
                principal.id()
        );
    }

    public Party addGame(AddPartyGameCommand command, PartymakerPrincipal principal) {
        if (!isMaster(principal)) {
            throw new PartymakerException(ACCESS_DENIED, "Only game masters and platform admins can add game");
        }

        if (status != PartyStatus.CREATED) {
            throw new PartymakerException(
                    GENERIC_CLIENT,
                    "Modifying party is only possible for parties in status %s. Current party status is %s"
                            .formatted(PartyStatus.CREATED, status));
        }

        Game<?> game = AbstractGameFactory.instance.getFactory(command, this, principal).get();
        this.games.add(game);

        this.events.add(new OutgoingMessage<>("/app/party/%s".formatted(id), new PartyStateChanged(this, principal)));

        return this;
    }

    public void act(PartymakerPrincipal actor, AsyncAction asyncAction) {
        log.info("Handling action {}; actor: {}, party.id: {}", asyncAction, actor, id);

        if (!isAccessAllowed(actor)) {
            log.warn("User {} don't have access to party {}", actor, id);
            throw new PartymakerException(ACCESS_DENIED, "User don't have access to current party");
        }

        switch (asyncAction) {
            case JoinPartyAction action:
                handleJoin(actor, action);
                break;
            case VariableOptionSetAction action:
                handleVariableOptionSet(actor, action);
                break;
            case PlayerReadyAction action:
                handlePlayerReady(actor, action);
                break;

            default:
                // do nothing, needed for compiler
        }

        currentGame().ifPresent(game -> game.act(actor, asyncAction, events::add));

        this.events.add(new OutgoingMessage<>("/app/party/%s".formatted(id), new PartyStateChanged(this, actor)));
    }

    private void handlePlayerReady(PartymakerPrincipal actor, PlayerReadyAction action) {

    }

    private void handleVariableOptionSet(PartymakerPrincipal actor, VariableOptionSetAction action) {
        try {
            VariableOptionType.valueOf(action.getOptionType());
        } catch (IllegalArgumentException e) {
            log.warn("User %s sent invalid action: Unsupported variable option type".formatted(actor),
                     e);

            String message = "Unknown variable option type %s, supported: [%s]"
                    .formatted(action.getOptionType(),
                               Arrays.stream(VariableOptionType.values())
                                     .map(Enum::name)
                                     .collect(Collectors.joining(", ")));

            throw new PartymakerException(GENERIC_CLIENT, message);
        }

        PartyMember member = context.members
                .stream()
                .filter(pm -> pm.getId().equals(actor.id()))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("User tried to set variable option, but didn't join the party");
                    return new PartymakerException(ACCESS_DENIED, "User is not joined party");
                });

        log.info("Setting variable option {}; value: {}; actor: {}", action.getOptionType(), action.getValue(), actor);
        member.getVariableOptions().put(VariableOptionType.valueOf(action.getOptionType()), action.getValue());
    }

    private void handleJoin(PartymakerPrincipal actor, JoinPartyAction action) {
        if (users.stream().noneMatch(id -> id.equals(actor.id()))) {
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
        return context.members != null && context.members.stream().anyMatch(m -> Objects.equals(m.getId(), actor.id()));
    }

    public boolean isAccessAllowed(PartymakerPrincipal principal) {
        return isMaster(principal) || isMember(principal);
    }

    public void addUser(AddPartyUserCommand.AddExistentPartyUserCommand command, PartymakerPrincipal principal) {
        if (!isMaster(principal)) {
            throw new PartymakerException(ACCESS_DENIED, "Only party master can modify party");
        }

        this.users.add(command.getId());

        this.events.add(new OutgoingMessage<>("/app/party/%s".formatted(id), new PartyStateChanged(this, principal)));
    }

    public enum PartyStatus {
        CREATED, // Admin, PartyMaster can edit party
        INITIALIZING, // Users can join party and fill PartyMember profile
        IN_PROGRESS, // Party started - Party's games rolls
        FINALIZING, // Party results, end party message
        FINISHED, // Party closed
    }

    public record PartyContext(Set<PartyMember> members,
                               Map<String, Integer> partyScores,
                               Map<String, Integer> lastStageScores,
                               List<VariableOptionType> supportedOptions) implements DistillableForPrincipleContext<PartyContext> {
        public static PartyContext defaultContext(List<VariableOptionType> supportedOptions) {
            return new PartyContext(
                    new HashSet<>(),
                    new HashMap<>(),
                    new HashMap<>(),
                    supportedOptions
            );
        }

        @Override
        public PartyContext getDistilledContext(PartymakerPrincipal principal) {
            return this;
        }
    }
}
