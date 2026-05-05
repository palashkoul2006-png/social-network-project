package com.socialnetwork.config;

import com.socialnetwork.model.User;
import com.socialnetwork.service.SocialGraphService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SEED DATA LOADER
 * ================
 * Mirrors the SEED_USERS and SEED_FRIENDSHIPS arrays from server.js exactly.
 * Runs once on startup via CommandLineRunner.
 */
@Component
public class SeedDataLoader implements CommandLineRunner {
  private final SocialGraphService graph;

  public SeedDataLoader(SocialGraphService graph) {
    this.graph = graph;
  }

  @Override
  public void run(String... args) {
    // ── Circle A: Original Main Cluster (u1–u15) ─────────────────────────────
    graph.addUser(new User("u1",  "Arjun Sharma",   "ML Student",                 "AIDS",      3, List.of("Coding", "Gaming", "Music")));
    graph.addUser(new User("u2",  "Priya Mehta",    "UX Student",                 "IT",        2, List.of("Art", "Design", "Reading")));
    graph.addUser(new User("u3",  "Rahul Gupta",    "Data Science Major",         "AIDS",      4, List.of("AI", "Math", "Gaming")));
    graph.addUser(new User("u4",  "Sneha Patel",    "Product Management Student", "CS",        3, List.of("Business", "Tech", "Travel")));
    graph.addUser(new User("u5",  "Vikram Singh",   "Backend Developer",          "CS",        4, List.of("Coding", "Cloud", "Coffee")));
    graph.addUser(new User("u6",  "Ananya Rao",     "ML Engineer",               "AIDS",      2, List.of("AI", "Robotics", "Music")));
    graph.addUser(new User("u7",  "Kiran Nair",     "DevOps Enthusiast",         "IT",        3, List.of("Cloud", "Automation", "Gaming")));
    graph.addUser(new User("u8",  "Deepika Iyer",   "Frontend Developer",        "CS",        1, List.of("Design", "Coding", "Art")));
    graph.addUser(new User("u9",  "Suresh Kumar",   "Software Engineer",         "IT",        4, List.of("Cloud", "Management", "Business")));
    graph.addUser(new User("u10", "Meera Krishnan", "Cybersecurity Student",     "Cyber Sec", 3, List.of("Security", "Tech", "Reading")));
    graph.addUser(new User("u11", "Rohan Desai",    "Blockchain Developer",      "CS",        2, List.of("Crypto", "Coding", "Finance")));
    graph.addUser(new User("u12", "Nisha Agarwal",  "AI Researcher",             "AIDS",      4, List.of("AI", "Tech", "Math")));
    graph.addUser(new User("u13", "Amit Joshi",     "Software Engineer",         "CS",        3, List.of("Architecture", "Tech", "Coffee")));
    graph.addUser(new User("u14", "Pooja Verma",    "Mobile Developer",          "IT",        1, List.of("Coding", "Mobile", "Design")));
    graph.addUser(new User("u15", "Sanjay Reddy",   "Database Systems Major",    "Cyber Sec", 4, List.of("Data", "Security", "Business")));

    // ── Circle B: Cyber Security Island (u16–u20) — bridged via u10 & u15 ────
    graph.addUser(new User("u16", "Tanvi Kulkarni", "Ethical Hacker",            "Cyber Sec", 3, List.of("Security", "CTF", "Linux")));
    graph.addUser(new User("u17", "Ishaan Bose",    "Network Security Analyst",  "Cyber Sec", 4, List.of("Networking", "Security", "Gaming")));
    graph.addUser(new User("u18", "Kavitha Menon",  "Penetration Tester",        "Cyber Sec", 2, List.of("Security", "Python", "CTF")));
    graph.addUser(new User("u19", "Dhruv Malhotra", "Cryptography Enthusiast",   "Cyber Sec", 3, List.of("Math", "Security", "Finance")));
    graph.addUser(new User("u20", "Riya Choudhary", "SOC Analyst Student",       "Cyber Sec", 1, List.of("Security", "Networking", "Reading")));

    // ── Circle C: AIDS Research Pod (u21–u25) — ISOLATED, no cross-edges ─────
    graph.addUser(new User("u21", "Yash Tiwari",    "Deep Learning Researcher",  "AIDS",      4, List.of("AI", "Research", "Python")));
    graph.addUser(new User("u22", "Simran Kaur",    "Computer Vision Student",   "AIDS",      3, List.of("AI", "Photography", "Math")));
    graph.addUser(new User("u23", "Aditya Pillai",  "NLP Engineer Intern",       "AIDS",      4, List.of("AI", "Linguistics", "Coding")));
    graph.addUser(new User("u24", "Tanya Mishra",   "Data Engineering Student",  "AIDS",      2, List.of("Data", "Cloud", "Music")));
    graph.addUser(new User("u25", "Om Prakash",     "MLOps Enthusiast",          "AIDS",      3, List.of("Cloud", "AI", "Automation")));

    // ── Circle D: CS Gaming & Open Source Gang (u26–u30) — bridges to Circle A ─
    graph.addUser(new User("u26", "Kartik Bhatt",   "Game Developer",            "CS",        2, List.of("Gaming", "Coding", "Art")));
    graph.addUser(new User("u27", "Shruti Patil",   "Open Source Contributor",   "CS",        3, List.of("Coding", "Linux", "Coffee")));
    graph.addUser(new User("u28", "Nikhil Yadav",   "React Developer",           "IT",        1, List.of("Coding", "Design", "Gaming")));
    graph.addUser(new User("u29", "Farida Sheikh",  "Cloud Solutions Architect", "IT",        4, List.of("Cloud", "Business", "Travel")));
    graph.addUser(new User("u30", "Manav Oberoi",   "Systems Programmer",        "CS",        3, List.of("Coding", "Architecture", "Coffee")));

    // ── Friendships ──────────────────────────────────────────────────────────
    // Circle A internal edges
    String[][] edges = {
      {"u1","u2"}, {"u1","u3"}, {"u1","u4"},
      {"u2","u5"}, {"u2","u6"}, {"u3","u7"},
      {"u4","u8"}, {"u4","u9"}, {"u5","u10"},
      {"u6","u11"},{"u7","u12"},{"u8","u13"},
      {"u9","u14"},{"u10","u15"},{"u11","u12"},
      {"u12","u13"},{"u13","u14"},{"u14","u15"},
      {"u3","u5"}, {"u6","u8"}, {"u2","u9"},
      {"u1","u10"},{"u7","u11"},{"u4","u12"},

      // Circle B internal edges (Cyber Security Island)
      {"u16","u17"},{"u17","u18"},{"u18","u19"},
      {"u19","u20"},{"u16","u19"},{"u17","u20"},

      // Circle B ↔ Circle A bridge (via u10 & u15)
      {"u10","u16"},{"u15","u17"},

      // Circle C ISOLATED — no bridges at all
      {"u21","u22"},{"u22","u23"},{"u23","u24"},
      {"u24","u25"},{"u21","u23"},{"u22","u25"},

      // Circle D internal edges (CS Gaming Squad)
      {"u26","u27"},{"u27","u28"},{"u28","u29"},
      {"u29","u30"},{"u26","u30"},{"u27","u30"},

      // Circle D ↔ Circle A bridges
      {"u5","u27"}, {"u8","u28"}, {"u9","u29"},
    };

    for (String[] e : edges) {
      graph.addFriendship(e[0], e[1], null);
    }

    System.out.println("✅ Graph seeded: 30 users, " + edges.length + " friendships");
  }
}
