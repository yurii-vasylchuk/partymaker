package org.yvasylchuk.bursdag.common.dto.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor
public sealed class AsyncEvent permits GameStateChanged {
    private final AsyncEvent.Type type;


    public enum Type {
        GAME_STATE_CHANGED,
    }
}
