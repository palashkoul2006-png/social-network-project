package com.socialnetwork.ds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TRIE — Prefix Search for Student Names
 * Complexity: insert O(L), search O(L), remove O(L)  where L = word length
 *
 * Each word in a name is tokenised so "Arjun Sharma" is findable by
 * "arj", "sha", etc.
 * Used by GET /search/name?q=<prefix>
 */
public class Trie {

  private static class TrieNode {
    final Map<Character, TrieNode> children = new HashMap<>();
    /** All user IDs whose name passes through this node. */
    final List<String> userIds = new ArrayList<>();
  }

  private final TrieNode root = new TrieNode();

  // ─── Insert ──────────────────────────────────────────────────────────────────

  /**
   * Insert a user.  Tokenises each word in the name so every word-prefix
   * maps to this userId.
   */
  public void insert(String userId, String name) {
    if (name == null || name.isBlank()) return;
    String[] words = name.toLowerCase().split("\\s+");
    for (String word : words) {
      TrieNode node = root;
      for (char ch : word.toCharArray()) {
        node.children.putIfAbsent(ch, new TrieNode());
        node = node.children.get(ch);
        if (!node.userIds.contains(userId)) node.userIds.add(userId);
      }
    }
  }

  // ─── Remove ──────────────────────────────────────────────────────────────────

  /**
   * Remove a user from all trie paths (future-proof: call on user deletion).
   */
  public void remove(String userId, String name) {
    if (name == null || name.isBlank()) return;
    String[] words = name.toLowerCase().split("\\s+");
    for (String word : words) {
      TrieNode node = root;
      for (char ch : word.toCharArray()) {
        if (!node.children.containsKey(ch)) break;
        node = node.children.get(ch);
        node.userIds.remove(userId);
      }
    }
  }

  // ─── Search ──────────────────────────────────────────────────────────────────

  /**
   * Return all unique user IDs matching a prefix query.
   * O(L + K)  where L = query length, K = result count.
   */
  public List<String> search(String prefix) {
    if (prefix == null || prefix.isBlank()) return List.of();
    TrieNode node = root;
    for (char ch : prefix.toLowerCase().toCharArray()) {
      if (!node.children.containsKey(ch)) return List.of();
      node = node.children.get(ch);
    }
    // LinkedHashSet preserves insertion order while deduplicating
    Set<String> unique = new LinkedHashSet<>(node.userIds);
    return new ArrayList<>(unique);
  }
}
