# 🔗 Social Network DSA Project (JS + Java)

This project is a mini social network platform used to demonstrate **Data Structures and Algorithms** in a practical system.

You now have:
- `backend/` → original Node.js implementation
- `backend-java/` → Java Spring Boot implementation
- `frontend/` → static frontend UI

---

## 🗂️ Refined Project Structure

```
social-network/
├── backend/                      # Node.js backend (original)
│   ├── graph.js
│   ├── server.js
│   └── ... DSA modules
├── backend-java/                 # Java Spring Boot backend (new)
│   ├── pom.xml
│   └── src/main/java/com/socialnetwork/
│       ├── controller/
│       ├── service/
│       ├── ds/
│       ├── model/
│       └── config/
├── frontend/                     # Static HTML frontend
│   └── social-network-app.html
└── README.md
```

---

## 🚀 Quick Start (Java Backend)

### 1) Java Backend
```bash
cd backend-java
mvn spring-boot:run
# Server runs on http://localhost:4000
```

### 2) Frontend
Open `frontend/social-network-app.html` directly in your browser — no build step required!

---

## Java DSA Features Implemented

- Graph adjacency list with weighted undirected edges
- BFS recommendations and shortest path
- DFS recommendations (depth limited)
- Dijkstra weighted shortest path
- Union-Find community detection
- Trie prefix search (`/search/name`)
- B+ Tree-like range query via sorted index (`/search/year`)
- LRU cache for top-K recommendations
- Top-K ranking with priority/score-based sorting

---

## Major API Endpoints (Java)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/users` | Get all users |
| GET | `/users/:userId` | Get user by ID |
| POST | `/add-user` | Add new user |
| POST | `/add-friend` | Add friendship edge |
| GET | `/recommend/bfs/:userId` | BFS recommendations |
| GET | `/recommend/dfs/:userId` | DFS recommendations |
| GET | `/recommend/top/:userId?k=5` | Top-K recommendations |
| GET | `/path/bfs/:source/:target` | Shortest path by hops |
| GET | `/path/dijkstra/:source/:target` | Weighted shortest path |
| GET | `/communities` | Union-Find communities |
| GET | `/influencers` | Highest-degree users |
| GET | `/search/name?q=ar` | Trie prefix search |
| GET | `/search/year?min=2&max=4` | Year range search |
| GET | `/graph-data` | Nodes + edges for visualization |

---

## 🔌 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/users` | Get all users with friend lists |
| GET | `/users/:userId` | Get single user |
| POST | `/add-user` | Add new user node to graph |
| POST | `/add-friend` | Add friendship edge |
| GET | `/recommend/bfs/:userId` | BFS recommendations (Level 2) |
| GET | `/recommend/dfs/:userId` | DFS recommendations (Level 3) |
| GET | `/recommend/combined/:userId` | Combined with scoring formula |
| GET | `/graph-data` | Graph as nodes+edges for D3.js |

---

## 🧠 DSA Implementation

### Graph Representation
```javascript
// Adjacency List — equivalent to: unordered_map<string, Set<string>>
this.adjacencyList = new Map();   // O(1) avg lookup
// Each value is a Set for O(1) membership check
this.adjacencyList.set('u1', new Set(['u2', 'u3']));
```

**Why Adjacency List?**
- Space: **O(V + E)** vs O(V²) for matrix
- Add edge: **O(1)** amortized
- Neighbor check: **O(1)** average with hash set

---

### BFS Algorithm — Close Friends (Level 2)

```javascript
// Queue-based BFS: FIFO traversal
const queue = [[userId, 0]];          // [node, level]
const visited = new Set([userId]);    // O(1) lookup

while (queue.length > 0) {
  const [current, level] = queue.shift();  // FIFO dequeue
  if (level >= 2) continue;                // limit depth
  
  for (const neighbor of neighbors) {
    if (!visited.has(neighbor)) {
      visited.add(neighbor);
      queue.push([neighbor, level + 1]);   // enqueue next level
    }
    // Level-2 nodes → candidates (friends-of-friends)
    if (level === 1 && !directFriends.has(neighbor)) {
      score = countMutualFriends(userId, neighbor);
    }
  }
}
```

**Complexity:** Time O(V+E), Space O(V)

---

### DFS Algorithm — Normal Friends (Level 3)

```javascript
// Recursive DFS with backtracking
function dfsHelper(current, depth, maxDepth) {
  if (depth >= maxDepth) return;  // base case
  
  for (const neighbor of neighbors) {
    if (!visited.has(neighbor)) {
      // add recommendation if not direct friend
      visited.add(neighbor);           // mark
      dfsHelper(neighbor, depth+1, maxDepth);  // recurse
      visited.delete(neighbor);        // BACKTRACK — unmark
    }
  }
}
```

**Complexity:** Time O(V+E), Space O(V) + O(depth) call stack

---

### Scoring Formula

```
finalScore = (bfsScore × 2) + dfsScore
```

BFS recommendations are weighted **2×** because:
- Level-2 connections are closer (higher relevance)
- BFS guarantees shortest path — minimum social distance
- DFS reaches farther but with lower confidence

---

### Hash Table (unordered_map) Details

| Operation | Average | Worst Case |
|-----------|---------|------------|
| Insert    | O(1)    | O(n)       |
| Lookup    | O(1)    | O(n)       |
| Delete    | O(1)    | O(n)       |

**Load Factor:** α = n/m (elements / buckets)
- JS engines maintain α ≈ 0.75 before **rehashing**
- Rehash: doubles m, re-inserts all n elements — O(n) one-time cost
- Amortized insert: O(1) across all operations

---

## 🎨 Frontend Features

- **Dashboard** — Stats + BFS/DFS recommendations side-by-side
- **Graph View** — D3.js force-directed network visualization
- **Users Grid** — Browse all 15 users with card layout
- **DSA Docs** — In-app algorithm documentation
- **Search** — Live filter across name, bio, location
- **Add User** — Dynamically add nodes to the graph
- **Add Friend** — Connect users with edge insertion
- **Score bars** — Visual representation of recommendation strength
- **Dark theme** — System-default dark aesthetic

---

## 📊 Sample Graph (Initial Seed)

```
u1 (Arjun) — u2, u3, u4, u10
u2 (Priya) — u1, u5, u6, u9
u3 (Rahul) — u1, u5, u7
u4 (Sneha) — u1, u8, u9, u12
...15 users, 24 edges total
```

**BFS from u1:** Finds u5, u6, u7, u8, u9, u12, u15 (Level 2)  
**DFS from u1:** Also reaches u11, u13, u14 (Level 3 via backtracking)
