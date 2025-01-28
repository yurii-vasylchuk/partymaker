package org.yvasylchuk.partymaker.config.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal;
import org.yvasylchuk.partymaker.exception.PartymakerException;
import org.yvasylchuk.partymaker.party.dto.process.CommandProcessingFailedEvent;
import org.yvasylchuk.partymaker.user.JwtTokenService;

@Slf4j
@Component
public class ExceptionHandlingInterceptor implements ChannelInterceptor {
    private final JwtTokenService jwtService;
    private final SimpMessagingTemplate template;

    public ExceptionHandlingInterceptor(JwtTokenService jwtService,
                                        @Lazy SimpMessagingTemplate template) {
        this.jwtService = jwtService;
        this.template = template;
    }

    @Override
    public void afterReceiveCompletion(Message<?> message, MessageChannel channel, Exception ex) {
        if (ex == null || ex.getClass() != PartymakerException.class) {
            return;
        }

        PartymakerException partymakerException = (PartymakerException) ex;

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        String authorizationHeader = accessor.getFirstNativeHeader("Authorization");
        if (authorizationHeader == null) {
            return;
        }

        PartymakerPrincipal principal = jwtService.extractUser(authorizationHeader.substring("Bearer ".length()));

        template.convertAndSend("/app/user/%s".formatted(principal.id()),
                                new CommandProcessingFailedEvent(partymakerException));
    }
}
