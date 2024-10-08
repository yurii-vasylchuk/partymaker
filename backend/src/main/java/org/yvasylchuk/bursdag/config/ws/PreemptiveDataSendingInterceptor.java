package org.yvasylchuk.bursdag.config.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.yvasylchuk.bursdag.common.JwtTokenService;
import org.yvasylchuk.bursdag.common.dto.BursdagPrincipal;
import org.yvasylchuk.bursdag.common.dto.events.AsyncEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class PreemptiveDataSendingInterceptor implements ChannelInterceptor {
    public static final String USER_ID_PLACEHOLDER = "{user_id}";
    private static final String AUTH_HEADER = "Authorization";
    private static final String JWT_HEADER_PREFIX = "Bearer ";


    private final List<PreemptiveTask> tasks = new CopyOnWriteArrayList<>();
    private final AtomicInteger nextId = new AtomicInteger();

    private final JwtTokenService jwtService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public PreemptiveDataSendingInterceptor(JwtTokenService jwtService,
                                            @Lazy SimpMessagingTemplate simpMessagingTemplate) {
        this.jwtService = jwtService;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public PreemptiveSendCanceler addTask(String pattern, Function<BursdagPrincipal, AsyncEvent> loadData) {
        int id = nextId.incrementAndGet();

        PreemptiveTask task = new PreemptiveTask(id, pattern, loadData);
        this.tasks.add(task);

        return () -> this.tasks.remove(task);
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return;
        }

        String destination = accessor.getDestination();

        if (destination == null) {
            log.error("Destination is null {}", accessor);
            return;
        }

        String jwtHeaderVal = accessor.getFirstNativeHeader(AUTH_HEADER);
        BursdagPrincipal user = jwtService.extractUser(jwtHeaderVal.substring(JWT_HEADER_PREFIX.length()));

        for (PreemptiveTask task : this.tasks) {
            String channelPattern = task.channelPattern;

            if (channelPattern.contains(USER_ID_PLACEHOLDER)) {
                channelPattern = channelPattern.replace(USER_ID_PLACEHOLDER, "%d".formatted(user.getId()));
            }

            Pattern pattern = Pattern.compile(channelPattern);
            Matcher matcher = pattern.matcher(destination);

            if (matcher.matches()) {
                simpMessagingTemplate.convertAndSend(destination, task.dataLoader.apply(user));
            }
        }
    }

    @FunctionalInterface
    public interface PreemptiveSendCanceler {
        void cancel();
    }

    public record PreemptiveTask(
            int id,
            String channelPattern,
            Function<BursdagPrincipal, ?> dataLoader
    ) {
    }
}
