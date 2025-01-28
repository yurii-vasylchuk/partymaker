package org.yvasylchuk.partymaker.util

import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal
import org.yvasylchuk.partymaker.user.core.PartymakerRole

class TestPrincipalFactory {
    static final String TEST_1_ID = "user_id_1"
    static final String TEST_1_USERNAME = "user 1"

    static final String TEST_2_ID = "user_id_2"
    static final String TEST_2_USERNAME = "user 2"

    static final String TEST_3_ID = "user_id_3"
    static final String TEST_3_USERNAME = "user 3"

    static final String TEST_4_ID = "user_id_4"
    static final String TEST_4_USERNAME = "user 4"

    static final String TEST_5_ID = "user_id_5"
    static final String TEST_5_USERNAME = "user 5"

    PartymakerPrincipal testUser1() {
        return regularUser(TEST_1_ID, TEST_1_USERNAME)
    }

    PartymakerPrincipal testUser2() {
        return regularUser(TEST_2_ID, TEST_2_USERNAME)
    }

    PartymakerPrincipal testUser3() {
        return regularUser(TEST_3_ID, TEST_3_USERNAME)
    }

    PartymakerPrincipal testUser4() {
        return regularUser(TEST_4_ID, TEST_4_USERNAME)
    }

    PartymakerPrincipal testUser5() {
        return regularUser(TEST_5_ID, TEST_5_USERNAME)
    }

    PartymakerPrincipal regularUser(String id, String username) {
        return new PartymakerPrincipal(
                id,
                List.of(PartymakerRole.REGULAR.name()),
                username
        )
    }
}
