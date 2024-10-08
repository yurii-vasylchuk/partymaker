package org.yvasylchuk.bursdag.common;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yvasylchuk.bursdag.common.dto.AccessTokenRsp;
import org.yvasylchuk.bursdag.common.dto.BursdagPrincipal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/common")
public class AuthController {
    private final UserService userService;
    private final JwtTokenService jwtService;

    @GetMapping("/access-token")
    public AccessTokenRsp exchangeAuthToken(@RequestParam("authToken") String authToken) {
        BursdagPrincipal user = userService.findUserByToken(authToken)
                                           .orElseThrow(() -> new RuntimeException(
                                                   "User with provided token is not found"));

        String jwt = jwtService.generateJwt(user);

        return new AccessTokenRsp(jwt);
    }
}
