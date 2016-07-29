/*
 * Copyright 2016 the original author or authors.
 */

package com.biqasoft.microservice.communicator.interfaceimpl.demo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * @author Nikita Bakaev, ya@nbakaev.ru
 *         Date: 7/27/2016
 *         All Rights Reserved
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserAccount {

    private String id = UUID.randomUUID().toString();

    private String firstname;
    private String lastname;
    private AddressDTO address = new AddressDTO();

    public AddressDTO getAddress() {
        return address;
    }

    public void setAddress(AddressDTO address) {
        this.address = address;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }
}
