package org.brail.jwhat.stream;

import java.util.ArrayDeque;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

public class QueueWithSize<T> {
  private final ArrayDeque<Entry<T>> queue = new ArrayDeque<>();
  private double totalSize;

  public boolean isEmpty() {
    return queue.isEmpty();
  }

  public double getTotalSize() {
    return totalSize;
  }

  public T dequeue() {
    var val = queue.removeFirst();
    totalSize = Math.max(totalSize - val.size, 0.0);
    return val.value;
  }

  public void enqueue(Context cx, Scriptable scope, T val, double size) {
    if (!Double.isFinite(size) || size < 0.0) {
      throw ScriptRuntime.rangeError("Invalid size");
    }
    queue.addLast(new Entry<>(val, size));
    totalSize += size;
  }

  public T peek() {
    assert !queue.isEmpty();
    return queue.peekFirst().value;
  }

  public void reset() {
    queue.clear();
    totalSize = 0.0;
  }

  private static final class Entry<T> {
    T value;
    double size;

    Entry(T value, double size) {
      this.value = value;
      this.size = size;
    }
  }
}
