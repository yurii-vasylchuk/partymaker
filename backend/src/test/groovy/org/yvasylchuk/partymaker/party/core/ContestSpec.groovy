package org.yvasylchuk.partymaker.party.core

import org.yvasylchuk.partymaker.exception.PartymakerException
import org.yvasylchuk.partymaker.party.core.command.TasksDistribution
import org.yvasylchuk.partymaker.util.PartyBaseSpec

import static org.yvasylchuk.partymaker.party.dto.process.PartyAction.*

class ContestSpec extends PartyBaseSpec {

    def "Correct flow with 2 players"() {
        given:

        def master = DEF_PARTY_MASTER
        def player1 = DEF_PARTY_USERS[0]
        def player2 = DEF_PARTY_USERS[1]

        def party = defaultParty()
                .withGame(defaultContest())
                .withUsers([player1, player2])
                .withVariableOptions([PartyMember.VariableOptionType.NICKNAME])
                .build()


        when:
        party.act(master, new StartPartyAction())
        party.act(player1, new JoinPartyAction())
        party.act(player2, new JoinPartyAction())
        party.act(player1, new VariableOptionSetAction('NICKNAME', "Player 1"))
        party.act(player2, new VariableOptionSetAction('NICKNAME', "Player 2"))
        party.act(master, new NextGameAction())
        party.act(master, new NextMatchAction())
        party.act(master, new StartVotingAction())
        party.act(player1, new ContestVotingAction([player2.id()]))
        party.act(player2, new ContestVotingAction([player1.id()]))
        party.act(master, new NextGameAction())

        then:
        party.status == Party.PartyStatus.FINISHED
        def ctx = party.context
        ctx.lastStageScores == [
                (player1.id()): DEF_CONTEST_SCORE_PER_PLACE[0],
                (player2.id()): DEF_CONTEST_SCORE_PER_PLACE[0],
        ]
        ctx.partyScores == [
                (player1.id()): DEF_CONTEST_SCORE_PER_PLACE[0],
                (player2.id()): DEF_CONTEST_SCORE_PER_PLACE[0],
        ]


    }

    def "Correct flow with 3 players"() {
        given:

        def master = DEF_PARTY_MASTER
        def player1 = DEF_PARTY_USERS[0]
        def player2 = DEF_PARTY_USERS[1]
        def player3 = DEF_PARTY_USERS[2]

        def party = defaultParty()
                .withGame(defaultContest())
                .withUsers([
                        player1,
                        player2,
                        player3,
                ])
                .withVariableOptions([PartyMember.VariableOptionType.NICKNAME])
                .build()

        when:
        party.act(master, new StartPartyAction())
        party.act(player1, new JoinPartyAction())
        party.act(player2, new JoinPartyAction())
        party.act(player3, new JoinPartyAction())
        party.act(player1, new VariableOptionSetAction('NICKNAME', "Player 1"))
        party.act(player2, new VariableOptionSetAction('NICKNAME', "Player 2"))
        party.act(player3, new VariableOptionSetAction('NICKNAME', "Player 3"))
        party.act(master, new NextGameAction())
        party.act(master, new NextMatchAction())
        party.act(master, new NextMatchAction())
        party.act(master, new StartVotingAction())
        party.act(player1, new ContestVotingAction([player2.id(), player3.id()]))
        party.act(player2, new ContestVotingAction([player1.id(), player3.id()]))
        party.act(player3, new ContestVotingAction([player1.id(), player2.id()]))
        party.act(master, new NextGameAction())

        then:
        party.status == Party.PartyStatus.FINISHED
        def ctx = party.context
        ctx.lastStageScores == [
                (player1.id()): DEF_CONTEST_SCORE_PER_PLACE[0],
                (player2.id()): DEF_CONTEST_SCORE_PER_PLACE[1],
                (player3.id()): DEF_CONTEST_SCORE_PER_PLACE[2],
        ]
        ctx.partyScores == [
                (player1.id()): DEF_CONTEST_SCORE_PER_PLACE[0],
                (player2.id()): DEF_CONTEST_SCORE_PER_PLACE[1],
                (player3.id()): DEF_CONTEST_SCORE_PER_PLACE[2],
        ]


    }

    def "Correct flow with 4 players"() {
        given:
        def master = DEF_PARTY_MASTER
        def player1 = DEF_PARTY_USERS[0]
        def player2 = DEF_PARTY_USERS[1]
        def player3 = DEF_PARTY_USERS[2]
        def player4 = DEF_PARTY_USERS[3]

        def party = defaultParty()
                .withGame(defaultContest())
                .withVariableOptions([PartyMember.VariableOptionType.NICKNAME])
                .build()

        when:
        party.act(master, new StartPartyAction())
        party.act(player1, new JoinPartyAction())
        party.act(player2, new JoinPartyAction())
        party.act(player3, new JoinPartyAction())
        party.act(player4, new JoinPartyAction())
        party.act(player1, new VariableOptionSetAction('NICKNAME', "Player 1"))
        party.act(player2, new VariableOptionSetAction('NICKNAME', "Player 2"))
        party.act(player3, new VariableOptionSetAction('NICKNAME', "Player 3"))
        party.act(player3, new VariableOptionSetAction('NICKNAME', "Player 4"))
        party.act(master, new NextGameAction())
        party.act(master, new NextMatchAction())
        party.act(master, new NextMatchAction())
        party.act(master, new NextMatchAction())
        party.act(master, new StartVotingAction())
        party.act(player1, new ContestVotingAction([player2.id(), player3.id(), player4.id()]))
        party.act(player2, new ContestVotingAction([player1.id(), player4.id(), player3.id()]))
        party.act(player3, new ContestVotingAction([player1.id(), player2.id(), player4.id()]))
        party.act(player4, new ContestVotingAction([player1.id(), player3.id(), player2.id()]))
        party.act(master, new NextGameAction())

        then:
        party.status == Party.PartyStatus.FINISHED
        def ctx = party.context
        ctx.lastStageScores == [
                (player1.id()): DEF_CONTEST_SCORE_PER_PLACE[0],
                (player2.id()): DEF_CONTEST_SCORE_PER_PLACE[1],
                (player3.id()): DEF_CONTEST_SCORE_PER_PLACE[2],
        ]
        ctx.partyScores == [
                (player1.id()): DEF_CONTEST_SCORE_PER_PLACE[0],
                (player2.id()): DEF_CONTEST_SCORE_PER_PLACE[1],
                (player3.id()): DEF_CONTEST_SCORE_PER_PLACE[2],
        ]


    }

    def "Flow with 2 members on same place"() {
        given:
        def player1 = principalFactory.regularUser("user_1_id", "user 1")
        def player2 = principalFactory.regularUser("user_2_id", "user 2")
        def player3 = principalFactory.regularUser("user_3_id", "user 3")
        def player4 = principalFactory.regularUser("user_4_id", "user 4")

        def party = defaultParty()
                .withGame(defaultContest())
                .withUsers([
                        player1,
                        player2,
                        player3,
                        player4,
                ])
                .withVariableOptions([PartyMember.VariableOptionType.NICKNAME])
                .build()


        def master = DEF_PARTY_MASTER

        when:
        party.act(master, new StartPartyAction())
        party.act(player1, new JoinPartyAction())
        party.act(player2, new JoinPartyAction())
        party.act(player3, new JoinPartyAction())
        party.act(player4, new JoinPartyAction())
        party.act(player1, new VariableOptionSetAction('NICKNAME', "Player 1"))
        party.act(player2, new VariableOptionSetAction('NICKNAME', "Player 2"))
        party.act(player3, new VariableOptionSetAction('NICKNAME', "Player 3"))
        party.act(player3, new VariableOptionSetAction('NICKNAME', "Player 4"))
        party.act(master, new NextGameAction())
        party.act(master, new NextMatchAction())
        party.act(master, new NextMatchAction())
        party.act(master, new NextMatchAction())
        party.act(master, new StartVotingAction())
        party.act(player1, new ContestVotingAction([player2.id(), player3.id(), player4.id()]))
        party.act(player2, new ContestVotingAction([player1.id(), player3.id(), player4.id()]))
        party.act(player3, new ContestVotingAction([player2.id(), player1.id(), player4.id()]))
        party.act(player4, new ContestVotingAction([player1.id(), player2.id(), player3.id()]))
        party.act(master, new NextGameAction())

        then:
        party.status == Party.PartyStatus.FINISHED
        def ctx = party.context
        ctx.lastStageScores == [
                (player1.id()): DEF_CONTEST_SCORE_PER_PLACE[0],
                (player2.id()): DEF_CONTEST_SCORE_PER_PLACE[0],
                (player3.id()): DEF_CONTEST_SCORE_PER_PLACE[1],
                (player4.id()): DEF_CONTEST_SCORE_PER_PLACE[2],
        ]
        ctx.partyScores == [
                (player1.id()): DEF_CONTEST_SCORE_PER_PLACE[0],
                (player2.id()): DEF_CONTEST_SCORE_PER_PLACE[0],
                (player3.id()): DEF_CONTEST_SCORE_PER_PLACE[1],
                (player4.id()): DEF_CONTEST_SCORE_PER_PLACE[2],
        ]


    }

    def "Should not allow  #action from not-joined user"() {

        given:
        def player1 = DEF_PARTY_USERS[0]
        def player2 = DEF_PARTY_USERS[1]
        def player3 = DEF_PARTY_USERS[2]

        def party = defaultParty()
                .withGame(defaultContest())
                .withUsers([player1, player2])
                .withVariableOptions([PartyMember.VariableOptionType.NICKNAME])
                .build()

        when:
        party.act(player3, action)

        then:
        def e = thrown(PartymakerException)
        e != null
        e.message == "You don't have access to current party"

        where:
        action << [
                new VariableOptionSetAction("NICKNAME", "some nickname"),
                new ContestVotingAction([DEF_PARTY_USERS[0].id(), DEF_PARTY_USERS[1].id()])
        ]
    }

    def "Should block start voting if contest is already in VOTING stage"() {
        given:
        def master = DEF_PARTY_MASTER
        def player1 = DEF_PARTY_USERS[0]
        def player2 = DEF_PARTY_USERS[1]

        def party = defaultParty()
                .withGame(defaultContest())
                .withUsers([player1, player2])
                .build()

        when:
        party.act(master, new StartPartyAction())
        party.act(player1, new JoinPartyAction())
        party.act(player2, new JoinPartyAction())
        party.act(master, new NextGameAction())
        party.act(master, new NextMatchAction())
        party.act(master, new StartVotingAction())
        party.act(master, new StartVotingAction())

        then:
        def e = thrown(PartymakerException)
        e
                .message == "'START_VOTING' is not available: wrong contest stage; Allowed stage: 'CONTEST', current stage: 'VOTING'"
    }

    def "Should block start voting if not all matches are played"() {
        given:

        def master = DEF_PARTY_MASTER
        def player1 = DEF_PARTY_USERS[0]
        def player2 = DEF_PARTY_USERS[1]

        def party = defaultParty()
                .withGame(defaultContest())
                .withUsers([player1, player2])
                .build()

        when:
        party.act(master, new StartPartyAction())
        party.act(player1, new JoinPartyAction())
        party.act(player2, new JoinPartyAction())
        party.act(master, new NextGameAction())
        party.act(master, new StartVotingAction())

        then:
        def e = thrown(PartymakerException)
        e.message == "Unable to start voting: not all matches are played; Current match is 1, total matches: 2"
    }

    def "Should block switching to previous match if contest is on first match"() {
        given:
        def master = DEF_PARTY_MASTER
        def player1 = DEF_PARTY_USERS[0]
        def player2 = DEF_PARTY_USERS[1]

        def party = defaultParty()
                .withGame(defaultContest())
                .withUsers([player1, player2])
                .build()

        when:
        party.act(master, new StartPartyAction())
        party.act(player1, new JoinPartyAction())
        party.act(player2, new JoinPartyAction())
        party.act(master, new NextGameAction())
        party.act(master, new PrevMatchAction())

        then:
        def e = thrown(PartymakerException)
        e.message == "Can't go to previous match: current match is already first"
    }

    def "Should block switching to previous match if contest is not in CONTEST stage"() {
        given:
        def master = DEF_PARTY_MASTER
        def player1 = DEF_PARTY_USERS[0]
        def player2 = DEF_PARTY_USERS[1]

        def party = defaultParty()
                .withGame(defaultContest())
                .withUsers([player1, player2])
                .build()

        when:
        party.act(master, new StartPartyAction())
        party.act(player1, new JoinPartyAction())
        party.act(player2, new JoinPartyAction())
        party.act(master, new NextGameAction())
        party.act(master, new NextMatchAction())
        party.act(master, new StartVotingAction())
        party.act(master, new PrevMatchAction())

        then:
        def e = thrown(PartymakerException)
        e
                .message == "'PREV_MATCH' is not available: wrong contest stage; Allowed stage: 'CONTEST', current stage: 'VOTING'"
    }

    def "Should block switching to next match if contest is on last match"() {
        given:

        def master = DEF_PARTY_MASTER
        def player1 = DEF_PARTY_USERS[0]
        def player2 = DEF_PARTY_USERS[1]

        def party = defaultParty()
                .withGame(defaultContest())
                .withUsers([player1, player2])
                .build()


        when:
        party.act(master, new StartPartyAction())
        party.act(player1, new JoinPartyAction())
        party.act(player2, new JoinPartyAction())
        party.act(master, new NextGameAction())
        party.act(master, new NextMatchAction())
        party.act(master, new NextMatchAction())

        then:
        def e = thrown(PartymakerException)
        e.message == "Can't go to next match: current match is already last"
    }

    def "Should block switching to next match if contest is on VOTING stage"() {
        given:

        def master = DEF_PARTY_MASTER
        def player1 = DEF_PARTY_USERS[0]
        def player2 = DEF_PARTY_USERS[1]

        def party = defaultParty()
                .withGame(defaultContest())
                .withUsers([player1, player2])
                .build()


        when:
        party.act(master, new StartPartyAction())
        party.act(player1, new JoinPartyAction())
        party.act(player2, new JoinPartyAction())
        party.act(master, new NextGameAction())
        party.act(master, new NextMatchAction())
        party.act(master, new StartVotingAction())
        party.act(master, new NextMatchAction())

        then:
        def e = thrown(PartymakerException)
        e.message == "'NEXT_MATCH' is not available: wrong contest stage; Allowed stage: 'CONTEST', current stage: 'VOTING'"
    }

    def "Should block voting if contest is not in VOTING stage"() {
        given:
        def master = DEF_PARTY_MASTER
        def player1 = DEF_PARTY_USERS[0]
        def player2 = DEF_PARTY_USERS[1]

        def party = defaultParty()
                .withGame(defaultContest())
                .withUsers([player1, player2])
                .build()


        when:
        party.act(master, new StartPartyAction())
        party.act(player1, new JoinPartyAction())
        party.act(player2, new JoinPartyAction())
        party.act(master, new NextGameAction())
        party.act(player1, new ContestVotingAction([player2.id()]))

        then:
        def e = thrown(PartymakerException)
        e
                .message == "'CONTEST_VOTING' is not available: wrong contest stage; Allowed stage: 'VOTING', current stage: 'CONTEST'"
    }

    def "Should block voting for yourself"() {
        given:
        def master = DEF_PARTY_MASTER
        def player1 = DEF_PARTY_USERS[0]
        def player2 = DEF_PARTY_USERS[1]

        def party = defaultParty()
                .withGame(defaultContest())
                .withUsers([player1, player2])
                .build()


        when:
        party.act(master, new StartPartyAction())
        party.act(player1, new JoinPartyAction())
        party.act(player2, new JoinPartyAction())
        party.act(master, new NextGameAction())
        party.act(master, new NextMatchAction())
        party.act(master, new StartVotingAction())
        party.act(player1, new ContestVotingAction([player1.id()]))

        then:
        def e = thrown(PartymakerException)
        e.message == "You can not vote for yourself"
    }

    def "Should block voting for non-member user"() {
        given:
        def master = DEF_PARTY_MASTER
        def player1 = DEF_PARTY_USERS[0]
        def player2 = DEF_PARTY_USERS[1]
        def player3 = DEF_PARTY_USERS[2]

        def party = defaultParty()
                .withGame(defaultContest())
                .withUsers([player1, player2])
                .build()


        when:
        party.act(master, new StartPartyAction())
        party.act(player1, new JoinPartyAction())
        party.act(player2, new JoinPartyAction())
        party.act(master, new NextGameAction())
        party.act(master, new NextMatchAction())
        party.act(master, new StartVotingAction())
        party.act(player1, new ContestVotingAction([player3.id()]))

        then:
        def e = thrown(PartymakerException)
        e.message == "Invalid vote: ${player3.id()} is not a member"
    }

    def "Should block initialization of contest with random distribution if there are less tasks then party members"() {
        given:
        def master = DEF_PARTY_MASTER
        def player1 = DEF_PARTY_USERS[0]
        def player2 = DEF_PARTY_USERS[1]
        def player3 = DEF_PARTY_USERS[2]

        def party = defaultParty()
                .withGame(defaultContest()
                                  .withTasksDistribution(TasksDistribution.RANDOM)
                                  .withTasks([
                                          Tuple.tuple("Task 1", null),
                                          Tuple.tuple("Task 2", null),
                                  ]))
                .withUsers([player1, player2, player3])
                .build()

        when:
        party.act(master, new StartPartyAction())
        party.act(player1, new JoinPartyAction())
        party.act(player2, new JoinPartyAction())
        party.act(player3, new JoinPartyAction())
        party.act(master, new NextGameAction())

        then:
        def e = thrown(PartymakerException)
        e.message == "Invalid contest ${DEF_CONTEST_NAME} configuration: members more than tasks"
    }

    def "Should block initialization of contest with predefined distribution if there are members without configured match"() {
        given:
        def master = DEF_PARTY_MASTER
        def player1 = DEF_PARTY_USERS[0]
        def player2 = DEF_PARTY_USERS[1]
        def player3 = DEF_PARTY_USERS[2]

        def party = defaultParty()
                .withGame(defaultContest()
                                  .withTasksDistribution(TasksDistribution.PREDEFINED)
                                  .withTasks([
                                          Tuple.tuple("Task 1", player1.id()),
                                          Tuple.tuple("Task 2", player2.id()),
                                  ]))
                .withUsers([player1, player2, player3])
                .build()


        when:
        party.act(master, new StartPartyAction())
        party.act(player1, new JoinPartyAction())
        party.act(player2, new JoinPartyAction())
        party.act(player3, new JoinPartyAction())
        party.act(master, new NextGameAction())

        then:
        def e = thrown(PartymakerException)
        e
                .message == "Invalid contest matches configuration: match distribution is ${TasksDistribution.PREDEFINED.name()}, but some competitor(s) doesn't configured match"
    }

    // Case is covered by readiness check on party level
    def "Should block finalization of contest, not in VOTING stage"() {
        given:
        def master = DEF_PARTY_MASTER
        def player1 = DEF_PARTY_USERS[0]
        def player2 = DEF_PARTY_USERS[1]

        def party = defaultParty()
                .withGame(defaultContest())
                .withUsers([player1, player2])
                .build()


        when:
        party.act(master, new StartPartyAction())
        party.act(player1, new JoinPartyAction())
        party.act(player2, new JoinPartyAction())
        party.act(master, new NextGameAction())
        party.act(master, new NextGameAction())
        then:
        def e = thrown(PartymakerException)
//        e.message == "Unable to finalize contest: finalizing is only available for contests in status VOTING, but current status is CONTEST"
        e.message == "Can't finish stage: not everybody is ready"
    }

    // Case is covered by readiness check on party level
    def "Should block finalization of contest if some player haven't fully voted"() {
        given:
        def master = DEF_PARTY_MASTER
        def player1 = DEF_PARTY_USERS[0]
        def player2 = DEF_PARTY_USERS[1]

        def party = defaultParty()
                .withGame(defaultContest())
                .withUsers([player1, player2])
                .build()

        when:
        party.act(master, new StartPartyAction())
        party.act(player1, new JoinPartyAction())
        party.act(player2, new JoinPartyAction())
        party.act(master, new NextGameAction())
        party.act(master, new NextMatchAction())
        party.act(master, new StartVotingAction())
        party.act(player1, new ContestVotingAction([player2.id()]))
        party.act(master, new NextGameAction())

        then:
        def e = thrown(PartymakerException)
        // e.message == "Player haven't fully voted: 1 of 2"
        e.message == "Can't finish stage: not everybody is ready"
    }

    def "Should identify configuration violation when there are less tasks then party users"() {
        given:
        def master = DEF_PARTY_MASTER
        def player1 = DEF_PARTY_USERS[0]
        def player2 = DEF_PARTY_USERS[1]
        def player3 = DEF_PARTY_USERS[2]

        def party = defaultParty()
                .withGame(defaultContest()
                                  .withTasksDistribution(TasksDistribution.RANDOM)
                                  .withTasks([
                                          Tuple.tuple("Task 1", null),
                                          Tuple.tuple("Task 2", null),
                                  ]))
                .withUsers([player1, player2, player3])
                .build()

        when:
        def violations = party.validate()

        then:
        violations.size() == 1
        def violation = violations[0]

        violation.path() == 'games[0].tasks'
        violation.message() == "Invalid contest 'Test contest' configuration: possible members more than tasks. Members - 3, Contest tasks - 2"

    }

    def "Should identify configuration violation when contest has predefined distribution and some of party user don't have configured match"() {
        given:
        def master = DEF_PARTY_MASTER
        def player1 = DEF_PARTY_USERS[0]
        def player2 = DEF_PARTY_USERS[1]
        def player3 = DEF_PARTY_USERS[2]

        def party = defaultParty()
                .withGame(defaultContest()
                                  .withTasksDistribution(TasksDistribution.PREDEFINED)
                                  .withTasks([
                                          Tuple.tuple("Task 1", player1.id()),
                                          Tuple.tuple("Task 2", player2.id()),
                                          Tuple.tuple("Task 3", 'some_invalid_id'),
                                  ]))
                .withUsers([player1, player2, player3])
                .build()

        when:
        def violations = party.validate()

        then:
        violations.size() == 1
        def violation = violations[0]

        violation.path() == 'games[0].tasks'
        violation.message() == "Invalid contest 'Test contest' configuration: match distribution is 'PREDEFINED', but tasks were configured not for all possible competitors"
    }
}