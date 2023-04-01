import io.qameta.allure.Description;
import io.qameta.allure.Step;
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

    @Step("Создаём пользователя")
    public void createUser(User user) {
        given()
                .header("Content-type", "application/json")
                .body(user)
                .post("/api/auth/register");
    }

    @Step("Авторизуемся")
    public void loginUser(User user) {
        String accessToken = given()
                .header("Content-type", "application/json")
                .body(user)
                .post("/api/auth/login")
                .then()
                .extract()
                .body()
                .path("accessToken").toString();
        authToken = accessToken.substring(7);
    }

    @Step("Создаем заказ")
    public void createOrder() {
        given()
                .auth().oauth2(authToken)
                .header("Content-type", "application/json")
                .body(json)
                .post("/api/orders");
    }

    @Step("GET запрос на ручку /api/orders, проверяем статус ответа авторизованного пользователя")
    public void checkStatusCodeWhenGetUserOrdersWithLogin() {
        given()
                .auth().oauth2(authToken)
                .get("/api/orders")
                .then().statusCode(200);
    }

    @Step("GET запрос на ручку /api/orders, проверяем тело ответа авторизванного пользователя")
    public void checkResponseBodyWhenGetUserOrdersWithLogin() {
        given().auth().oauth2(authToken)
                .get("/api/orders").then().assertThat()
                .body("success", equalTo(true));
    }

    @Step("GET запрос на ручку /api/orders без авторизации, проверяем статус ответа")
    public void checkStatusCodeWhenGetUserOrdersWithoutLogin() {
        given()
                .header("Content-type", "application/json")
                .body(json)
                .get("/api/orders").then().statusCode(401);
    }

    @Step("GET запрос на ручку /api/orders без авторизации, проверяем тело ответа")
    public void checkBodyWhenGetUserOrdersWithoutLogin() {
        given()
                .header("Content-type", "application/json")
                .body(json)
                .get("/api/orders")
                .then().assertThat()
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
    }

    @Test
    @DisplayName("Получить заказ авторизованного пользователя")
    @Description("Регистрируем, авторизуемся, создаем заказ, получаем заказ с проверкой статуса и тела ответа")
    public void getUserOrdersWithLogin() {
        createUser(user);
        loginUser(user);
        createOrder();
        checkResponseBodyWhenGetUserOrdersWithLogin();
        checkStatusCodeWhenGetUserOrdersWithLogin();
    }

    @Test
    @DisplayName("Получить заказ не авторизованного пользователя")
    @Description("Просим выдать заказ без авторизации пользователя, с проверкой статуса и тела ответа")
    public void getUserOrdersWithoutLogin() {
        checkStatusCodeWhenGetUserOrdersWithoutLogin();
        checkBodyWhenGetUserOrdersWithoutLogin();
    }
}
