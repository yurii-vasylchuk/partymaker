package org.yvasylchuk.partymaker;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@EnableScheduling
@SpringBootApplication
@RequiredArgsConstructor
public class BackendApplication {
    private final SimpMessagingTemplate simpMessagingTemplate;

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    @Scheduled(initialDelay = 3000L, fixedDelay = 5000L)
    public void ping() {
        this.simpMessagingTemplate.convertAndSend("/queue/response", "PING-PONG");
    }

}
