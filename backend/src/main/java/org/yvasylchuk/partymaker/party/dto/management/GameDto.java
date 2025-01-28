package org.yvasylchuk.partymaker.party.dto.management;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public sealed class GameDto {
    protected final Integer order;
    protected final String name;
    protected final String description;
    protected final String type;

    @Getter
    public static final class ContestDto extends GameDto {
        private final List<ContestTaskDto> tasks;
        private final List<Integer> scoresPerPlace;
        private final String matchDistribution;

        public ContestDto(Integer order, String name, String description, String type, List<ContestTaskDto> tasks, List<Integer> scoresPerPlace, String matchDistribution) {
            super(order, name, description, type);
            this.tasks = tasks;
            this.scoresPerPlace = scoresPerPlace;
            this.matchDistribution = matchDistribution;
        }
    }
}
