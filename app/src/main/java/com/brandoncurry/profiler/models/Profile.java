package com.brandoncurry.profiler.models;

public class Profile {


    public String id;
    public String userId;
    public String name;
    public String age;
    public String gender;
    public String hobbies;
    public String imageUrl;
    public String bgColor;

    public Profile(String id, String userId, String imageUrl, String name, String age, String gender, String hobbies, String bgColor) {
        this.id = id;
        this.userId = userId;
        this.imageUrl = imageUrl;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.hobbies = hobbies;
        this.bgColor = bgColor;
    }

}