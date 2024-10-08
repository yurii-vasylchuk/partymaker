package org.yvasylchuk.bursdag.game;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.yvasylchuk.bursdag.config.ws.PreemptiveDataSendingInterceptor;

@Component
@RequiredArgsConstructor
public class GamePreemptiveDataSendConfiguration {
    private final PreemptiveDataSendingInterceptor preemptiveSender;
    private final GameService gameService;

    @PostConstruct
    public void configure() {
        this.preemptiveSender.addTask("/topic/game", gameService::loadCurrentState);
    }
}
