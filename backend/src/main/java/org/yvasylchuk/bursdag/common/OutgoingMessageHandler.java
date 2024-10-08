package org.yvasylchuk.bursdag.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.yvasylchuk.bursdag.common.dto.OutgoingMessage;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutgoingMessageHandler {
    private final SimpMessagingTemplate template;

    public void sendMessages(List<OutgoingMessage<?>> messages) {
        for (OutgoingMessage<?> msg : messages) {
            this.template.convertAndSend(msg.destination(), msg.data());
        }
    }
}
