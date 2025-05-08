// app/model/user.java
package com.owlerdev.owler.model.user;

public class User {
    private String id;
    private String email;
    private String name;
    private Integer age;
    private Character gender;
    private Integer productivityScore; // 0–100

    public User() { }

    public User(String id, String email, String name, Integer age, Character gender, Integer productivityScore) {
        this.id = id;
        this.email = email;
        this.name  = name;
        this.age   = age;
        this.gender = gender;
        this.productivityScore = productivityScore;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }
    public void setAge(Integer age) {
        this.age = age;
    }

    public Character getGender() {
        return gender;
    }

    public void setGender(Character gender) {
        this.gender = gender;
    }

    public Integer getProductivityScore() {
        return productivityScore;
    }

//    public void setProductivityScore(Integer productivityScore) {
//        this.productivityScore = productivityScore;
//    }
}
