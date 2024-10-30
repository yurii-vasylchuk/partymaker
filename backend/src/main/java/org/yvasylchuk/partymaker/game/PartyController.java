package org.yvasylchuk.partymaker.game;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal;
import org.yvasylchuk.partymaker.game.core.command.AddPartyGameCommand;
import org.yvasylchuk.partymaker.game.core.command.AddPartyUserCommand;
import org.yvasylchuk.partymaker.game.core.command.CreatePartyCommand;
import org.yvasylchuk.partymaker.game.dto.AsyncAction;
import org.yvasylchuk.partymaker.game.dto.PartyDto;

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
                    @Payload AsyncAction payload) {
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
                            @RequestBody AddPartyGameCommand command,
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

}
