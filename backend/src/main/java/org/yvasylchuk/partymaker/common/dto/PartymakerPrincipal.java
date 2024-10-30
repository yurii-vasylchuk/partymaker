package org.yvasylchuk.partymaker.common.dto;

import java.util.List;

public record PartymakerPrincipal(String id, List<String> roles, String username) {
}
