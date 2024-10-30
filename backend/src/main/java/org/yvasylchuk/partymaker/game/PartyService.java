package org.yvasylchuk.partymaker.game;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yvasylchuk.partymaker.common.OutgoingMessageHandler;
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal;
import org.yvasylchuk.partymaker.exception.PartymakerException;
import org.yvasylchuk.partymaker.game.core.Party;
import org.yvasylchuk.partymaker.game.core.command.AddPartyGameCommand;
import org.yvasylchuk.partymaker.game.core.command.AddPartyUserCommand;
import org.yvasylchuk.partymaker.game.core.command.CreatePartyCommand;
import org.yvasylchuk.partymaker.game.dto.AsyncAction;
import org.yvasylchuk.partymaker.game.dto.PartyDto;
import org.yvasylchuk.partymaker.game.dto.PartyStateChanged;
import org.yvasylchuk.partymaker.user.UserInternalController;
import org.yvasylchuk.partymaker.user.command.CreateUserCommand;
import org.yvasylchuk.partymaker.user.dto.UserDto;

import static org.yvasylchuk.partymaker.exception.PartymakerException.Kind.INTERNAL;
import static org.yvasylchuk.partymaker.exception.PartymakerException.Kind.NOT_FOUND;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PartyService {
    private final PartyRepository partyRepository;
    private final OutgoingMessageHandler outgoingMessageHandler;
    private final UserInternalController userInternalController;

    public PartyStateChanged loadPartyState(String partyId, PartymakerPrincipal principal) {
        Party party = partyRepository.findById(partyId).orElseThrow(
                () -> {
                    log.warn("Party {} is not found", partyId);
                    return new PartymakerException(NOT_FOUND, "Party is not found");
                });
        return new PartyStateChanged(party, principal);
    }

    public void act(String partyId, PartymakerPrincipal player, AsyncAction action) {
        Party party = partyRepository.findById(partyId).orElseThrow();

        party.act(player, action);

        partyRepository.save(party);

        if (party.hasEvents()) {
            outgoingMessageHandler.sendMessages(party.getEvents());
        }
    }

    public PartyDto createParty(CreatePartyCommand command, PartymakerPrincipal principal) {
        Party party = Party.create(command, principal);

        party = partyRepository.save(party);

        if (party.hasEvents()) {
            outgoingMessageHandler.sendMessages(party.getEvents());
        }

        return PartyDto.of(party, principal);
    }

    public PartyDto addGame(String partyId, AddPartyGameCommand command, PartymakerPrincipal principal) {
        Party party = partyRepository.findById(partyId)
                                     .orElseThrow(() -> new PartymakerException(
                                             NOT_FOUND,
                                             "Party '%s' is not found".formatted(partyId)));

        party.addGame(command, principal);

        party = partyRepository.save(party);

        if (party.hasEvents()) {
            outgoingMessageHandler.sendMessages(party.getEvents());
        }

        return PartyDto.of(party, principal);
    }

    public PartyDto addUser(String partyId, AddPartyUserCommand command, PartymakerPrincipal principal) {
        Party party = partyRepository.findById(partyId)
                                     .orElseThrow(() -> new PartymakerException(
                                             NOT_FOUND,
                                             "Party '%s' is not found".formatted(partyId)));
        UserDto user = switch (command) {
            // Need to validate user existence
            case AddPartyUserCommand.AddExistentPartyUserCommand cmd -> userInternalController.findById(cmd.getId());

            case AddPartyUserCommand.AddNewPartyUserCommand cmd ->
                    userInternalController.createUser(new CreateUserCommand(cmd.getUsername(), cmd.getToken()));

            default -> throw new PartymakerException(INTERNAL, "Should never happen");
        };

        party.addUser(new AddPartyUserCommand.AddExistentPartyUserCommand(user.id()), principal);

        party = partyRepository.save(party);

        if (party.hasEvents()) {
            outgoingMessageHandler.sendMessages(party.getEvents());
        }

        return PartyDto.of(party, principal);
    }
}
