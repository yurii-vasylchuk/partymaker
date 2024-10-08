package org.yvasylchuk.bursdag.dao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.yvasylchuk.bursdag.dao.entity.UserEntity;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Integer> {
    Optional<UserEntity> findUserByToken(String token);
}
