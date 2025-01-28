package org.yvasylchuk.partymaker.party.core.command;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import org.yvasylchuk.partymaker.party.core.Game.GameType;
import org.yvasylchuk.partymaker.party.dto.management.ContestTaskDto;

import java.util.List;


@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "gameType", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes(
        failOnRepeatedNames = true,
        value = {
                @Type(value = UpsertPartyGameCommand.UpsertContestCommand.class, name = "CONTEST")
        }
)
public abstract sealed class UpsertPartyGameCommand {
    public abstract GameType getGameType();

    protected final Integer gameIdx;
    @NotBlank
    @Size(min = 5, max = 50)
    protected final String name;
    @NotBlank
    @Size(min = 20, max = 200)
    protected final String description;
    @NotEmpty
    protected final List<Integer> scorePerPlace;


    @Getter
    public static final class UpsertContestCommand extends UpsertPartyGameCommand {
        private final GameType gameType = GameType.CONTEST;
        private final TasksDistribution tasksDistribution;
        private final List<ContestTaskDto> tasks;

        @Jacksonized
        @Builder
        public UpsertContestCommand(Integer gameIdx,
                                    String name,
                                    String description,
                                    @Singular() List<ContestTaskDto> tasks,
                                    List<Integer> scorePerPlace,
                                    TasksDistribution tasksDistribution) {
            super(gameIdx, name, description, scorePerPlace);
            this.tasksDistribution = tasksDistribution;
            this.tasks = tasks;
        }

    }

}
