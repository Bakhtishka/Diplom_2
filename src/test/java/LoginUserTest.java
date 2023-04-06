import com.github.javafaker.Faker;
import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;


public class LoginUserTest {
    Faker faker = new Faker();
    String email = faker.internet().emailAddress();
    String password = faker.internet().password();
    String name = faker.name().firstName();
    User user = new User(email, password, name);

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


    @Step("POST запрос на ручку /api/auth/login авторизация пользователя")
    public Response loginUserWithIncorrectPasswordAndEmail() {
        User json = new User(email, password);
        return given()
                .header("Content-type", "application/json")
                .body(json)
                .post("/api/auth/login");
    }

    @Test
    @DisplayName("Логин под существующим пользователем")
    @Description("При успешной авторизации статус и тело ответа будут соответствовать ТЗ")
    public void test() {
        given()
                .header("Content-type", "application/json")
                .body(user)
                .post("/api/auth/register");
        given()
                .header("Content-type", "application/json")
                .body(user)
                .post("/api/auth/login")
                .then()
                .assertThat()
                .body("success", equalTo(true))
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue())
                .body("user.email", equalTo(email))
                .body("user.name", equalTo(name));

    }

    @Test
    @DisplayName("Авторизация не существующим пользователем")
    @Description("Должна вернуться ошибка")
    public void shouldWhenLoginIncorrectUserReturnError() {
        Response response = loginUserWithIncorrectPasswordAndEmail();
        response.then().statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("email or password are incorrect"));
    }
}
