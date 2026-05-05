package com.socialnetwork.model;

import java.util.ArrayList;
import java.util.List;

public class User {
  private String id;
  private String name;
  private String bio;
  private String branch;
  private Integer year;
  private List<String> interests = new ArrayList<>();

  public User() {}

  public User(String id, String name, String bio, String branch, Integer year, List<String> interests) {
    this.id = id;
    this.name = name;
    this.bio = bio;
    this.branch = branch;
    this.year = year;
    if (interests != null) this.interests = interests;
  }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getBio() { return bio; }
  public void setBio(String bio) { this.bio = bio; }
  public String getBranch() { return branch; }
  public void setBranch(String branch) { this.branch = branch; }
  public Integer getYear() { return year; }
  public void setYear(Integer year) { this.year = year; }
  public List<String> getInterests() { return interests; }
  public void setInterests(List<String> interests) { this.interests = interests; }
}
