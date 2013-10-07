package org.apache.mahout.sparkbindings.drm

import org.apache.mahout.math.{SparseMatrix, DenseMatrix, Matrix, Vector}
import scala.math._
import mahout.math._
import mahout.math.RLikeOps._
import scala.collection.JavaConversions._
import org.apache.spark.storage.StorageLevel
import org.apache.spark.rdd.RDD
import org.apache.hadoop.io.Writable

/**
 *
 * @author dmitriy
 */
class DRM[K <% Writable : ClassManifest](rdd: RDD[(K, Vector)] = null,
                                         _nrow: Long = -1L, _ncol: Int = -1,
                                         _cacheStorageLevel: StorageLevel = StorageLevel.MEMORY_AND_DISK) {

  private var _cached: RDD[(K, Vector)] = _


  private[sparkbindings] def getRDD: RDD[(K, Vector)] = if (_cached != null) _cached else rdd

  lazy val nrow = if (_nrow >= 0) _nrow else computeNRow
  lazy val ncol = if (_ncol >= 0) _ncol else computeNCol

  /**
   * persist matrix into cache (at most one time)
   *
   * @return self
   */
  def cached = {
    if (_cached == null) _cached = getRDD.persist(_cacheStorageLevel)
    this
  }

  /**
   * if matrix was previously persisted into cache,
   * delete cached representation
   */
  def uncache = {
    if ( _cached != null ) {
      // TODO: in the newer spark branch there's already the "unpersist" api.
      // use it when it is available to drop cache memory.
      _cached.unpersist(false)
      _cached = null
    }
  }

  def mapRows(mapfun: (K, Vector) => Vector): DRM[K] =
    new DRM[K](getRDD.map(t => (t._1, mapfun(t._1, t._2))))


  /**
   * Collecting DRM to fron-end in-core Matrix.
   *
   * If key in DRM is Int, then matrix is collected using key as row index.
   * Otherwise, order of rows in result is undefined but key.toString is applied
   * as rowLabelBindings of the in-core matrix .
   *
   * Note that this pre-allocates target matrix and then assigns collected RDD to it
   * thus this likely would require about 2 times the RDD memory
   * @return
   */
  def collect: Matrix = {
    val rdd = getRDD

    val intRowIndices = implicitly[ClassManifest[K]] == classManifest[Int]

    val cols = rdd.map(_._2.length).fold(0)(max(_, _))
    val rows = if (intRowIndices) rdd.map(_._1.asInstanceOf[Int]).fold(-1)(max(_, _)) + 1 else rdd.count().toInt

    // since currently spark #collect() requires Serializeable support,
    // we serialize DRM vectors into byte arrays on backend and restore Vector
    // instances on the front end:
    val data = rdd.map(t => (t._1, t._2.toByteArray)).collect().map(t => (t._1, DRMVectorOps.fromByteArray(t._2)))


    val m = if (data.forall(_._2.isDense))
      new DenseMatrix(rows, cols)

    else
      new SparseMatrix(rows, cols)

    if (intRowIndices)
      data.foreach(t => m(t._1.asInstanceOf[Int], ::) := t._2)
    else {

      // assign all rows sequentially
      val d = data.zipWithIndex
      d.foreach(t => m(t._2, ::) := t._1._2)

      // row bindings
      val rowBindings = d.map(t => (t._1._1.toString, t._2: java.lang.Integer)).toMap

      m.setRowLabelBindings(rowBindings)
    }

    m
  }

  protected def computeNRow = {
    val intRowIndex = classManifest[K] == classManifest[Int]

    if (intRowIndex)
      cached.getRDD.map(_._1.asInstanceOf[Int]).fold(-1)(max(_, _)) + 1L
    else
      cached.getRDD.count()
  }

  protected def computeNCol =
    cached.getRDD.map(_._2.length).fold(-1)(max(_, _))


}
