package org.yvasylchuk.partymaker.party.core

import org.yvasylchuk.partymaker.exception.PartymakerException
import org.yvasylchuk.partymaker.party.core.command.AddPartyUserCommand
import org.yvasylchuk.partymaker.party.core.command.CreatePartyCommand
import org.yvasylchuk.partymaker.party.core.command.TasksDistribution
import org.yvasylchuk.partymaker.party.core.command.UpsertPartyGameCommand
import org.yvasylchuk.partymaker.party.dto.management.ContestTaskDto
import org.yvasylchuk.partymaker.party.dto.process.PartyStateChanged
import org.yvasylchuk.partymaker.util.TestPartyFactory
import org.yvasylchuk.partymaker.util.TestPrincipalFactory
import spock.lang.Specification

import static org.yvasylchuk.partymaker.party.core.PartyMember.VariableOptionType.AVATAR
import static org.yvasylchuk.partymaker.party.core.PartyMember.VariableOptionType.NICKNAME
import static org.yvasylchuk.partymaker.party.core.command.UpsertPartyGameCommand.UpsertContestCommand.builder

class PartySpec extends Specification {
    def tokenGenerator = Mock(TokenGenerator)

    def principalFactory = new TestPrincipalFactory()
    def partyFactory     = new TestPartyFactory(tokenGenerator)

    def "should create party"() {
        given:
        def principal = principalFactory.testUser1()

        when:
        def party = Party.create(
                new CreatePartyCommand(
                        'Test party',
                        'Test description',
                        ['NICKNAME', 'AVATAR'],
                        true),
                principal,
                tokenGenerator)

        then:
        party.name == 'Test party'
        party.description == 'Test description'
        party.context.supportedOptions - [NICKNAME, AVATAR] == []
        party.currentGameIdx == null
        party.partyMaster == TestPrincipalFactory.TEST_1_ID

        1 * tokenGenerator.generateToken() >> "token 1"
        0 * _
    }

    def "should fail creating new party if unknown variable option is passed"() {
        given:
        def principal = principalFactory.testUser1()
        def wrongOption = 'UNKNOWN'

        when:
        Party.create(new CreatePartyCommand('Test party',
                                            'Test description',
                                            ['NICKNAME', wrongOption],
                                            false),
                     principal,
                     tokenGenerator)

        then:
        def e = thrown(PartymakerException)
        e.message == "Can't parse CreatePartyCommand: variableOptions from [NICKNAME, ${wrongOption}]"
        e.kind == PartymakerException.Kind.GENERIC_CLIENT

        0 * _
    }

    def "should add game to party"() {
        given:
        def master = principalFactory.testUser1()

        when:
        def party = partyFactory.emptyParty(master)

        party.upsertGame(
                builder()
                        .name('contest')
                        .description('contest description')
                        .tasksDistribution(TasksDistribution.RANDOM)
                        .scorePerPlace([1, 2, 3])
                        .task(new ContestTaskDto('task 1', null))
                        .task(new ContestTaskDto('task 2', null))
                        .task(new ContestTaskDto('task 3', null))
                        .build(),
                master
        )

        then:
        party.games.size() == 1
        party.games[0].order == 0
        party.currentGameIdx == null
        party.participants.size() == 1

        def user = party.participants[0]
        user.token() == 'token1'
        user.id() == TestPrincipalFactory.TEST_1_ID

        party.events.size() == 1
        def event = party.events[0]
        event.destination() == "/app/party/${TestPartyFactory.PARTY_1_ID}"
        event.data().class == PartyStateChanged

        and:
        1 * tokenGenerator.generateToken() >> 'token1'
    }

    def "should properly add contest game to party"() {
        given:
        def master = principalFactory.testUser1()

        when:
        def party = partyFactory.emptyParty(master)

        party.upsertGame(
                new UpsertPartyGameCommand.UpsertContestCommand(
                        null,
                        'contest',
                        'contest desc',
                        [
                                new ContestTaskDto('task 1', null),
                                new ContestTaskDto('task 2', null),
                        ],
                        [1, 2, 3],
                        TasksDistribution.RANDOM),
                master
        )

        then:
        def game = party.games[0]
        game.name == 'contest'
        game.description == 'contest desc'
        game.type == Game.GameType.CONTEST
        game.context.class == Contest.Context
        def context = game.context as Contest.Context
        context.members.size() == 0
        context.tasks ==~ [new Contest.MatchTask('task 1'), new Contest.MatchTask('task 2')]
        context.matchDistribution == TasksDistribution.RANDOM
        context.scoresPerPlace == [1, 2, 3]


        and:
        1 * tokenGenerator.generateToken() >> 'token1'
    }

    def "should add user to party"() {
        given:
        def master = principalFactory.testUser1()
        def user = principalFactory.testUser2()

        when:
        def party = partyFactory.emptyParty(master)

        party.addUser(new AddPartyUserCommand.AddExistentPartyUserCommand(user.id()), master)

        then:
        party.participants.size() == 2
        party.participants ==~ [
                new PartyParticipant(TestPrincipalFactory.TEST_1_ID, 'token1'),
                new PartyParticipant(TestPrincipalFactory.TEST_2_ID, 'token2')
        ]

        def event = party.events[0]
        event.destination() == "/app/party/${TestPartyFactory.PARTY_1_ID}"
        event.data().class == PartyStateChanged


        2 * tokenGenerator.generateToken() >>> ['token1', 'token2']
    }

    def "Upsert game command should replace game if gameIdx param is provided"() {
        given:
        def master = principalFactory.testUser1()
        def party = partyFactory.emptyParty(master)
        party.upsertGame(
                UpsertPartyGameCommand.UpsertContestCommand.builder()
                                      .name('Game 1')
                                      .description('Game 1 desc')
                                      .tasksDistribution(TasksDistribution.RANDOM)
                                      .scorePerPlace([1, 2, 3])
                                      .task(new ContestTaskDto('task 11'))
                                      .task(new ContestTaskDto('task 12'))
                                      .task(new ContestTaskDto('task 13'))
                                      .task(new ContestTaskDto('task 14'))
                                      .build()
                , master)
        party.upsertGame(
                UpsertPartyGameCommand.UpsertContestCommand.builder()
                                      .name('Game 2')
                                      .description('Game 2 desc')
                                      .tasksDistribution(TasksDistribution.RANDOM)
                                      .scorePerPlace([1, 2, 3])
                                      .task(new ContestTaskDto('task 21'))
                                      .task(new ContestTaskDto('task 22'))
                                      .task(new ContestTaskDto('task 23'))
                                      .task(new ContestTaskDto('task 24'))
                                      .build()
                , master)

        when:
        party.upsertGame(
                UpsertPartyGameCommand.UpsertContestCommand.builder()
                                      .gameIdx(0)
                                      .name('Game 1 UPD')
                                      .description('Game 1 desc UPD')
                                      .tasksDistribution(TasksDistribution.RANDOM)
                                      .scorePerPlace([4, 5, 6])
                                      .task(new ContestTaskDto('task 11 upd'))
                                      .task(new ContestTaskDto('task 12 upd'))
                                      .task(new ContestTaskDto('task 13 upd'))
                                      .task(new ContestTaskDto('task 14 upd'))
                                      .build()
                , master)

        then:
        party.games.size() == 2
        party.games.get(0).class == Contest

        and:
        def game = party.games.get(0) as Contest
        game.name == 'Game 1 UPD'
        game.description == 'Game 1 desc UPD'
        game.order == 1

        and:
        def context = game.context
        context.scoresPerPlace == [4, 5, 6]
        context.tasks ==~ [
                new Contest.MatchTask('task 11 upd'),
                new Contest.MatchTask('task 12 upd'),
                new Contest.MatchTask('task 13 upd'),
                new Contest.MatchTask('task 14 upd'),
        ]
    }

    def "Upsert game command should fail if there is no games configured for party"() {
        given:
        def master = principalFactory.testUser1()
        def party = partyFactory.emptyParty(master)

        when:
        party.upsertGame(
                UpsertPartyGameCommand.UpsertContestCommand.builder()
                                      .gameIdx(0)
                                      .name('Game 1 UPD')
                                      .description('Game 1 desc UPD')
                                      .tasksDistribution(TasksDistribution.RANDOM)
                                      .scorePerPlace([4, 5, 6])
                                      .task(new ContestTaskDto('task 11 upd'))
                                      .task(new ContestTaskDto('task 12 upd'))
                                      .task(new ContestTaskDto('task 13 upd'))
                                      .task(new ContestTaskDto('task 14 upd'))
                                      .build()
                , master)

        then:
        def e = thrown(PartymakerException)
        e.kind == PartymakerException.Kind.GENERIC_CLIENT
        e.message == 'Can\'t update game at idx 0: not enough games'
    }

    def "Upsert game command should fail if gameIdx is higher than count of games configured for party"() {
        given:
        def master = principalFactory.testUser1()
        def party = partyFactory.emptyParty(master)
        party.upsertGame(
                UpsertPartyGameCommand.UpsertContestCommand.builder()
                                      .name('Game 1')
                                      .description('Game 1 desc')
                                      .tasksDistribution(TasksDistribution.RANDOM)
                                      .scorePerPlace([1, 2, 3])
                                      .task(new ContestTaskDto('task 11'))
                                      .task(new ContestTaskDto('task 12'))
                                      .task(new ContestTaskDto('task 13'))
                                      .task(new ContestTaskDto('task 14'))
                                      .build()
                , master)
        party.upsertGame(
                UpsertPartyGameCommand.UpsertContestCommand.builder()
                                      .name('Game 2')
                                      .description('Game 2 desc')
                                      .tasksDistribution(TasksDistribution.RANDOM)
                                      .scorePerPlace([1, 2, 3])
                                      .task(new ContestTaskDto('task 21'))
                                      .task(new ContestTaskDto('task 22'))
                                      .task(new ContestTaskDto('task 23'))
                                      .task(new ContestTaskDto('task 24'))
                                      .build()
                , master)

        when:
        party.upsertGame(
                UpsertPartyGameCommand.UpsertContestCommand.builder()
                                      .gameIdx(2)
                                      .name('Game 1 UPD')
                                      .description('Game 1 desc UPD')
                                      .tasksDistribution(TasksDistribution.RANDOM)
                                      .scorePerPlace([4, 5, 6])
                                      .task(new ContestTaskDto('task 11 upd'))
                                      .task(new ContestTaskDto('task 12 upd'))
                                      .task(new ContestTaskDto('task 13 upd'))
                                      .task(new ContestTaskDto('task 14 upd'))
                                      .build()
                , master)

        then:
        def e = thrown(PartymakerException)
        e.kind == PartymakerException.Kind.GENERIC_CLIENT
        e.message == 'Can\'t update game at idx 2: not enough games'
    }
}
