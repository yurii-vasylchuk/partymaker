package org.yvasylchuk.partymaker.game;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.yvasylchuk.partymaker.config.ws.PreemptiveDataSendingInterceptor;

@Component
@RequiredArgsConstructor
public class GamePreemptiveDataSendConfiguration {
    private final PreemptiveDataSendingInterceptor preemptiveSender;
    private final PartyService partyService;

    @PostConstruct
    public void configure() {
        this.preemptiveSender.addTask(
                "/topic/party/([^/]+)",
                (principal, matchResult) -> partyService.loadPartyState(matchResult.group(1), principal));
    }
}
