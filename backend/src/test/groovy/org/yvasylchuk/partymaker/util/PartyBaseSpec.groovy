package org.yvasylchuk.partymaker.util


import org.springframework.util.ReflectionUtils
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal
import org.yvasylchuk.partymaker.party.core.Party
import org.yvasylchuk.partymaker.party.core.PartyMember.VariableOptionType
import org.yvasylchuk.partymaker.party.core.TokenGenerator
import org.yvasylchuk.partymaker.party.core.command.AddPartyUserCommand
import org.yvasylchuk.partymaker.party.core.command.CreatePartyCommand
import org.yvasylchuk.partymaker.party.core.command.TasksDistribution
import org.yvasylchuk.partymaker.party.core.command.UpsertPartyGameCommand
import org.yvasylchuk.partymaker.party.dto.management.ContestTaskDto
import spock.lang.Specification

abstract class PartyBaseSpec extends Specification {
    protected static final principalFactory = new TestPrincipalFactory()

    static final DEF_PARTY_ID                      = 'PARTY1'
    static final DEF_PARTY_NAME                    = 'Test party'
    static final DEF_PARTY_DESCRIPTION             = 'Test party description'
    static final DEF_VARIABLE_OPTIONS              = [] as Set
    static final DEF_PARTY_IS_MASTER_PARTICIPATING = false

    static final DEF_PARTY_MASTER = principalFactory.testUser1()
    static final DEF_PARTY_USERS  = [
            principalFactory.testUser2(),
            principalFactory.testUser3(),
            principalFactory.testUser4(),
            principalFactory.testUser5(),
    ]

    static final DEF_CONTEST_NAME               = 'Test contest'
    static final DEF_CONTEST_DESCRIPTION        = 'Test contest description'
    static final DEF_CONTEST_TASKS              = [
            new Tuple2<String, String>("Task 1", null),
            new Tuple2<String, String>("Task 2", null),
            new Tuple2<String, String>("Task 3", null),
            new Tuple2<String, String>("Task 4", null),
            new Tuple2<String, String>("Task 5", null),
            new Tuple2<String, String>("Task 6", null),
    ]
    static final DEF_CONTEST_SCORE_PER_PLACE    = [5, 4, 3]
    static final DEF_CONTEST_TASKS_DISTRIBUTION = TasksDistribution.RANDOM

    def tokenGenerator = Mock(TokenGenerator)


    PartyBuilder defaultParty() {
        return new PartyBuilder(principalFactory, tokenGenerator);
    }

    UpsertContestCommandBuilder defaultContest() {
        return new UpsertContestCommandBuilder()
    }

    static class PartyBuilder {
        private final TestPrincipalFactory principalFactory
        private final TokenGenerator       tokenGenerator

        String                       partyId               = DEF_PARTY_ID
        String                       partyName             = DEF_PARTY_NAME
        String                       partyDescription      = DEF_PARTY_DESCRIPTION
        Set<VariableOptionType>      variableOptions       = DEF_VARIABLE_OPTIONS
        PartymakerPrincipal          master                = DEF_PARTY_MASTER
        Set<PartymakerPrincipal>     users                 = DEF_PARTY_USERS
        List<UpsertPartyGameCommand> games                 = []
        Boolean                      isMasterParticipating = DEF_PARTY_IS_MASTER_PARTICIPATING

        PartyBuilder withId(String id) {
            partyId = id
            return this
        }

        PartyBuilder withName(String name) {
            partyName = name
            return this
        }

        PartyBuilder withDescription(String description) {
            partyDescription = description
            return this
        }

        PartyBuilder withVariableOption(VariableOptionType variableOption) {
            this.variableOptions.add(variableOption)
            return this
        }

        PartyBuilder withVariableOptions(Collection<VariableOptionType> variableOptions) {
            this.variableOptions = variableOptions.toSet()
            return this
        }

        PartyBuilder withMaster(PartymakerPrincipal master) {
            this.master = master
            return this
        }

        PartyBuilder isMasterParticipating(Boolean isMasterParticipating) {
            this.isMasterParticipating = isMasterParticipating
            return this
        }

        PartyBuilder withMember(PartymakerPrincipal member) {
            this.users.add(member)
            return this
        }

        PartyBuilder withUsers(Collection<PartymakerPrincipal> users) {
            this.users = users.toSet()
            return this
        }

        PartyBuilder withGame(UpsertPartyGameCommand game) {
            this.games.add(game)
            return this
        }

        PartyBuilder withGame(UpsertGameCommandBuilder builder) {
            this.games.add(builder.build())
            return this
        }

        PartyBuilder withGames(Collection<UpsertPartyGameCommand> games) {
            this.games = new ArrayList(games)
            return this
        }

        PartyBuilder(TestPrincipalFactory principalFactory, TokenGenerator tokenGenerator) {
            this.principalFactory = principalFactory
            this.tokenGenerator = tokenGenerator
        }


        Party build() {
            def party = Party.create(
                    new CreatePartyCommand(
                            partyName,
                            partyDescription,
                            variableOptions.stream()
                                           .map(VariableOptionType::name)
                                           .toList(),
                            isMasterParticipating
                    ),
                    master,
                    tokenGenerator
            )

            def idField = ReflectionUtils.findField(Party, 'id', String)
            ReflectionUtils.makeAccessible(idField)
            ReflectionUtils.setField(idField, party, partyId)

            for (final def user in users) {
                party.addUser(new AddPartyUserCommand.AddExistentPartyUserCommand(user.id()), master)
            }

            for (final game in games) {
                party.upsertGame(game, master)
            }

            return party
        }

    }

    static class UpsertContestCommandBuilder implements UpsertGameCommandBuilder {
        String                       name              = DEF_CONTEST_NAME
        String                       description       = DEF_CONTEST_DESCRIPTION
        List<Tuple2<String, String>> tasks             = DEF_CONTEST_TASKS
        List<Integer>                scorePerPlace     = DEF_CONTEST_SCORE_PER_PLACE
        TasksDistribution            tasksDistribution = DEF_CONTEST_TASKS_DISTRIBUTION

        UpsertContestCommandBuilder withName(String name) {
            this.name = name
            return this
        }

        UpsertContestCommandBuilder withDescription(String description) {
            this.description = description
            return this
        }

        UpsertContestCommandBuilder withTasks(List<Tuple2<String, String>> tasks) {
            this.tasks = tasks
            return this
        }

        UpsertContestCommandBuilder withTask(String text) {
            this.tasks += new Tuple2<String, String>(text, null)
            return this
        }

        UpsertContestCommandBuilder withTask(String text, String participantId) {
            this.tasks += new Tuple2<String, String>(text, participantId)
            return this
        }

        UpsertContestCommandBuilder withScorePerPlace(List<Integer> scorePerPlace) {
            this.scorePerPlace = scorePerPlace
            return this
        }

        UpsertContestCommandBuilder withTasksDistribution(TasksDistribution tasksDistribution) {
            this.tasksDistribution = tasksDistribution
            return this
        }

        @Override
        UpsertPartyGameCommand build() {
            return new UpsertPartyGameCommand.UpsertContestCommand(null,
                                                                   name,
                                                                   description,
                                                                   tasks
                                                                           .collect { new ContestTaskDto(it.getV1(), it.getV2()) },
                                                                   scorePerPlace,
                                                                   tasksDistribution)
        }
    }

    static interface UpsertGameCommandBuilder {
        UpsertPartyGameCommand build()
    }
}
