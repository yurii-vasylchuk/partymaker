package org.yvasylchuk.partymaker.game.core.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.yvasylchuk.partymaker.game.core.Game;

import java.util.List;

public record AddPartyGameCommand(
        @NotNull
        Game.GameType gameType,
        @NotBlank
        @Size(min = 5, max = 50)
        String name,
        @NotBlank
        @Size(min = 20, max = 200)
        String description,
        List<@NotBlank @Size(min = 20, max = 500) String> tasks
) {
}
