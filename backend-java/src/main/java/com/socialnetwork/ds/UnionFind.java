package com.socialnetwork.ds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnionFind {
  private final Map<String, String> parent = new HashMap<>();
  private final Map<String, Integer> rank = new HashMap<>();

  public void add(String x) {
    parent.putIfAbsent(x, x);
    rank.putIfAbsent(x, 0);
  }

  public String find(String x) {
    add(x);
    if (parent.get(x).equals(x)) return x;
    parent.put(x, find(parent.get(x)));
    return parent.get(x);
  }

  public void union(String a, String b) {
    String ra = find(a);
    String rb = find(b);
    if (ra.equals(rb)) return;
    int rka = rank.get(ra);
    int rkb = rank.get(rb);
    if (rka < rkb) parent.put(ra, rb);
    else if (rka > rkb) parent.put(rb, ra);
    else {
      parent.put(rb, ra);
      rank.put(ra, rka + 1);
    }
  }

  public List<List<String>> communities() {
    Map<String, List<String>> groups = new HashMap<>();
    for (String node : parent.keySet()) {
      String root = find(node);
      groups.computeIfAbsent(root, k -> new ArrayList<>()).add(node);
    }
    return new ArrayList<>(groups.values());
  }
}
