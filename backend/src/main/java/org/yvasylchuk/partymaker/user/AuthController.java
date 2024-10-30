package org.yvasylchuk.partymaker.user;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yvasylchuk.partymaker.common.dto.AccessTokenRsp;
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/common")
public class AuthController {
    private final UserService userService;
    private final JwtTokenService jwtService;

    @GetMapping("/access-token")
    public AccessTokenRsp exchangeAuthToken(@RequestParam("authToken") String authToken) {
        PartymakerPrincipal user = userService.findUserByToken(authToken)
                                              .orElseThrow(() -> new RuntimeException(
                                                   "User with provided token is not found"));

        String jwt = jwtService.generateJwt(user);

        return new AccessTokenRsp(jwt);
    }
}
