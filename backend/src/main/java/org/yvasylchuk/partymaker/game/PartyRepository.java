package org.yvasylchuk.partymaker.game;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.yvasylchuk.partymaker.game.core.Party;

public interface PartyRepository extends MongoRepository<Party, String> {
}
