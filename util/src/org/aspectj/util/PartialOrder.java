/* *******************************************************************
 * Copyright (c) 1999-2001 Xerox Corporation, 
 *               2002 Palo Alto Research Center, Incorporated (PARC).
 * All rights reserved. 
 * This program and the accompanying materials are made available 
 * under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors: 
 *     Xerox/PARC     initial implementation 
 * ******************************************************************/

package org.aspectj.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This class implements a partial order
 * <p/>
 * It includes routines for doing a topo-sort
 */

public final class PartialOrder {

  /**
   * @param objects must all implement PartialComparable
   * @return the same members as objects, but sorted according to their partial order. returns null if the objects are cyclical
   */
  @Nullable
  public static <T extends PartialComparable> List<T> sort(@NotNull List<T> objects) {
    // lists of size 0 or 1 don't need any sorting
    if (objects.size() < 2) {
      return objects;
    }

    // ??? we might want to optimize a few other cases of small size

    // ??? I don't like creating this data structure, but it does give good
    // ??? separation of concerns.
    final List<SortObject<T>> sortList = new LinkedList<>(); // objects.size());
    for (final Iterator<T> i = objects.iterator(); i.hasNext(); ) {
      addNewPartialComparable(sortList, i.next());
    }

    // System.out.println(sortList);

    // now we have built our directed graph
    // use a simple sort algorithm from here
    // can increase efficiency later
    // List ret = new ArrayList(objects.size());
    final int N = objects.size();
    for (int index = 0; index < N; index++) {
      // System.out.println(sortList);
      // System.out.println("-->" + ret);

      SortObject<T> leastWithNoSmallers = null;

      for (final Iterator<SortObject<T>> i = sortList.iterator(); i.hasNext(); ) {
        final SortObject<T> so = i.next();
        // System.out.println(so);
        if (so.hasNoSmallerObjects()) {
          if (leastWithNoSmallers == null || so.object.fallbackCompareTo(leastWithNoSmallers.object) < 0) {
            leastWithNoSmallers = so;
          }
        }
      }

      if (leastWithNoSmallers == null) {
        return null;
      }

      removeFromGraph(sortList, leastWithNoSmallers);
      objects.set(index, leastWithNoSmallers.object);
    }

    return objects;
  }

  private static <T extends PartialComparable> void addNewPartialComparable(@NotNull List<SortObject<T>> graph, @NotNull T o) {
    final SortObject<T> so = new SortObject<>(o);
    for (final Iterator<SortObject<T>> i = graph.iterator(); i.hasNext(); ) {
      final SortObject<T> other = i.next();
      so.addDirectedLinks(other);
    }
    graph.add(so);
  }

  private static <T extends PartialComparable> void removeFromGraph(@NotNull List<SortObject<T>> graph, @NotNull SortObject<T> o) {
    for (final Iterator<SortObject<T>> i = graph.iterator(); i.hasNext(); ) {
      final SortObject<T> other = i.next();

      if (o == other) {
        i.remove();
      }
      // ??? could use this to build up a new queue of objects with no
      // ??? smaller ones
      other.removeSmallerObject(o);
    }
  }

  /**
   * All classes that want to be part of a partial order must implement PartialOrder.PartialComparable.
   */
  public static interface PartialComparable {
    /**
     * @return <ul>
     * <li>+1 if this is greater than other</li>
     * <li>-1 if this is less than other</li>
     * <li>0 if this is not comparable to other</li>
     * </ul>
     * <p/>
     * <b> Note: returning 0 from this method doesn't mean the same thing as returning 0 from
     * java.util.Comparable.compareTo()</b>
     */
    public int compareTo(@NotNull Object other);

    /**
     * This method can provide a deterministic ordering for elements that are strictly not comparable. If you have no need for
     * this, this method can just return 0 whenever called.
     */
    public int fallbackCompareTo(@NotNull Object other);
  }

  private static final class SortObject<T extends PartialComparable> {
    @NotNull
    final T object;
    @NotNull
    final List<SortObject<T>> smallerObjects = new LinkedList<>();
    @NotNull
    final List<SortObject<T>> biggerObjects = new LinkedList<>();

    public SortObject(@NotNull T o) {
      object = o;
    }

    public String toString() {
      return object.toString(); // +smallerObjects+biggerObjects;
    }

    boolean hasNoSmallerObjects() {
      return smallerObjects.isEmpty();
    }

    boolean removeSmallerObject(@NotNull SortObject<T> o) {
      smallerObjects.remove(o);
      return hasNoSmallerObjects();
    }

    void addDirectedLinks(@NotNull SortObject<T> other) {
      final int cmp = object.compareTo(other.object);
      if (cmp == 0) {
        return;
      }
      if (cmp > 0) {
        this.smallerObjects.add(other);
        other.biggerObjects.add(this);
      } else {
        this.biggerObjects.add(other);
        other.smallerObjects.add(this);
      }
    }
  }

  /**
   * ********************************************************************************
   * /* a minimal testing harness
   * *********************************************************************************
   */
  static final class Token implements PartialComparable {
    @NotNull
    private final String s;

    Token(@NotNull String s) {
      this.s = s;
    }

    @Override
    public int compareTo(@NotNull Object other) {
      final Token t = (Token) other;

      final int cmp = s.charAt(0) - t.s.charAt(0);
      if (cmp == 1) {
        return 1;
      }
      if (cmp == -1) {
        return -1;
      }
      return 0;
    }

    @Override
    public int fallbackCompareTo(@NotNull Object other) {
      return -s.compareTo(((Token) other).s);
    }

    public String toString() {
      return s;
    }
  }

  public static void main(@NotNull String[] args) {
    final List<Token> l = new ArrayList<Token>();
    l.add(new Token("a1"));
    l.add(new Token("c2"));
    l.add(new Token("b3"));
    l.add(new Token("f4"));
    l.add(new Token("e5"));
    l.add(new Token("d6"));
    l.add(new Token("c7"));
    l.add(new Token("b8"));

    l.add(new Token("z"));
    l.add(new Token("x"));

    l.add(new Token("f9"));
    l.add(new Token("e10"));
    l.add(new Token("a11"));
    l.add(new Token("d12"));
    l.add(new Token("b13"));
    l.add(new Token("c14"));

    System.out.println(l);

    sort(l);

    System.out.println(l);
  }
}
