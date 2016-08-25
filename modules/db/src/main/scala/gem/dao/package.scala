package gem

import doobie.imports._
import edu.gemini.spModel.core._
import scala.reflect.runtime.universe.TypeTag

import scalaz._, Scalaz._

package object dao extends MoreTupleOps {

  // Angle mapping to signed arcseconds. NOT implicit.
  val AngleMetaAsSignedArcseconds: Meta[Angle] =
    Meta[Double].xmap(Angle.fromArcsecs, _.toSignedDegrees * 3600)

  // OffsetP maps to a signed angle in arcseconds
  implicit val OffsetPMeta: Meta[OffsetP] =
    AngleMetaAsSignedArcseconds.xmap(OffsetP(_), _.toAngle)

  // OffsetQ maps to a signed angle in arcseconds
  implicit val OffsetQMeta: Meta[OffsetQ] =
    AngleMetaAsSignedArcseconds.xmap(OffsetQ(_), _.toAngle)

  // Program.Id as string
  implicit val ProgramIdMeta: Meta[Program.Id] =
    Meta[String].nxmap(Program.Id.parse, _.toString)

  // Observation.Id as string
  implicit val ObservationIdMeta: Meta[Observation.Id] =
    Meta[String].nxmap(Observation.Id.unsafeFromString, _.toString)

  // Enumerated by tag as string
  implicit def enumeratedMeta[A >: Null : TypeTag](implicit ev: Enumerated[A]): Meta[A] =
    Meta[String].nxmap[A](ev.unsafeFromTag(_), ev.tag(_))

  def capply2[A, B, T](f: (A, B) => T)(
    implicit ca: Composite[(Option[A], Option[B])]
  ): Composite[Option[T]] =
    ca.xmap(_.apply2(f), _ => sys.error("decode only"))

  def capply3[A, B, C, T](f: (A, B, C) => T)(
    implicit ca: Composite[(Option[A], Option[B], Option[C])]
  ): Composite[Option[T]] =
    ca.xmap(_.apply3(f), _ => sys.error("decode only"))

  def capply4[A, B, C, D, T](f: (A, B, C, D) => T)(
    implicit ca: Composite[(Option[A], Option[B], Option[C], Option[D])]
  ): Composite[Option[T]] =
    ca.xmap(_.apply4(f), _ => sys.error("decode only"))

}

trait MoreTupleOps {

  import scalaz._, Scalaz._

  implicit class MoreTuple2Ops[F[_], A, B](t: (F[A], F[B]))(implicit ev: Apply[F]) {
    def apply2[T](f: (A, B) => T): F[T] =
      t.fold(ev.lift2(f))
  }

  implicit class MoreTuple3Ops[F[_], A, B, C](t: (F[A], F[B], F[C]))(implicit ev: Apply[F]) {
    def apply3[T](f: (A, B, C) => T): F[T] =
      t.fold(ev.lift3(f))
  }

  implicit class MoreTuple4Ops[F[_], A, B, C, D](t: (F[A], F[B], F[C], F[D]))(implicit ev: Apply[F]) {
    def apply4[T](f: (A, B, C, D) => T): F[T] =
      t.fold(ev.lift4(f))
  }

}