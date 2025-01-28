package org.yvasylchuk.partymaker.user.dao;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.yvasylchuk.partymaker.user.core.User;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String>, UserCustomRepository {
    Optional<User> findUserByEmail(@Email @NotEmpty String email);
}
