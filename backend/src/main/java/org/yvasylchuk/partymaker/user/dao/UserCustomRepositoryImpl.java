package org.yvasylchuk.partymaker.user.dao;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.yvasylchuk.partymaker.user.core.User;

import java.util.Optional;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Component
@RequiredArgsConstructor
public class UserCustomRepositoryImpl implements UserCustomRepository {
    private final MongoTemplate mongoTemplate;

    @Override
    public Optional<User> findUserByToken(String token) {
        Query query = new Query();

        query.addCriteria(where("participant.token").is(token));
        query.fields().include("participant.$");

        Document document = mongoTemplate.findOne(query, Document.class, "parties");

        if (document == null) {
            return Optional.empty();
        }

        Document participant = document.getList("participant", Document.class).getFirst();
        String participantId = participant.get("id", String.class);

        User user = mongoTemplate.findById(participantId, User.class);

        return Optional.ofNullable(user);
    }
}
