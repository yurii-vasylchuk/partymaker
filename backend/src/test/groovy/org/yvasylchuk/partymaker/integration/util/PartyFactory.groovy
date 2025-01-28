package org.yvasylchuk.partymaker.integration.util

import org.springframework.stereotype.Component
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal
import org.yvasylchuk.partymaker.party.core.Party
import org.yvasylchuk.partymaker.party.core.PartyMember.VariableOptionType
import org.yvasylchuk.partymaker.party.core.TokenGenerator
import org.yvasylchuk.partymaker.party.core.command.CreatePartyCommand
import org.yvasylchuk.partymaker.party.core.command.TasksDistribution
import org.yvasylchuk.partymaker.party.core.command.UpsertPartyGameCommand
import org.yvasylchuk.partymaker.party.dao.PartyDao
import org.yvasylchuk.partymaker.party.dto.management.ContestTaskDto
import org.yvasylchuk.partymaker.party.dto.management.PartyParticipantDto

import static org.yvasylchuk.partymaker.party.core.command.AddPartyUserCommand.AddExistentPartyUserCommand

@Component
class PartyFactory {
    public static final String                   DEFAULT_PARTY_NAME                 = "Test party"
    public static final String                   DEFAULT_PARTY_DESCRIPTION          = "Test party description"
    public static final boolean                  DEFAULT_PARTY_MASTER_PARTICIPATING = false
    public static final List<VariableOptionType> DEFAULT_PARTY_OPTIONS              = [
            VariableOptionType.NICKNAME
    ]

    public static final String DEFAULT_MASTER_USERNAME = 'master'
    public static final String DEFAULT_MASTER_EMAIL    = 'master@email.com'
    public static final String DEFAULT_MASTER_PASSWORD = 'master_password'
    public static final String DEFAULT_MASTER_ID       = 'master_id'

    public static final String            DEFAULT_CONTEST_NAME              = 'Contest'
    public static final String            DEFAULT_CONTEST_DESCRIPTION       = 'Contest description'
    public static final String            DEFAULT_CONTEST_TASK              = 'Contest task #'
    public static final List<Integer>     DEFAULT_CONTEST_SCORES            = [5, 3, 1]
    public static final TasksDistribution DEFAULT_CONTEST_TASK_DISTRIBUTION = TasksDistribution.RANDOM


    private final TokenGenerator  tokenGenerator
    private final UserFactory     userFactory
    private final PartyDao partyDao

    PartyFactory(TokenGenerator tokenGenerator, UserFactory userFactory, PartyDao partyDao) {
        this.tokenGenerator = tokenGenerator
        this.userFactory = userFactory
        this.partyDao = partyDao
    }

    PartyBuilder emptyParty() {
        return new PartyBuilder(tokenGenerator, partyDao).tap {
            name = DEFAULT_PARTY_NAME
            description = DEFAULT_PARTY_DESCRIPTION
            variableOptions = DEFAULT_PARTY_OPTIONS
            isMasterParticipating = DEFAULT_PARTY_MASTER_PARTICIPATING
            master = defaultMaster()
        }
    }

    UserFactory.UserBuilder defaultMaster() {
        return userFactory.defaultUser().tap {
            username = DEFAULT_MASTER_USERNAME
            email = DEFAULT_MASTER_EMAIL
            password = DEFAULT_MASTER_PASSWORD
            id = DEFAULT_MASTER_ID
        }
    }

    ContestBuilder defaultContest(int tasksCount, String modifier = null, Integer gameIdx = null) {
        new ContestBuilder(gameIdx).tap {
            name = DEFAULT_CONTEST_NAME + (modifier ?: '')
            description = DEFAULT_CONTEST_DESCRIPTION + (modifier ?: '')
            tasksDistribution = DEFAULT_CONTEST_TASK_DISTRIBUTION

            for (i in 0..<tasksCount) {
                tasks << new ContestTaskDto(DEFAULT_CONTEST_TASK + "${i + 1}")
            }
            for (i in 0..<(Math.min(3, tasksCount))) {
                scorePerPlace << DEFAULT_CONTEST_SCORES[i]
            }
        }
    }

    class PartyBuilder {
        String                    id           = null
        List<GameBuilder>         games        = []
        String                    name
        String                    description
        List<VariableOptionType>  variableOptions
        boolean                   isMasterParticipating
        PartymakerPrincipal       masterPrincipal
        List<PartyParticipantDto> participants = []

        private boolean                 shouldSave = false
        private UserFactory.UserBuilder master


        private final TokenGenerator  tokenGenerator
        private final PartyDao partyDao

        private PartyBuilder(TokenGenerator tokenGenerator, PartyDao partyDao) {
            this.tokenGenerator = tokenGenerator
            this.partyDao = partyDao
        }

        void setShouldSave(boolean shouldSave) {
            this.shouldSave = shouldSave
            master?.saveToDb = shouldSave
        }

        void setMaster(UserFactory.UserBuilder master) {
            this.master = master
            this.master?.saveToDb = shouldSave
        }

        ContestBuilder withContest() {
            def cb = new ContestBuilder(null)
            games += cb
            return cb
        }

        Party build() {
            def command = new CreatePartyCommand(
                    name,
                    description,
                    variableOptions.collect { it.name() },
                    isMasterParticipating)

            def masterPrincipal = masterPrincipal ?: new PartymakerPrincipal(master.build())

            def party = Party.create(command, masterPrincipal, tokenGenerator)

            if (games != null) {
                games.forEach {
                    party.upsertGame(it.build(), masterPrincipal)
                }
            }

            if (participants != null) {
                participants.forEach {
                    def id = it.id()
                    def username = it.username()
                    def addUserCommand = id != null ?
                                         new AddExistentPartyUserCommand(id) :
                                         new AddExistentPartyUserCommand(userFactory.participant(username).build().id)
                    party.addUser(addUserCommand, masterPrincipal)
                }
            }

            if (shouldSave) {
                party = partyDao.save(party)
            }

            return party
        }
    }

    abstract class GameBuilder {
        protected abstract UpsertPartyGameCommand build()
    }

    class ContestBuilder extends GameBuilder {
        private final Integer gameIdx

        String               name
        String               description
        List<ContestTaskDto> tasks         = []
        List<Integer>        scorePerPlace = []
        TasksDistribution    tasksDistribution

        private ContestBuilder(Integer gameIdx) {
            this.gameIdx = gameIdx
        }

        protected UpsertPartyGameCommand build() {
            return new UpsertPartyGameCommand.UpsertContestCommand(
                    gameIdx, name, description, tasks, scorePerPlace, tasksDistribution
            )
        }
    }
}
