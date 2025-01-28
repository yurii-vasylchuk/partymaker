package org.yvasylchuk.partymaker.party.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.stereotype.Component;
import org.yvasylchuk.partymaker.common.dto.PartymakerPrincipal;
import org.yvasylchuk.partymaker.party.core.Party;
import org.yvasylchuk.partymaker.party.core.TokenGenerator;
import org.yvasylchuk.partymaker.party.core.command.CreatePartyCommand;

import java.util.List;
import java.util.Optional;

import static org.springframework.data.domain.Sort.Order.desc;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@Slf4j
@Component
@RequiredArgsConstructor
public class PartyDao {
    private final TokenGenerator tokenGenerator;
    private final MongoTemplate template;

    public Optional<Party> get(String id) {
        return Optional.ofNullable(template.findById(id, Party.class))
                       .map(party -> {
                           party.setTokenGenerator(tokenGenerator);

                           return party;
                       });
    }

    public List<Party> findBy(PartyFilter filter) {
        Query query = new Query();

        query.addCriteria(switch (filter.participationLevel()) {
            case MASTER -> where("partyMaster").is(filter.userId());
            case PARTICIPANT -> new Criteria().orOperator(
                    where("participants.id").is(filter.userId()),
                    where("partyMaster").is(filter.userId()));
        });

        if (filter.status() != null) {
            query.addCriteria(where("status").is(filter.status()));
        }

        if (filter.text() != null) {
            query.addCriteria(TextCriteria.forDefaultLanguage()
                                          .matching(filter.text()));
            query.with(Sort.by(desc("score")));
        }

        return template.find(query, Party.class);
    }

    public Party save(Party party) {
        return template.save(party);
    }

    public Party create(CreatePartyCommand command, PartymakerPrincipal principal) {
        return Party.create(command, principal, tokenGenerator);
    }
}
