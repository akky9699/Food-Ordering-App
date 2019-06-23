package com.upgrad.FoodOrderingApp.service.dao;

import com.upgrad.FoodOrderingApp.service.entity.AddressEntity;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.NoResultException;


@Repository
public class AddressDao {

    @PersistenceContext
    private EntityManager entityManager;


    public AddressEntity createAddress(AddressEntity addressEntity) {
        entityManager.persist(addressEntity);
        return addressEntity;
    }


    public AddressEntity getAddressByUUID(String uuid) {
        try {
            return entityManager.createNamedQuery("addressByUUID", AddressEntity.class).setParameter("uuid", uuid).getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }


    public AddressEntity updateAddressEntity(AddressEntity addressEntity) {
        return entityManager.merge(addressEntity);
    }


    public AddressEntity deleteAddressEntity(AddressEntity addressEntity) {
        entityManager.remove(addressEntity);
        return addressEntity;
    }
}