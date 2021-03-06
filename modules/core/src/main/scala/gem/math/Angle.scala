// Copyright (c) 2016-2017 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package gem.math

import scalaz.{ Equal, Monoid, Show }
import scalaz.std.anyVal.longInstance
import scalaz.syntax.equal._

/**
 * Exact angles represented as integral microarcseconds. These values form an Abelian group over
 * addition, where the inverse is reflection around the 0-180° axis. The subgroup of angles where
 * integral microarcseconds correspond with clock microseconds (i.e., where they are evenly
 * divisible by 15 microarcseconds) is represented by the HourAgle subtype.
 * @param toMicroarcseconds This angle in microarcseconds. Exact.
 */
sealed class Angle protected (val toMicroarcseconds: Long) {

  // Sanity checks … should be correct via the companion constructor.
  assert(toMicroarcseconds >= 0, s"Invariant violated. $toMicroarcseconds is negative.")
  assert(toMicroarcseconds < 360L * 60L * 60L * 1000L * 1000L, s"Invariant violated. $toMicroarcseconds is >= 360°.")

  /** Flip this angle 180°. Exact, invertible. */
  def flip: Angle =
    this + Angle.Angle180

  /** Additive inverse of this angle (by mirroring around the 0-180 axis). Exact, invertible. */
  def unary_- : Angle =
    Angle.fromMicroarcseconds(-toMicroarcseconds.toLong)

  /** Signed microarcseconds, in [-180°, 180°). */
  def toSignedMicroarcseconds: Long = {
    val µas360 = Angle.Angle180.toMicroarcseconds * 2L
    if (toMicroarcseconds >= Angle.Angle180.toMicroarcseconds) toMicroarcseconds - µas360
    else toMicroarcseconds
  }

  /** This angle in decimal degrees. Approximate, non-invertible */
  def toDoubleDegrees: Double =
    toMicroarcseconds.toDouble / (60.0 * 60.0 * 1000.0 * 1000.0)

  /** This angle in decimal radisns. Approximate, non-invertible */
  def toDoubleRadians: Double =
    toDoubleDegrees.toRadians

  /**
   * Convert to the closest hour angle by rounding down to the closest 15 milliarcseconds.
   * Exact, non-invertible.
   */
  def toHourAngle: HourAngle =
    HourAngle.fromMicroseconds(toMicroarcseconds.toLong / 15L)

  /**
   * Convert to the closest hour angle iff its magnitude is an even multiple of 15 milliarcseconds.
   * Exact and invertible where defined.
   */
  def toHourAngleExact: Option[HourAngle] =
    if (toMicroarcseconds % 15L === 0L) Some(toHourAngle) else None

  /**
   * Destructure this value into a sum of degrees, arcminutes, arcseconds, milliarcseconds, and
   * microseconds. Exact, invertible via `Angle.fromDMS`.
   */
  def toDMS: Angle.DMS =
    Angle.DMS(this)

  /** Sum of this angle and `a`. Exact, commutative, invertible. */
  def +(a: Angle): Angle =
    Angle.fromMicroarcseconds(toMicroarcseconds + a.toMicroarcseconds)

  /** Difference of this angle and `a`. Exact, invertible. */
  def -(a: Angle): Angle =
    Angle.fromMicroarcseconds(toMicroarcseconds - a.toMicroarcseconds)

  /** String representation of this Angle, for debugging purposes only. */
  override def toString =
    f"Angle($toDMS, $toDoubleDegrees%1.10f°)"

  /** Angles are equal if their magnitudes are equal. Exact. */
  override final def equals(a: Any) =
    a match {
      case a: Angle => a.toMicroarcseconds === toMicroarcseconds
      case _        => false
    }

  override final def hashCode =
    toMicroarcseconds.toInt

}

object Angle {

  val Angle0:   Angle = fromDegrees(0)
  val Angle90:  Angle = fromDegrees(90)
  val Angle180: Angle = fromDegrees(180)
  val Angle270: Angle = fromDegrees(270)

  /** Construct a new Angle of the given magnitude in integral microarcseconds, modulo 360°. Exact. */
  def fromMicroarcseconds(µas: Long): Angle = {
    val µasPer360 = 360L * 60L * 60L * 1000L * 1000L
    val µasʹ = (((µas % µasPer360) + µasPer360) % µasPer360)
    new Angle(µasʹ)
  }

  /** Construct a new Angle of the given magnitude in integral arcseconds, modulo 360°. Exact. */
  def fromMilliarcseconds(as: Int): Angle =
    fromMicroarcseconds(as.toLong * 1000L)

  /** Construct a new Angle of the given magnitude in integral arcseconds, modulo 360°. Exact. */
  def fromArcseconds(as: Int): Angle =
    fromMilliarcseconds(as * 1000)

  /** Construct a new Angle of the given magnitude in integral arcminutes, modulo 360°. Exact. */
  def fromArcminutes(ms: Int): Angle =
    fromArcseconds(ms * 60)

  /** Construct a new Angle of the given magnitude in integral degrees, modulo 360°. Exact. */
  def fromDegrees(ms: Int): Angle =
    fromArcminutes(ms * 60)

  /** Construct a new Angle of the given magnitude in double degrees, modulo 360°. Approximate. */
  def fromDoubleDegrees(ds: Double): Angle =
    fromMicroarcseconds((ds * 60 * 60 * 1000 * 1000).toLong)

  /** Construct a new Angle of the given magnitude in double arcseconds, modulo 360°. Approximate. */
  def fromDoubleArcseconds(as: Double): Angle =
    fromMicroarcseconds((as * 1000 * 1000).toLong)

  /** Construct a new Angle of the given magnitude in radians, modulo 2π. Approximate. */
  def fromDoubleRadians(rad: Double): Angle =
    fromDoubleDegrees(rad.toDegrees)

  /** Angle is an Abelian group, but monoid is the best we can do for now. */
  implicit val AngleMonoid: Monoid[Angle] =
    Monoid.instance(_ + _, Angle.Angle0)

  implicit val AngleShow: Show[Angle] =
    Show.showA

  /** Angles are equal if their magnitudes are equal. */
  implicit val AngleEqual: Equal[Angle] =
    Equal.equalBy(_.toMicroarcseconds)

  // This works for both DMS and HMS so let's just do it once.
  protected[math] def toMicrosexigesimal(micros: Long): (Int, Int, Int, Int, Int) = {
    val µs =  micros                               % 1000L
    val ms = (micros / (1000L))                    % 1000L
    val s  = (micros / (1000L * 1000L))            % 60L
    val m  = (micros / (1000L * 1000L * 60L))      % 60L
    val d  = (micros / (1000L * 1000L * 60L * 60L))
    (d.toInt, m.toInt, s.toInt, ms.toInt, µs.toInt)
  }

  /**
   * Integral angle represented as a sum of degrees, arcminutes, arcseconds, milliarcseconds and
   * microarcseconds. This type is exact and isomorphic to Angle.
   */
  final case class DMS(toAngle: Angle) {
    val (
      degrees: Int,
      arcminutes: Int,
      arcseconds: Int,
      milliarcseconds: Int,
      microarcseconds: Int
    ) = Angle.toMicrosexigesimal(toAngle.toMicroarcseconds)
    override final def toString =
      f"$degrees:$arcminutes%02d:$arcseconds%02d.$milliarcseconds%03d$microarcseconds%03d"
  }

  /**
   * Construct a new Angle of the given magnitude as a sum of degrees, arcminutes, arcseconds,
   * milliarcseconds, and microarcseconds. Exact modulo 360°.
   */
  def fromDMS(
    degrees:         Int,
    arcminutes:      Int,
    arcseconds:      Int,
    milliarcseconds: Int,
    microarcseconds: Int
  ): Angle =
    fromMicroarcseconds(
      microarcseconds.toLong +
      milliarcseconds.toLong * 1000 +
      arcseconds.toLong      * 1000 * 1000 +
      arcminutes.toLong      * 1000 * 1000 * 60 +
      degrees.toLong         * 1000 * 1000 * 60 * 60
    )

}



/**
 * Exact hour angles represented as integral microseconds. These values form an Abelian group over
 * addition, where the inverse is reflection around the 0-12h axis. This is a subgroup of the
 * integral Angles where microarcseconds are evenly divisible by 15.
 */
final class HourAngle private (µas: Long) extends Angle(µas) {

  // Sanity checks … should be correct via the companion constructor.
  assert(toMicroarcseconds %  15 === 0, s"Invariant violated. $µas isn't divisible by 15.")

  /** Forget this is an HourAngle. */
  def toAngle: Angle =
    this

  /**
   * Flip this HourAngle by 12h. This is logically identical to the superclass implementation
   * and serves only to refine the return type. Exact, invertible.
   */
  override def flip: HourAngle =
    this + HourAngle.HourAngle12

  /**
   * Additive inverse of this HourAngle (by mirroring around the 0-12h axis). This is logically
   * identical to the superclass implementation and serves only to refine the return type. Exact,
   * invertible.
   */
  override def unary_- : HourAngle =
    HourAngle.fromMicroseconds(-toMicroseconds.toLong)

  // Overridden for efficiency
  override def toHourAngle = this
  override def toHourAngleExact = Some(this)

  // Define in terms of toMicroarcseconds to avoid a second member
  def toMicroseconds: Long =
    toMicroarcseconds / 15

  def toHMS: HourAngle.HMS =
    HourAngle.HMS(this)

  /** Sum of this HourAngle and `ha`. Exact, commutative, invertible. */
  @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
  def +(ha: HourAngle): HourAngle =
    HourAngle.fromMicroseconds(toMicroseconds.toLong + ha.toMicroseconds.toLong)

  /** Difference of this HourAngle and `ha`. Exact, invertible. */
  @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
  def -(ha: HourAngle): HourAngle =
    HourAngle.fromMicroseconds(toMicroseconds.toLong - ha.toMicroseconds.toLong)

  /** String representation of this HourAngle, for debugging purposes only. */
  override def toString =
    f"HourAngle($toDMS, $toHMS, $toDoubleDegrees%1.9f°)"

}

object HourAngle {

  val HourAngle0 : HourAngle = fromMicroseconds(0)
  val HourAngle12: HourAngle = fromHours(12)

  /** Construct a new Angle of the given magnitude in integral microseconds, modulo 24h. Exact. */
  def fromMicroseconds(µs: Long): HourAngle = {
    val µsPer24 = 24L * 60L * 60L * 1000L * 1000L
    val µsʹ = (((µs % µsPer24) + µsPer24) % µsPer24)
    new HourAngle(µsʹ * 15L)
  }

  /** Construct a new HourAngle of the given magnitude in integral milliseconds, modulo 24h. Exact. */
  def fromMilliseconds(milliseconds: Int): HourAngle =
    fromMicroseconds(milliseconds.toLong * 1000L)

  /** Construct a new HourAngle of the given magnitude in integral seconds, modulo 24h. Exact. */
  def fromSeconds(seconds: Int): HourAngle =
    fromMilliseconds(seconds * 1000)

  /** Construct a new HourAngle of the given magnitude in integral minutes, modulo 24h. Exact. */
  def fromMinutes(minutes: Int): HourAngle =
    fromSeconds(minutes * 60)

  /** Construct a new HourAngle of the given magnitude in integral hours, modulo 24h. Exact. */
  def fromHours(hours: Int):     HourAngle =
    fromMinutes(hours * 60)

  def fromDoubleHours(hs: Double): HourAngle =
    fromMicroseconds((hs * 60.0 * 60.0 * 1000.0).toLong)

  /**
   * Construct a new HourAngle of the given magnitude as a sum of hours, minutes, seconds,
   * milliseconds, and microseconds. Exact modulo 24h.
   */
  def fromHMS(hours: Int, minutes: Int, seconds: Int, milliseconds: Int, microseconds: Int): HourAngle =
    fromMicroseconds(
      microseconds.toLong +
      milliseconds.toLong * 1000L +
      seconds.toLong      * 1000L * 1000L +
      minutes.toLong      * 1000L * 1000L * 60L +
      hours.toLong        * 1000L * 1000L * 60L * 60L
    )

  /** HourAngle is an Abelian group (a subgroup of Angle), but monoid is the best we can do for now. */
  implicit val HourAngleMonoid: Monoid[HourAngle] =
    Monoid.instance(_ + _, HourAngle.HourAngle0)

  implicit val HourAngleShow: Show[HourAngle] =
    Show.showA

  /** Angles are equal if their magnitudes are equal. */
  implicit val HourAngleEqual: Equal[HourAngle] =
    Equal.equalBy(_.toMicroarcseconds)

  /**
   * Integral hour angle represented as a sum of hours, minutes, seconds, milliseconds, and
   * microseconds. This type is exact and isomorphic to HourAngle.
   */
  final case class HMS(toHourAngle: HourAngle) {
    def toAngle: Angle = toHourAngle // forget it's an hour angle
    val (
      hours: Int,
      minutes: Int,
      seconds: Int,
      milliseconds: Int,
      microseconds: Int
    ) = Angle.toMicrosexigesimal(toHourAngle.toMicroseconds)
    override final def toString =
      f"$hours:$minutes%02d:$seconds%02d.$milliseconds%03d$microseconds%03d"
  }

}
