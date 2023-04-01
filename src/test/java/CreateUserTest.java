import io.qameta.allure.Description;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import io.restassured.RestAssured;
import com.github.javafaker.Faker;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;


public class CreateUserTest {
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


    @Step("POST запрос на ручку /api/auth/register создание пользователя")
    public Response createUser() {
        return given()
                .header("Content-type", "application/json")
                .body(user)
                .post("/api/auth/register");
    }

    @Step("Проверяем код статуса в верном ответе после регистрации")
    public void checkStatusCodeInSuccessfulResponse(Response response) {
        response.then().statusCode(200);
    }

    @Step("Проверяем тело верного ответа после регистрации")
    public void checkResponseBodyInSuccessfulResponse(Response response) {
        response
                .then()
                .assertThat()
                .body("success", equalTo(true))
                .body("user.email", equalTo(email))
                .body("user.name", equalTo(name))
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue());
    }

    @Step("Поверяем код статуса при создании дублирующего пользователя")
    public void checkStatusCodeInSuccessfulSameResponse(Response response) {
        response.then().statusCode(403);
    }

    @Step("Проверяем тело ответа, при авторизации с повторными данными")
    public void checkResponseBodyThatSameLogin(Response response) {
        response.then().assertThat()
                .body("success", equalTo(false))
                .body("message", equalTo("User already exists"));
    }

    @Step("POST запрос на ручку /api/auth/register без заполнения поля password")
    public Response requestWithoutPasswordField() {
        User json = new User(email, "", name);
        return given()
                .header("Content-type", "application/json")
                .body(json)
                .post("/api/auth/register");
    }

    @Step("POST запрос на ручку /api/auth/register без заполнения поля email")
    public Response requestWithoutEmailField() {
        User json = new User("", password, name);
        return given()
                .header("Content-type", "application/json")
                .body(json)
                .post("/api/auth/register");
    }

    @Step("POST запрос на ручку /api/auth/register без заполнения поля name")
    public Response requestWithoutNameField() {
        User json = new User(email, password, "");
        return given()
                .header("Content-type", "application/json")
                .body(json)
                .post("/api/auth/register");
    }

    @Step("Проверяем код статуса при запросе с любым пустым полем")
    public void checkStatusCodeThatRequestWithoutAnyField(Response response) {
        response.then().statusCode(403);
    }

    @Step("Проверяем тело ответа при запросе с любым пустым полем")
    public void checkResponseBodyThanRequestWithoutAnyField(Response response) {
        response.then().assertThat().body("success", equalTo(false))
                .and()
                .body("message", equalTo("Email, password and name " +
                        "are required fields"));
    }

    @Test
    @DisplayName("Проверяем можно-ли создать уникального пользователя")
    @Description("Если код статуса в ответе будет 200 и тело будет соответствовать ОР, " +
            "значит пользователь создан успешно")
    public void createUserThenReturnCorrectResponse() {
        Response response = createUser();
        checkStatusCodeInSuccessfulResponse(response);
        checkResponseBodyInSuccessfulResponse(response);
    }

    @Test
    @DisplayName("Проверяем можно-ли создать пользователя, который уже зареган")
    @Description("Если в ответе вернётся ошибка с кодом статуса 403, значит ОР = ФР")
    public void createExistedUserThenReturnCorrectStatusCode() {
        createUser();
        Response response = createUser();
        checkStatusCodeInSuccessfulSameResponse(response);
        checkResponseBodyThatSameLogin(response);
    }

    @Test
    @DisplayName("Проверяем можно-ли создать пользователя и " +
            "не заполнить одно из обязательных полей")
    @Description("Если в ответе вернётся ошибка, значит ОР = ФР")
    public void createUserWithoutPasswordThenReturnCorrectResponse() {
        Response response = requestWithoutPasswordField();
        checkStatusCodeThatRequestWithoutAnyField(response);
        checkResponseBodyThanRequestWithoutAnyField(response);
    }

    @Test
    @DisplayName("Проверяем можно-ли создать пользователя и " +
            "не заполнить одно из обязательных полей")
    @Description("Если в ответе вернётся ошибка, значит ОР = ФР")
    public void createUserWithoutEmailFieldThenReturnCorrectResponse() {
        Response response = requestWithoutEmailField();
        checkStatusCodeThatRequestWithoutAnyField(response);
        checkResponseBodyThanRequestWithoutAnyField(response);
    }

    @Test
    @DisplayName("Проверяем можно-ли создать пользователя и " +
            "не заполнить одно из обязательных полей")
    @Description("Если в ответе вернётся ошибка, значит ОР = ФР")
    public void createUserWithoutNameFieldThenReturnCorrectResponse() {
        Response response = requestWithoutNameField();
        checkStatusCodeThatRequestWithoutAnyField(response);
        checkResponseBodyThanRequestWithoutAnyField(response);
    }

}




