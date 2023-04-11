package com.bookit.step_definitions;

import com.bookit.pages.SelfPage;
import com.bookit.utilities.BookitUtils;
import com.bookit.utilities.ConfigurationReader;
import com.bookit.utilities.DB_Util;
import com.bookit.utilities.Environment;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.Assert;

import java.sql.ResultSet;
import java.util.Map;

import static io.restassured.RestAssured.*;

public class ApiStepDefs {

    String token;
    Response response;


    String email;

    @Given("I logged Bookit api as a {string}")
    public void i_logged_bookit_api_as_a(String role) {
        token = BookitUtils.generateTokenByRole(role);
        System.out.println("token = " + token);

        Map<String, String> credentialMap = BookitUtils.returnCredentials(role);

        email = credentialMap.get("email");

    }

    @When("I sent get request to {string} endpoint")
    public void i_sent_get_request_to_endpoint(String endpoint) {

        response = given()
                .accept(ContentType.JSON)
                .header("Authorization", token)
                .when().get(Environment.BASE_URL + endpoint);

    }

    @Then("status code should be {int}")
    public void status_code_should_be(int expectedStatusCode) {
        int actualStatusCode = response.statusCode();
        Assert.assertEquals(expectedStatusCode, actualStatusCode);
    }

    @Then("content type is {string}")
    public void content_type_is(String expectedContentType) {
        String actualContentType = response.contentType();
        Assert.assertEquals(expectedContentType, actualContentType);
    }

    @Then("role is {string}")
    public void role_is(String expectedRole) {
        String actualRole = response.path("role");
        Assert.assertEquals(expectedRole, actualRole);
        System.out.println("actualRole = " + actualRole);
    }

    @Then("the information about current user from api and database should match")
    public void the_information_about_current_user_from_api_and_database_should_match() {
        //we need to get data from Api

        JsonPath jsonPath = response.jsonPath();
        String actualRole = jsonPath.getString("role");
        String actualFirstName = jsonPath.getString("firstName");
        String actualLastName = jsonPath.getString("lastName");
        int actualId = jsonPath.getInt("id");

        // get data from DB
        //1# connection handles in hook
        DB_Util.runQuery("select firstname, lastname, role from users\n" +
                "where email = '" + email + "'");

        Map<String, String> dbMap = DB_Util.getRowMap(1);
        System.out.println("dbMap = " + dbMap);

        String expectedFirstName = dbMap.get("firstname");
        String expectedLastName = dbMap.get("lastname");
        String expectedRole = dbMap.get("role");

        //compare api with db

        Assert.assertEquals(expectedFirstName, actualFirstName);
        Assert.assertEquals(expectedLastName, actualLastName);
        Assert.assertEquals(expectedRole, actualRole);

    }

    @Then("UI,API and Database user information must be match")
    public void ui_api_and_database_user_information_must_be_match() {

        //we need to get data from Api
        JsonPath jsonPath = response.jsonPath();
        String actualRole = jsonPath.getString("role");
        String actualFirstName = jsonPath.getString("firstName");
        String actualLastName = jsonPath.getString("lastName");

        // get data from DB
        //1# connection handles in hook
        DB_Util.runQuery("select firstname, lastname, role from users\n" +
                "where email = '" + email + "'");
        Map<String, String> dbMap = DB_Util.getRowMap(1);
        System.out.println("dbMap = " + dbMap);
        String expectedFirstName = dbMap.get("firstname");
        String expectedLastName = dbMap.get("lastname");
        String expectedRole = dbMap.get("role");

        Assert.assertEquals(expectedFirstName, actualFirstName);
        Assert.assertEquals(expectedLastName, actualLastName);
        Assert.assertEquals(expectedRole, actualRole);

        //get data from UI
        SelfPage selfPage = new SelfPage();
        String actualFullNameUI = selfPage.name.getText();
        System.out.println("actualFullNameUI = " + actualFullNameUI);
        String actualRoleUI = selfPage.role.getText();
        //compare UI vs db
        String expectedFullNameAPI = expectedFirstName + " " + expectedLastName;
        Assert.assertEquals(expectedFullNameAPI, actualFullNameUI);//db expected ui actual
        // comparing ui and api ui is actual and api is expected
        Assert.assertEquals(expectedRole, actualRoleUI);//db expected ui actual

        //compare UI vs api

        String expectedFullName = actualFirstName + " " + actualLastName;
        Assert.assertEquals(expectedFullName, actualFullNameUI);// api expected ui actual
        Assert.assertEquals(actualRole, actualRoleUI);// api expected ui actual
    }

    //adding new student and then delete it

    @When("I send POST request {string} endpoint with following information")
    public void i_send_post_request_endpoint_with_following_information(String endpoint, Map<String, String> studentInfo) {


        response = given().accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .header("Authorization", token)
                .queryParams(studentInfo)
                .when().post(Environment.BASE_URL + endpoint).prettyPeek();


    }

    @Then("I delete previously added student")
    public void i_delete_previously_added_student() {

        // we need to get the entryiId from the post request and send
        int idToDelete = response.path("entryiId");
        System.out.println("idToDelete = " + idToDelete);

        given().log().uri()
                .header("Authorization", token)
                .pathParam("id", idToDelete)
                .when().delete(Environment.BASE_URL + "/api/students/{id}")
                .then()
                .statusCode(204);
    }

}
