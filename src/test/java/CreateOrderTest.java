import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.RestAssured;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class CreateOrderTest {
    @Before
    public void setUp() {
        RestAssured.baseURI = Constants.BASE_URL;
    }

    User user = new User("krab@krab.ru", "password", "Sharik");
    String json = "{\n\"ingredients\": [\"61c0c5a71d1f82001bdaaa6d\"]\n}";
    String json1 = "{\n\"ingredients\": \"\"\n}";
    String json2 = "{\n\"ingredients\": [\"61c0c5alkd1f82001bdaaa6d\"]\n}";


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

    @Step("POST запрос на ручку /api/auth/register регистрация пользователя")
    public void createUser() {
        given()
                .header("Content-type", "application/json")
                .body(user)
                .post("/api/auth/register");
    }

    @Step("Проверка корректного статуса кода при POST запросе на ручку /api/orders")
    private void returnCorrectStatusCode(String json, int expectedStatusCode) {
        given()
                .header("Content-type", "application/json")
                .body(json)
                .post("/api/orders")
                .then().statusCode(expectedStatusCode);
    }

    @Test
    @DisplayName("Создание заказа c авторизацией и ингредиентами")
    @Description("Авторизуемся, делаем запрос создать заказ и проверяем тело и статус ответа")
    public void createOrder() {
        createUser();
        given()
                .header("Content-type", "application/json")
                .body(json)
                .post("/api/orders")
                .then()
                .body("success", equalTo(true))
                .body("name", equalTo("Флюоресцентный бургер"));
        returnCorrectStatusCode(json, 200);
    }

    @Test
    @DisplayName("Создание заказа без авторизации с ингредиентами")
    @Description("Проверяем статус и тело ответа без авторизации, но с ингредиентами")
    public void createOrderWithoutLoginWithIngredients() {
        returnCorrectStatusCode(json, 200);
        given()
                .header("Content-type", "application/json")
                .body(json)
                .post("/api/orders")
                .then()
                .assertThat()
                .body("name", equalTo("Флюоресцентный бургер"))
                .body("order.number", notNullValue())
                .body("success", equalTo(true));
    }

    @Test
    @DisplayName("Создание заказа с авторизацией и без ингредиентов")
    @Description("Авторизуемся, проверяем статус и тело ответа с авторизацией и без ингредиентов")
    public void createOrderWithLoginWithoutIngredients() {
        createUser();
        returnCorrectStatusCode(json1, 400);
        given()
                .header("Content-type", "application/json")
                .body(json1)
                .then().body("succes", equalTo(false))
                .body("message", equalTo("Ingredient ids must be provided"));
    }

    @Test
    @DisplayName("Создание заказа без ингредиентов и без авторизациии")
    @Description("Проверяем статус ответа")
    public void createOrderWithoutLoginWithoutIngredients() {
        returnCorrectStatusCode(json2, 500);
    }
}


