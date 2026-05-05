package com.socialnetwork.ds;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;

/**
 * Graph Coloring using backtracking.
 * Assigns a color (0..numColors-1) to each node such that adjacent nodes have different colors.
 * Returns a Map<userId, Integer> where the integer is the assigned color.
 */
public class GraphColoring {
    private final Map<String, Map<String, Integer>> adjacency;
    private final int numColors;
    private final Map<String, Integer> colors = new HashMap<>();
    private final String[] nodes;

    public GraphColoring(Map<String, Map<String, Integer>> adjacency, int numColors) {
        this.adjacency = adjacency;
        this.numColors = numColors;
        this.nodes = adjacency.keySet().toArray(new String[0]);
    }

    public Map<String, Integer> color() {
        if (backtrack(0)) {
            return colors;
        }
        return null; // no valid coloring
    }

    private boolean backtrack(int idx) {
        if (idx == nodes.length) {
            return true; // all nodes colored
        }
        String node = nodes[idx];
        for (int c = 0; c < numColors; c++) {
            if (isSafe(node, c)) {
                colors.put(node, c);
                if (backtrack(idx + 1)) {
                    return true;
                }
                colors.remove(node);
            }
        }
        return false; // no color works
    }

    private boolean isSafe(String node, int color) {
        Map<String, Integer> neighbors = adjacency.getOrDefault(node, Map.of());
        for (String nb : neighbors.keySet()) {
            Integer assigned = colors.get(nb);
            if (assigned != null && assigned == color) {
                return false;
            }
        }
        return true;
    }
}
