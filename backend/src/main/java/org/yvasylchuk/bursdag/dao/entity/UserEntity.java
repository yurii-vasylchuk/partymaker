package org.yvasylchuk.bursdag.dao.entity;

import jakarta.persistence.*;
import lombok.*;
import org.yvasylchuk.bursdag.game.core.Game;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;

@Entity
@Table(name = "users")
@Getter
@Setter(AccessLevel.PROTECTED)
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "token")
    private String token;

    @Column(name = "roles")
    @Getter(AccessLevel.PROTECTED)
    private String rolesStr;

    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    @Transient
    public List<String> getRoles() {
        return rolesStr == null ?
                emptyList() :
                Arrays.stream(rolesStr.split(",")).toList();
    }

    public void setRoles(List<String> roles) {
        this.rolesStr = roles == null || roles.isEmpty() ?
                null :
                String.join(",", roles);
    }
}
