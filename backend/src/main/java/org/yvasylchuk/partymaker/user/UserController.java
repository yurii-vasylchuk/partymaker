package org.yvasylchuk.partymaker.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal;
import org.yvasylchuk.partymaker.user.command.AuthenticateByAuthTokenCommand;
import org.yvasylchuk.partymaker.user.command.AuthenticateByEmailAndPasswordCommand;
import org.yvasylchuk.partymaker.user.command.CreateRegularUserCommand;
import org.yvasylchuk.partymaker.user.dto.AccessTokenRsp;
import org.yvasylchuk.partymaker.user.dto.UserDto;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;
    private final JwtTokenService jwtService;

    @PostMapping
    public AccessTokenRsp register(@RequestBody CreateRegularUserCommand command) {
        UserDto user = userService.register(command);
        return new AccessTokenRsp(
                jwtService.generateJwt(new PartymakerPrincipal(user))
        );
    }

    @GetMapping("/access-token")
    public AccessTokenRsp exchangeAuthToken(@RequestParam("authToken") String authToken) {
        UserDto user = userService.findUserByAuthToken(new AuthenticateByAuthTokenCommand(authToken));
        return new AccessTokenRsp(
                jwtService.generateJwt(new PartymakerPrincipal(user))
        );
    }

    @PostMapping("/access-token")
    public AccessTokenRsp exchangeAuthToken(@RequestBody AuthenticateByEmailAndPasswordCommand command) {
        UserDto user = userService.findUserByEmailAndPassword(command);
        return new AccessTokenRsp(
                jwtService.generateJwt(new PartymakerPrincipal(user))
        );
    }
}
