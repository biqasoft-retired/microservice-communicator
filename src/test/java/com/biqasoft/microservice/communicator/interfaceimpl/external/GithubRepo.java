package com.biqasoft.microservice.communicator.interfaceimpl.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by ya on 1/31/2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GithubRepo {

    private String id;
    private String name;

    private String fullName;
    private RepoOwner owner;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RepoOwner{
        String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    @JsonProperty("full_name")
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public RepoOwner getOwner() {
        return owner;
    }

    public void setOwner(RepoOwner owner) {
        this.owner = owner;
    }
}
