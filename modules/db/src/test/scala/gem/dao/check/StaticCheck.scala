// Copyright (c) 2016-2017 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package gem.dao
package check

import gem.enum.Instrument.Flamingos2

class StaticCheck extends Check {
  import StaticConfigDao.Statements._

  "StaticDao.Statements" should
            "selectF2"        in check(selectF2(0))
  it should "insertBaseSlice" in check(insertBaseSlice(Flamingos2))
  it should "insertF2"        in check(insertF2(0, Dummy.f2Static))
}