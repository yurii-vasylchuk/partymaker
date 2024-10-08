package org.yvasylchuk.bursdag.common.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes(failOnRepeatedNames = true,
        value = {
                @Type(value = AsyncAction.ChoseAvatarAction.class, name = "CHOOSE_AVATAR"),
                @Type(value = AsyncAction.ChoseNicknameAction.class, name = "CHOOSE_NICKNAME"),
                @Type(value = AsyncAction.ChoseTraitsAction.class, name = "CHOOSE_TRAITS"),
                @Type(value = AsyncAction.PlayerReadyAction.class, name = "READY"),
                @Type(value = AsyncAction.GuessesChangedAction.class, name = "GUESSES_CHANGED"),
                @Type(value = AsyncAction.PlacementsSetAction.class, name = "PLACEMENTS_SET"),
                @Type(value = AsyncAction.NextTaskAction.class, name = "NEXT_TASK"),
                @Type(value = AsyncAction.PrevTaskAction.class, name = "PREV_TASK"),
                @Type(value = AsyncAction.VoteAction.class, name = "VOTE"),
                @Type(value = AsyncAction.ChoseWinnerAction.class, name = "CHOSE_WINNER"),
                @Type(value = AsyncAction.NextMatchAction.class, name = "NEXT_MATCH"),
        })
public abstract sealed class AsyncAction {
    public abstract AsyncAction.Type getType();

    public enum Type {
        //REGISTRATION
        CHOOSE_AVATAR,
        CHOOSE_NICKNAME,
        CHOOSE_TRAITS,

        //GUESS
        GUESSES_CHANGED,

        //CONTEST
        NEXT_TASK,
        PREV_TASK,
        PLACEMENTS_SET,

        //TOURNAMENT
        VOTE,
        CHOSE_WINNER,
        NEXT_MATCH,

        //COMMON
        READY,
    }

    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor
    public static final class ChoseAvatarAction extends AsyncAction {
        private final Type type = Type.CHOOSE_AVATAR;
        private final String avatar;
    }

    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor
    public static final class ChoseNicknameAction extends AsyncAction {
        private final Type type = Type.CHOOSE_NICKNAME;
        private final String nickname;
    }

    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor
    public static final class ChoseTraitsAction extends AsyncAction {
        private final Type type = Type.CHOOSE_TRAITS;
        private final Map<String, String> traits;
    }

    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor
    public static final class PlayerReadyAction extends AsyncAction {
        private final Type type = Type.READY;
    }

    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor
    public static final class NextTaskAction extends AsyncAction {
        private final Type type = Type.NEXT_TASK;
    }

    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor
    public static final class PrevTaskAction extends AsyncAction {
        private final Type type = Type.PREV_TASK;
    }

    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor
    public static final class PlacementsSetAction extends AsyncAction {
        private final Type type = Type.PLACEMENTS_SET;
        // Place to userId
        private final Map<Integer, Integer> placements;
    }

    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor
    public static final class GuessesChangedAction extends AsyncAction {
        private final Type type = Type.GUESSES_CHANGED;
        private final Map<Integer, Integer> guesses;
    }

    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor
    public static final class VoteAction extends AsyncAction {
        private final Type type = Type.VOTE;
        private final Integer votedPlayer;
    }

    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor
    public static final class ChoseWinnerAction extends AsyncAction {
        private final Type type = Type.CHOSE_WINNER;
        private final Integer winnerId;
    }

    @Getter
    @Builder
    @Jacksonized
    @RequiredArgsConstructor
    public static final class NextMatchAction extends AsyncAction {
        private final Type type = Type.NEXT_MATCH;
    }
}
