# Assignment 3 - JWT Authentication

This EMS project uses JWT authentication for login sessions and protects application APIs after login.

The permanent account is stored in MySQL. The password is stored as a BCrypt hash, not as plain text. The JWT is a temporary signed token and is not the password.

Protected application endpoints require a valid bearer token.
