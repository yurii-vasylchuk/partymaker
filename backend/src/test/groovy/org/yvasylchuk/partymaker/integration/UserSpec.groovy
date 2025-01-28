package org.yvasylchuk.partymaker.integration

import io.restassured.http.ContentType
import org.hamcrest.Matchers
import org.springframework.beans.factory.annotation.Autowired
import org.yvasylchuk.partymaker.integration.util.UserFactory
import org.yvasylchuk.partymaker.user.JwtTokenService
import org.yvasylchuk.partymaker.user.core.PartymakerRole
import static io.restassured.RestAssured.*

class UserSpec extends BaseIntegrationSpec {

    @Autowired
    private JwtTokenService jwtTokenService
    @Autowired
    private UserFactory     userFactory

    def "User should be able to register"() {

        when:
        def response = given()
                .body("""
                { 
                    "email": "test@test.com",
                    "username": "test",
                    "password": "password"
                }
                """)
                .contentType(ContentType.JSON)
                .post("/api/user")

        then:
        def responseBody = response
                .then()
                .statusCode(200)
                .body("accessToken", Matchers.notNullValue())
                .extract()
                .body()
                .as(Map)

        def user = jwtTokenService.extractUser(responseBody.accessToken as String)

        user.username() == 'test'
        user.roles() ==~ ['PARTICIPANT', 'REGULAR']
    }

    def "Registered user should be able to login"() {
        given:
        def expectedUsername = "Test username"
        def expectedRoles = ['PARTICIPANT', 'REGULAR']


        userFactory.defaultUser().tap {
            username = expectedUsername
            roles = expectedRoles.collect { PartymakerRole.valueOf(it) }
            saveToDb = true
        }.build()

        when:
        def response = given()
                .body("""
                { 
                    "email": "${UserFactory.DEFAULT_EMAIL}",
                    "password": "${UserFactory.DEFAULT_PASSWORD}"
                }
                """)
                .contentType(ContentType.JSON)
                .post("/api/user/access-token")

        then:
        def responseBody = response
                .then()
                .statusCode(200)
                .body("accessToken", Matchers.notNullValue())
                .extract()
                .body()
                .as(Map)

        def actual = jwtTokenService.extractUser(responseBody.accessToken as String)

        actual.username() == expectedUsername
        actual.roles() ==~ expectedRoles
    }
}
