package org.yvasylchuk.partymaker.party.dto.process;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes(failOnRepeatedNames = true,
        value = {
                @Type(value = PartyAction.VariableOptionSetAction.class, name = "VARIABLE_OPTION_SET"),
                @Type(value = PartyAction.JoinPartyAction.class, name = "JOIN"),
                @Type(value = PartyAction.NextGameAction.class, name = "NEXT_GAME"),
                @Type(value = PartyAction.StartPartyAction.class, name = "START_PARTY"),

                @Type(value = PartyAction.PlayerReadyAction.class, name = "READY"),
                @Type(value = PartyAction.NextMatchAction.class, name = "NEXT_MATCH"),
                @Type(value = PartyAction.PrevMatchAction.class, name = "PREV_MATCH"),

                @Type(value = PartyAction.ContestVotingAction.class, name = "CONTEST_VOTING"),
                @Type(value = PartyAction.StartVotingAction.class, name = "START_VOTING"),

//                @Type(value = AsyncAction.GuessesChangedAction.class, name = "GUESSES_CHANGED"),
//                @Type(value = AsyncAction.PlacementsSetAction.class, name = "PLACEMENTS_SET"),
//                @Type(value = AsyncAction.NextTaskAction.class, name = "NEXT_TASK"),
//                @Type(value = AsyncAction.PrevTaskAction.class, name = "PREV_TASK"),
//                @Type(value = AsyncAction.VoteAction.class, name = "VOTE"),
//                @Type(value = AsyncAction.ChoseWinnerAction.class, name = "CHOSE_WINNER"),
//                @Type(value = AsyncAction.NextMatchAction.class, name = "NEXT_MATCH"),
        })
public abstract sealed class PartyAction {
    public abstract PartyAction.Type getType();

    //TODO: Describe required party status, game status and user "status" (aka PARTICIPANT, MEMBER, MASTER, ADMIN)
    //TODO: Needed to automate access checks in party act method

    public enum Type {
        //PARTY-LEVEL ACTIONS
        JOIN,
        VARIABLE_OPTION_SET,
        NEXT_GAME,
        START_PARTY,

        //CONTEST
        CONTEST_VOTING,
        START_VOTING,

//        //GUESS
//        GUESSES_CHANGED,
//
//        NEXT_TASK,
//        PREV_TASK,
//
//        //TOURNAMENT
//        VOTE,
//        CHOSE_WINNER,
//        NEXT_MATCH,

        //COMMON
        READY,
        NEXT_MATCH,
        PREV_MATCH,
    }

    @Getter
    @Builder
    @ToString
    @Jacksonized
    @RequiredArgsConstructor
    public static final class JoinPartyAction extends PartyAction {
        private final Type type = Type.JOIN;
    }

    @Getter
    @Builder
    @ToString
    @Jacksonized
    @RequiredArgsConstructor
    public static final class VariableOptionSetAction extends PartyAction {
        private final Type type = Type.VARIABLE_OPTION_SET;
        private final String optionType;
        private final Object value;
    }

    @Getter
    @Builder
    @ToString
    @Jacksonized
    @RequiredArgsConstructor
    public static final class PlayerReadyAction extends PartyAction {
        private final Type type = Type.READY;
    }

    @Getter
    @Builder
    @ToString
    @Jacksonized
    @RequiredArgsConstructor
    public static final class ContestVotingAction extends PartyAction {
        private final Type type = Type.CONTEST_VOTING;

        private final List<String> places;
    }

    @Getter
    @Builder
    @ToString
    @Jacksonized
    @RequiredArgsConstructor
    public static final class NextMatchAction extends PartyAction {
        private final Type type = Type.NEXT_MATCH;
    }

    @Getter
    @Builder
    @ToString
    @Jacksonized
    @RequiredArgsConstructor
    public static final class PrevMatchAction extends PartyAction {
        private final Type type = Type.PREV_MATCH;
    }

    @Getter
    @Builder
    @ToString
    @Jacksonized
    @RequiredArgsConstructor
    public static final class StartVotingAction extends PartyAction {
        private final Type type = Type.START_VOTING;
    }

    @Getter
    @Builder
    @ToString
    @Jacksonized
    @RequiredArgsConstructor
    public static final class NextGameAction extends PartyAction {
        private final Type type = Type.NEXT_GAME;
    }

    @Getter
    @Builder
    @ToString
    @Jacksonized
    @RequiredArgsConstructor
    public static final class StartPartyAction extends PartyAction {
        private final Type type = Type.START_PARTY;
    }

//    @Getter
//    @Builder
//    @Jacksonized
//    @RequiredArgsConstructor
//    public static final class NextTaskAction extends AsyncAction {
//        private final Type type = Type.NEXT_TASK;
//    }
//
//    @Getter
//    @Builder
//    @Jacksonized
//    @RequiredArgsConstructor
//    public static final class PrevTaskAction extends AsyncAction {
//        private final Type type = Type.PREV_TASK;
//    }
//
//    @Getter
//    @Builder
//    @Jacksonized
//    @RequiredArgsConstructor
//    public static final class PlacementsSetAction extends AsyncAction {
//        private final Type type = Type.PLACEMENTS_SET;
//        // Place to userId
//        private final Map<Integer, Integer> placements;
//    }
//
//    @Getter
//    @Builder
//    @Jacksonized
//    @RequiredArgsConstructor
//    public static final class GuessesChangedAction extends AsyncAction {
//        private final Type type = Type.GUESSES_CHANGED;
//        private final Map<Integer, Integer> guesses;
//    }
//
//    @Getter
//    @Builder
//    @Jacksonized
//    @RequiredArgsConstructor
//    public static final class VoteAction extends AsyncAction {
//        private final Type type = Type.VOTE;
//        private final Integer votedPlayer;
//    }
//
//    @Getter
//    @Builder
//    @Jacksonized
//    @RequiredArgsConstructor
//    public static final class ChoseWinnerAction extends AsyncAction {
//        private final Type type = Type.CHOSE_WINNER;
//        private final Integer winnerId;
//    }
//

}
