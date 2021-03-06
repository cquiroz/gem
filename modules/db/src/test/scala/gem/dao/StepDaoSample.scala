// Copyright (c) 2016-2017 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package gem.dao

import gem.{Location, Observation, Step}
import gem.config.DynamicConfig

import doobie.imports._

import scalaz.==>>

object StepDaoSample extends TimedSample {
  type Result = Location.Middle ==>> Step[DynamicConfig]

  val oid: Observation.Id = Observation.Id.unsafeFromString("GS-2016A-Q-102-108")

  override def runl(args: List[String]): ConnectionIO[Result] =
    StepDao.selectAll(oid)

  override def format(r: Result): String =
    r.toAscList.map { case (l, s) => f"$l%10s -> $s" }.mkString(", \n")
}
