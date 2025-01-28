package org.yvasylchuk.partymaker.party.dto.process;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor
public class AsyncEvent {
    private final AsyncEvent.Type type;


    public enum Type {
        GAME_STATE_CHANGED,
        ERROR
    }
}
