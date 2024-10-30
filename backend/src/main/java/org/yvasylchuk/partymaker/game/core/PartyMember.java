package org.yvasylchuk.partymaker.game.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.PersistenceCreator;

import java.util.Map;

@Getter
@Builder
@AllArgsConstructor(onConstructor_ = @PersistenceCreator)
public class PartyMember {
    private String id;
    private String name;
    private Map<VariableOptionType, Object> variableOptions;

    public enum VariableOptionType {
        NICKNAME,
        AVATAR,
        TREATS,
    }
}
