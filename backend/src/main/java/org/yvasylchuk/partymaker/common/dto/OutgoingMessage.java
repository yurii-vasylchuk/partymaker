package org.yvasylchuk.partymaker.common.dto;

import org.yvasylchuk.partymaker.common.dto.events.AsyncEvent;

public record OutgoingMessage<T extends AsyncEvent>(String destination,
                                                    T data) {
}
