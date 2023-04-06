import io.qameta.allure.Description;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.RestAssured;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class GetOrdersFromSpecificUserTest {
    User user = new User("krab@krab.ru", "password", "Sharik");
    String json = "{\n\"ingredients\": [\"61c0c5a71d1f82001bdaaa6d\"]\n}";
    String authToken;

    @Before
    public void setUp() {
        RestAssured.baseURI = Constants.BASE_URL;
    }

    @After
    public void cleanUser() {
        UserResponse response = given()
                .header("Content-type", "application/json")
                .body(user)
                .post("/api/auth/login")
                .getBody()
                .as(UserResponse.class);
        if (response != null && response.success) {
            given().auth().oauth2(response.accessToken.substring(7)).delete("/api/auth/user");
        }
    }

    @Test
    @DisplayName("Получить заказ авторизованного пользователя")
    @Description("Регистрируем, авторизуемся, создаем заказ, получаем заказ с проверкой статуса и тела ответа")
    public void getUserOrdersWithLogin() {
        given()
                .header("Content-type", "application/json")
                .body(user)
                .post("/api/auth/register");
        String accessToken = given()
                .header("Content-type", "application/json")
                .body(user)
                .post("/api/auth/login")
                .then()
                .extract()
                .body()
                .path("accessToken").toString();
        authToken = accessToken.substring(7);
        given()
                .auth().oauth2(authToken)
                .header("Content-type", "application/json")
                .body(json)
                .post("/api/orders");
        given().auth().oauth2(authToken)
                .get("/api/orders").then().assertThat()
                .body("success", equalTo(true));
        given()
                .auth().oauth2(authToken)
                .get("/api/orders")
                .then().statusCode(200);
    }

    @Test
    @DisplayName("Получить заказ не авторизованного пользователя")
    @Description("Просим выдать заказ без авторизации пользователя, с проверкой статуса и тела ответа")
    public void getUserOrdersWithoutLogin() {
        given()
                .header("Content-type", "application/json")
                .body(json)
                .get("/api/orders").then().statusCode(401);
        given()
                .header("Content-type", "application/json")
                .body(json)
                .get("/api/orders")
                .then().assertThat()
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
    }
}
