package com.socialnetwork.ds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * MIN-HEAP PRIORITY QUEUE
 * =======================
 * Direct Java conversion of priority-queue.js.
 * Generic min-heap backed by an ArrayList.
 * Operations:
 *   push  — O(log N)
 *   pop   — O(log N)
 *   peek  — O(1)
 *   size  — O(1)
 */
public class MinHeap<T> {
  private final List<T> heap = new ArrayList<>();
  private final Comparator<T> comparator;

  public MinHeap(Comparator<T> comparator) {
    this.comparator = comparator;
  }

  /** Insert a value and restore the heap property upward. */
  public void push(T val) {
    heap.add(val);
    bubbleUp(heap.size() - 1);
  }

  /** Remove and return the minimum element (heap[0]). */
  public T pop() {
    if (heap.isEmpty()) return null;
    if (heap.size() == 1) return heap.remove(0);
    T top = heap.get(0);
    heap.set(0, heap.remove(heap.size() - 1));
    bubbleDown(0);
    return top;
  }

  /** Peek at the minimum without removing. */
  public T peek() {
    return heap.isEmpty() ? null : heap.get(0);
  }

  public int size() { return heap.size(); }
  public boolean isEmpty() { return heap.isEmpty(); }

  private void bubbleUp(int idx) {
    while (idx > 0) {
      int parent = (idx - 1) / 2;
      if (comparator.compare(heap.get(idx), heap.get(parent)) >= 0) break;
      swap(idx, parent);
      idx = parent;
    }
  }

  private void bubbleDown(int idx) {
    int len = heap.size();
    while (true) {
      int left = idx * 2 + 1;
      int right = idx * 2 + 2;
      int smallest = idx;

      if (left < len && comparator.compare(heap.get(left), heap.get(smallest)) < 0) smallest = left;
      if (right < len && comparator.compare(heap.get(right), heap.get(smallest)) < 0) smallest = right;
      if (smallest == idx) break;

      swap(idx, smallest);
      idx = smallest;
    }
  }

  private void swap(int i, int j) {
    T tmp = heap.get(i);
    heap.set(i, heap.get(j));
    heap.set(j, tmp);
  }
}
