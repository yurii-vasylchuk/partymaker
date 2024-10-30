package org.yvasylchuk.partymaker.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Document(collection = "users")
@AllArgsConstructor
public class User {
    @Id
    private String id;
    private String username;
    private List<String> roles;
    private String token;
}
