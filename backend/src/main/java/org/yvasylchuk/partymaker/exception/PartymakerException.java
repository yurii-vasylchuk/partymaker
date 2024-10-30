package org.yvasylchuk.partymaker.exception;

import lombok.Getter;

@Getter
public class PartymakerException extends RuntimeException {
    private final Kind kind;

    public PartymakerException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public PartymakerException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public enum Kind {
        INTERNAL,
        GENERIC_CLIENT,
        NOT_FOUND,
        ACCESS_DENIED
    }
}
