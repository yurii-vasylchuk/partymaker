package org.yvasylchuk.partymaker.party;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.yvasylchuk.partymaker.party.dto.process.OutgoingMessage;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutgoingMessageHandler {
    private final SimpMessagingTemplate template;

    public void sendMessages(List<OutgoingMessage<?>> messages) {
        for (OutgoingMessage<?> msg : messages) {
            log.trace("Sending outgoing event to {}; Data: {}", msg.destination(), msg.data());
            this.template.convertAndSend(msg.destination(), msg.data());
        }
    }
}
