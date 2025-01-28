package org.yvasylchuk.partymaker.integration.util

import lombok.Setter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.yvasylchuk.partymaker.user.core.PartymakerRole
import org.yvasylchuk.partymaker.user.core.User

@Component
class UserFactory {
    public static String               DEFAULT_USERNAME = "User"
    public static String               DEFAULT_EMAIL    = "user@user.com"
    public static String               DEFAULT_PASSWORD = "password"
    public static List<PartymakerRole> DEFAULT_ROLES    = [PartymakerRole.REGULAR, PartymakerRole.PARTICIPANT]



    @Autowired
    MongoTemplate   mongoTemplate
    @Autowired
    PasswordEncoder passwordEncoder

    def defaultUser() {
        def user = new UserBuilder(mongoTemplate)
        user.with {
            username = DEFAULT_USERNAME
            email = DEFAULT_EMAIL
            password = DEFAULT_PASSWORD
            roles = DEFAULT_ROLES
        }
        return user
    }

    def participant(String username) {
        return new UserBuilder(mongoTemplate).tap {
            it.username = username
            roles = [PartymakerRole.PARTICIPANT]
        }
    }

    @Setter
    class UserBuilder {
        private String               id             = null
        private String               username
        private String               email
        private String               password
        private List<PartymakerRole> roles
        private boolean saveToDb = false

        private final MongoTemplate mongoTemplate

        private UserBuilder(MongoTemplate mongoTemplate) {
            this.mongoTemplate = mongoTemplate
        }

        void setId(String id) {
            this.id = id
        }

        void setUsername(String username) {
            this.username = username
        }

        void setEmail(String email) {
            this.email = email
        }

        void setPassword(String password) {
            this.password = password
        }

        void setRoles(List<PartymakerRole> roles) {
            this.roles = roles
        }

        void setSaveToDb(boolean shouldSaveToDb) {
            this.saveToDb = shouldSaveToDb
        }

        User build() {
            def userConstructor = User.class.getDeclaredConstructor(String, String, String, String, List)
            userConstructor.setAccessible(true)

            def encodedPassword = password == null ? null : passwordEncoder.encode(password)
            def user = userConstructor.newInstance(id, email, username, encodedPassword, roles)

            def collection = User.class.getAnnotation(Document).collection()

            if (saveToDb) {
                mongoTemplate.insert(user, collection)
            }

            return user
        }
    }
}
