// Copyright (c) 2016-2017 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package gem.ocs2

import gem._
import gem.config._
import gem.enum._
import gem.math.Offset
import gem.ocs2.pio._

import java.time.Duration

import scala.xml.Node
import scalaz._
import Scalaz._

/** Decoder for the OCS2 sequence XML.
  */
object SequenceDecoder extends PioDecoder[List[Step[DynamicConfig]]] {

  def parseInstConfig(i: Instrument, cm: ConfigMap): PioError \/ DynamicConfig =
    i match {
      case Instrument.AcqCam     => AcqCamDynamicConfig()         .right
      case Instrument.Bhros      => BhrosDynamicConfig()          .right

      case Instrument.Flamingos2 => f2Dynamic(cm)
      case Instrument.GmosN      => gmosNorthDynamic(cm)
      case Instrument.GmosS      => gmosSouthDynamic(cm)

      case Instrument.Gnirs      => GnirsDynamicConfig()          .right
      case Instrument.Gpi        => GpiDynamicConfig()            .right
      case Instrument.Gsaoi      => GsaoiDynamicConfig()          .right
      case Instrument.Michelle   => MichelleDynamicConfig()       .right
      case Instrument.Nici       => NiciDynamicConfig()           .right
      case Instrument.Nifs       => NifsDynamicConfig()           .right
      case Instrument.Niri       => NiriDynamicConfig()           .right
      case Instrument.Phoenix    => PhoenixDynamicConfig()        .right
      case Instrument.Trecs      => TrecsDynamicConfig()          .right
      case Instrument.Visitor    => VisitorDynamicConfig()        .right
    }

  private def f2Dynamic(cm: ConfigMap): PioError \/ DynamicConfig = {
    import Legacy.Instrument.Flamingos2._
    for {
      d <- Disperser.parse(cm)
      e <- Legacy.Observe.ExposureTime.cparseOrElse(cm, Duration.ofMillis(0))
      f <- Filter.parse(cm)
      u <- Fpu.parse(cm)
      l <- LyotWheel.parse(cm)
      r <- ReadMode.parse(cm)
      w <- WindowCover.cparseOrElse(cm, F2WindowCover.Close)
    } yield F2DynamicConfig(d, e, f, u, l, r, w)
  }

  private def gmosCommonDynamic(cm: ConfigMap): PioError \/ Gmos.GmosCommonDynamicConfig = {
    import Legacy.Instrument.Gmos._

    for {
      x  <- XBinning.parse(cm)
      y  <- YBinning.parse(cm)
      ac <- AmpCount.parse(cm)
      ag <- AmpGain.parse(cm)
      ar <- AmpReadMode.parse(cm)
      dx <- Dtax.parse(cm)
      e  <- Legacy.Observe.ExposureTime.cparseOrElse(cm, Duration.ofMillis(0))
    } yield Gmos.GmosCommonDynamicConfig(Gmos.GmosCcdReadout(x, y, ac, ag, ar), dx, e)
  }

  private def gmosCustomMask(cm: ConfigMap): PioError \/ Option[Gmos.GmosCustomMask] = {
    import gem.ocs2.Legacy.Instrument.Gmos.{CustomMaskMdf, CustomSlitWidth}

    (for {
      f <- PioOptional(CustomMaskMdf.cparse(cm))
      s <- PioOptional(CustomSlitWidth.parse(cm))
    } yield Gmos.GmosCustomMask(f, s)).run
  }

  private def gmosNorthDynamic(cm: ConfigMap): PioError \/ DynamicConfig = {
    import Legacy.Instrument.Gmos._
    import Legacy.Instrument.GmosNorth._

    val grating: PioError \/ Option[Gmos.GmosGrating[GmosNorthDisperser]] =
      (for {
        d <- PioOptional(Disperser.parse(cm))
        o <- PioOptional(DisperserOrder.cparse(cm))
        w <- PioOptional(DisperserLambda.cparse(cm)).map(Gmos.GmosCentralWavelength)
      } yield Gmos.GmosGrating(d, o, w)).run

    for {
      c <- gmosCommonDynamic(cm)
      g <- grating
      f <- Filter.parse(cm)
      u <- Fpu.cparse(cm).map(_.flatten)
      m <- gmosCustomMask(cm)
      fpu = u.map(_.right[Gmos.GmosCustomMask]) orElse m.map(_.left[GmosNorthFpu])
    } yield GmosNorthDynamicConfig(c, g, f, fpu)
  }

  private def gmosSouthDynamic(cm: ConfigMap): PioError \/ DynamicConfig = {
    import Legacy.Instrument.Gmos._
    import Legacy.Instrument.GmosSouth._

    val grating: PioError \/ Option[Gmos.GmosGrating[GmosSouthDisperser]] =
      (for {
        d <- PioOptional(Disperser.parse(cm))
        o <- PioOptional(DisperserOrder.cparse(cm))
        w <- PioOptional(DisperserLambda.cparse(cm)).map(Gmos.GmosCentralWavelength)
      } yield Gmos.GmosGrating(d, o, w)).run

    for {
      c <- gmosCommonDynamic(cm)
      g <- grating
      f <- Filter.parse(cm)
      u <- Fpu.cparse(cm).map(_.flatten)
      m <- gmosCustomMask(cm)
      fpu = u.map(_.right[Gmos.GmosCustomMask]) orElse m.map(_.left[GmosSouthFpu])
    } yield GmosSouthDynamicConfig(c, g, f, fpu)
  }

  def parseStep(cm: ConfigMap): PioError \/ Step[DynamicConfig] = {
    def go(observeType: String, instrument: DynamicConfig): PioError \/ Step[DynamicConfig] =
      observeType match {
        case "BIAS" =>
          BiasStep(instrument).right

        case "DARK" =>
          DarkStep(instrument).right

        case "OBJECT" | "CAL" =>
          for {
            p <- Legacy.Telescope.P.cparseOrElse(cm, Offset.P.Zero)
            q <- Legacy.Telescope.Q.cparseOrElse(cm, Offset.Q.Zero)
          } yield ScienceStep(instrument, TelescopeConfig(p, q))

        case "ARC" | "FLAT" =>
          import Legacy.Calibration._
          for {
            l <- Lamp.parse(cm)
            f <- Filter.parse(cm)
            d <- Diffuser.parse(cm)
            s <- Shutter.parse(cm)
            e <- ExposureTime.parse(cm)
            c <- Coadds.parse(cm)
          } yield GcalStep(instrument, GcalConfig(l, f, d, s, e, c.toShort))

        case x =>
          PioError.parseError(x, "ObserveType").left
      }

    for {
      o <- Legacy.Observe.ObserveType.cparseOrElse(cm, "OBJECT")
      i <- Legacy.Instrument.Instrument.parse(cm)
      c <- parseInstConfig(i, cm)
      s <- go(o, c)
    } yield s
  }

  // Extracts the steps in the XML sequence to a simple list of Maps where
  // each Map is all the keys and values that apply to the step.
  def decode(n: Node): PioError \/ List[Step[DynamicConfig]] =
    (n \ "step").toList.scanLeft(EmptyConfigMap) { (m, stepNode) =>
      stepNode.addStepConfig(m)
    }.drop(1).traverseU(parseStep)
}
