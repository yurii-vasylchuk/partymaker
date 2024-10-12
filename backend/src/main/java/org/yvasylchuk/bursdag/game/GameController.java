package org.yvasylchuk.bursdag.game;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yvasylchuk.bursdag.common.BursdagUserDetailsService.BursdagUserDetails;
import org.yvasylchuk.bursdag.common.dto.AsyncAction;
import org.yvasylchuk.bursdag.common.dto.BursdagPrincipal;

@Slf4j
@RestController
@RequestMapping("/api/game")
@MessageMapping("/game")
@RequiredArgsConstructor
public class GameController {
    private final GameService gameService;

    @MessageMapping("/act")
    public void act(BursdagPrincipal player,
                    @Payload AsyncAction payload) {
        gameService.act(player, payload);
    }

    @MessageMapping("/admin-act")
    public void adminAct(BursdagPrincipal player,
                         @Payload AsyncAction payload) {
        if (!player.getRoles().contains("ADMIN")) {
            log.warn("Unauthorized access: {} sent message to /admin-act", player);
        }
        gameService.adminAct(player, payload);
    }

    @MessageMapping("/join")
    public void join(BursdagPrincipal player) {
        gameService.join(player);
    }

    @PutMapping("/go-to-next-stage")
    @PreAuthorize("hasAuthority('ADMIN')")
    public void startGame(@AuthenticationPrincipal BursdagUserDetails user) {
        gameService.goToNextStage(user);
    }

    @PutMapping("/complete-stage")
    @PreAuthorize("hasAuthority('ADMIN')")
    public void completeStage(@AuthenticationPrincipal BursdagUserDetails user) {
        gameService.completeStage(user);
    }
}
