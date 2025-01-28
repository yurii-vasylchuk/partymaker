package org.yvasylchuk.partymaker.user.dao;

import org.yvasylchuk.partymaker.user.core.User;

import java.util.Optional;

public interface UserCustomRepository {
    Optional<User> findUserByToken(String token);
}
