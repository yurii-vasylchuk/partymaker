package org.yvasylchuk.partymaker.integration.util


import org.slf4j.LoggerFactory
import org.springframework.lang.Nullable
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.stereotype.Component
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.yvasylchuk.partymaker.party.dto.process.PartyAction

import java.lang.reflect.Type
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

@Component
class PartyOperations {

    private final WebSocketStompClient stompClient

    PartyOperations() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient())
        stompClient.setMessageConverter(new MappingJackson2MessageConverter())
    }

    CompletableFuture<GameWsSession> connect(int port, String authorization, String partyId) {
        return GameWsSession.connect(stompClient, port, authorization, partyId)
    }

    //TODO: Log like RestAssured
    static class GameWsSession {
        private static final log = LoggerFactory.getLogger(GameWsSession)

        final String authorization
        final String partyId

        private StompSession stompSession

        private static CompletableFuture<GameWsSession> connect(WebSocketStompClient client,
                                                                int port,
                                                                String authorization,
                                                                String partyId) {
            def headers = new StompHeaders()

            headers.set('Authorization', "Bearer ${authorization}")

            return client.connectAsync(
                    "ws://localhost:${port}/ws",
                    null,
                    headers,
                    new StompSessionHandlerAdapter() {
                    }
            ).thenApply {
                log.info("Connected to ws on port=${port}")
                return new GameWsSession(it, authorization, partyId)
            }.exceptionally {
                log.error("Unable to connect to WS endpoint on port=${port}")
            }
        }

        private GameWsSession(StompSession stompSession, String authorization, String partyId) {
            this.stompSession = stompSession
            this.authorization = authorization
            this.partyId = partyId
        }

        def handlePartyEvents(Consumer<Map> consumer) {
            def destination = "/topic/party/${partyId}"

            log.info("Subscribing on party events topic: '${destination}'")
            def subscriptionHeaders = authorizedHeaders()
            subscriptionHeaders.setDestination(destination)

            def subscription = stompSession.subscribe(subscriptionHeaders, new StompFrameHandler() {
                @Override
                Type getPayloadType(StompHeaders headers) {
                    return Map
                }

                @Override
                void handleFrame(StompHeaders headers, @Nullable Object payload) {
                    log.info("Got message on party events topic. PartyId=${partyId}, message=${payload}")
                    consumer.accept(payload as Map)
                }
            })
            return subscription
        }

        def act(PartyAction.Type type, Map payload = [:]) {
            log.info("Act on PartyId=${partyId}. ActionType=${type}, Data=${payload}")

            def headers = authorizedHeaders()
            headers.setDestination("/app/party/${partyId}/act")

            stompSession.send(headers, payload.tap {
                it['type'] = type.name()
            })

            // Need to sleep a bit to give a server time to handle message
            Thread.sleep(50)
        }

        private StompHeaders authorizedHeaders() {
            def headers = new StompHeaders()

            headers.set('Authorization', "Bearer ${authorization}")

            return headers
        }
    }

}
