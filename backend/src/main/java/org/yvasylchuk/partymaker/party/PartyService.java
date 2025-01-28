package org.yvasylchuk.partymaker.party;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal;
import org.yvasylchuk.partymaker.exception.PartymakerException;
import org.yvasylchuk.partymaker.party.core.Party;
import org.yvasylchuk.partymaker.party.core.command.AddPartyUserCommand;
import org.yvasylchuk.partymaker.party.core.command.CreatePartyCommand;
import org.yvasylchuk.partymaker.party.core.command.UpsertPartyGameCommand;
import org.yvasylchuk.partymaker.party.dao.PartyDao;
import org.yvasylchuk.partymaker.party.dao.PartyFilter;
import org.yvasylchuk.partymaker.party.dto.management.PartyDto;
import org.yvasylchuk.partymaker.party.dto.management.PartyFilterDto;
import org.yvasylchuk.partymaker.party.dto.process.PartyAction;
import org.yvasylchuk.partymaker.party.dto.process.PartyStateChanged;
import org.yvasylchuk.partymaker.user.UserInternalController;
import org.yvasylchuk.partymaker.user.command.CreateParticipantUserCommand;
import org.yvasylchuk.partymaker.user.dto.UserDto;

import java.util.List;

import static org.yvasylchuk.partymaker.exception.PartymakerException.Kind.INTERNAL;
import static org.yvasylchuk.partymaker.exception.PartymakerException.Kind.NOT_FOUND;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PartyService {
    private final PartyDao partyDao;
    private final PartyMapper partyMapper;
    private final OutgoingMessageHandler outgoingMessageHandler;
    private final UserInternalController userInternalController;

    public PartyStateChanged loadPartyState(String partyId, PartymakerPrincipal principal) {
        Party party = partyDao.get(partyId).orElseThrow(
                () -> {
                    log.warn("Party {} is not found", partyId);
                    return new PartymakerException(NOT_FOUND, "Party is not found");
                });
        return new PartyStateChanged(party, principal);
    }

    @Retry(name = "party-act")
    public void act(String partyId, PartymakerPrincipal player, PartyAction action) {
        Party party = partyDao.get(partyId).orElseThrow();

        party.act(player, action);

        partyDao.save(party);

        if (party.hasEvents()) {
            outgoingMessageHandler.sendMessages(party.getEvents());
        }
    }

    public PartyDto createParty(CreatePartyCommand command, PartymakerPrincipal principal) {
        Party party = partyDao.create(command, principal);

        party = partyDao.save(party);

        if (party.hasEvents()) {
            outgoingMessageHandler.sendMessages(party.getEvents());
        }

        return partyMapper.map2Dto(party, principal);
    }

    public PartyDto addGame(String partyId, UpsertPartyGameCommand command, PartymakerPrincipal principal) {
        Party party = partyDao.get(partyId)
                              .orElseThrow(() -> new PartymakerException(
                                             NOT_FOUND,
                                             "Party '%s' is not found",
                                             partyId));

        party.upsertGame(command, principal);

        party = partyDao.save(party);

        if (party.hasEvents()) {
            outgoingMessageHandler.sendMessages(party.getEvents());
        }

        return partyMapper.map2Dto(party, principal);
    }

    public PartyDto addUser(String partyId, AddPartyUserCommand command, PartymakerPrincipal principal) {
        Party party = partyDao.get(partyId)
                              .orElseThrow(() -> new PartymakerException(
                                             NOT_FOUND,
                                             "Party '%s' is not found",
                                             partyId));

        AddPartyUserCommand.AddExistentPartyUserCommand cmd = switch (command) {
            case AddPartyUserCommand.AddExistentPartyUserCommand addExistentPartyUserCommand -> {
                boolean exists = userInternalController.isUserExists(addExistentPartyUserCommand.getId());
                if (!exists) {
                    throw new PartymakerException(NOT_FOUND,
                                                  "User with id %s is not found",
                                                  addExistentPartyUserCommand.getId());
                }
                yield addExistentPartyUserCommand;
            }

            case AddPartyUserCommand.AddNewPartyUserCommand addNewPartyUserCommand -> {
                UserDto user = userInternalController.createGameUser(
                        new CreateParticipantUserCommand(addNewPartyUserCommand.getUsername()));
                yield new AddPartyUserCommand.AddExistentPartyUserCommand(user.id());
            }

            default -> throw new PartymakerException(INTERNAL, "Should never happen");
        };

        party.addUser(cmd, principal);

        party = partyDao.save(party);

        if (party.hasEvents()) {
            outgoingMessageHandler.sendMessages(party.getEvents());
        }

        return partyMapper.map2Dto(party, principal);
    }

    public List<PartyDto> find(PartyFilterDto filterDto, PartymakerPrincipal principal) {
        PartyFilter.PartyFilterBuilder filter = PartyFilter.builder();

        PartyFilter.ParticipationLevel participationLevel = filterDto.participationLevel() != null ?
                filterDto.participationLevel() :
                PartyFilter.ParticipationLevel.PARTICIPANT;

        Party.PartyStatus status = filterDto.status() != null ?
                Party.PartyStatus.valueOf(filterDto.status()) :
                null;

        filter.participationLevel(participationLevel)
              .userId(principal.id())
              .text(filterDto.text())
              .status(status);

        return partyDao.findBy(filter.build())
                              .stream()
                              .map(p -> partyMapper.map2Dto(p, principal))
                              .toList();
    }

    public List<Party.PartyConfigurationViolation> validate(String partyId) {
        Party party = partyDao.get(partyId)
                                     .orElseThrow(() -> new PartymakerException(NOT_FOUND,
                                                                                "Party with id %s is not found",
                                                                                partyId));

        return party.validate();
    }
}
