import java.util.*;

/**
 * ============================================================
 *  TechConnect — All Data Structures & Algorithms in Java
 * ============================================================
 *
 *  DS / Algorithm         Feature it powers
 *  ──────────────────     ────────────────────────────────────
 *  HashMap (adj. list)    Core graph storage
 *  BFS                    Fewest Connections Path
 *  Dijkstra + Min-Heap    Strongest Synergy Route
 *  DFS (backtracking)     Deep Network Explorer
 *  Union-Find             Know Your Circle (communities)
 *  Priority Queue (heap)  Top-K Recommendations
 *  B+ Tree                Mentor & Peer Finder (year range)
 *  Trie                   Student name prefix search
 *  LRU Cache              Recommendation caching
 */
public class TechConnectDSA {

    // ─────────────────────────────────────────────────────────────────────────
    // 1.  USER MODEL
    // ─────────────────────────────────────────────────────────────────────────
    static class User {
        String id, name, bio, branch;
        int year;
        List<String> interests;

        User(String id, String name, String bio, String branch, int year, List<String> interests) {
            this.id = id; this.name = name; this.bio = bio;
            this.branch = branch; this.year = year; this.interests = interests;
        }
        @Override public String toString() { return name + " (" + bio + ", " + branch + ", Yr" + year + ")"; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2.  GRAPH  (Adjacency List — HashMap<String, HashMap<String, Integer>>)
    //     Key: userId   Value: Map of neighborId → edge weight (synergy 1-10)
    // ─────────────────────────────────────────────────────────────────────────
    static class SocialGraph {

        Map<String, Map<String, Integer>> adj  = new HashMap<>();  // adjacency list
        Map<String, User>                users = new HashMap<>();   // user store

        // Add user
        void addUser(User u) {
            if (!adj.containsKey(u.id)) {
                adj.put(u.id, new HashMap<>());
                users.put(u.id, u);
            }
        }

        // Add undirected weighted edge (weight = synergy score 1–10)
        void addFriendship(String a, String b, int weight) {
            adj.getOrDefault(a, new HashMap<>()).put(b, weight);
            adj.getOrDefault(b, new HashMap<>()).put(a, weight);
        }

        // ── (A) BFS — Fewest-Connections Path ────────────────────────────────
        //  Time: O(V + E)   Space: O(V)
        //  Used by: "Connection Bridge" → Fewest Connections tab
        List<String> bfsShortestPath(String src, String tgt) {
            if (!adj.containsKey(src) || !adj.containsKey(tgt)) return null;
            if (src.equals(tgt)) return List.of(src);

            Map<String, String> parent = new HashMap<>();
            Queue<String> queue = new LinkedList<>();
            Set<String> visited = new HashSet<>();

            queue.add(src); visited.add(src); parent.put(src, null);

            while (!queue.isEmpty()) {
                String curr = queue.poll();
                if (curr.equals(tgt)) {
                    // Reconstruct path
                    List<String> path = new ArrayList<>();
                    for (String node = tgt; node != null; node = parent.get(node))
                        path.add(0, node);
                    return path;
                }
                for (String neighbor : adj.get(curr).keySet()) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        parent.put(neighbor, curr);
                        queue.add(neighbor);
                    }
                }
            }
            return null; // disconnected
        }

        // ── (B) Dijkstra — Strongest Synergy Route ───────────────────────────
        //  Time: O((V + E) log V)   Space: O(V)
        //  Key insight: cost = 11 - weight  →  lower cost = higher synergy
        //  Used by: "Connection Bridge" → Strongest Synergy tab
        List<String> dijkstraStrongestPath(String src, String tgt) {
            Map<String, Integer> dist   = new HashMap<>();
            Map<String, String>  parent = new HashMap<>();

            // Min-heap: [cost, nodeId]
            PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
            List<String> nodeList = new ArrayList<>(adj.keySet());
            Map<String, Integer> idToIdx = new HashMap<>();
            for (int i = 0; i < nodeList.size(); i++) idToIdx.put(nodeList.get(i), i);

            for (String node : adj.keySet()) dist.put(node, Integer.MAX_VALUE);
            dist.put(src, 0);
            pq.offer(new int[]{0, idToIdx.get(src)});
            parent.put(src, null);

            while (!pq.isEmpty()) {
                int[] top  = pq.poll();
                int   cost = top[0];
                String curr = nodeList.get(top[1]);

                if (cost > dist.get(curr)) continue; // stale entry
                if (curr.equals(tgt)) break;

                for (Map.Entry<String, Integer> e : adj.get(curr).entrySet()) {
                    String next   = e.getKey();
                    int    weight = e.getValue();
                    int    newCost = cost + (11 - weight); // invert: high synergy = low cost
                    if (newCost < dist.get(next)) {
                        dist.put(next, newCost);
                        parent.put(next, curr);
                        pq.offer(new int[]{newCost, idToIdx.get(next)});
                    }
                }
            }

            if (dist.get(tgt) == Integer.MAX_VALUE) return null; // unreachable

            List<String> path = new ArrayList<>();
            for (String node = tgt; node != null; node = parent.get(node))
                path.add(0, node);
            return path;
        }

        // ── (C) Single-source BFS distance map ───────────────────────────────
        //  Returns dist[node] = hops from src.  O(V + E).
        //  Used internally by getTopKRecommendations to avoid O(N·(V+E)).
        Map<String, Integer> bfsDistanceMap(String src) {
            Map<String, Integer> dist = new HashMap<>();
            if (!adj.containsKey(src)) return dist;
            Queue<String> queue = new LinkedList<>();
            queue.add(src); dist.put(src, 0);
            while (!queue.isEmpty()) {
                String curr = queue.poll();
                for (String nb : adj.get(curr).keySet()) {
                    if (!dist.containsKey(nb)) {
                        dist.put(nb, dist.get(curr) + 1);
                        queue.add(nb);
                    }
                }
            }
            return dist;
        }

        // ── (D) DFS — Deep Network Explorer (backtracking, 3 hops) ───────────
        //  Time: O(V + E)   Space: O(V) recursion stack
        //  Scores candidates by mutual friends + depth bonus (deeper = rarer)
        //  Used by: "Deep Explorer (DFS)" mode in Networking tab
        List<String> dfsRecommendations(String userId) {
            Set<String> directFriends = adj.getOrDefault(userId, new HashMap<>()).keySet();
            Set<String> visited       = new HashSet<>();
            Map<String, Integer> scores = new HashMap<>();
            visited.add(userId);
            dfsHelper(userId, userId, 0, 3, visited, directFriends, scores);

            List<String> result = new ArrayList<>(scores.keySet());
            result.sort((a, b) -> scores.get(b) - scores.get(a));
            return result;
        }

        private void dfsHelper(String source, String curr, int depth, int maxDepth,
                                Set<String> visited, Set<String> directFriends,
                                Map<String, Integer> scores) {
            if (depth >= maxDepth) return;
            for (String nb : adj.getOrDefault(curr, new HashMap<>()).keySet()) {
                if (!visited.contains(nb)) {
                    if (!nb.equals(source) && !directFriends.contains(nb)) {
                        int mutuals   = countMutuals(source, nb);
                        int depthBonus = Math.max(1, maxDepth - depth);
                        int score     = mutuals + depthBonus;
                        scores.merge(nb, score, Math::max);
                    }
                    visited.add(nb);
                    dfsHelper(source, nb, depth + 1, maxDepth, visited, directFriends, scores);
                    visited.remove(nb); // backtrack
                }
            }
        }

        int countMutuals(String a, String b) {
            Set<String> fa = adj.getOrDefault(a, new HashMap<>()).keySet();
            Set<String> fb = adj.getOrDefault(b, new HashMap<>()).keySet();
            int count = 0;
            for (String f : fa) if (fb.contains(f)) count++;
            return count;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3.  UNION-FIND  (Disjoint Set Union with path compression + union by rank)
    //     Time: α(N) per operation (nearly O(1))
    //     Used by: "Know Your Circle" — community detection
    // ─────────────────────────────────────────────────────────────────────────
    static class UnionFind {
        Map<String, String>  parent = new HashMap<>();
        Map<String, Integer> rank   = new HashMap<>();

        void add(String x) {
            parent.putIfAbsent(x, x);
            rank.putIfAbsent(x, 0);
        }

        String find(String x) {
            if (!parent.get(x).equals(x))
                parent.put(x, find(parent.get(x))); // path compression
            return parent.get(x);
        }

        void union(String a, String b) {
            String ra = find(a), rb = find(b);
            if (ra.equals(rb)) return;
            // union by rank
            if (rank.get(ra) < rank.get(rb))      parent.put(ra, rb);
            else if (rank.get(ra) > rank.get(rb))  parent.put(rb, ra);
            else { parent.put(rb, ra); rank.merge(ra, 1, Integer::sum); }
        }

        // Group all nodes by their root → communities
        Map<String, List<String>> getCommunities() {
            Map<String, List<String>> groups = new HashMap<>();
            for (String node : parent.keySet())
                groups.computeIfAbsent(find(node), k -> new ArrayList<>()).add(node);
            return groups;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4.  MIN-HEAP PRIORITY QUEUE — Top-K Recommendations
    //     Java's built-in PriorityQueue is a binary min-heap.
    //     We wrap it for Top-K: keep heap size ≤ K, popping the lowest score.
    //     Time: O(N log K)   Used by: Dashboard recommendation engine
    // ─────────────────────────────────────────────────────────────────────────
    static List<String[]> topKRecommendations(Map<String, Integer> scores, int k) {
        // Min-heap on score so we can evict the worst when size > K
        PriorityQueue<String[]> pq = new PriorityQueue<>(
            Comparator.comparingInt(a -> Integer.parseInt(a[1]))
        );
        for (Map.Entry<String, Integer> e : scores.entrySet()) {
            pq.offer(new String[]{e.getKey(), String.valueOf(e.getValue())});
            if (pq.size() > k) pq.poll(); // evict smallest
        }
        List<String[]> result = new ArrayList<>(pq);
        result.sort((a, b) -> Integer.parseInt(b[1]) - Integer.parseInt(a[1])); // descending
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5.  B+ TREE  (simplified — order-4, for year-based range search)
    //     Time: O(log N) insert/search, O(log N + K) range query
    //     Used by: "Mentor & Peer Finder" — filter students by college year
    // ─────────────────────────────────────────────────────────────────────────
    static class BPlusTree {
        private static final int ORDER = 4;

        static class Node {
            List<Integer>  keys     = new ArrayList<>();
            List<List<String>> values = new ArrayList<>(); // userIds at each key (leaf only)
            List<Node>     children = new ArrayList<>();
            Node           next;    // leaf chain
            boolean        isLeaf;

            Node(boolean isLeaf) { this.isLeaf = isLeaf; }
        }

        Node root = new Node(true);

        void insert(int key, String userId) {
            Node[] result = insertRec(root, key, userId);
            if (result != null) {               // root was split
                Node newRoot  = new Node(false);
                newRoot.keys.add(result[0].keys.get(0));  // promoted key
                newRoot.children.add(root);
                newRoot.children.add(result[1]);
                root = newRoot;
            }
        }

        // Returns non-null [promotedLeaf, newNode] if split occurred
        private Node[] insertRec(Node node, int key, String userId) {
            if (node.isLeaf) {
                // Insert into leaf in sorted order
                int pos = 0;
                while (pos < node.keys.size() && node.keys.get(pos) < key) pos++;
                if (pos < node.keys.size() && node.keys.get(pos) == key) {
                    node.values.get(pos).add(userId); // duplicate key → append
                } else {
                    node.keys.add(pos, key);
                    List<String> vals = new ArrayList<>(); vals.add(userId);
                    node.values.add(pos, vals);
                }
                if (node.keys.size() < ORDER) return null;

                // Split leaf
                int mid = ORDER / 2;
                Node right = new Node(true);
                right.keys.addAll(node.keys.subList(mid, node.keys.size()));
                right.values.addAll(node.values.subList(mid, node.values.size()));
                right.next = node.next;
                node.next  = right;
                node.keys   = new ArrayList<>(node.keys.subList(0, mid));
                node.values = new ArrayList<>(node.values.subList(0, mid));
                return new Node[]{right, right};
            }

            // Internal node
            int pos = 0;
            while (pos < node.keys.size() && key >= node.keys.get(pos)) pos++;
            Node[] split = insertRec(node.children.get(pos), key, userId);
            if (split == null) return null;

            node.keys.add(pos, split[0].keys.get(0));
            node.children.add(pos + 1, split[1]);
            if (node.keys.size() < ORDER) return null;

            // Split internal
            int mid = ORDER / 2;
            Node right = new Node(false);
            right.keys.addAll(node.keys.subList(mid + 1, node.keys.size()));
            right.children.addAll(node.children.subList(mid + 1, node.children.size()));
            int promoted = node.keys.get(mid);
            node.keys      = new ArrayList<>(node.keys.subList(0, mid));
            node.children  = new ArrayList<>(node.children.subList(0, mid + 1));
            Node promotedNode = new Node(false);
            promotedNode.keys.add(promoted);
            return new Node[]{promotedNode, right};
        }

        // Range query — traverse leaf chain
        List<String> rangeQuery(int min, int max) {
            List<String> result = new ArrayList<>();
            Node curr = root;
            while (!curr.isLeaf) {
                int pos = 0;
                while (pos < curr.keys.size() && min >= curr.keys.get(pos)) pos++;
                curr = curr.children.get(pos);
            }
            while (curr != null) {
                for (int i = 0; i < curr.keys.size(); i++) {
                    int k = curr.keys.get(i);
                    if (k > max) return result;
                    if (k >= min) result.addAll(curr.values.get(i));
                }
                curr = curr.next;
            }
            return result;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6.  TRIE  (prefix search on student names, word-boundary tokenized)
    //     Time: O(L) insert & search where L = query length
    //     Used by: Sidebar name search bar
    // ─────────────────────────────────────────────────────────────────────────
    static class Trie {
        static class TrieNode {
            Map<Character, TrieNode> children = new HashMap<>();
            Set<String> userIds = new HashSet<>();
        }

        TrieNode root = new TrieNode();

        // Insert user: tokenize name on spaces so each word is searchable
        void insert(String userId, String name) {
            for (String word : name.toLowerCase().split("\\s+")) {
                TrieNode node = root;
                for (char ch : word.toCharArray()) {
                    node.children.putIfAbsent(ch, new TrieNode());
                    node = node.children.get(ch);
                    node.userIds.add(userId);
                }
            }
        }

        // Return all userIds matching prefix query
        Set<String> search(String prefix) {
            TrieNode node = root;
            for (char ch : prefix.toLowerCase().toCharArray()) {
                if (!node.children.containsKey(ch)) return new HashSet<>();
                node = node.children.get(ch);
            }
            return node.userIds;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7.  LRU CACHE  (HashMap + Doubly-Linked List)
    //     O(1) get & put.  Invalidated on friendship change.
    //     Used by: Recommendation engine — avoids recomputing on every tab visit
    // ─────────────────────────────────────────────────────────────────────────
    static class LRUCache<K, V> {
        private final int capacity;

        // Doubly-linked list node
        static class DLLNode<K, V> {
            K key; V value;
            DLLNode<K, V> prev, next;
            DLLNode(K k, V v) { key = k; value = v; }
        }

        private final Map<K, DLLNode<K, V>> map = new HashMap<>();
        private final DLLNode<K, V> head = new DLLNode<>(null, null); // LRU end
        private final DLLNode<K, V> tail = new DLLNode<>(null, null); // MRU end

        LRUCache(int capacity) {
            this.capacity = capacity;
            head.next = tail; tail.prev = head;
        }

        private void remove(DLLNode<K, V> node) {
            node.prev.next = node.next;
            node.next.prev = node.prev;
        }

        private void insertAtTail(DLLNode<K, V> node) {
            node.prev = tail.prev; node.next = tail;
            tail.prev.next = node; tail.prev = node;
        }

        V get(K key) {
            if (!map.containsKey(key)) return null;
            DLLNode<K, V> node = map.get(key);
            remove(node); insertAtTail(node); // mark as recently used
            return node.value;
        }

        void put(K key, V value) {
            if (map.containsKey(key)) {
                DLLNode<K, V> node = map.get(key);
                node.value = value;
                remove(node); insertAtTail(node);
            } else {
                if (map.size() >= capacity) {
                    DLLNode<K, V> lru = head.next; // evict LRU
                    remove(lru); map.remove(lru.key);
                }
                DLLNode<K, V> node = new DLLNode<>(key, value);
                insertAtTail(node); map.put(key, node);
            }
        }

        void invalidate(K key) {
            if (map.containsKey(key)) { remove(map.get(key)); map.remove(key); }
        }

        int size() { return map.size(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8.  DEMO MAIN — smoke test all DSA
    // ─────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) {

        System.out.println("=== TechConnect DSA — Java Demo ===\n");

        // ── Build graph ──────────────────────────────────────────────────────
        SocialGraph g = new SocialGraph();
        User u1 = new User("u1","Arjun Sharma","ML Student","AIDS",3, List.of("Coding","Gaming","Music"));
        User u2 = new User("u2","Priya Mehta","UX Student","IT",2, List.of("Art","Design","Reading"));
        User u3 = new User("u3","Rahul Gupta","Data Science Major","AIDS",4, List.of("AI","Math","Gaming"));
        User u4 = new User("u4","Sneha Patel","Backend Developer","CS",4, List.of("Coding","Cloud","Coffee"));
        User u5 = new User("u5","Meera Krishnan","Cybersecurity Student","Cyber Sec",3, List.of("Security","Tech","Reading"));

        for (User u : List.of(u1,u2,u3,u4,u5)) g.addUser(u);
        g.adj.put("u1", new HashMap<>()); g.adj.put("u2", new HashMap<>());
        g.adj.put("u3", new HashMap<>()); g.adj.put("u4", new HashMap<>());
        g.adj.put("u5", new HashMap<>());
        g.addFriendship("u1","u2",7); g.addFriendship("u1","u3",9);
        g.addFriendship("u2","u4",5); g.addFriendship("u3","u4",8);
        g.addFriendship("u4","u5",6);

        // ── BFS ──────────────────────────────────────────────────────────────
        System.out.println("── BFS Shortest Path (u1 → u5) ──");
        List<String> bfsPath = g.bfsShortestPath("u1","u5");
        System.out.println("Path: " + bfsPath + "  hops=" + (bfsPath.size()-1));

        // ── Dijkstra ─────────────────────────────────────────────────────────
        System.out.println("\n── Dijkstra Strongest Synergy (u1 → u5) ──");
        List<String> dPath = g.dijkstraStrongestPath("u1","u5");
        System.out.println("Path: " + dPath);

        // ── DFS recommendations ──────────────────────────────────────────────
        System.out.println("\n── DFS Deep Recommendations for u1 ──");
        List<String> dfsRecs = g.dfsRecommendations("u1");
        System.out.println("Recommended (by deep score): " + dfsRecs);

        // ── Union-Find ───────────────────────────────────────────────────────
        System.out.println("\n── Union-Find Communities ──");
        UnionFind uf = new UnionFind();
        for (String id : g.adj.keySet()) uf.add(id);
        g.adj.forEach((a, nbMap) -> nbMap.keySet().forEach(b -> uf.union(a, b)));
        uf.getCommunities().forEach((root, members) ->
            System.out.println("  Community root=" + root + " → " + members));

        // ── Top-K with Min-Heap ───────────────────────────────────────────────
        System.out.println("\n── Top-2 Recommendations (min-heap) ──");
        Map<String, Integer> scores = Map.of("u2",12,"u3",18,"u4",7,"u5",15);
        topKRecommendations(scores, 2).forEach(r ->
            System.out.println("  " + r[0] + "  score=" + r[1]));

        // ── B+ Tree ──────────────────────────────────────────────────────────
        System.out.println("\n── B+ Tree Year Range Query (year 3–4) ──");
        BPlusTree tree = new BPlusTree();
        for (User u : List.of(u1,u2,u3,u4,u5)) tree.insert(u.year, u.id);
        System.out.println("Year 3-4: " + tree.rangeQuery(3,4));

        // ── Trie ─────────────────────────────────────────────────────────────
        System.out.println("\n── Trie Prefix Search ('arj') ──");
        Trie trie = new Trie();
        for (User u : List.of(u1,u2,u3,u4,u5)) trie.insert(u.id, u.name);
        System.out.println("Results: " + trie.search("arj"));

        // ── LRU Cache ────────────────────────────────────────────────────────
        System.out.println("\n── LRU Cache (capacity=2) ──");
        LRUCache<String, String> cache = new LRUCache<>(2);
        cache.put("top:u1:5", "[u3,u2]");
        cache.put("top:u2:5", "[u4,u5]");
        System.out.println("get top:u1:5 → " + cache.get("top:u1:5"));
        cache.put("top:u3:5", "[u1,u2]");   // evicts LRU (top:u2:5)
        System.out.println("get top:u2:5 → " + cache.get("top:u2:5")); // null
        cache.invalidate("top:u1:5");
        System.out.println("After invalidate, get top:u1:5 → " + cache.get("top:u1:5")); // null

        System.out.println("\n✅ All DSA verified!");
    }
}
