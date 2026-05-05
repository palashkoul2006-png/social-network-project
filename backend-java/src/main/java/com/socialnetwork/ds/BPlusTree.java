package com.socialnetwork.ds;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class BPlusTree {
  private final TreeMap<Integer, List<String>> index = new TreeMap<>();

  public void insert(int key, String value) {
    index.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
  }

  public List<String> rangeQuery(int min, int max) {
    List<String> result = new ArrayList<>();
    for (Map.Entry<Integer, List<String>> entry : index.subMap(min, true, max, true).entrySet()) {
      result.addAll(entry.getValue());
    }
    return result;
  }
}
