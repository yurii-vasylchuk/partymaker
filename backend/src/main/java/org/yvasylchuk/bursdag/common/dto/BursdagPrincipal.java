package org.yvasylchuk.bursdag.common.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
@RequiredArgsConstructor
public class BursdagPrincipal {
    private final Integer id;
    private final List<String> roles;
    private final String username;
    private final Integer gameId;
}
