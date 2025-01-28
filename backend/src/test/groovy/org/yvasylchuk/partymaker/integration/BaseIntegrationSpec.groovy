package org.yvasylchuk.partymaker.integration

import io.restassured.RestAssured
import io.restassured.filter.log.LogDetail
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles('test')
@Testcontainers
class BaseIntegrationSpec extends Specification {
    @Shared
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:8.0.1")

    @LocalServerPort
    protected int port

    @Autowired
    private MongoTemplate mongoTemplate

    @PostConstruct
    def initRestAssured() {
        RestAssured.baseURI = 'http://localhost'
        RestAssured.port = port
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL)
        RestAssured.preemptive()
    }

    def cleanup() {
        for (collection in mongoTemplate.collectionNames) {
            mongoTemplate.remove(new Query(), collection)
        }
    }

    @DynamicPropertySource
    static void containersProperties(DynamicPropertyRegistry registry) {
        mongoDBContainer.start()
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl)
    }
}