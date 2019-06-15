package com.upgrad.FoodOrderingApp.service.dao;

import com.upgrad.FoodOrderingApp.service.entity.CustomerAuthEntity;
import com.upgrad.FoodOrderingApp.service.entity.CustomerEntity;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

@Repository
public class CustomerDao {

    @PersistenceContext
    private EntityManager entityManager;

    public CustomerEntity createCustomer(final CustomerEntity customerEntity) {
        entityManager.persist(customerEntity);
        return customerEntity;
    }

    public CustomerAuthEntity createAuthToken(final CustomerAuthEntity customerAuthTokenEntity) {
        entityManager.persist(customerAuthTokenEntity);
        return customerAuthTokenEntity;
    }

    public CustomerEntity findCustomerByContactNumber(final String contact_number) {
        try {
            String query = "select u from CustomerEntity u where u.contact_number = :contact_number";
            return entityManager.createQuery(query, CustomerEntity.class)
                    .setParameter("contact_number", contact_number).getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

    public CustomerEntity findCustomerByEmail(final String email) {
        try {
            String query = "select u from CustomerEntity u where u.email = :email";
            return entityManager.createQuery(query, CustomerEntity.class)
                    .setParameter("email", email).getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

    public CustomerAuthEntity findCustomerAuthEntityByAccessToken(final String access_token){
        try{
            String query = "select u from CustomerAuthEntity u where u.accessToken = :accessToken";
            return entityManager.createQuery(query, CustomerAuthEntity.class)
                    .setParameter("accessToken", access_token).getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

}

