package org.yvasylchuk.partymaker.party.dto.process;

public record OutgoingMessage<T extends AsyncEvent>(String destination,
                                                    T data) {
}
