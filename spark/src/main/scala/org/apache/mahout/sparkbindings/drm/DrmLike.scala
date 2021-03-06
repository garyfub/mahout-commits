/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mahout.sparkbindings.drm

import org.apache.spark.storage.StorageLevel

/**
 *
 * Basic spark DRM trait.
 *
 * Since we already call the package "sparkbindings", I will not use stem "spark" with classes in
 * this package. Spark backing is already implied.
 *
 */
trait DrmLike[K] {

  private[sparkbindings] def partitioningTag:Long

  /** R-like syntax for number of rows. */
  def nrow: Long

  /** R-like syntax for number of columns */
  def ncol: Int

  /**
   * Action operator -- does not necessary means Spark action; but does mean running BLAS optimizer
   * and writing down Spark graph lineage since last checkpointed DRM.
   */
  def checkpoint(cacheHint: CacheHint.CacheHint = CacheHint.MEMORY_ONLY): CheckpointedDrm[K]

}
