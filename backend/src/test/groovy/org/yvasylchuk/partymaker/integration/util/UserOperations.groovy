package org.yvasylchuk.partymaker.integration.util

import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.springframework.stereotype.Component

@Component
class UserOperations {

    String authenticate(String email, String password) {
        return RestAssured
                 .given()
                 .body("""
                      {
                        "email": "${email}",
                        "password": "${password}"
                      }
                      """)
                 .contentType(ContentType.JSON)
                 .post("/api/user/access-token")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(Map)['accessToken'] as String
    }
}
