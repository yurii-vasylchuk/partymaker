package org.yvasylchuk.partymaker.party.dto.process;

import lombok.Getter;
import org.yvasylchuk.partymaker.exception.PartymakerException;

@Getter
public class CommandProcessingFailedEvent extends AsyncEvent {
    private final String message;

    public CommandProcessingFailedEvent(PartymakerException ex) {
        super(Type.ERROR);
        this.message = ex.getMessage();
    }
}
