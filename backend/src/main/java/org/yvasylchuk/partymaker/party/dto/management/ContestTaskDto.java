package org.yvasylchuk.partymaker.party.dto.management;

public record ContestTaskDto(
        String task,
        String participantId
) {
    public ContestTaskDto(String task) {
        this(task, null);
    }
}
