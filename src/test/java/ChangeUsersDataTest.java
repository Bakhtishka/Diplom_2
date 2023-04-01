import com.github.javafaker.Faker;
import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.RestAssured;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class ChangeUsersDataTest {
    Faker faker = new Faker();
    String email = faker.internet().emailAddress();
    String password = faker.internet().password();
    String name = faker.name().firstName();
    User user = new User(email, password, name);
    User user1 = new User(email, name);
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

    @Step("Регистрация нового пользователя")
    public void registrationUser(User user) {
        given()
                .header("Content-type", "application/json")
                .body(user)
                .post("/api/auth/register")
                .then().statusCode(200);
    }

    @Step("Авторизация пользователя")
    public void loginUser(User user) {
        String accessToken = given()
                .header("Content-type", "application/json")
                .body(user)
                .post("/api/auth/login")
                .then().statusCode(200)
                .extract()
                .body()
                .path("accessToken").toString();
        authToken = accessToken.substring(7);
    }

    @Step("PATCH запрос на ручку /api/auth/user с авторизацией и измененными полями")
    public void checkStatusCodeWhenChangeUserDataWithAuthorization() {
        loginUser(user);
        given().auth().oauth2(authToken)
                .body(user1)
                .patch("/api/auth/user").then().statusCode(200)
                .assertThat().body("user.email", equalTo(user1.getEmail()));
    }


    @Step("PATCH запрос на ручку /api/auth/user без авторизации с измененными полями")
    public void checkStatusCodeWhenChangeUserDataWithoutAuthorization() {
        registrationUser(user);
        given()
                .header("Content-type", "application/json")
                .body(user1)
                .patch("/api/auth/user")
                .then().statusCode(401);
    }

    @Step("PATCH запрос на ручку /api/auth/user без авторизации с измененными полями")
    public void checkBodyWhenChangeUserDataWithoutAuthorization() {
        given()
                .header("Content-type", "application/json")
                .body(user1)
                .patch("/api/auth/user")
                .then().assertThat().body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
    }

    @Step("PATCH запрос на ручку /api/auth/user с авторизацией и почтой, которая уже есть")
    public void checkStatusCodeWhenRequestWithExistUserEmail() {
        registrationUser(user);
        final User sobaken = new User("sobaka@met.ru", "kolbaska", "Spike");
        registrationUser(sobaken);
        loginUser(sobaken);
        given().auth().oauth2(authToken)
                .contentType("application/json")
                .body(user)
                .patch("/api/auth/user")
                .then().statusCode(403);
        given().auth().oauth2(authToken).delete("/api/auth/user");
    }

    @Step("PATCH запрос на ручку /api/auth/user с авторизацией и почтой, которая уже есть")
    public void checkBodyWhenRequestWithExistUserEmail() {
        final User koshken = new User("koshka@myshka.ru", "Holly", "Thom");
        registrationUser(koshken);
        loginUser(koshken);
        given().auth().oauth2(authToken)
                .contentType("application/json")
                .body(user)
                .patch("/api/auth/user")
                .then().assertThat()
                .body("success", equalTo(false))
                .body("message", equalTo("User with such email already exists"));
        given().auth().oauth2(authToken).delete("/api/auth/user");
    }

    @Test
    @DisplayName("Изменение данных пользователя с авторизацией")
    @Description("При смене данных авторизованного пользователя, вернётся статус 200")
    public void changeUserDataWithAuthorization() {
        registrationUser(user);
        loginUser(user);
        checkStatusCodeWhenChangeUserDataWithAuthorization();
    }

    @Test
    @DisplayName("Изменение данных пользователя без авторизации")
    @Description("Неавторизованный пользователь не сможет менять данные, вернется ошибка")
    public void shouldReturnErrorWhenChangeUserDataWithoutAuthorization() {
        checkStatusCodeWhenChangeUserDataWithoutAuthorization();
        checkBodyWhenChangeUserDataWithoutAuthorization();

    }

    @Test
    @DisplayName("Изменеие данных пользователя с авторизацией")
    @Description("PATCH запрос авторизованного пользователя с почтой, которая уже используется")
    public void shouldReturnErrorWhenChangeUserNameWithAuthorization() {
        checkStatusCodeWhenRequestWithExistUserEmail();
        checkBodyWhenRequestWithExistUserEmail();
    }
}
