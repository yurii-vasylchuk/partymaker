package org.yvasylchuk.partymaker.integration


import com.fasterxml.jackson.databind.ObjectMapper
import io.restassured.http.ContentType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal
import org.yvasylchuk.partymaker.integration.util.PartyFactory
import org.yvasylchuk.partymaker.integration.util.PartyOperations
import org.yvasylchuk.partymaker.integration.util.UserFactory
import org.yvasylchuk.partymaker.integration.util.UserOperations
import org.yvasylchuk.partymaker.party.core.command.TasksDistribution
import org.yvasylchuk.partymaker.party.dto.management.PartyParticipantDto
import org.yvasylchuk.partymaker.party.dto.process.PartyAction

import java.util.concurrent.CompletableFuture

import static io.restassured.RestAssured.given
import static java.util.concurrent.TimeUnit.MILLISECONDS
import static org.yvasylchuk.partymaker.integration.util.PartyFactory.DEFAULT_MASTER_EMAIL
import static org.yvasylchuk.partymaker.integration.util.PartyFactory.DEFAULT_MASTER_PASSWORD

class PartySpec extends BaseIntegrationSpec {
    def log = LoggerFactory.getLogger(PartySpec)

    @Autowired
    private UserFactory     userFactory
    @Autowired
    private UserOperations  userOperations
    @Autowired
    private PartyFactory    partyFactory
    @Autowired
    private PartyOperations partyOperations
    @Autowired
    private ObjectMapper    objectMapper

    def "Regular user should be able to create a party"() {
        given:
        userFactory.defaultUser().tap {
            saveToDb = true
        }.build()

        def accessToken = userOperations.authenticate(UserFactory.DEFAULT_EMAIL, UserFactory.DEFAULT_PASSWORD)

        when:
        def response = given()
                .body("""
                      {
                          "name": "Test party",
                          "description": "Some test description",
                          "variableOptions": ["NICKNAME", "TREATS"],
                          "isMasterParticipating": false
                      }
                      """)
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer ${accessToken}")
                .post("/api/party")
                .then()
                .statusCode(200)
                .extract().body().as(Map)

        then:
        with(response) {
            name == "Test party"
            description == "Some test description"
            status == "CREATED"
            currentGameIdx == null
            users == []
            with(context as Map) {
                members == []
                supportedOptions == ["NICKNAME", "TREATS"]
            }
        }
    }

    def "Master should be able to invite existing users to the party"() {
        given:
        def party = partyFactory.emptyParty().tap {
            shouldSave = true
        }.build()

        def user = userFactory.defaultUser().tap {
            password = 'password1'
            username = 'username1'
            email = 'test@email.com'
            saveToDb = true
        }.build()

        def accessToken = userOperations.authenticate(
                partyFactory.DEFAULT_MASTER_EMAIL,
                partyFactory.DEFAULT_MASTER_PASSWORD)

        when:
        def rsp = given()
                .header('Authorization', "Bearer ${accessToken}")
                .contentType(ContentType.JSON)
                .body("""
                {
                  "id": "${user.id}"
                }
                """)
                .put('/api/party/{id}/user', [id: party.id])
                .then()
                .statusCode(200)
                .extract().body().as(Map)

        then:
        rsp.with {
            users.size() == 1
            users[0].with {
                id == user.id
                username == user.username
            }
        }
    }

    def "Master should be able to add new users to the party"() {
        given:
        def party = partyFactory.emptyParty().tap {
            shouldSave = true
        }.build()

        def accessToken = userOperations.authenticate(
                partyFactory.DEFAULT_MASTER_EMAIL,
                partyFactory.DEFAULT_MASTER_PASSWORD)

        when:
        def rsp = given()
                .header('Authorization', "Bearer ${accessToken}")
                .contentType(ContentType.JSON)
                .body("""
                {
                  "username": "test-username"
                }
                """)
                .put('/api/party/{id}/user', [id: party.id])
                .then()
                .statusCode(200)
                .extract().body().as(Map)

        then:
        rsp.with {
            users.size() == 1
            users[0].with {
                id == null
                username == 'test-username'
            }
        }
    }

    def "Master should be able to add contest game to the party"() {
        given:
        def party = partyFactory.emptyParty().tap {
            shouldSave = true
        }.build()

        def accessToken = userOperations.authenticate(
                partyFactory.DEFAULT_MASTER_EMAIL,
                partyFactory.DEFAULT_MASTER_PASSWORD)

        when:
        def rsp = given()
                .header('Authorization', "Bearer ${accessToken}")
                .contentType(ContentType.JSON)
                .body("""
                {
                  "name": "Game 1",
                  "description": "Game 1 description",
                  "scorePerPlace": [1, 2, 3],
                  "gameType": "CONTEST",
                  "tasksDistribution": "RANDOM",
                  "tasks": [{
                    "task": "task 1"
                  }, {
                    "task": "task 2"
                  }]
                }
                """)
                .put('/api/party/{id}/game', [id: party.id])
                .then()
                .statusCode(200)
                .extract().body().as(Map)

        then:
        with(rsp.games as List<Map>) {
            size() == 1
            with(it[0]) {
                type == 'CONTEST'
                order == 0
                name == 'Game 1'
                description == 'Game 1 description'
                scoresPerPlace == [1, 2, 3]
                matchDistribution == 'RANDOM'
                tasks ==~ [
                        [task: 'task 1', participantId: null],
                        [task: 'task 2', participantId: null],
                ]
            }
        }
    }

    def "User should be able to see list of all games he created or participating"() {
        given:
        def email = 'target.user@email.com'
        def password = 'target'

        def user = userFactory.defaultUser().tap {
            username = 'target_user'
            it.email = email
            it.password = password
            saveToDb = true
        }.build()

        partyFactory.emptyParty().tap {
            shouldSave = true
            name = "Party 1"
            games << partyFactory.defaultContest(5)
            masterPrincipal = new PartymakerPrincipal(user)
        }.build()

        partyFactory.emptyParty().tap {
            shouldSave = true
            name = "Party 2"
            participants << new PartyParticipantDto(user.id, user.username, null)

            master = userFactory.defaultUser().tap {
                username = 'another_user'
                it.email = 'another.user@email.com'
                it.password = 'another'
            }
        }.build()

        def jwt = userOperations.authenticate(email, password)

        when:
        def rsp = given()
                .header('Authorization', "Bearer ${jwt}")
                .queryParam('participationLevel', 'PARTICIPANT')
                .get("/api/party")
                .then()
                .statusCode(200)
                .extract().body().as(List<Map>)

        then:

        rsp.size() == 2
        rsp.any {
            it['name'] == 'Party 1'
        }
        rsp.any {
            it['name'] == 'Party 2'
        }
    }

    def "User should be able to see list of all parties he created or participating"() {
        given:
        def email = 'target.user@email.com'
        def password = 'target'

        def user = userFactory.defaultUser().tap {
            it.email = email
            it.password = password
            saveToDb = true
        }.build()

        partyFactory.emptyParty().tap {
            shouldSave = true
            name = "Party 1"
            masterPrincipal = new PartymakerPrincipal(user)
        }.build()

        partyFactory.emptyParty().tap {
            shouldSave = true
            name = "Party 2"
            participants << new PartyParticipantDto(user.id, user.username, null)

            master = userFactory.defaultUser().tap {
                it.email = 'another.user@email.com'
                it.password = 'another'
            }
        }.build()

        def jwt = userOperations.authenticate(email, password)

        when:
        def rsp = given()
                .header('Authorization', "Bearer ${jwt}")
                .queryParam('participationLevel', 'PARTICIPANT')
                .get("/api/party")
                .then()
                .statusCode(200)
                .extract().body().as(List<Map>)

        then:

        rsp.size() == 2
        rsp.any {
            it['name'] == 'Party 1'
        }
        rsp.any {
            it['name'] == 'Party 2'
        }
    }

    def "User should be able to see list of all parties he created"() {
        given:
        def email = 'target.user@email.com'
        def password = 'target'

        def user = userFactory.defaultUser().tap {
            it.email = email
            it.password = password
            saveToDb = true
        }.build()

        partyFactory.emptyParty().tap {
            shouldSave = true
            name = "Party 1"
            masterPrincipal = new PartymakerPrincipal(user)
        }.build()

        partyFactory.emptyParty().tap {
            shouldSave = true
            name = "Party 2"
            participants << new PartyParticipantDto(user.id, user.username, null)

            master = userFactory.defaultUser().tap {
                it.email = 'another.user@email.com'
                it.password = 'another'
            }
        }.build()

        def jwt = userOperations.authenticate(email, password)

        when:
        def rsp = given()
                .header('Authorization', "Bearer ${jwt}")
                .queryParam('participationLevel', 'MASTER')
                .get("/api/party")
                .then()
                .statusCode(200)
                .extract().body().as(List<Map>)

        then:

        rsp.size() == 1
        rsp.any {
            it['name'] == 'Party 1'
        }
    }

    def "User should be able to get list of parties filtered by it's name/description"() {
        given:
        def email = 'target.user@email.com'
        def password = 'target'
        def textCriteria = 'SEARCH'

        def user = userFactory.defaultUser().tap {
            it.email = email
            it.password = password
            saveToDb = true
        }.build()

        //expected parties
        def expected1 = partyFactory.emptyParty().tap {
            shouldSave = true
            name = "${textCriteria}"
            description = 'Congue consetetur delenit volutpat consetetur vel voluptate eros feugait culpa ullamco id adipiscing.'

            masterPrincipal = new PartymakerPrincipal(user)
        }.build().id

        def expected2 = partyFactory.emptyParty().tap {
            shouldSave = true
            name = "Veniam aliquam erat tation ${textCriteria}"
            description = 'Iriure iriure sit eos, eos liber aliquip eros praesent nobis accusam et placerat nulla sit sunt. Vel mazim obcaecat, zzril nihil nonumy nihil quis mollit vero commodo ut ea excepteur wisi proident, id sed nisl mollit at exercitation'
            masterPrincipal = new PartymakerPrincipal(user)
        }.build().id

        def expected3 = partyFactory.emptyParty().tap {
            shouldSave = true
            name = 'Autem iriure dolor'
            description = "Eu ullamco officia nisi sea nobis nonumy. Accumsan ${textCriteria} aute nisi facer wisi placerat. Volutpat eleifend exercitation facilisis iure soluta, zzril cillum ad nisl reprehenderit imperdiet sunt"
            masterPrincipal = new PartymakerPrincipal(user)
        }.build().id

        // Not expected parties
        partyFactory.emptyParty().tap {
            shouldSave = true
            name = 'Sint cum placerat.'
            description = 'Laborum euismod justo voluptua consectetur eleifend sint, id aliqua delenit stet sint exerci ad facilisi hendrerit incidunt anim eleifend.'
            masterPrincipal = new PartymakerPrincipal(user)
        }.build()

        partyFactory.emptyParty().tap {
            shouldSave = true
            name = 'Minim cupiditat diam.'
            description = 'Consectetur cum vero consetetur illum iriure facilisi iriure nonummy soluta esse ullamcorper zzril qui. Amet sed laboris euismod wisi, assum dignissim lorem qui duo qui exerci consequat'
            masterPrincipal = new PartymakerPrincipal(user)
        }.build()

        def jwt = userOperations.authenticate(email, password)

        when:
        def rsp = given()
                .header('Authorization', "Bearer ${jwt}")
                .queryParam('text', textCriteria)
                .get("/api/party")
                .then()
                .statusCode(200)
                .extract().body().as(List<Map>)

        then:
        rsp.size() == 3
        rsp.collect { it['id'] } == [expected1, expected2, expected3]
    }

    def "User should be able to validate his own party"() {
        given:
        def party = partyFactory.emptyParty().tap {
            shouldSave = true
            name = ''
            participants = [
                    new PartyParticipantDto(null, 'participant1', null),
                    new PartyParticipantDto(null, 'participant2', null),
                    new PartyParticipantDto(null, 'participant3', null),
                    new PartyParticipantDto(null, 'participant4', null),
                    new PartyParticipantDto(null, 'participant5', null),
            ]
            games = [
                    partyFactory.defaultContest(1).tap {
                        tasks = []
                        name = ''
                        tasksDistribution = TasksDistribution.PREDEFINED
                    }
            ]
        }.build()

        def jwt = userOperations.authenticate(DEFAULT_MASTER_EMAIL, DEFAULT_MASTER_PASSWORD)

        when:
        def rsp = given()
                .header('Authorization', "Bearer ${jwt}")
                .get("/api/party/{id}/validation-violations", party.id)
                .then()
                .statusCode(200)
                .extract().body().as(Map)

        then:
        rsp.valid == false
        rsp.violations.size() == 4
        rsp.violations.any { it.path == 'name'}
        rsp.violations.any { it.path == 'games[0].name'}
        rsp.violations.findAll() { it.path == 'games[0].tasks'}.size() == 2
    }

    def "Test contest game flow"() {
        given:
        def master = userFactory.defaultUser().tap {
            username = 'master'
            email = 'master@email.com'
            password = 'master'
            saveToDb = true
        }.build()
        def masterJwt = userOperations.authenticate('master@email.com', 'master')

        def player1 = userFactory.defaultUser().tap {
            username = 'player1'
            email = 'player1@email.com'
            password = 'player1'
            saveToDb = true
        }.build()
        def p1Jwt = userOperations.authenticate('player1@email.com', 'player1')

        def player2 = userFactory.defaultUser().tap {
            username = 'player2'
            email = 'player2@email.com'
            password = 'player2'
            saveToDb = true
        }.build()
        def p2Jwt = userOperations.authenticate('player2@email.com', 'player2')

        def player3 = userFactory.defaultUser().tap {
            username = 'player3'
            email = 'player3@email.com'
            password = 'player3'
            saveToDb = true
        }.build()
        def p3Jwt = userOperations.authenticate('player3@email.com', 'player3')

        def party = partyFactory.emptyParty().tap {
            masterPrincipal = new PartymakerPrincipal(master)
            participants = [
                    new PartyParticipantDto(player1.id, player1.username, null),
                    new PartyParticipantDto(player2.id, player2.username, null),
                    new PartyParticipantDto(player3.id, player3.username, null),
            ]
            games = [partyFactory.defaultContest(3)]
            shouldSave = true
        }.build()

        def masterSession = partyOperations.connect(port, masterJwt, party.id).get(50, MILLISECONDS)
        def p1Session = partyOperations.connect(port, p1Jwt, party.id).get(50, MILLISECONDS)
        def p2Session = partyOperations.connect(port, p2Jwt, party.id).get(50, MILLISECONDS)
        def p3Session = partyOperations.connect(port, p3Jwt, party.id).get(50, MILLISECONDS)

        when:
        masterSession.act(PartyAction.Type.START_PARTY)

        p1Session.act(PartyAction.Type.JOIN)
        p2Session.act(PartyAction.Type.JOIN)
        p3Session.act(PartyAction.Type.JOIN)

        p1Session.act(PartyAction.Type.VARIABLE_OPTION_SET, [
                optionType: 'NICKNAME',
                value     : 'p1_nick'
        ])
        p2Session.act(PartyAction.Type.VARIABLE_OPTION_SET, [
                optionType: 'NICKNAME',
                value     : 'p2_nick'
        ])
        p3Session.act(PartyAction.Type.VARIABLE_OPTION_SET, [
                optionType: 'NICKNAME',
                value     : 'p3_nick'
        ])

        masterSession.act(PartyAction.Type.NEXT_GAME)
        masterSession.act(PartyAction.Type.NEXT_MATCH)
        masterSession.act(PartyAction.Type.NEXT_MATCH)
        masterSession.act(PartyAction.Type.START_VOTING)

        p1Session.act(PartyAction.Type.CONTEST_VOTING, [
                places: [player2.id, player3.id]
        ])
        p2Session.act(PartyAction.Type.CONTEST_VOTING, [
                places: [player1.id, player3.id]
        ])
        p3Session.act(PartyAction.Type.CONTEST_VOTING, [
                places: [player2.id, player1.id]
        ])

        CompletableFuture<Map> event = new CompletableFuture<>()
        p3Session.handlePartyEvents {
            if (it.get('status') == 'FINISHED') {
                event.completeAsync(() -> it)
            }
        }
        masterSession.act(PartyAction.Type.NEXT_GAME)

        then:
        def result = event.get(100, MILLISECONDS)

        result.status == 'FINISHED'
        println result
    }

}