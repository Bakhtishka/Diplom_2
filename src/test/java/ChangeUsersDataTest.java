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
    Faker johnFaker = new Faker();
    Faker jacobFaker = new Faker();

    User user = new User(johnFaker.internet().emailAddress(), johnFaker.internet().password(), johnFaker.name().firstName());
    User jacob = new User(jacobFaker.internet().emailAddress(), jacobFaker.internet().password(), jacobFaker.name().firstName());
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

    @Test
    @DisplayName("Изменение данных пользователя с авторизацией")
    @Description("При смене данных авторизованного пользователя, вернётся статус 200")
    public void changeUserDataWithAuthorization() {
        registrationUser(user);
        loginUser(user);
        given().auth()
                .oauth2(authToken)
                .body(jacob)
                .contentType("application/json")
                .patch("/api/auth/user")
                .then()
                .statusCode(200)
                .assertThat()
                .body("user.email", equalTo(jacob.getEmail()));
        user.setEmail(jacob.getEmail());
        user.setName(jacob.getName());
    }

    @Test
    @DisplayName("Изменение данных пользователя без авторизации")
    @Description("Неавторизованный пользователь не сможет менять данные, вернется ошибка")
    public void shouldReturnErrorWhenChangeUserDataWithoutAuthorization() {
        registrationUser(user);
        given()
                .header("Content-type", "application/json")
                .body(jacob)
                .patch("/api/auth/user")
                .then().statusCode(401);
        given()
                .header("Content-type", "application/json")
                .body(jacob)
                .patch("/api/auth/user")
                .then().assertThat().body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));

    }

    @Test
    @DisplayName("Изменеие данных пользователя с авторизацией")
    @Description("PATCH запрос авторизованного пользователя с почтой, которая уже используется, " +
            "при запросе вернётся ошибка.")
    public void shouldReturnErrorWhenChangeUserNameWithAuthorization() {
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
}
