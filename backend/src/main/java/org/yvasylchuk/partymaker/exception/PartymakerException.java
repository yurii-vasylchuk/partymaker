package org.yvasylchuk.partymaker.exception;

import lombok.Getter;

@Getter
public class PartymakerException extends RuntimeException {
    private final Kind kind;

    public PartymakerException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public PartymakerException(Kind kind, String message, Object... msgArgs) {
        super(message.formatted(msgArgs));
        this.kind = kind;
    }

    public PartymakerException(Kind kind, String message, Throwable cause, Object... msgArgs) {
        super(message.formatted(msgArgs), cause);
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
        INVALID_CONFIGURATION,
        ACCESS_DENIED
    }
}
