Feature: User login
  The system should authenticate users and create a session when valid credentials are provided.

  Background:
    Given the application is running
    And a user "g@g.com" with password "HinnyeNuh#" exists and is active
    And I am on the login page

  @happy_path
  Scenario: Successful login with valid credentials
    When I submit the login form with username "g@g.com" and password "HinnyeNuh#"
    Then I should be redirected to the index page
    And a session should be created
    And I should see my display name "Sanyi" in the header

  @negative
  Scenario: Existing user but wrong password
    When I submit the login form with username "g@g.com" and password "HnHun&"
    Then I should remain on the login page
    And I should see an error "Invalid username or password."
    And no session should be created

  @negative
  Scenario: Non-existent user
    When I submit the login form with username "c@citromail.hu" and password "0123456"
    Then I should remain on the login page
    And I should see an error "Invalid username or password."
    And no session should be created

  @validation
  Scenario Outline: Client-side/field validation
    When I submit the login form with username "<username>" and password "<password>"
    Then I should remain on the login page
    And I should see a field validation message "<message>"
    And no session should be created

    Examples:
      | username | password   | message                      |
      |          |            | Username is required.        |
      | g@g.com  |            | Password is required.        |
      |          | HinnyeNuh# | Username is required.        |
      | g.g.com  | HinnyeNuh# | Enter a valid email address. |

  @account_state
  Scenario: Account locked after many failed attempts
    Given the account "g@g.com" is locked for 15 minutes due to too many failed logins
    When I submit the login form with username "g@g.com" and password "HinnyeNuh#"
    Then I should remain on the login page
    And I should see an error "Your account is locked. Try again later."
    And no session should be created

  @account_state
  Scenario: Account disabled by admin
    Given the account "g@g.com" is disabled by an administrator
    When I submit the login form with username "g@g.com" and password "HinnyeNuh#"
    Then I should remain on the login page
    And I should see an error "Your account has been disabled. Contact support."
    And no session should be created

  @account_state
  Scenario: Email not verified
    Given the email for "g@g.com" is not verified
    When I submit the login form with username "g@g.com" and password "HinnyeNuh#"
    Then I should remain on the login page
    And I should see an error "Please verify your email to continue."
    And I should see a "Resend verification email" action
    And no session should be created

  @password_expired
  Scenario: Password expired forces reset
    Given the password for "g@g.com" is expired
    When I submit the login form with username "g@g.com" and password "HinnyeNuh#"
    Then I should be redirected to the "Change password" page
    And no session should be created
