package mahout.math

import org.apache.mahout.math.Vector
import scala.collection.JavaConversions._
import org.apache.mahout.math.function.Functions

/**
 * Syntactic sugar for mahout vectors
 * @param v Mahout vector
 */
class VectorOps(val v: Vector) {

  def apply(i: Int) = v.get(i)

  def apply(r: Range) = v.viewPart(r.start, r.length)

  def sum = v.zSum()

  def :=(that: Vector): Vector = {

    // assign op in Mahout requires same
    // cardinality between vectors .
    // we want to relax it here and require
    // v to have _at least_ as large cardinality
    // as "that".
    if (that.length == length)
      v.assign(that)

    else if (that.length < length) {
      that.nonZeroes().foreach(t => v.setQuick(t.index, t.get))
      v

    } else throw new IllegalArgumentException("assignment argument vector longer than assignee")
  }

  def +=(that: Vector) = v.assign(that, Functions.PLUS)

  def +=(that: Double) = v.assign(Functions.PLUS, that)

  def +(that: Vector) = cloned += that

  def +(that: Double) = cloned += that

  /**
   * Hadamard
   * @param that
   */
  def *=(that: Vector) = v.assign(that, Functions.MULT)

  def *(that: Vector) = cloned *= that

  def *=(that: Double) = v.assign(Functions.MULT, that)

  def *(that: Double) = cloned *= that

  def length = v.size()

  def cloned = v.like := v

}
