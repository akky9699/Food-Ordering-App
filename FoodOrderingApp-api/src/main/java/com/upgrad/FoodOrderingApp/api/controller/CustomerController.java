package com.upgrad.FoodOrderingApp.api.controller;

import com.upgrad.FoodOrderingApp.api.model.*;
import com.upgrad.FoodOrderingApp.service.businness.CustomerService;
import com.upgrad.FoodOrderingApp.service.entity.CustomerAuthEntity;
import com.upgrad.FoodOrderingApp.service.entity.CustomerEntity;
import com.upgrad.FoodOrderingApp.service.exception.AuthenticationFailedException;
import com.upgrad.FoodOrderingApp.service.exception.AuthorizationFailedException;
import com.upgrad.FoodOrderingApp.service.exception.SignUpRestrictedException;
import com.upgrad.FoodOrderingApp.service.exception.UpdateCustomerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Controller
public class CustomerController {

    @Autowired
    CustomerService customerService;

    @CrossOrigin
    @PostMapping(path = "/customer/signup", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<?> signUp(
            @RequestBody(required = false) final SignupCustomerRequest signupCustomerRequest)
            throws SignUpRestrictedException {

        // Create customer entity
        final CustomerEntity customerEntity = new CustomerEntity();

        // Set customer details by getting values from signUpCustomerRequest
        customerEntity.setUuid(UUID.randomUUID().toString());
        customerEntity.setFirstName(signupCustomerRequest.getFirstName());
        customerEntity.setLastName(signupCustomerRequest.getLastName());
        customerEntity.setEmail(signupCustomerRequest.getEmailAddress());
        customerEntity.setContact_number(signupCustomerRequest.getContactNumber());
        customerEntity.setPassword(signupCustomerRequest.getPassword());

        // Validate password format and length using regex
        if (!customerEntity.getPassword()
                .matches("^.*(?=.{8,})(?=..*[0-9])(?=.*[A-Z])(?=.*[#@$%&*!^]).*$")) {
            throw new SignUpRestrictedException("SGR-004", "Weak password!");
        }

        // Variables to perform validation
        String customerExists = String.valueOf(customerService.getCustomerByContactNumber(signupCustomerRequest.getContactNumber()));
        String contactNumberExists = String.valueOf(signupCustomerRequest.getContactNumber());
        String firstNameExists = String.valueOf(signupCustomerRequest.getFirstName());
        String emailExists = String.valueOf(signupCustomerRequest.getEmailAddress());
        String passwordExists = String.valueOf(signupCustomerRequest.getPassword());

        // If any of the fields except lastName are null or empty, throw exception
        if (contactNumberExists.equals("null") || contactNumberExists.isEmpty()
                || firstNameExists.equals("null") || firstNameExists.isEmpty()
                || emailExists.equals("null") || emailExists.isEmpty()
                || passwordExists.equals("null") || passwordExists.isEmpty()) {
            throw new SignUpRestrictedException("SGR-005", "Except last name all fields should be filled");
        }

        if (!customerExists.equals("null")) {
            throw new SignUpRestrictedException("SGR-001", "This contact number is already registered! Try other contact number.");
        } else {
            CustomerEntity createdCustomerEntity = customerService.saveCustomer(customerEntity);
            SignupCustomerResponse signupCustomerResponse = new SignupCustomerResponse()
                    .id(createdCustomerEntity.getUuid()).status("CUSTOMER SUCCESSFULLY REGISTERED");
            return new ResponseEntity<SignupCustomerResponse>(signupCustomerResponse, HttpStatus.CREATED);
        }
    }

    @CrossOrigin
    @PostMapping(path = "/customer/login", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<LoginResponse> login(@RequestHeader("authorization") final String authorization)
            throws AuthenticationFailedException {

        // Initial validation for basic authentication

        // Split authorization header and validate base64 encoding using regex
        String splitAuthHeader = authorization.split("Basic ")[1];
        if (!splitAuthHeader
                .matches("^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$")) {
            throw new AuthenticationFailedException("ATH-003",
                    "Incorrect format of decoded customer name and password");
        }

        // Validate basic authentication formatting in the authorization header
        if (!authorization.startsWith("Basic")) {
            throw new AuthenticationFailedException("ATH-003",
                    "Incorrect format of decoded customer name and password");
        }

        // Decode authorization header after initial validation
        byte[] decodeAuth = Base64.getDecoder().decode(authorization.split("Basic ")[1]);
        String decodedAuth = new String(decodeAuth);
        String[] decodedAuthArray = decodedAuth.split(":");

        // Create instance of CustomerAuthEntity
        CustomerAuthEntity customerAuthToken = new CustomerAuthEntity();

        // Final basic authentication validation check
        if (decodedAuthArray.length > 0) {
            customerAuthToken = customerService
                    .authenticate(decodedAuthArray[0], decodedAuthArray[1]);
        } else {
            throw new AuthenticationFailedException("ATH-003",
                    "Incorrect format of decoded customer name and password");
        }

        // Get the associated CustomerEntity
        CustomerEntity customerEntity = customerAuthToken.getCustomer();

        // Build LoginResponse
        LoginResponse loginResponse = new LoginResponse()
                .id(customerEntity.getUuid())
                .firstName(customerEntity.getFirstName())
                .lastName(customerEntity.getLastName())
                .contactNumber(customerEntity.getContact_number())
                .emailAddress(customerEntity.getEmail())
                .message("LOGGED IN SUCCESSFULLY");

        // Add access token to the header
        List<String> header = new ArrayList<>();
        header.add("access-token");
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("access-token", customerAuthToken.getAccessToken());
        httpHeaders.setAccessControlExposeHeaders(header);

        // Return loginResponse, header, and the corresponding HTTP status
        return new ResponseEntity<LoginResponse>(loginResponse, httpHeaders, HttpStatus.OK);
    }

    @CrossOrigin
    @PostMapping(path = "/customer/logout", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<LogoutResponse> logOut(@RequestHeader("authorization") final String authorization)
            throws AuthorizationFailedException {
        // Split authorization header and get the access token
        String accessToken = authorization.split("Bearer ")[1];
        // Get the associated CustomerAuthEntity
        CustomerAuthEntity customerAuthEntity = customerService.getCustomerAuth(accessToken);

        if (customerAuthEntity != null) {
            if (customerAuthEntity.getLogoutAt() != null) {
                throw new AuthorizationFailedException("ATH-002",
                        "Customer is logged out. Log in again to access this endpoint");
            }

            // Get expiry time of the associated access token
            ZonedDateTime expiryTime = customerAuthEntity.getExpiresAt();
            // Get current time
            final ZonedDateTime currentTime = ZonedDateTime.now();

            // If the expiry time is not null and is before the current time, throw an error
            // Else, proceed
            if (expiryTime != null) {
                if (expiryTime.isBefore(currentTime)) {
                    throw new AuthorizationFailedException("ATHR-003",
                            "Your session is expired. Log in again to access this endpoint");
                }
            }
        }

        // Create the final CustomerAuthEntity instance, and logout the customer
        CustomerAuthEntity finalCustomerAuthEntity = customerService.logout(accessToken);
        // Create logout response
        final LogoutResponse logoutResponse = new LogoutResponse()
                .id(finalCustomerAuthEntity.getCustomer().getUuid())
                .message("LOGGED OUT SUCCESSFULLY");
        // Return response entity with logout response and HTTP status
        return new ResponseEntity<LogoutResponse>(logoutResponse, HttpStatus.OK);
    }
   
}

