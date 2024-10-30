package org.yvasylchuk.partymaker.game.core.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes(value = {
        @JsonSubTypes.Type(AddPartyUserCommand.AddNewPartyUserCommand.class),
        @JsonSubTypes.Type(AddPartyUserCommand.AddExistentPartyUserCommand.class),
})
public sealed class AddPartyUserCommand {
    @Getter
    @RequiredArgsConstructor(onConstructor_ = @JsonCreator)
    public static final class AddNewPartyUserCommand extends AddPartyUserCommand {
        private final String username;
        private final String token;
    }

    @Getter
    @RequiredArgsConstructor(onConstructor_ = @JsonCreator)
    public static final class AddExistentPartyUserCommand extends AddPartyUserCommand {
        private final String id;
    }
}
