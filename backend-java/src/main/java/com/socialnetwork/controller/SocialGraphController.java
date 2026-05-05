package com.socialnetwork.controller;

import com.socialnetwork.model.User;
import com.socialnetwork.service.SocialGraphService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class SocialGraphController {
  private final SocialGraphService graph;

  public SocialGraphController(SocialGraphService graph) {
    this.graph = graph;
  }

  @GetMapping("/users")
  public Map<String, Object> users() {
    List<Map<String, Object>> users = graph.getAllUsersView();
    return Map.of("success", true, "count", users.size(), "users", users);
  }

  @GetMapping("/users/{userId}")
  public Map<String, Object> user(@PathVariable("userId") String userId) {
    Map<String, Object> user = graph.getUserView(userId);
    if (user == null) return Map.of("success", false, "error", "User not found");
    return Map.of("success", true, "user", user);
  }

  @PostMapping("/add-user")
  public Map<String, Object> addUser(@RequestBody AddUserRequest req) {
    if (req.name() == null || req.name().isBlank()) return Map.of("success", false, "error", "Name required");
    User u = graph.addUser(req.name(), req.bio(), req.branch(), req.year(), req.interests());

    // Frontend may send connectTo to auto-link the newly created user.
    if (req.connectTo() != null && !req.connectTo().isBlank()) {
      graph.addFriendship(u.getId(), req.connectTo().trim(), null);
    }

    return Map.of("success", true, "user", graph.getUserView(u.getId()));
  }

  @PostMapping("/add-friend")
  public Map<String, Object> addFriend(@RequestBody AddFriendRequest req) {
    if (req.userId1() == null || req.userId2() == null) {
      return Map.of("success", false, "error", "Both userId1 and userId2 required");
    }
    boolean ok = graph.addFriendship(req.userId1(), req.userId2(), req.weight());
    return ok ? Map.of("success", true) : Map.of("success", false, "error", "Friendship failed");
  }

  @GetMapping("/recommend/bfs/{userId}")
  public Map<String, Object> recommendBfs(@PathVariable("userId") String userId) {
    List<Map<String, Object>> recommendations = graph.bfsRecommendations(userId);
    return Map.of("success", true, "algorithm", "BFS", "count", recommendations.size(), "recommendations", recommendations);
  }

  @GetMapping("/recommend/dfs/{userId}")
  public Map<String, Object> recommendDfs(@PathVariable("userId") String userId) {
    List<Map<String, Object>> recommendations = graph.dfsRecommendations(userId);
    return Map.of("success", true, "algorithm", "DFS", "count", recommendations.size(), "recommendations", recommendations);
  }

  @GetMapping("/recommend/top/{userId}")
  public Map<String, Object> recommendTop(@PathVariable("userId") String userId, @RequestParam(name = "k", defaultValue = "5") int k) {
    List<Map<String, Object>> recommendations = graph.topKRecommendations(userId, k);
    return Map.of("success", true, "algorithm", "Top-K", "count", recommendations.size(), "recommendations", recommendations);
  }

  @GetMapping("/recommend/role/{userId}")
  public Map<String, Object> recommendRole(@PathVariable("userId") String userId, @RequestParam(name = "role", defaultValue = "") String role) {
    List<Map<String, Object>> recommendations = graph.roleRecommendations(userId, role);
    return Map.of("success", true, "algorithm", "Targeted Role Engine", "targetRole", role, "count", recommendations.size(), "recommendations", recommendations);
  }

  @GetMapping("/path/bfs/{source}/{target}")
  public Map<String, Object> pathBfs(@PathVariable("source") String source, @PathVariable("target") String target) {
    List<String> path = graph.shortestPathBfs(source, target);
    if (path.isEmpty()) return Map.of("success", false, "error", "No path found");
    return Map.of("success", true, "algorithm", "BFS", "degreesOfSeparation", path.size() - 1, "path", graph.pathUsers(path));
  }

  @GetMapping("/path/dijkstra/{source}/{target}")
  public Map<String, Object> pathDijkstra(@PathVariable("source") String source, @PathVariable("target") String target) {
    Map<String, Object> result = graph.shortestPathDijkstra(source, target);
    if (result.isEmpty()) return Map.of("success", false, "error", "No path found");
    @SuppressWarnings("unchecked")
    List<String> path = (List<String>) result.get("path");
    return Map.of("success", true, "algorithm", "Dijkstra", "totalWeight", result.get("distance"), "path", graph.pathUsers(path));
  }

  @GetMapping("/communities")
  public Map<String, Object> communities() {
    List<Map<String, Object>> communities = graph.getCommunities();
    return Map.of("success", true, "communityCount", communities.size(), "communities", communities);
  }

  @GetMapping("/influencers")
  public Map<String, Object> influencers(@RequestParam(name = "topN", defaultValue = "8") int topN) {
    List<Map<String, Object>> influencers = graph.getInfluencers(topN);
    return Map.of("success", true, "count", influencers.size(), "influencers", influencers);
  }

  @GetMapping("/search/name")
  public Map<String, Object> searchName(@RequestParam(name = "q", defaultValue = "") String q) {
    List<Map<String, Object>> users = graph.searchNamePrefix(q);
    return Map.of("success", true, "count", users.size(), "users", users);
  }

  @GetMapping("/search/year")
  public Map<String, Object> searchYear(@RequestParam(name = "min", defaultValue = "1") int min, @RequestParam(name = "max", defaultValue = "4") int max) {
    List<Map<String, Object>> users = graph.searchYearRange(min, max);
    return Map.of("success", true, "count", users.size(), "users", users);
  }

  /**
   * GET /search/interest?q=Gaming
   * HashTable O(1) lookup: interest string → bucket → list of user IDs.
   * No linear scan of all users needed.
   */
  @GetMapping("/search/interest")
  public Map<String, Object> searchInterest(@RequestParam(name = "q", defaultValue = "") String q) {
    if (q.isBlank()) return Map.of("success", false, "error", "Query required");
    List<Map<String, Object>> users = graph.searchByInterest(q);
    return Map.of("success", true, "dataStructure", "HashTable (Inverted Index)",
        "complexity", "O(1) average", "interest", q, "count", users.size(), "users", users);
  }

  /**
   * GET /interests
   * Lists all distinct interests currently in the HashTable index.
   */
  @GetMapping("/interests")
  public Map<String, Object> allInterests() {
    List<String> interests = graph.listAllInterests();
    return Map.of("success", true, "dataStructure", "HashTable (Inverted Index)",
        "count", interests.size(), "interests", interests);
  }


  @GetMapping("/graph-data")
  public Map<String, Object> graphData() {
    Map<String, Object> g = graph.graphData();
    return Map.of("success", true, "nodes", g.get("nodes"), "edges", g.get("edges"));
  }

  @GetMapping("/hackathon-team/{userId}")
  public Map<String, Object> hackathonTeam(@PathVariable("userId") String userId) {
    return Map.of("success", true, "team", graph.buildHackathonTeam(userId));
  }

  /**
   * GET /recommend/combined/:userId
   * Combined BFS + DFS with weighted scoring — delegates to Top-K with k=100.
   */
  @GetMapping("/recommend/combined/{userId}")
  public Map<String, Object> recommendCombined(@PathVariable("userId") String userId) {
    if (graph.getUser(userId) == null) {
      return Map.of("success", false, "error", "User not found");
    }
    List<Map<String, Object>> recommendations = graph.topKRecommendations(userId, 100);
    return Map.of(
      "success", true,
      "userId", userId,
      "scoringFormula", "finalScore = (bfsScore * 2) + dfsScore",
      "count", recommendations.size(),
      "recommendations", recommendations
    );
  }

  public record AddUserRequest(
    @NotBlank String name,
    String bio,
    String branch,
    Integer year,
    List<String> interests,
    String connectTo
  ) {}

  public static class AddFriendRequest {
    private String userId1;
    private String userId2;
    private Integer weight;

    public String userId1() { return userId1; }
    public String userId2() { return userId2; }
    public Integer weight() { return weight; }
  }
}
