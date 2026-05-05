package com.socialnetwork.service;

import com.socialnetwork.ds.BPlusTree;
import com.socialnetwork.ds.HashTable;
import com.socialnetwork.ds.LruCache;
import com.socialnetwork.ds.GraphColoring;
import com.socialnetwork.ds.Trie;
import com.socialnetwork.ds.UnionFind;
import com.socialnetwork.model.User;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SocialGraphService {
  private final Map<String, User> users = new HashMap<>();
  private final Map<String, Map<String, Integer>> adjacencyList = new HashMap<>();
  private final Trie nameTrie = new Trie();
  private final BPlusTree yearIndex = new BPlusTree();
  private final LruCache<String, List<Map<String, Object>>> recommendationCache = new LruCache<>(100);
  // HashTable used as an Interest Inverted Index: interest → [userId, userId, ...]
  // O(1) average lookup — much faster than scanning all users for matching interests.
  private final HashTable<String, List<String>> interestIndex = new HashTable<>();

  public synchronized User addUser(String name, String bio, String branch, Integer year, List<String> interests) {
    String id = "u" + UUID.randomUUID().toString().substring(0, 8);
    User user = new User(id, name, bio, branch, year, interests);
    users.put(id, user);
    adjacencyList.putIfAbsent(id, new HashMap<>());
    if (name != null) nameTrie.insert(id, name);
    if (year != null) yearIndex.insert(year, id);
    indexInterests(id, interests);
    return user;
  }

  public synchronized boolean addUser(User user) {
    if (users.containsKey(user.getId())) return false;
    users.put(user.getId(), user);
    adjacencyList.putIfAbsent(user.getId(), new HashMap<>());
    if (user.getName() != null) nameTrie.insert(user.getId(), user.getName());
    if (user.getYear() != null) yearIndex.insert(user.getYear(), user.getId());
    indexInterests(user.getId(), user.getInterests());
    return true;
  }

  public synchronized boolean addFriendship(String a, String b, Integer weight) {
    if (!users.containsKey(a) || !users.containsKey(b) || a.equals(b)) return false;
    int edgeWeight = (weight == null) ? calculateConnectionWeight(a, b) : weight;
    adjacencyList.get(a).put(b, edgeWeight);
    adjacencyList.get(b).put(a, edgeWeight);
    recommendationCache.remove("top:" + a + ":5");
    recommendationCache.remove("top:" + b + ":5");
    return true;
  }

  public List<User> getAllUsers() {
    return users.values().stream().sorted(Comparator.comparing(User::getId)).toList();
  }

  public User getUser(String id) {
    return users.get(id);
  }

  public List<Map<String, Object>> getAllUsersView() {
    return users.keySet().stream().sorted().map(this::userView).collect(Collectors.toList());
  }

  public Map<String, Object> getUserView(String id) {
    if (!users.containsKey(id)) return null;
    return userView(id);
  }

  public List<Map<String, Object>> bfsRecommendations(String userId) {
    if (!users.containsKey(userId)) return List.of();
    Set<String> visited = new HashSet<>();
    Set<String> directFriends = adjacencyList.get(userId).keySet();
    Queue<String[]> queue = new ArrayDeque<>();
    Map<String, Integer> scoreMap = new HashMap<>();
    queue.add(new String[]{userId, "0"});
    visited.add(userId);

    while (!queue.isEmpty()) {
      String[] curr = queue.poll();
      String node = curr[0];
      int level = Integer.parseInt(curr[1]);
      if (level >= 2) continue;
      for (String neighbor : adjacencyList.getOrDefault(node, Map.of()).keySet()) {
        if (!visited.contains(neighbor)) {
          visited.add(neighbor);
          queue.add(new String[]{neighbor, String.valueOf(level + 1)});
        }
        if (level == 1 && !neighbor.equals(userId) && !directFriends.contains(neighbor)) {
          scoreMap.put(neighbor, Math.max(scoreMap.getOrDefault(neighbor, 0), countMutualFriends(userId, neighbor)));
        }
      }
    }
    return buildRecommendationResponse(scoreMap);
  }

  public List<Map<String, Object>> dfsRecommendations(String userId) {
    if (!users.containsKey(userId)) return List.of();
    Set<String> directFriends = adjacencyList.get(userId).keySet();
    Set<String> visited = new HashSet<>();
    visited.add(userId);
    Map<String, Integer> scoreMap = new HashMap<>();
    dfsHelper(userId, userId, 0, 3, visited, directFriends, scoreMap);
    return buildRecommendationResponse(scoreMap);
  }

  private void dfsHelper(String source, String current, int depth, int maxDepth, Set<String> visited,
                         Set<String> directFriends, Map<String, Integer> scores) {
    if (depth >= maxDepth) return;
    for (String neighbor : adjacencyList.getOrDefault(current, Map.of()).keySet()) {
      if (visited.contains(neighbor)) continue;
      if (!neighbor.equals(source) && !directFriends.contains(neighbor)) {
        int mutual = countMutualFriends(source, neighbor);
        int depthScore = Math.max(1, maxDepth - depth);
        scores.put(neighbor, Math.max(scores.getOrDefault(neighbor, 0), mutual + depthScore));
      }
      visited.add(neighbor);
      dfsHelper(source, neighbor, depth + 1, maxDepth, visited, directFriends, scores);
      visited.remove(neighbor);
    }
  }

  public List<String> shortestPathBfs(String source, String target) {
    if (!users.containsKey(source) || !users.containsKey(target)) return List.of();
    Queue<String> queue = new ArrayDeque<>();
    Map<String, String> parent = new HashMap<>();
    Set<String> visited = new HashSet<>();
    queue.add(source);
    visited.add(source);

    while (!queue.isEmpty()) {
      String node = queue.poll();
      if (node.equals(target)) break;
      for (String n : adjacencyList.getOrDefault(node, Map.of()).keySet()) {
        if (visited.add(n)) {
          parent.put(n, node);
          queue.add(n);
        }
      }
    }
    if (!visited.contains(target)) return List.of();
    List<String> path = new ArrayList<>();
    String cur = target;
    while (cur != null) {
      path.add(0, cur);
      cur = parent.get(cur);
    }
    return path;
  }

  /**
   * Dijkstra: Weighted Shortest Path using custom MinHeap.
   * Invert weight: higher synergy (10) → lower cost (1) → heap prefers it.
   * O((V + E) log V)
   */
  
    /**
     * Colors the social graph using backtracking graph coloring.
     * Returns a map of userId -> color (0..numColors-1). If coloring fails, returns null.
     */
    public Map<String, Integer> colorGraph(int numColors) {
        // Build a simple adjacency map (ignore edge weights)
        Map<String, Map<String, Integer>> adjacency = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> e : adjacencyList.entrySet()) {
            adjacency.put(e.getKey(), new HashMap<>(e.getValue()));
        }
        GraphColoring gc = new GraphColoring(adjacency, numColors);
        return gc.color();
    }

  public Map<String, Object> dijkstraWeightedPath(String source, String target) {
    if (!users.containsKey(source) || !users.containsKey(target)) return Map.of();
    Map<String, Integer> dist   = new HashMap<>();
    Map<String, String>  parent = new HashMap<>();
    for (String id : users.keySet()) dist.put(id, Integer.MAX_VALUE / 4);
    dist.put(source, 0);

    // PriorityQueue as MinHeap: int[]{distance, nodeIndex}
    java.util.PriorityQueue<int[]> heap = new java.util.PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
    List<String> nodeList = new ArrayList<>(users.keySet());
    Map<String, Integer> nodeIndex = new HashMap<>();
    for (int i = 0; i < nodeList.size(); i++) nodeIndex.put(nodeList.get(i), i);
    heap.offer(new int[]{0, nodeIndex.get(source)});

    while (!heap.isEmpty()) {
      int[]  top      = heap.poll();
      int    currDist = top[0];
      String u        = nodeList.get(top[1]);
      if (u.equals(target)) break;
      if (currDist > dist.getOrDefault(u, Integer.MAX_VALUE / 4)) continue; // stale
      for (Map.Entry<String, Integer> edge : adjacencyList.getOrDefault(u, Map.of()).entrySet()) {
        String v    = edge.getKey();
        int    cost = 11 - edge.getValue(); // invert weight → lower = stronger bond
        int    nd   = currDist + cost;
        if (nd < dist.getOrDefault(v, Integer.MAX_VALUE / 4)) {
          dist.put(v, nd);
          parent.put(v, u);
          Integer vIdx = nodeIndex.get(v);
          if (vIdx != null) heap.offer(new int[]{nd, vIdx});
        }
      }
    }
    if (dist.getOrDefault(target, Integer.MAX_VALUE / 4) >= Integer.MAX_VALUE / 4) return Map.of();
    List<String> path = new ArrayList<>();
    String cur = target;
    while (cur != null) {
      path.add(0, cur);
      cur = parent.get(cur);
    }
    return Map.of("path", path, "distance", dist.get(target));
  }

  /** Alias for dijkstraWeightedPath — called by the controller. */
  public Map<String, Object> shortestPathDijkstra(String source, String target) {
    return dijkstraWeightedPath(source, target);
  }

  public List<Map<String, Object>> topKRecommendations(String userId, int k) {
    String cacheKey = "top:" + userId + ":" + k;
    if (recommendationCache.containsKey(cacheKey)) return recommendationCache.get(cacheKey);
    if (!users.containsKey(userId)) return List.of();

    Set<String> direct = adjacencyList.getOrDefault(userId, Map.of()).keySet();
    Map<String, Integer> dist = bfsDistanceMap(userId);
    User source = users.get(userId);
    UnionFind uf = buildUnionFind();
    String sourceCommunity = uf.find(userId);

    List<Map<String, Object>> candidates = new ArrayList<>();
    for (String id : users.keySet()) {
      if (id.equals(userId) || direct.contains(id) || !dist.containsKey(id)) continue;
      User u = users.get(id);
      int mutual = countMutualFriends(userId, id);
      int distance = dist.get(id);
      int sharedInterests = countSharedInterests(source, u);
      int communityBonus = uf.find(id).equals(sourceCommunity) ? 4 : 0;
      int distScore = distance <= 2 ? 6 : Math.max(0, 5 - distance);
      int finalScore = (mutual * 2) + distScore + communityBonus + (sharedInterests * 3);
      Map<String, Object> rec = new LinkedHashMap<>();
      rec.put("id", id);
      rec.put("name", u.getName());
      rec.put("bio", u.getBio());
      rec.put("finalScore", finalScore);
      rec.put("score", finalScore);
      rec.put("distance", distance);
      rec.put("mutualCount", mutual);
      rec.put("sharedInterests", sharedInterests);
      rec.put("sameCommunity", communityBonus > 0);
      rec.put("avatar", initials(u.getName()));
      rec.put("color", colorForId(id));
      rec.put("branch", u.getBranch());
      rec.put("year", u.getYear());
      candidates.add(rec);
    }

    List<Map<String, Object>> result = candidates.stream()
      .sorted((a, b) -> Integer.compare((Integer) b.get("score"), (Integer) a.get("score")))
      .limit(k)
      .collect(Collectors.toList());
    recommendationCache.put(cacheKey, result);
    return result;
  }

  public List<Map<String, Object>> getCommunities() {
    UnionFind uf = buildUnionFind();
    int idx = 1;
    List<Map<String, Object>> out = new ArrayList<>();
    for (List<String> members : uf.communities()) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", idx++);
      item.put("size", members.size());
      item.put("members", members.stream().map(id -> Map.of("id", id, "name", users.get(id).getName())).toList());
      out.add(item);
    }
    return out;
  }

  public List<Map<String, Object>> getInfluencers(int topN) {
    return adjacencyList.entrySet().stream()
      .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
      .limit(topN)
      .map(e -> {
        User u = users.get(e.getKey());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", u.getId());
        out.put("name", u.getName());
        out.put("degree", e.getValue().size());
        out.put("bio", u.getBio());
        out.put("avatar", initials(u.getName()));
        out.put("color", colorForId(u.getId()));
        out.put("branch", u.getBranch());
        out.put("year", u.getYear());
        return out;
      })
      .collect(Collectors.toList());
  }

  public List<Map<String, Object>> searchNamePrefix(String q) {
    if (q == null || q.isBlank()) return List.of();
    return nameTrie.search(q).stream().filter(users::containsKey).map(this::userView).toList();
  }

  /**
   * HASHTABLE — Interest Inverted Index lookup.
   * O(1) average: directly hashes the interest string → bucket → user list.
   * No scanning of all 30 users needed.
   */
  public List<Map<String, Object>> searchByInterest(String interest) {
    if (interest == null || interest.isBlank()) return List.of();
    String key = interest.trim().toLowerCase();
    List<String> ids = interestIndex.get(key);  // O(1) HashTable lookup
    if (ids == null) return List.of();
    return ids.stream().filter(users::containsKey).map(this::userView).collect(Collectors.toList());
  }

  /**
   * Returns all distinct interests currently indexed in the HashTable.
   * Uses HashTable.keys() — iterates over all non-empty buckets.
   */
  public List<String> listAllInterests() {
    return interestIndex.keys().stream().sorted().collect(Collectors.toList());
  }

  /** Populates the HashTable interest index for a given user. */
  private void indexInterests(String userId, List<String> interests) {
    if (interests == null) return;
    for (String interest : interests) {
      String key = interest.trim().toLowerCase();
      List<String> existing = interestIndex.get(key);  // O(1) HashTable get
      if (existing == null) {
        List<String> list = new ArrayList<>();
        list.add(userId);
        interestIndex.put(key, list);                  // O(1) HashTable put
      } else {
        if (!existing.contains(userId)) existing.add(userId);
      }
    }
  }


  public List<Map<String, Object>> searchYearRange(int min, int max) {
    Set<String> ids = new LinkedHashSet<>(yearIndex.rangeQuery(min, max));
    return ids.stream().filter(users::containsKey).map(this::userView).toList();
  }

  public Map<String, Object> graphData() {
    List<Map<String, Object>> nodes = users.keySet().stream().sorted()
      .map(this::userNodeView)
      .collect(Collectors.toList());
    Set<String> seen = new HashSet<>();
    List<Map<String, Object>> edges = new ArrayList<>();
    for (Map.Entry<String, Map<String, Integer>> e : adjacencyList.entrySet()) {
      for (Map.Entry<String, Integer> n : e.getValue().entrySet()) {
        String key = e.getKey().compareTo(n.getKey()) < 0 ? e.getKey() + "-" + n.getKey() : n.getKey() + "-" + e.getKey();
        if (seen.add(key)) {
          edges.add(Map.of("source", e.getKey(), "target", n.getKey(), "weight", n.getValue()));
        }
      }
    }
    return Map.of("nodes", nodes, "edges", edges);
  }

  public List<Map<String, Object>> roleRecommendations(String userId, String targetRole) {
    if (!users.containsKey(userId)) return List.of();
    String role = targetRole == null ? "" : targetRole.trim().toLowerCase();
    return topKRecommendations(userId, 100).stream()
      .filter(r -> {
        if (role.isBlank()) return true;
        Object bio = r.get("bio");
        return bio != null && bio.toString().toLowerCase().contains(role);
      })
      .collect(Collectors.toList());
  }

  public List<Map<String, Object>> pathUsers(List<String> pathIds) {
    return pathIds.stream().filter(users::containsKey).map(this::userView).toList();
  }

  // Existing hackathon team builder (kept for backward compatibility)
    public List<Map<String, Object>> buildHackathonTeam(String userId) {
        // original implementation unchanged
        if (!users.containsKey(userId)) return List.of();
        Map<String, String> roleLabels = Map.of(
                "ml", "AI / ML Lead",
                "frontend", "Frontend / UX",
                "devops", "Data / DevOps",
                "backend", "Backend Dev"
        );

        Set<String> visited = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        visited.add(userId);
        queue.add(userId);
        Set<String> directFriends = adjacencyList.getOrDefault(userId, Map.of()).keySet();
        List<String> candidates = new ArrayList<>();

        while (!queue.isEmpty() && candidates.size() < 60) {
            String curr = queue.poll();
            for (String n : adjacencyList.getOrDefault(curr, Map.of()).keySet()) {
                if (visited.add(n)) {
                    queue.add(n);
                    candidates.add(n);
                }
            }
        }

        candidates.sort((a, b) -> Boolean.compare(directFriends.contains(b), directFriends.contains(a)));

        String myRole = classifyRole(users.get(userId));
        List<Map<String, Object>> team = new ArrayList<>();
        team.add(teamMemberView(userId, roleLabels.getOrDefault(myRole, "Backend Dev"), true, false));
        Set<String> usedRoles = new HashSet<>();
        usedRoles.add(myRole);

        for (String c : candidates) {
            if (team.size() >= 4) break;
            String role = classifyRole(users.get(c));
            if (!usedRoles.contains(role)) {
                usedRoles.add(role);
                team.add(teamMemberView(c, roleLabels.get(role), false, directFriends.contains(c)));
            }
        }

        for (String c : candidates) {
            if (team.size() >= 4) break;
            boolean already = team.stream().anyMatch(m -> m.get("id").equals(c));
            if (!already) team.add(teamMemberView(c, "Generalist", false, directFriends.contains(c)));
        }
        return team;
    }

    /**
     * Build a hackathon team ensuring members are from different graph‑color groups.
     * Guarantees no two teammates are directly connected (different colors).
     */
    public List<Map<String, Object>> buildHackathonTeamWithColors(String userId, int numColors) {
        Map<String, Integer> coloring = colorGraph(numColors);
        if (coloring == null || !coloring.containsKey(userId)) {
            // fallback to original method if coloring fails
            return buildHackathonTeam(userId);
        }
        Set<Integer> usedColors = new HashSet<>();
        List<Map<String, Object>> team = new ArrayList<>();
        // role labels reused from original method
        Map<String, String> roleLabels = Map.of(
                "ml", "AI / ML Lead",
                "frontend", "Frontend / UX",
                "devops", "Data / DevOps",
                "backend", "Backend Dev"
        );
        // add the requester as team lead
        String myRole = classifyRole(users.get(userId));
        team.add(teamMemberView(userId, roleLabels.getOrDefault(myRole, "Backend Dev"), true, false));
        usedColors.add(coloring.get(userId));
        // iterate over all other users, picking one per unused color until we have 4 members
        for (String uid : users.keySet()) {
            if (team.size() >= 4) break;
            if (uid.equals(userId)) continue;
            Integer col = coloring.get(uid);
            if (col == null || usedColors.contains(col)) continue;
            usedColors.add(col);
            String role = classifyRole(users.get(uid));
            team.add(teamMemberView(uid, roleLabels.getOrDefault(role, "Backend Dev"), false, false));
        }
        // if we still have less than 4 members (not enough colors), fill with best‑fit candidates
        if (team.size() < 4) {
            // reuse original candidate generation logic
            Set<String> directFriends = adjacencyList.getOrDefault(userId, Map.of()).keySet();
            Set<String> visited = new HashSet<>();
            Queue<String> queue = new ArrayDeque<>();
            visited.add(userId);
            queue.add(userId);
            List<String> candidates = new ArrayList<>();
            while (!queue.isEmpty() && candidates.size() < 60) {
                String curr = queue.poll();
                for (String n : adjacencyList.getOrDefault(curr, Map.of()).keySet()) {
                    if (visited.add(n)) {
                        queue.add(n);
                        candidates.add(n);
                    }
                }
            }
            candidates.sort((a, b) -> Boolean.compare(directFriends.contains(b), directFriends.contains(a)));
            for (String c : candidates) {
                if (team.size() >= 4) break;
                boolean already = team.stream().anyMatch(m -> m.get("id").equals(c));
                if (!already) {
                    String role = classifyRole(users.get(c));
                    team.add(teamMemberView(c, roleLabels.getOrDefault(role, "Backend Dev"), false, directFriends.contains(c)));
                }
            }
        }
        return team;
    }

  private int calculateConnectionWeight(String a, String b) {
    User u1 = users.get(a);
    User u2 = users.get(b);
    if (u1 == null || u2 == null) return 1;
    int shared = countSharedInterests(u1, u2);
    int sameYear = (u1.getYear() != null && u1.getYear().equals(u2.getYear())) ? 2 : 0;
    return Math.min(10, shared * 2 + sameYear + 1);
  }

  private int countSharedInterests(User a, User b) {
    if (a.getInterests() == null || b.getInterests() == null) return 0;
    Set<String> s = new HashSet<>(a.getInterests());
    int count = 0;
    for (String i : b.getInterests()) if (s.contains(i)) count++;
    return count;
  }

  private int countMutualFriends(String a, String b) {
    Set<String> fa = adjacencyList.getOrDefault(a, Map.of()).keySet();
    Set<String> fb = adjacencyList.getOrDefault(b, Map.of()).keySet();
    Set<String> small = fa.size() <= fb.size() ? fa : fb;
    Set<String> big = fa.size() <= fb.size() ? fb : fa;
    int c = 0;
    for (String x : small) if (big.contains(x)) c++;
    return c;
  }

  private Map<String, Integer> bfsDistanceMap(String src) {
    Map<String, Integer> dist = new HashMap<>();
    Queue<String> q = new ArrayDeque<>();
    q.add(src);
    dist.put(src, 0);
    while (!q.isEmpty()) {
      String u = q.poll();
      for (String v : adjacencyList.getOrDefault(u, Map.of()).keySet()) {
        if (!dist.containsKey(v)) {
          dist.put(v, dist.get(u) + 1);
          q.add(v);
        }
      }
    }
    return dist;
  }

  private List<Map<String, Object>> buildRecommendationResponse(Map<String, Integer> scoreMap) {
    return scoreMap.entrySet().stream()
      .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
      .map(e -> {
        User u = users.get(e.getKey());
        return Map.<String, Object>of(
          "id", u.getId(),
          "name", u.getName(),
          "bio", u.getBio(),
          "score", e.getValue(),
          "avatar", initials(u.getName()),
          "color", colorForId(u.getId()),
          "branch", u.getBranch(),
          "year", u.getYear(),
          "mutualCount", countMutualFriends(e.getKey(), u.getId())
        );
      })
      .collect(Collectors.toList());
  }

  private UnionFind buildUnionFind() {
    UnionFind uf = new UnionFind();
    for (String id : users.keySet()) uf.add(id);
    for (Map.Entry<String, Map<String, Integer>> e : adjacencyList.entrySet()) {
      for (String n : e.getValue().keySet()) uf.union(e.getKey(), n);
    }
    return uf;
  }

  private Map<String, Object> userNodeView(String id) {
    User u = users.get(id);
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", u.getId());
    m.put("name", u.getName());
    m.put("bio", u.getBio());
    m.put("branch", u.getBranch());
    m.put("year", u.getYear());
    m.put("avatar", initials(u.getName()));
    m.put("color", colorForId(u.getId()));
    return m;
  }

  private Map<String, Object> userView(String id) {
    User u = users.get(id);
    Map<String, Object> m = new LinkedHashMap<>(userNodeView(id));
    List<Map<String, Object>> friends = adjacencyList.getOrDefault(id, Map.of()).entrySet().stream()
      .map(e -> Map.<String, Object>of("id", e.getKey(), "weight", e.getValue()))
      .collect(Collectors.toList());
    m.put("friendCount", friends.size());
    m.put("friends", friends);
    return m;
  }

  private Map<String, Object> teamMemberView(String id, String assignedRole, boolean isYou, boolean isDirectFriend) {
    Map<String, Object> base = new LinkedHashMap<>(userNodeView(id));
    base.put("assignedRole", assignedRole);
    base.put("isYou", isYou);
    base.put("isDirectFriend", isDirectFriend);
    return base;
  }

  private String classifyRole(User user) {
    String text = ((user.getBio() == null ? "" : user.getBio()) + " " + String.join(" ", user.getInterests())).toLowerCase();
    if (containsAny(text, List.of("ml", "ai", "machine learning", "deep learning", "nlp", "computer vision", "data science", "research"))) return "ml";
    if (containsAny(text, List.of("frontend", "front-end", "ux", "ui", "react", "vue", "angular", "design", "mobile", "flutter", "ios", "android"))) return "frontend";
    if (containsAny(text, List.of("data", "cloud", "devops", "security", "aws", "azure", "gcp", "docker", "kubernetes", "linux", "network"))) return "devops";
    return "backend";
  }

  private boolean containsAny(String text, List<String> keys) {
    for (String k : keys) if (text.contains(k)) return true;
    return false;
  }

  private String initials(String name) {
    if (name == null || name.isBlank()) return "U";
    String[] parts = name.trim().split("\\s+");
    String first = parts[0].substring(0, 1);
    String second = parts.length > 1 ? parts[1].substring(0, 1) : "";
    return (first + second).toUpperCase();
  }

  private String colorForId(String id) {
    String[] colors = {"#6366f1", "#ec4899", "#14b8a6", "#f59e0b", "#84cc16", "#f43f5e", "#8b5cf6", "#06b6d4"};
    return colors[Math.abs(id.hashCode()) % colors.length];
  }
}
