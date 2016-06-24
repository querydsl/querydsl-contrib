package com.querydsl.elasticsearch2.domain;

import java.util.Date;

import com.querydsl.core.annotations.QueryEntity;

@QueryEntity
public class User extends AbstractEntity {

    public enum Gender { MALE, FEMALE }

    private String firstName;

    private String lastName;

    private Date created;

    private Gender gender;

    //@QueryEmbedded
    //private final List<Address> addresses = new ArrayList<Address>();

    //@QueryEmbedded
    //private Address mainAddress;

    private int age;

    public User() {
    }

    public User(String firstName, String lastName) {
        this.firstName = firstName; this.lastName = lastName;
        this.created = new Date();
    }

    public User(String firstName, String lastName, int age, Date created) {
        this.firstName = firstName; this.lastName = lastName; this.age = age; this.created = created;
    }

    @Override
    public String toString() {
        return "TestUser [id=" + getId() + ", firstName=" + firstName + ", lastName=" + lastName
                + "]";
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

}
