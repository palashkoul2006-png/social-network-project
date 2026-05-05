package com.socialnetwork.ds;

import java.util.HashMap;
import java.util.Map;

/**
 * LRU CACHE — Least-Recently-Used eviction
 * Implementation: HashMap + doubly-linked list (manual, mirrors lru-cache.js exactly)
 * get / put / invalidate: O(1) amortized
 *
 * Used to cache getTopKRecommendations results per userId.
 * Invalidated when that user adds or removes a friendship.
 */
public class LruCache<K, V> {

  // ─── Doubly-Linked-List node ────────────────────────────────────────────────
  private static class DLLNode<K, V> {
    K key;
    V value;
    DLLNode<K, V> prev, next;

    DLLNode(K key, V value) {
      this.key   = key;
      this.value = value;
    }
  }

  // ─── Fields ─────────────────────────────────────────────────────────────────
  private final int capacity;
  private final Map<K, DLLNode<K, V>> map = new HashMap<>();

  /** Sentinel head = LRU end, sentinel tail = MRU end. Never stored in map. */
  private final DLLNode<K, V> head = new DLLNode<>(null, null);
  private final DLLNode<K, V> tail = new DLLNode<>(null, null);

  public LruCache(int capacity) {
    this.capacity = capacity;
    head.next = tail;
    tail.prev = head;
  }

  // ─── Public API ─────────────────────────────────────────────────────────────

  /** Returns value or null; marks entry as recently used. O(1). */
  public V get(K key) {
    DLLNode<K, V> node = map.get(key);
    if (node == null) return null;
    remove(node);
    insertAtTail(node);   // mark as most-recently-used
    return node.value;
  }

  /** Inserts or updates a key-value pair. Evicts LRU entry when full. O(1). */
  public void put(K key, V value) {
    if (map.containsKey(key)) {
      DLLNode<K, V> node = map.get(key);
      node.value = value;
      remove(node);
      insertAtTail(node);
    } else {
      if (map.size() >= capacity) {
        // Evict least-recently-used (node right after sentinel head)
        DLLNode<K, V> lru = head.next;
        remove(lru);
        map.remove(lru.key);
      }
      DLLNode<K, V> node = new DLLNode<>(key, value);
      insertAtTail(node);
      map.put(key, node);
    }
  }

  /** Returns true if the key exists (does NOT update access order). */
  public boolean containsKey(K key) {
    return map.containsKey(key);
  }

  /** Invalidate a single key — call after friendship change. O(1). */
  public void remove(K key) {
    DLLNode<K, V> node = map.remove(key);
    if (node != null) remove(node);
  }

  public int size() { return map.size(); }

  // ─── Private helpers ─────────────────────────────────────────────────────────

  private void remove(DLLNode<K, V> node) {
    node.prev.next = node.next;
    node.next.prev = node.prev;
  }

  private void insertAtTail(DLLNode<K, V> node) {
    node.prev       = tail.prev;
    node.next       = tail;
    tail.prev.next  = node;
    tail.prev       = node;
  }
}
