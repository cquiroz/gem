// Copyright (c) 2016-2017 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package gem.ocs2

import gem.{Dataset, Observation, Program}
import gem.enum._
import gem.config.GcalConfig.GcalLamp
import gem.math.{ Angle, Offset }

import scalaz._
import Scalaz._

/** String parsers for our model types.
  */
object Parsers {

  import gem.ocs2.pio.PioParse
  import gem.ocs2.pio.PioParse._

  val yesNo: PioParse[Boolean] = enum(
    "No"  -> false,
    "Yes" -> true
  )

  val mosPreImaging: PioParse[MosPreImaging] = enum(
    "No"  -> MosPreImaging.IsNotMosPreImaging,
    "Yes" -> MosPreImaging.IsMosPreImaging
  )

  val arcsec: PioParse[Angle] =
    double.map(d => Angle.fromDoubleDegrees(d * 60 * 60))

  val instrument: PioParse[Instrument] = enum(
    "AcqCam"     -> gem.enum.Instrument.AcqCam,
    "bHROS"      -> gem.enum.Instrument.Bhros,
    "BHROS"      -> gem.enum.Instrument.Bhros,
    "Flamingos2" -> gem.enum.Instrument.Flamingos2,
    "GMOS"       -> gem.enum.Instrument.GmosN,
    "GMOS-N"     -> gem.enum.Instrument.GmosN,
    "GMOSSouth"  -> gem.enum.Instrument.GmosS,
    "GMOS-S"     -> gem.enum.Instrument.GmosS,
    "GNIRS"      -> gem.enum.Instrument.Gnirs,
    "GPI"        -> gem.enum.Instrument.Gpi,
    "GSAOI"      -> gem.enum.Instrument.Gsaoi,
    "Michelle"   -> gem.enum.Instrument.Michelle,
    "NICI"       -> gem.enum.Instrument.Nici,
    "NIFS"       -> gem.enum.Instrument.Nifs,
    "NIRI"       -> gem.enum.Instrument.Niri,
    "Phoenix"    -> gem.enum.Instrument.Phoenix,
    "TReCS"      -> gem.enum.Instrument.Trecs,
    "Visitor"    -> gem.enum.Instrument.Visitor,
    "Visitor Instrument" -> gem.enum.Instrument.Visitor
  )

  val progId: PioParse[Program.Id] =
    PioParse(s => Option(Program.Id.unsafeFromString(s)))

  val obsId: PioParse[Observation.Id] =
    PioParse(Observation.Id.fromString)

  val datasetLabel: PioParse[Dataset.Label] =
    PioParse(Dataset.Label.fromString)

  val offsetP: PioParse[Offset.P] =
    arcsec.map(Offset.P.apply)

  val offsetQ: PioParse[Offset.Q] =
    arcsec.map(Offset.Q.apply)

  object Calibration {

    val lamp: PioParse[GcalLamp] = {
      import GcalArc._
      import GcalContinuum._

      val lampToContinuum = Map[String, GcalContinuum](
        "IR grey body - high" -> IrGreyBodyHigh,
        "IR grey body - low"  -> IrGreyBodyLow,
        "Quartz Halogen"      -> QuartzHalogen
      ).lift

      val lampToArc       = Map[String, GcalArc](
        "Ar arc"              -> ArArc,
        "CuAr arc"            -> CuArArc,
        "ThAr Arc"            -> ThArArc,
        "Xe Arc"              -> XeArc
      ).lift

      PioParse(lamps => {
        val (continuum, arc) = ((List.empty[GcalContinuum], List.empty[GcalArc])/:lamps.split(',')) {
          case ((cs,as), s) =>
            val cs2 = lampToContinuum(s).fold(cs) { _ :: cs }
            val as2 = lampToArc(s).fold(as) { _ :: as }
            (cs2, as2)
        }
        GcalLamp.fromConfig(continuum.headOption, arc.strengthR(true): _*)
      })
    }

    val filter: PioParse[GcalFilter] = enum(
      "none"         -> GcalFilter.None,
      "ND1.0"        -> GcalFilter.Nd10,
      "ND1.6"        -> GcalFilter.Nd16,
      "ND2.0"        -> GcalFilter.Nd20,
      "ND3.0"        -> GcalFilter.Nd30,
      "ND4.0"        -> GcalFilter.Nd40,
      "ND4-5"        -> GcalFilter.Nd45,
      "ND5.0"        -> GcalFilter.Nd50,
      "GMOS balance" -> GcalFilter.Gmos,
      "HROS balance" -> GcalFilter.Hros,
      "NIR balance"  -> GcalFilter.Nir
    )

    val diffuser: PioParse[GcalDiffuser] = enum(
      "IR"      -> GcalDiffuser.Ir,
      "visible" -> GcalDiffuser.Visible
    )

    val shutter: PioParse[GcalShutter] = enum(
      "Closed" -> GcalShutter.Closed,
      "Open"   -> GcalShutter.Open
    )
  }

  private def fstParser[A](table: List[(String, String, A)]): PioParse[A] =
    enum(table.map { case (a, _, b) => (a, b) }:_*)

  private def sndParser[A](table: List[(String, String, A)]): PioParse[A] =
    enum(table.map { case (_, a, b) => (a, b) }:_*)

  object Flamingos2 {

    import F2Disperser._

    val disperserTable: List[(String, String, F2Disperser)] =
      List(
        ("NONE",    "None",                       NoDisperser),
        ("R1200HK", "R=1200 (H + K) grism",       R1200HK    ),
        ("R1200JH", "R=1200 (J + H) grism",       R1200JH    ),
        ("R3000",   "R=3000 (J or H or K) grism", R3000      )
      )

    val disperser: PioParse[F2Disperser] =
      fstParser(disperserTable)

    val disperserDisplayValue: PioParse[F2Disperser] =
      sndParser(disperserTable)

    import F2Filter._

    val filterTable: List[(String, String, F2Filter)] =
      List(
        ("OPEN",    "Open",               Open  ),
        ("DARK",    "Dark",               Dark  ),
        ("F1056",   "F1056 (1.056 um)",   F1056 ),
        ("F1063",   "F1063 (1.063 um)",   F1063 ),
        ("H",       "H (1.65 um)",        H     ),
        ("HK",      "HK (spectroscopic)", HK    ),
        ("J",       "J (1.25 um)",        J     ),
        ("J_LOW",   "J-low (1.15 um)",    JLow  ),
        ("JH",      "JH (spectroscopic)", JH    ),
        ("K_LONG",  "K-long (2.20 um)",   KLong ),
        ("K_SHORT", "K-short (2.15 um)",  KShort),
        ("K_BLUE",  "K-blue (2.06 um)",   KBlue ),
        ("K_RED",   "K-red (2.31 um)",    KRed  ),
        ("Y",       "Y (1.02 um)",        Y     )
      )

    val filter: PioParse[F2Filter] =
      fstParser(filterTable)

    val filterDisplayValue: PioParse[F2Filter] =
      sndParser(filterTable)

    import F2FpUnit._

    val fpuTable: List[(String, String, F2FpUnit)] =
      List(
        ("PINHOLE",        "2-pix pinhole grid",  Pinhole      ),
        ("SUBPIX_PINHOLE", "subpix pinhole grid", SubPixPinhole),
        ("FPU_NONE",       "Imaging (none)",      None         ),
        ("CUSTOM_MASK",    "Custom Mask",         Custom       ),
        ("LONGSLIT_1",     "1-pix longslit",      LongSlit1    ),
        ("LONGSLIT_2",     "2-pix longslit",      LongSlit2    ),
        ("LONGSLIT_3",     "3-pix longslit",      LongSlit3    ),
        ("LONGSLIT_4",     "4-pix longslit",      LongSlit4    ),
        ("LONGSLIT_6",     "6-pix longslit",      LongSlit6    ),
        ("LONGSLIT_8",     "8-pix longslit",      LongSlit8    )
      )

    val fpu: PioParse[F2FpUnit] =
      fstParser(fpuTable)

    val fpuDisplayValue: PioParse[F2FpUnit] =
      sndParser(fpuTable)

    import F2LyotWheel._
    val lyotWheel: PioParse[F2LyotWheel] = enum(
      "GEMS"       -> F33Gems,
      "GEMS_OVER"  -> GemsUnder,
      "GEMS_UNDER" -> GemsOver,
      "H1"         -> HartmannA,
      "H2"         -> HartmannB,
      "HIGH"       -> F32High,
      "LOW"        -> F32Low,
      "OPEN"       -> F16
    )

    import F2ReadMode._
    val readMode: PioParse[F2ReadMode] = enum(
      "BRIGHT_OBJECT_SPEC" -> Bright,
      "MEDIUM_OBJECT_SPEC" -> Medium,
      "FAINT_OBJECT_SPEC"  -> Faint
    )

    val windowCover: PioParse[F2WindowCover] = enum(
      "CLOSE" -> F2WindowCover.Close,
      "OPEN"  -> F2WindowCover.Open
    )
  }


  object Gmos {
    import GmosAdc._

    val adc: PioParse[Option[GmosAdc]] = enum(
      "No Correction"          -> Option.empty[GmosAdc],
      "Best Static Correction" -> Some(BestStatic),
      "Follow During Exposure" -> Some(Follow)
    )


    import GmosAmpCount._

    val ampCount: PioParse[GmosAmpCount] = enum(
      "Three"  -> Three,
      "Six"    -> Six,
      "Twelve" -> Twelve
    )


    import GmosAmpGain._

    val ampGain: PioParse[GmosAmpGain] = enum(
      "Low"  -> Low,
      "High" -> High
    )


    import GmosAmpReadMode._

    val ampReadMode: PioParse[GmosAmpReadMode] = enum(
      "Slow" -> Slow,
      "Fast" -> Fast
    )


    import GmosBuiltinRoi._

    val builtinRoi: PioParse[Option[GmosBuiltinRoi]] = enum(
      "Custom ROI"         -> Option.empty[GmosBuiltinRoi],
      "Full Frame Readout" -> Some(FullFrame      ),
      "CCD 2"              -> Some(Ccd2           ),
      "Central Spectrum"   -> Some(CentralSpectrum),
      "Central Stamp"      -> Some(CentralStamp   ),
      "Top Spectrum"       -> Some(TopSpectrum    ),
      "Bottom Spectrum"    -> Some(BottomSpectrum )
    )


    import GmosCustomSlitWidth._

    val customSlitWidth: PioParse[Option[GmosCustomSlitWidth]] = enum(
      "OTHER"             -> Option.empty[GmosCustomSlitWidth],
      "CUSTOM_WIDTH_0_25" -> Some(CustomWidth_0_25),
      "CUSTOM_WIDTH_0_50" -> Some(CustomWidth_0_50),
      "CUSTOM_WIDTH_0_75" -> Some(CustomWidth_0_75),
      "CUSTOM_WIDTH_1_00" -> Some(CustomWidth_1_00),
      "CUSTOM_WIDTH_1_50" -> Some(CustomWidth_1_50),
      "CUSTOM_WIDTH_2_00" -> Some(CustomWidth_2_00),
      "CUSTOM_WIDTH_5_00" -> Some(CustomWidth_5_00)
    )


    import GmosDetector._

    val detector: PioParse[GmosDetector] = enum(
      "E2V"       -> E2V,
      "HAMAMATSU" -> HAMAMATSU
    )


    val disperserOrder: PioParse[GmosDisperserOrder] = enum(
      "0" -> GmosDisperserOrder.Zero,
      "1" -> GmosDisperserOrder.One,
      "2" -> GmosDisperserOrder.Two
    )


    val disperserLambda: PioParse[BigDecimal] =
      bigDecimal


    val dtax: PioParse[GmosDtax] = enum(
      "-6" -> GmosDtax.MinusSix,
      "-5" -> GmosDtax.MinusFive,
      "-4" -> GmosDtax.MinusFour,
      "-3" -> GmosDtax.MinusThree,
      "-2" -> GmosDtax.MinusTwo,
      "-1" -> GmosDtax.MinusOne,
      "0"  -> GmosDtax.Zero,
      "1"  -> GmosDtax.One,
      "2"  -> GmosDtax.Two,
      "3"  -> GmosDtax.Three,
      "4"  -> GmosDtax.Four,
      "5"  -> GmosDtax.Five,
      "6"  -> GmosDtax.Six
    )


    val xBinning: PioParse[GmosXBinning] = enum(
      "1" -> GmosXBinning.One,
      "2" -> GmosXBinning.Two,
      "4" -> GmosXBinning.Four
    )


    val yBinning: PioParse[GmosYBinning] = enum(
      "1" -> GmosYBinning.One,
      "2" -> GmosYBinning.Two,
      "4" -> GmosYBinning.Four
    )
  }

  object GmosNorth {
    import GmosNorthDisperser._

    val disperser: PioParse[Option[GmosNorthDisperser]] = enum(
      "Mirror"      -> Option.empty[GmosNorthDisperser],
      "B1200_G5301" -> Some(B1200_G5301),
      "R831_G5302"  -> Some(R831_G5302 ),
      "B600_G5303"  -> Some(B600_G5303 ),
      "B600_G5307"  -> Some(B600_G5307 ),
      "R600_G5304"  -> Some(R600_G5304 ),
      "R400_G5305"  -> Some(R400_G5305 ),
      "R150_G5306"  -> Some(R150_G5306 ),
      "R150_G5308"  -> Some(R150_G5308 )
    )

    import GmosNorthFilter._

    val filter: PioParse[Option[GmosNorthFilter]] = enum(
      "None"                      -> Option.empty[GmosNorthFilter],
      "g_G0301"                   -> Some(GPrime),
      "r_G0303"                   -> Some(RPrime),
      "i_G0302"                   -> Some(IPrime),
      "z_G0304"                   -> Some(ZPrime),
      "Z_G0322"                   -> Some(Z),
      "Y_G0323"                   -> Some(Y),
      "GG455_G0305"               -> Some(GG455),
      "OG515_G0306"               -> Some(OG515),
      "RG610_G0307"               -> Some(RG610),
      "CaT_G0309"                 -> Some(CaT),
      "Ha_G0310"                  -> Some(Ha),
      "HaC_G0311"                 -> Some(HaC),
      "DS920_G0312"               -> Some(DS920),
      "SII_G0317"                 -> Some(SII),
      "OIII_G0318"                -> Some(OIII),
      "OIIIC_G0319"               -> Some(OIIIC),
      "HeII_G0320"                -> Some(HeII),
      "HeIIC_G0321"               -> Some(HeIIC),
      "HartmannA_G0313 + r_G0303" -> Some(HartmannA_RPrime),
      "HartmannB_G0314 + r_G0303" -> Some(HartmannB_RPrime),
      "g_G0301 + GG455_G0305"     -> Some(GPrime_GG455),
      "g_G0301 + OG515_G0306"     -> Some(GPrime_OG515),
      "r_G0303 + RG610_G0307"     -> Some(RPrime_RG610),
      "i_G0302 + CaT_G0309"       -> Some(IPrime_CaT),
      "z_G0304 + CaT_G0309"       -> Some(ZPrime_CaT),
      "u_G0308"                   -> Some(UPrime)
    )


    import GmosNorthFpu._

    val fpu: PioParse[Option[GmosNorthFpu]] = enum(
      "None"                 -> Option.empty[GmosNorthFpu],
      "Longslit 0.25 arcsec" -> Some(Longslit1),
      "Longslit 0.50 arcsec" -> Some(Longslit2),
      "Longslit 0.75 arcsec" -> Some(Longslit3),
      "Longslit 1.00 arcsec" -> Some(Longslit4),
      "Longslit 1.50 arcsec" -> Some(Longslit5),
      "Longslit 2.00 arcsec" -> Some(Longslit6),
      "Longslit 5.00 arcsec" -> Some(Longslit7),
      "IFU 2 Slits"          -> Some(Ifu1),
      "IFU Left Slit (blue)" -> Some(Ifu2),
      "IFU Right Slit (red)" -> Some(Ifu3),
      "N and S 0.25 arcsec"  -> Some(Ns0),
      "N and S 0.50 arcsec"  -> Some(Ns1),
      "N and S 0.75 arcsec"  -> Some(Ns2),
      "N and S 1.00 arcsec"  -> Some(Ns3),
      "N and S 1.50 arcsec"  -> Some(Ns4),
      "N and S 2.00 arcsec"  -> Some(Ns5),
      "Custom Mask"          -> Option.empty[GmosNorthFpu]
    )


    import GmosNorthStageMode._

    val stageMode: PioParse[GmosNorthStageMode] = enum(
      "Do Not Follow"        -> NoFollow,
      "Follow in XYZ(focus)" -> FollowXyz,
      "Follow in XY"         -> FollowXy,
      "Follow in Z Only"     -> FollowZ
    )
  }

  object GmosSouth {
    import GmosSouthDisperser._

    val disperser: PioParse[Option[GmosSouthDisperser]] = enum(
      "Mirror"      -> Option.empty[GmosSouthDisperser],
      "B1200_G5321" -> Some(B1200_G5321),
      "R831_G5322"  -> Some(R831_G5322 ),
      "B600_G5323"  -> Some(B600_G5323 ),
      "R600_G5324"  -> Some(R600_G5324 ),
      "R400_G5325"  -> Some(R400_G5325 ),
      "R150_G5326"  -> Some(R150_G5326 )
    )

    import GmosSouthFilter._

    val filter: PioParse[Option[GmosSouthFilter]] = enum(
      "None"                      -> Option.empty[GmosSouthFilter],
      "u_G0332"                   -> Some(UPrime),
      "g_G0325"                   -> Some(GPrime),
      "r_G0326"                   -> Some(RPrime),
      "i_G0327"                   -> Some(IPrime),
      "z_G0328"                   -> Some(ZPrime),
      "Z_G0343"                   -> Some(Z),
      "Y_G0344"                   -> Some(Y),
      "GG455_G0329"               -> Some(GG455),
      "OG515_G0330"               -> Some(OG515),
      "RG610_G0331"               -> Some(RG610),
      "RG780_G0334"               -> Some(RG780),
      "CaT_G0333"                 -> Some(CaT),
      "HartmannA_G0337 + r_G0326" -> Some(HartmannA_RPrime),
      "HartmannB_G0338 + r_G0326" -> Some(HartmannB_RPrime),
      "g_G0325 + GG455_G0329"     -> Some(GPrime_GG455),
      "g_G0325 + OG515_G0330"     -> Some(GPrime_OG515),
      "r_G0326 + RG610_G0331"     -> Some(RPrime_RG610),
      "i_G0327 + RG780_G0334"     -> Some(IPrime_RG780),
      "i_G0327 + CaT_G0333"       -> Some(IPrime_CaT),
      "z_G0328 + CaT_G0333"       -> Some(ZPrime_CaT),
      "Ha_G0336"                  -> Some(Ha),
      "SII_G0335"                 -> Some(SII),
      "HaC_G0337"                 -> Some(HaC),
      "OIII_G0338"                -> Some(OIII),
      "OIIIC_G0339"               -> Some(OIIIC),
      "HeII_G0340"                -> Some(HeII),
      "HeIIC_G0341"               -> Some(HeIIC),
      "Lya395_G0342"              -> Some(Lya395)
    )


    import GmosSouthFpu._

    val fpu: PioParse[Option[GmosSouthFpu]] = enum(
      "None"                         -> Option.empty[GmosSouthFpu],
      "Longslit 0.25 arcsec"         -> Some(Longslit1),
      "Longslit 0.50 arcsec"         -> Some(Longslit2),
      "Longslit 0.75 arcsec"         -> Some(Longslit3),
      "Longslit 1.00 arcsec"         -> Some(Longslit4),
      "Longslit 1.50 arcsec"         -> Some(Longslit5),
      "Longslit 2.00 arcsec"         -> Some(Longslit6),
      "Longslit 5.00 arcsec"         -> Some(Longslit7),
      "IFU 2 Slits"                  -> Some(Ifu1),
      "IFU Left Slit (blue)"         -> Some(Ifu2),
      "IFU Right Slit (red)"         -> Some(Ifu3),
      "bHROS"                        -> Some(Bhros),
      "IFU N and S 2 Slits"          -> Some(IfuN),
      "IFU N and S Left Slit (blue)" -> Some(IfuNB),
      "IFU N and S Right Slit (red)" -> Some(IfuNR),
      "N and S 0.50 arcsec"          -> Some(Ns1),
      "N and S 0.75 arcsec"          -> Some(Ns2),
      "N and S 1.00 arcsec"          -> Some(Ns3),
      "N and S 1.50 arcsec"          -> Some(Ns4),
      "N and S 2.00 arcsec"          -> Some(Ns5),
      "Custom Mask"                  -> Option.empty[GmosSouthFpu]
    )


    import GmosSouthStageMode._

    val stageMode: PioParse[GmosSouthStageMode] = enum(
      "Do Not Follow"        -> NoFollow,
      "Follow in XYZ(focus)" -> FollowXyz,
      "Follow in XY"         -> FollowXy,
      "Follow in Z Only"     -> FollowZ
    )
  }
}
