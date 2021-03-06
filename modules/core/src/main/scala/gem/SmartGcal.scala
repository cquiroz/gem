// Copyright (c) 2016-2017 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package gem

import gem.config.DynamicConfig

object SmartGcal {

  sealed trait ExpansionError extends Product with Serializable

  type ExpandedSteps      = List[GcalStep[DynamicConfig]]

  final case class StepNotFound(loc: Location.Middle) extends ExpansionError
  case object      NotSmartGcal                       extends ExpansionError
  case object      NoMappingDefined                   extends ExpansionError

  def stepNotFound(loc: Location.Middle): ExpansionError = StepNotFound(loc)
  val notSmartGcal: ExpansionError                       = NotSmartGcal
  val noMappingDefined: ExpansionError                   = NoMappingDefined
}
