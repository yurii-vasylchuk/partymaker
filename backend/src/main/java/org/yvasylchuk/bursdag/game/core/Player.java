package org.yvasylchuk.bursdag.game.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Getter
@Setter
@Builder
@Jacksonized
@AllArgsConstructor
public class Player {
    private Integer userId;
    private String username;
    private String chosenAvatar;
    private String nickname;
    private Map<String, String> chosenTraits;
}
