package org.yvasylchuk.bursdag.common.dto;

import org.yvasylchuk.bursdag.common.dto.events.AsyncEvent;

public record OutgoingMessage<T extends AsyncEvent>(String destination,
                                                    T data) {
}
