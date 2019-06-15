package com.upgrad.FoodOrderingApp.service.businness;

import com.fasterxml.jackson.databind.annotation.JsonAppend;
import com.upgrad.FoodOrderingApp.service.dao.CustomerDao;
import com.upgrad.FoodOrderingApp.service.entity.CustomerAuthEntity;
import com.upgrad.FoodOrderingApp.service.entity.CustomerEntity;
import com.upgrad.FoodOrderingApp.service.exception.AuthenticationFailedException;
import com.upgrad.FoodOrderingApp.service.exception.AuthorizationFailedException;
import com.upgrad.FoodOrderingApp.service.exception.SignUpRestrictedException;
import com.upgrad.FoodOrderingApp.service.exception.UpdateCustomerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.UUID;

@Service
public class CustomerService {

    @Autowired
    CustomerDao customerDao;

    @Autowired
    private PasswordCryptographyProvider passwordCryptographyProvider;


    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = {SignUpRestrictedException.class})
    public CustomerEntity saveCustomer(final CustomerEntity customerEntity) throws SignUpRestrictedException {

        String password = customerEntity.getPassword();
        String[] encryptPassword = passwordCryptographyProvider.encrypt(password);
        String salt = encryptPassword[0];
        customerEntity.setSalt(salt);
        customerEntity.setPassword(encryptPassword[1]);

        // Validate contact number format and length using regex
        if (!customerEntity.getContact_number().matches("^.*(?=.{10,})^[0-9]*$")) {
            throw new SignUpRestrictedException("SGR-003", "Invalid contact number!");
        }
        // Validate email format using regex
        if (!customerEntity.getEmail()
                .matches("^([_a-zA-Z0-9-]+(\\.[_a-zA-Z0-9-]+)*@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*(\\.[a-zA-Z]{1,6}))?$")) {
            throw new SignUpRestrictedException("SGR-002", "Invalid email-id format!");
        }


        return customerDao.createCustomer(customerEntity);
    }

    public CustomerEntity getCustomerByContactNumber(String contactNumber) {
        return customerDao.findCustomerByContactNumber(contactNumber);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public boolean checkCustomer(String access_token) throws AuthorizationFailedException{
        CustomerAuthEntity customerAuthEntity = customerDao.findCustomerAuthEntityByAccessToken(access_token);
        if (customerAuthEntity == null){
            throw new AuthorizationFailedException("ATHR-001", "Customer is not Logged in.");
        } else if (!String.valueOf(customerDao
                .findCustomerAuthEntityByAccessToken(access_token).getLogoutAt())
                .equals("null")){
            throw new AuthorizationFailedException("ATHR-002", "Customer is logged out. Log in again to access this endpoint");
        } else if(customerAuthEntity.getExpiresAt() != null){
            ZonedDateTime expiryTime = customerAuthEntity.getExpiresAt();
            final ZonedDateTime currentTime = ZonedDateTime.now();
            if(expiryTime != null && expiryTime.isBefore(currentTime)) {
                throw new AuthorizationFailedException("ATHR-003",
                        "Your session is expired. Log in again to access this endpoint");
            }
        }
        return true;
    }

    public CustomerEntity getCustomer(String access_token) throws AuthorizationFailedException{
        CustomerEntity customerEntity = null;
        boolean validify = checkCustomer(access_token);
        if(validify){
            customerEntity = customerDao.findCustomerAuthEntityByAccessToken(access_token).getCustomer();
        }
        return customerEntity;
    }

    public CustomerAuthEntity getCustomerAuth(String access_token) throws AuthorizationFailedException{
        CustomerAuthEntity customerAuthEntity = null;
        if (checkCustomer(access_token)){
            customerAuthEntity = customerDao.findCustomerAuthEntityByAccessToken(access_token);
        }
        return customerAuthEntity;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public CustomerAuthEntity authenticate(final String email, final String password)
            throws AuthenticationFailedException {

        CustomerEntity customerEntity = customerDao.findCustomerByEmail(email);

        if (customerEntity == null) {
            throw new AuthenticationFailedException("ATH-001", "This contact number has not been registered!");
        }

        final String encryptedPassword = PasswordCryptographyProvider
                .encrypt(password, customerEntity.getSalt());

        if (encryptedPassword.equals(customerEntity.getPassword())) {
            JwtTokenProvider tokenProvider = new JwtTokenProvider(encryptedPassword);

            CustomerAuthEntity customerAuthTokenEntity = new CustomerAuthEntity();
            customerAuthTokenEntity.setCustomer(customerEntity);
            customerAuthTokenEntity.setUuid(UUID.randomUUID().toString());

            final ZonedDateTime currentTime = ZonedDateTime.now();
            final ZonedDateTime expiryTime = currentTime.plusHours(8);

            customerAuthTokenEntity.setAccessToken(tokenProvider
                    .generateToken(customerEntity.getUuid(), currentTime, expiryTime));
            customerAuthTokenEntity.setLoginAt(currentTime);
            customerAuthTokenEntity.setExpiresAt(expiryTime);

            customerDao.createAuthToken(customerAuthTokenEntity);
            return customerAuthTokenEntity;
        } else {
            throw new AuthenticationFailedException("ATH-002", "Invalid Credentials");
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public CustomerAuthEntity logout(String access_token) throws AuthorizationFailedException {

        final CustomerAuthEntity customerAuthEntity = customerDao
                .findCustomerAuthEntityByAccessToken(access_token);

        if (customerAuthEntity == null || customerAuthEntity.getUuid() == null){
            throw new AuthorizationFailedException("ATHR-001", "Customer is not Logged in");
        } else {
            final ZonedDateTime currentTime = ZonedDateTime.now();
            customerAuthEntity.setLogoutAt(currentTime);
            return customerAuthEntity;
        }
    }

}
