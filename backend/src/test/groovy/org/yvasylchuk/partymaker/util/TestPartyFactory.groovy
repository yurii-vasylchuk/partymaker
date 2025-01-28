package org.yvasylchuk.partymaker.util

import org.springframework.util.ReflectionUtils
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal
import org.yvasylchuk.partymaker.party.core.Party
import org.yvasylchuk.partymaker.party.core.PartyMember
import org.yvasylchuk.partymaker.party.core.TokenGenerator
import org.yvasylchuk.partymaker.party.core.command.CreatePartyCommand

class TestPartyFactory {
    public static final String PARTY_1_NAME        = 'Party 1'
    public static final String PARTY_1_DESCRIPTION = 'Party 1 description'
    public static final String PARTY_1_ID          = 'party#1'

    private TokenGenerator tokenGenerator;

    TestPartyFactory(TokenGenerator tokenGenerator) {
        this.tokenGenerator = tokenGenerator
    }

    Party emptyParty(PartymakerPrincipal principal) {
        def party = Party.create(
                new CreatePartyCommand(
                        PARTY_1_NAME,
                        PARTY_1_DESCRIPTION,
                        [PartyMember.VariableOptionType.AVATAR.name(), PartyMember.VariableOptionType.NICKNAME.name()],
                        true
                ),
                principal,
                tokenGenerator
        )

        def idField = ReflectionUtils.findField(Party, 'id', String)
        ReflectionUtils.makeAccessible(idField)
        ReflectionUtils.setField(idField, party, PARTY_1_ID)

        return party
    }
}
