package org.yvasylchuk.partymaker.game.core;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class Tournament /*extends Game<Tournament.Context>*/ {


    /// Match type. Should be used to identify, how to display particular task and what structure of
    /// match configuration to expect.
    ///
    /// ### Multiplayer matches:
    /// - SHARED - same task for every match player
    /// - INDIVIDUAL - distinct tasks for each player
    @RequiredArgsConstructor
    public enum MatchType {
        SHARED(false),
        INDIVIDUAL(true),
        ;

        private final boolean isMultitask;

        public boolean isSingletask() {
            return !isMultitask;
        }

        public boolean isMultitask() {
            return isMultitask;
        }
    }

    public record Context() {
    }

    @Getter
    @EqualsAndHashCode
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class MatchTemplate {
        private final MatchType type;
        private final Integer playersCount;
        private final List<Tournament.MatchTask> tasks;

        public static Tournament.MatchTemplate create(MatchType type,
                                                      Integer playersCount,
                                                      List<Tournament.MatchTask> tasks
                                                     ) {

            if (type.isSingletask()) {
                assertThat(tasks)
                        .describedAs("Match type %s is single-task and requires exactly 1 task")
                        .hasSize(1);
            }

            return new Tournament.MatchTemplate(type, playersCount, tasks);
        }
    }

    public record MatchTask(
            String markdown
    ) {
    }


}
