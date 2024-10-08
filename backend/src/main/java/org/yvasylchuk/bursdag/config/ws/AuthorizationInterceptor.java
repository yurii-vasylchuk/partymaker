package org.yvasylchuk.bursdag.config.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.yvasylchuk.bursdag.common.JwtTokenService;
import org.yvasylchuk.bursdag.common.dto.BursdagPrincipal;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizationInterceptor implements ChannelInterceptor {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_GROUP = "userId";
    private static final List<StompCommand> INTERCEPTING_COMMANDS = List.of(StompCommand.SUBSCRIBE,
                                                                            StompCommand.SEND,
                                                                            StompCommand.CONNECT);

    private static final List<Pattern> INTERCEPTING_PATHS = List.of(
            Pattern.compile("/topic/game")
                                                                   );

    private final JwtTokenService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        String destination = accessor.getDestination();
        StompCommand command = accessor.getCommand();

        if (!INTERCEPTING_COMMANDS.contains(command)) {
            return message;
        }

        String authorizationHeader = accessor.getFirstNativeHeader("Authorization");

        final BursdagPrincipal user = authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX) ?
                jwtService.extractUser(authorizationHeader.substring(BEARER_PREFIX.length())) :
                null;

        if (destination == null) {
            if (user == null) {
                throw new AccessDeniedException("Authentication required for STOMP %s COMMAND".formatted(command.name()));
            } else {
                return message;
            }
        }

        boolean shouldBlock = INTERCEPTING_PATHS.stream().anyMatch(path -> {
            Matcher matcher = path.matcher(destination);
            boolean matches = matcher.matches();

            if (!matches) {
                return false;
            }

            if (user == null) {
                return true;
            }

            Set<String> groups = matcher.namedGroups().keySet();
            //Block access to not owned by user topic
            return groups.contains(USER_ID_GROUP) && !matcher.group(USER_ID_GROUP).equals("%s".formatted(user.getId()));
        });

        if (shouldBlock) {
            log.warn("STOMP {} {} is blocked for user {}", command, destination, user);
        }


        return message;
    }
}
