package com.store.jobsboard.http.routes

class UnionFind(size: Int) {
  private val root: Array[Int] = Array.tabulate(size)(identity)

  def find(x: Int): Int = root(x)

  def union(x: Int, y: Int): Unit = {
    val rootX = find(x)
    val rootY = find(y)
    if (rootX != rootY) {
      for (i <- root.indices) {
        if (root(i) == rootY) {
          root(i) = rootX
        }
      }
    }
  }

  def connected(x: Int, y: Int): Boolean = find(x) == find(y)
}

object App extends App {
  val uf = new UnionFind(10)
  
  // 1-2-5-6-7 3-8-9 4
  uf.union(1, 2)
  uf.union(2, 5)
  uf.union(5, 6)
  uf.union(6, 7)
  uf.union(3, 8)
  uf.union(8, 9)
  
  println(uf.connected(1, 5)) // true
  println(uf.connected(5, 7)) // true
  println(uf.connected(4, 9)) // false
  
  // 1-2-5-6-7 3-8-9-4
  uf.union(9, 4)
  println(uf.connected(4, 9)) // true
}