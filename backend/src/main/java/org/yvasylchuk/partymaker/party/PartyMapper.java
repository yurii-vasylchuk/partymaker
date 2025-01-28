package org.yvasylchuk.partymaker.party;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal;
import org.yvasylchuk.partymaker.exception.PartymakerException;
import org.yvasylchuk.partymaker.party.core.Contest;
import org.yvasylchuk.partymaker.party.core.Game;
import org.yvasylchuk.partymaker.party.core.Party;
import org.yvasylchuk.partymaker.party.core.PartyParticipant;
import org.yvasylchuk.partymaker.party.dto.management.ContestTaskDto;
import org.yvasylchuk.partymaker.party.dto.management.GameDto;
import org.yvasylchuk.partymaker.party.dto.management.PartyDto;
import org.yvasylchuk.partymaker.party.dto.management.PartyParticipantDto;
import org.yvasylchuk.partymaker.user.UserInternalController;
import org.yvasylchuk.partymaker.user.dto.UserDto;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.yvasylchuk.partymaker.exception.PartymakerException.Kind.INTERNAL;

@Slf4j
@Component
@RequiredArgsConstructor
public class PartyMapper {
    private final UserInternalController userInternalController;

    public PartyDto map2Dto(Party party, PartymakerPrincipal principal) {
        Map<String, String> participantsIdsToToken = party.getParticipants().stream().collect(Collectors.toMap(
                PartyParticipant::id,
                PartyParticipant::token));

        List<UserDto> users = userInternalController.findByIdIn(participantsIdsToToken.keySet());
        boolean isMaster = party.isMaster(principal);

        if (users.size() != party.getParticipants().size()) {
            log.warn("Not all users were found for party#{}: party->users has size {}, but found only {};",
                     party.getId(), party.getParticipants().size(), users.size());
        }


        List<Game<?>> games = party.getGames();
        return new PartyDto(
                party.getId(),
                party.getName(),
                party.getDescription(),
                party.getStatus().name(),
                party.getPartyMaster(),
                party.getContext().getDistilledContext(principal),
                party.getCurrentGameIdx(),
                games != null ? games.size() : null,
                users.stream().map(u -> new PartyParticipantDto(
                        u.id(),
                        u.username(),
                        participantsIdsToToken.get(u.id())
                )).toList(),
                games != null ? games.stream().map(game -> this.map2Dto(game, isMaster)).toList() : null
        );
    }

    public GameDto map2Dto(Game<?> game, boolean isMaster) {
        return switch (game.getType()) {
            case CONTEST -> map2Dto((Contest) game, isMaster);
            default -> throw new PartymakerException(INTERNAL, "Unsupported game type %s", game.getType());
        };
    }

    public GameDto map2Dto(Contest g, boolean isMaster) {
        Contest.Context ctx = g.getContext();

        List<ContestTaskDto> tasks;
        if (isMaster) {
            tasks = switch (ctx.getMatchDistribution()) {
                case null -> Collections.emptyList();
                case RANDOM -> ctx.getTasks()
                                  .stream()
                                  .map(t -> new ContestTaskDto(t.markdown()))
                                  .toList();
                case PREDEFINED -> ctx.getMatches()
                                      .stream()
                                      .map(m -> new ContestTaskDto(m.task().markdown(), m.competitor()))
                                      .toList();
            };
        } else {
            tasks = Collections.emptyList();
        }

        return new GameDto.ContestDto(
                g.getOrder(),
                g.getName(),
                g.getDescription(),
                g.getType().name(),
                tasks,
                ctx.getScoresPerPlace(),
                ctx.getMatchDistribution().name()
        );
    }

}
