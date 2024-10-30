package org.yvasylchuk.partymaker.game.core.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePartyCommand(
        @NotBlank
        @Size(min = 5, max = 100)
        String name,
        String description,
        List<String> variableOptions
) {
}
