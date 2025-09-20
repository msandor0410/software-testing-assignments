package org.example;

import io.cucumber.java.en.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.Duration;
import java.util.*;

public class StepDefinitions {

    enum Page { LOGIN, INDEX, CHANGE_PASSWORD }

    static class User {
        final String email;
        final String password;
        String displayName = "Sanyi";
        boolean active = true;
        boolean disabled = false;
        boolean emailVerified = true;
        boolean passwordExpired = false;
        Instant lockedUntil = null;

        User(String email, String password, boolean active) {
            this.email = email;
            this.password = password;
            this.active = active;
        }
    }

    static class LoginResult {
        Page redirectPage = Page.LOGIN;
        boolean sessionCreated = false;
        String errorMessage = null;
        String validationMessage = null;
        String headerDisplayName = null;
        final List<String> actions = new ArrayList<>();
    }

    static class FakeAuthApp {
        final Map<String, User> users = new HashMap<>();
        Page currentPage = Page.LOGIN;
        LoginResult lastResult;

        void start() {
            users.clear();
            currentPage = Page.LOGIN;
            lastResult = null;
        }

        void addUser(String email, String password, boolean active) {
            User u = new User(email, password, active);
            users.put(email.toLowerCase(Locale.ROOT), u);
        }

        void navigateToLogin() {
            currentPage = Page.LOGIN;
        }

        void lockUser(String email, int minutes) {
            User u = users.get(email.toLowerCase(Locale.ROOT));
            if (u != null) u.lockedUntil = Instant.now().plus(Duration.ofMinutes(minutes));
        }

        void disableUser(String email) {
            User u = users.get(email.toLowerCase(Locale.ROOT));
            if (u != null) u.disabled = true;
        }

        void setEmailVerified(String email, boolean verified) {
            User u = users.get(email.toLowerCase(Locale.ROOT));
            if (u != null) u.emailVerified = verified;
        }

        void setPasswordExpired(String email, boolean expired) {
            User u = users.get(email.toLowerCase(Locale.ROOT));
            if (u != null) u.passwordExpired = expired;
        }

        static boolean looksLikeEmail(String s) {
            return s != null && s.contains("@") && s.indexOf('@') > 0 && s.indexOf('@') < s.length() - 1;
        }

        LoginResult submitLogin(String username, String password) {
            lastResult = new LoginResult();

            if (username == null || username.isBlank()) {
                lastResult.validationMessage = "Username is required.";
                return lastResult;
            }
            if (password == null || password.isBlank()) {
                lastResult.validationMessage = "Password is required.";
                return lastResult;
            }
            if (!looksLikeEmail(username)) {
                lastResult.validationMessage = "Enter a valid email address.";
                return lastResult;
            }

            User u = users.get(username.toLowerCase(Locale.ROOT));
            if (u == null || !Objects.equals(u.password, password)) {
                lastResult.errorMessage = "Invalid username or password.";
                return lastResult;
            }

            if (u.lockedUntil != null && Instant.now().isBefore(u.lockedUntil)) {
                lastResult.errorMessage = "Your account is locked. Try again later.";
                return lastResult;
            }
            if (u.disabled) {
                lastResult.errorMessage = "Your account has been disabled. Contact support.";
                return lastResult;
            }
            if (!u.emailVerified) {
                lastResult.errorMessage = "Please verify your email to continue.";
                lastResult.actions.add("Resend verification email");
                return lastResult;
            }
            if (u.passwordExpired) {
                lastResult.redirectPage = Page.CHANGE_PASSWORD;
                // még nincs session
                return lastResult;
            }

            lastResult.redirectPage = Page.INDEX;
            lastResult.sessionCreated = true;
            lastResult.headerDisplayName = u.displayName;
            return lastResult;
        }
    }

    private FakeAuthApp app;
    private LoginResult result;

    @Given("the application is running")
    public void the_application_is_running() {
        app = new FakeAuthApp();
        app.start();
    }

    @Given("a user {string} with password {string} exists and is active")
    public void a_user_with_password_exists_and_is_active(String email, String password) {
        app.addUser(email, password, true);
    }

    @Given("I am on the login page")
    public void i_am_on_the_login_page() {
        app.navigateToLogin();
    }

    @When("I submit the login form with username {string} and password {string}")
    public void i_submit_the_login_form_with_username_and_password(String username, String password) {
        result = app.submitLogin(username, password);
    }

    @Then("I should be redirected to the index page")
    public void i_should_be_redirected_to_the_index_page() {
        assertThat(result.redirectPage).isEqualTo(Page.INDEX);
    }

    @Then("I should remain on the login page")
    public void i_should_remain_on_the_login_page() {
        assertThat(result.redirectPage).isEqualTo(Page.LOGIN);
    }

    @Then("I should be redirected to the {string} page")
    public void i_should_be_redirected_to_named_page(String page) {
        if ("Change password".equalsIgnoreCase(page)) {
            assertThat(result.redirectPage).isEqualTo(Page.CHANGE_PASSWORD);
        } else if ("index".equalsIgnoreCase(page)) {
            assertThat(result.redirectPage).isEqualTo(Page.INDEX);
        } else if ("login".equalsIgnoreCase(page)) {
            assertThat(result.redirectPage).isEqualTo(Page.LOGIN);
        } else {
            throw new AssertionError("Unknown page: " + page);
        }
    }

    @Then("a session should be created")
    public void a_session_should_be_created() {
        assertThat(result.sessionCreated).isTrue();
    }

    @Then("no session should be created")
    public void no_session_should_be_created() {
        assertThat(result.sessionCreated).isFalse();
    }

    @Then("I should see my display name {string} in the header")
    public void i_should_see_my_display_name_in_the_header(String expected) {
        assertThat(result.headerDisplayName).isEqualTo(expected);
    }

    @Then("I should see an error {string}")
    public void i_should_see_an_error(String expected) {
        assertThat(result.errorMessage).isEqualTo(expected);
    }

    @Then("I should see a field validation message {string}")
    public void i_should_see_a_field_validation_message(String expected) {
        assertThat(result.validationMessage).isEqualTo(expected);
    }

    @Given("the account {string} is locked for {int} minutes due to too many failed logins")
    public void the_account_is_locked_for_minutes_due_to_too_many_failed_logins(String email, Integer minutes) {
        app.lockUser(email, minutes);
    }

    @Given("the account {string} is disabled by an administrator")
    public void the_account_is_disabled_by_an_administrator(String email) {
        app.disableUser(email);
    }

    @Given("the email for {string} is not verified")
    public void the_email_for_is_not_verified(String email) {
        app.setEmailVerified(email, false);
    }

    @Then("I should see a {string} action")
    public void i_should_see_an_action(String action) {
        assertThat(result.actions).contains(action);
    }

    @Given("the password for {string} is expired")
    public void the_password_for_is_expired(String email) {
        app.setPasswordExpired(email, true);
    }

}
