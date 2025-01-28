package org.yvasylchuk.partymaker.party;

import org.springframework.stereotype.Component;
import org.yvasylchuk.partymaker.party.core.TokenGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class TokenGeneratorImpl implements TokenGenerator {
    private static final int TOKEN_LENGTH = 6;
    private static final Character[] URI_SAFE_SYMBOLS;
    static {
        List<Character> characters = new ArrayList<>();
        for (char i = 'a'; i <= 'z'; i++) {
            characters.add(i);
        }
        for (char i = 'A'; i <= 'Z'; i++) {
            characters.add(i);
        }
        for (char i = '0'; i <= '9'; i++) {
            characters.add(i);
        }

        URI_SAFE_SYMBOLS = new Character[characters.size()];
        for (int i = 0; i < characters.size(); i++) {
            URI_SAFE_SYMBOLS[i] = characters.get(i);
        }
    }

    private static final Random RND = new Random();

    @Override public String generateToken() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < TOKEN_LENGTH; i++) {
            sb.append(URI_SAFE_SYMBOLS[RND.nextInt(URI_SAFE_SYMBOLS.length)]);
        }

        return sb.toString();
    }
}
