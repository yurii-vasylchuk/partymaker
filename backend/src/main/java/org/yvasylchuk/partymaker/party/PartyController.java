package org.yvasylchuk.partymaker.party;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal;
import org.yvasylchuk.partymaker.party.core.command.AddPartyUserCommand;
import org.yvasylchuk.partymaker.party.core.command.CreatePartyCommand;
import org.yvasylchuk.partymaker.party.core.command.UpsertPartyGameCommand;
import org.yvasylchuk.partymaker.party.dto.management.PartyDto;
import org.yvasylchuk.partymaker.party.dto.management.PartyFilterDto;
import org.yvasylchuk.partymaker.party.dto.management.PartyValidationResponse;
import org.yvasylchuk.partymaker.party.dto.process.PartyAction;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/party")
@MessageMapping("/party")
@RequiredArgsConstructor
public class PartyController {
    private final PartyService partyService;

    @MessageMapping("/{partyId}/act")
    public void act(PartymakerPrincipal player,
                    @DestinationVariable("partyId") String partyId,
                    @Payload PartyAction payload) {
        log.trace("Act on party {}; Payload: {}", partyId, payload);
        partyService.act(partyId, player, payload);
    }

    @PostMapping
    @PreAuthorize("isFullyAuthenticated()")
    public PartyDto createParty(@RequestBody CreatePartyCommand command,
                                @AuthenticationPrincipal PartymakerPrincipal principal) {
        log.trace("Creating a party {}", command);
        return partyService.createParty(command, principal);
    }

    @PutMapping("/{id}/game")
    @PreAuthorize("isFullyAuthenticated()")
    public PartyDto addGame(@PathVariable String id,
                            @RequestBody UpsertPartyGameCommand command,
                            @AuthenticationPrincipal PartymakerPrincipal principal) {
        log.trace("Adding game to party: {}; command:{}", id, command);
        return partyService.addGame(id, command, principal);
    }

    @PutMapping("/{id}/user")
    @PreAuthorize("isFullyAuthenticated()")
    public PartyDto addUser(@PathVariable String id,
                            @RequestBody AddPartyUserCommand command,
                            @AuthenticationPrincipal PartymakerPrincipal principal) {
        log.trace("Adding User to party: {}; command:{}", id, command);
        return partyService.addUser(id, command, principal);
    }

    @GetMapping()
    @PreAuthorize("isFullyAuthenticated()")
    public List<PartyDto> getParties(PartyFilterDto filter,
                                     @AuthenticationPrincipal PartymakerPrincipal principal) {
        log.trace("Getting list of parties; Filter={}", filter);
        return partyService.find(filter, principal);
    }

    @GetMapping("/{partyId}/validation-violations")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'REGULAR')")
    public PartyValidationResponse validateParty(@PathVariable("partyId") String partyId) {
        return new PartyValidationResponse(partyService.validate(partyId));
    }

}
