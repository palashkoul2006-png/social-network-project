package com.socialnetwork.ds;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HashTable<K, V> {
  private static final int DEFAULT_CAPACITY = 16;
  private static final float LOAD_FACTOR = 0.75f;

  private List<Entry<K, V>>[] buckets;
  private int size;

  @SuppressWarnings("unchecked")
  public HashTable() {
    this.buckets = (List<Entry<K, V>>[]) new List[DEFAULT_CAPACITY];
  }

  public int size() {
    return size;
  }

  public boolean isEmpty() {
    return size == 0;
  }

  public boolean containsKey(K key) {
    return findEntry(key) != null;
  }

  public V get(K key) {
    Entry<K, V> entry = findEntry(key);
    return entry == null ? null : entry.value;
  }

  public V put(K key, V value) {
    ensureCapacity();
    int idx = indexFor(key, buckets.length);
    if (buckets[idx] == null) buckets[idx] = new ArrayList<>();

    for (Entry<K, V> entry : buckets[idx]) {
      if (Objects.equals(entry.key, key)) {
        V old = entry.value;
        entry.value = value;
        return old;
      }
    }

    buckets[idx].add(new Entry<>(key, value));
    size++;
    return null;
  }

  public V remove(K key) {
    int idx = indexFor(key, buckets.length);
    List<Entry<K, V>> bucket = buckets[idx];
    if (bucket == null) return null;

    for (int i = 0; i < bucket.size(); i++) {
      Entry<K, V> entry = bucket.get(i);
      if (Objects.equals(entry.key, key)) {
        V old = entry.value;
        bucket.remove(i);
        size--;
        return old;
      }
    }
    return null;
  }

  public void clear() {
    for (int i = 0; i < buckets.length; i++) buckets[i] = null;
    size = 0;
  }

  public List<K> keys() {
    List<K> out = new ArrayList<>(size);
    for (List<Entry<K, V>> bucket : buckets) {
      if (bucket == null) continue;
      for (Entry<K, V> entry : bucket) out.add(entry.key);
    }
    return out;
  }

  private Entry<K, V> findEntry(K key) {
    int idx = indexFor(key, buckets.length);
    List<Entry<K, V>> bucket = buckets[idx];
    if (bucket == null) return null;

    for (Entry<K, V> entry : bucket) {
      if (Objects.equals(entry.key, key)) return entry;
    }
    return null;
  }

  private void ensureCapacity() {
    if (size < buckets.length * LOAD_FACTOR) return;
    resize(buckets.length * 2);
  }

  @SuppressWarnings("unchecked")
  private void resize(int newCapacity) {
    List<Entry<K, V>>[] oldBuckets = buckets;
    buckets = (List<Entry<K, V>>[]) new List[newCapacity];
    int oldSize = size;
    size = 0;

    for (List<Entry<K, V>> bucket : oldBuckets) {
      if (bucket == null) continue;
      for (Entry<K, V> entry : bucket) put(entry.key, entry.value);
    }

    size = oldSize;
  }

  private int indexFor(K key, int capacity) {
    if (key == null) return 0;
    return Math.floorMod(key.hashCode(), capacity);
  }

  private static class Entry<K, V> {
    private final K key;
    private V value;

    private Entry(K key, V value) {
      this.key = key;
      this.value = value;
    }
  }
}
