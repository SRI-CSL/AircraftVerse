package com.sri.nscore.uav2

import com.sri.nscore._
import com.sri.nscore.uav2.taperOffsetToInt
import com.sri.nscore.uav2.Motor.fromSeq
import com.sri.nscore.uav2.Wing.fromSeq
import org.json4s.ShortTypeHints
import org.json4s.native.Serialization

// --- Top-level design class --

object UAV2Design {

  val generator_version = "UAV2_gen12"

  def fromSeq(seq: DesignSeq): UAV2Design = {
    nameIndices.clear()
    val (gen_version, seq1) = readKeyValue[String](seq, "generator_version")
    if (gen_version != generator_version)
      println(s"WARNING! Attempting to parse a design from generator $gen_version using grammar $generator_version")
    val (name, seq2) = readKeyValue[String](seq1, "name")
    val (hub, seq3) = ConnectedHub.fromSeq(seq2)
    val (fuselageWithComponents, seq4) = FuselageWithComponents.fromSeq(seq3)
    if (!seq4.isEmpty)
      throw new IllegalArgumentException(s"Extra entries in sequence: $seq4")
    UAV2Design(name, hub, fuselageWithComponents)
  }

  def fromSeqBF(seq: DesignSeq): UAV2Design = {
    nameIndices.clear()
    val (gen_version, seq1) = readKeyValue[String](seq, "generator_version")
    if (gen_version != generator_version)
      println(s"WARNING! Attempting to parse a design from generator $gen_version using grammar $generator_version")
    val (name, seq2) = readKeyValue[String](seq1, "name")
    val (result, seqRem) = BFParser.fromSeqBF_2[ConnectedHub, FuselageWithComponents, UAV2Design](seq2,
      ConnectedHub.fromSeqBF(_), FuselageWithComponents.fromSeqBF(_), UAV2Design(generator_version, name, _, _))
    val uav = BFParser.parseBF(result, seqRem)
    uav
  }

  val formats = Serialization.formats(ShortTypeHints(List(
    classOf[SingleBatteryFuselageWithComponents],
    classOf[DualBatteryFuselageWithComponents],
    classOf[ConnectedHub2_Sym_Wide],
    classOf[ConnectedHub2_Sym_Long],
    classOf[ConnectedHub2_Sym_Wide],
    classOf[ConnectedHub2_Asym],
    classOf[ConnectedHub3_Sym],
    classOf[ConnectedHub3_2_1],
    classOf[ConnectedHub4_Sym],
    classOf[ConnectedHub4_Sym_Aligned],
    classOf[ConnectedHub4_2_2],
    classOf[ConnectedHub4_1_2_1],
    classOf[ConnectedHub5_Sym],
    classOf[ConnectedHub5_4_1],
    classOf[ConnectedHub5_2_2_1],
    classOf[ConnectedHub6_Sym],
    classOf[ConnectedHub6_Sym_Aligned],
    classOf[ConnectedHub6_2_2_2],
    classOf[ConnectedHub6_1_2_2_1],
    classOf[PropArm],
    classOf[AngledPropArm],
    classOf[WingArm],
    classOf[AngledWingArm],
    classOf[RotatedArmSegment],
    classOf[BendSegment],
    classOf[DoubleBendSegment],
    classOf[SidewaysBendSegment],
    classOf[SidewaysBendWithTopSegment],
    classOf[CrossSegment],
    classOf[BranchSegment_Sym],
    classOf[BranchWithTopSegment_Sym],
    classOf[BranchSegment_Asym],
    classOf[BranchWithTopSegment_Asym]
  ), "node_type"))

  def fromJson(jsonStr: String): UAV2Design = {
    implicit val formats = this.formats
    Serialization.read[UAV2Design](jsonStr)
  }

  def apply(name: String,
            hub: ConnectedHub,
            fuselageWithComponents: FuselageWithComponents): UAV2Design = {
    UAV2Design(generator_version, name, hub, fuselageWithComponents)
  }

}

case class UAV2Design(generator_version: String,
                      name: String,
                      hub: ConnectedHub,
                      fuselageWithComponents: FuselageWithComponents) extends HighLevelDesign {
  if (!name.matches(validNamePat)){
    throw new RuntimeException("Design name must match contain letters, numbers, and underscores only")
  }
  val batteryController = BatteryController()
  val cargo = Cargo(0.5)  // default cargo weight
  val autopilot = Sensor("Autopilot")
  val currentSensor = Sensor("Current")
  val gps = Sensor("GPS")
  val rpmTempSensor = Sensor("RpmTemp")
  val variometer = Sensor("Variometer")
  val voltageSensor = Sensor("Voltage")
  val cargoCase = CargoCase(360.0 - hub.rotationAngle)
  val bodyRotParam = NamedParam("BODY_ROT_ANGLE", hub.rotationAngle)
//  val cargoCase = CargoCase(hub.rotationAngle)
//  val bodyRotParam = NamedParam("BODY_ROT_ANGLE", 360.0 - hub.rotationAngle)
  override def apply(implicit g: LowLevelGenerator): LowLevelAssembly = {
    val fuse2hub = g.createConnection()
    val hub2case = g.createConnection()
    val case2cargo = g.createConnection()
    val motorPower = g.createConnection()
    fuselageWithComponents(fuse2hub, motorPower, bodyRotParam) ++
      hub(hub2case, fuse2hub, motorPower, bodyRotParam) ++
      cargoCase(hub2case, case2cargo) ++
      cargo(case2cargo)
  }

  def toJson(): String = {
    implicit val formats = UAV2Design.formats
    val jsonStr = Serialization.writePretty(this)
    jsonStr
  }

}

// --- High-level components ---
// These get compiled away in the low-level representation.

trait FuselageWithComponents {
  def apply(bottomConnector: Connection, powerBus: Connection, bodyRotParam: NamedParam)(implicit g: LowLevelGenerator): LowLevelAssembly
}

object FuselageWithComponents {
  def fromSeq(seq: DesignSeq): (FuselageWithComponents, DesignSeq) = {
    val (node_type, seq2) = readKeyValue[String](seq, "node_type")
    node_type match {
      case "SingleBatteryFuselageWithComponents" => SingleBatteryFuselageWithComponents.fromSeq(seq2)
      case "DualBatteryFuselageWithComponents" => DualBatteryFuselageWithComponents.fromSeq(seq2)
      case _ => throw new IllegalArgumentException(s"Unexpected node_type for FuselageWithComponents: $node_type")
    }
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[FuselageWithComponents], DesignSeq) = {
    val (node_type, seq2) = readKeyValue[String](seq, "node_type")
    node_type match {
      case "SingleBatteryFuselageWithComponents" => SingleBatteryFuselageWithComponents.fromSeqBF(seq2)
      case "DualBatteryFuselageWithComponents" => DualBatteryFuselageWithComponents.fromSeqBF(seq2)
      case _ => throw new IllegalArgumentException(s"Unexpected node_type for FuselageWithComponents: $node_type")
    }
  }
}

object SingleBatteryFuselageWithComponents {
  def fromSeq(seq: DesignSeq): (FuselageWithComponents, DesignSeq) = {
    val (battery, seq1) = Battery.fromSeq(seq)
    val (fuselage, seq2) = SingleBatteryFuselage.fromSeq(seq1)
    (SingleBatteryFuselageWithComponents(battery, fuselage), seq2)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[FuselageWithComponents], DesignSeq) = {
    BFParser.fromSeqBF_2(seq, Battery.fromSeqBF(_), SingleBatteryFuselage.fromSeqBF(_),
      SingleBatteryFuselageWithComponents(_,_))
  }
}

case class SingleBatteryFuselageWithComponents(battery: Battery,
                                               fuselage: SingleBatteryFuselage) extends FuselageWithComponents {
  val batteryController = BatteryController()
  val rpmTempSensor = Sensor("RpmTemp")
  val autopilot = Sensor("Autopilot")
  val currentSensor = Sensor("Current")
  val voltageSensor = Sensor("Voltage")
  val gps = Sensor("GPS")
  val variometer = Sensor("Variometer")

  override def apply(bottomConnector: Connection, powerBus: Connection, bodyRotParam: NamedParam)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    val fuse2bat = g.createConnection()
    val fuse2rpm = g.createConnection()
    val fuse2autopilot = g.createConnection()
    val fuse2current = g.createConnection()
    val fuse2voltage = g.createConnection()
    val fuse2gps = g.createConnection()
    val fuse2vario = g.createConnection()
    val batteryCtrl2Bat = g.createConnection()

    fuselage(bottomConnector, fuse2bat, fuse2rpm, fuse2autopilot, fuse2current, fuse2voltage, fuse2gps, fuse2vario, bodyRotParam) ++
      battery(fuse2bat, batteryCtrl2Bat) ++
      rpmTempSensor(fuse2rpm) ++
      autopilot(fuse2autopilot) ++
      currentSensor(fuse2current) ++
      voltageSensor(fuse2voltage) ++
      gps(fuse2gps) ++
      variometer(fuse2vario) ++
      batteryController(powerBus, batteryCtrl2Bat)
  }
}

object DualBatteryFuselageWithComponents {
  def fromSeq(seq: DesignSeq): (FuselageWithComponents, DesignSeq) = {
    val (battery, seq1) = Battery.fromSeq(seq)
    val (fuselage, seq2) = DualBatteryFuselage.fromSeq(seq1)
    (DualBatteryFuselageWithComponents(battery, fuselage), seq2)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[FuselageWithComponents], DesignSeq) = {
    BFParser.fromSeqBF_2(seq, Battery.fromSeqBF(_), DualBatteryFuselage.fromSeqBF(_),
      DualBatteryFuselageWithComponents(_,_))
  }
}

case class DualBatteryFuselageWithComponents(battery: Battery, fuselage: DualBatteryFuselage) extends FuselageWithComponents {
  val batteryController = BatteryController()
  val rpmTempSensor = Sensor("RpmTemp")
  val autopilot = Sensor("Autopilot")
  val currentSensor = Sensor("Current")
  val voltageSensor = Sensor("Voltage")
  val gps = Sensor("GPS")
  val variometer = Sensor("Variometer")

  override def apply(bottomConnector: Connection, powerBus: Connection, bodyRotParam: NamedParam)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    val fuse2bat1 = g.createConnection()
    val fuse2bat2 = g.createConnection()
    val fuse2rpm = g.createConnection()
    val fuse2autopilot = g.createConnection()
    val fuse2current = g.createConnection()
    val fuse2voltage = g.createConnection()
    val fuse2gps = g.createConnection()
    val fuse2vario = g.createConnection()
    val batteryCtrl2Bat = g.createConnection()

    fuselage(bottomConnector, fuse2bat1, fuse2bat2, fuse2rpm, fuse2autopilot, fuse2current, fuse2voltage, fuse2gps, fuse2vario, bodyRotParam) ++
      battery(fuse2bat1, batteryCtrl2Bat) ++
      battery(fuse2bat2, batteryCtrl2Bat) ++
      rpmTempSensor(fuse2rpm) ++
      autopilot(fuse2autopilot) ++
      currentSensor(fuse2current) ++
      voltageSensor(fuse2voltage) ++
      gps(fuse2gps) ++
      variometer(fuse2vario) ++
      batteryController(powerBus, batteryCtrl2Bat)
  }
}

object ConnectedHub {

  def fromSeqBF(seq: DesignSeq): (BFParseResult[ConnectedHub], DesignSeq) = {
    val (node_type, seq2) = readKeyValue[String](seq, "node_type")
    node_type match {
      case "ConnectedHub2_Sym_Long" => ConnectedHub2_Sym_Long.fromSeqBF(seq2)
      case "ConnectedHub2_Sym_Wide" => ConnectedHub2_Sym_Wide.fromSeqBF(seq2)
      case "ConnectedHub2_Asym" => ConnectedHub2_Asym.fromSeqBF(seq2)
      case "ConnectedHub3_Sym" => ConnectedHub3_Sym.fromSeqBF(seq2)
      case "ConnectedHub3_2_1" => ConnectedHub3_2_1.fromSeqBF(seq2)
      case "ConnectedHub4_Sym" => ConnectedHub4_Sym.fromSeqBF(seq2)
      case "ConnectedHub4_Sym_Aligned" => ConnectedHub4_Sym_Aligned.fromSeqBF(seq2)
      case "ConnectedHub4_2_2" => ConnectedHub4_2_2.fromSeqBF(seq2)
      case "ConnectedHub4_1_2_1" => ConnectedHub4_1_2_1.fromSeqBF(seq2)
      case "ConnectedHub5_Sym" => ConnectedHub5_Sym.fromSeqBF(seq2)
      case "ConnectedHub5_4_1" => ConnectedHub5_4_1.fromSeqBF(seq2)
      case "ConnectedHub5_2_2_1" => ConnectedHub5_2_2_1.fromSeqBF(seq2)
      case "ConnectedHub6_Sym" => ConnectedHub6_Sym.fromSeqBF(seq2)
      case "ConnectedHub6_Sym_Aligned" => ConnectedHub6_Sym_Aligned.fromSeqBF(seq2)
      case "ConnectedHub6_2_2_2" => ConnectedHub6_2_2_2.fromSeqBF(seq2)
      case "ConnectedHub6_1_2_2_1" => ConnectedHub6_1_2_2_1.fromSeqBF(seq2)
      case _ => throw new IllegalArgumentException(s"Unexpected node_type for ConnectedHub: $node_type")
    }
  }

  def fromSeq(seq: DesignSeq): (ConnectedHub ,DesignSeq) = {
    val (node_type, seq2) = readKeyValue[String](seq, "node_type")
    node_type match {
      case "ConnectedHub2_Sym_Long" => ConnectedHub2_Sym_Long.fromSeq(seq2)
      case "ConnectedHub2_Sym_Wide" => ConnectedHub2_Sym_Wide.fromSeq(seq2)
      case "ConnectedHub2_Asym" => ConnectedHub2_Asym.fromSeq(seq2)
      case "ConnectedHub3_Sym" => ConnectedHub3_Sym.fromSeq(seq2)
      case "ConnectedHub3_2_1" => ConnectedHub3_2_1.fromSeq(seq2)
      case "ConnectedHub4_Sym" => ConnectedHub4_Sym.fromSeq(seq2)
      case "ConnectedHub4_Sym_Aligned" => ConnectedHub4_Sym_Aligned.fromSeq(seq2)
      case "ConnectedHub4_2_2" => ConnectedHub4_2_2.fromSeq(seq2)
      case "ConnectedHub4_1_2_1" => ConnectedHub4_1_2_1.fromSeq(seq2)
      case "ConnectedHub5_Sym" => ConnectedHub5_Sym.fromSeq(seq2)
      case "ConnectedHub5_4_1" => ConnectedHub5_4_1.fromSeq(seq2)
      case "ConnectedHub5_2_2_1" => ConnectedHub5_2_2_1.fromSeq(seq2)
      case "ConnectedHub6_Sym" => ConnectedHub6_Sym.fromSeq(seq2)
      case "ConnectedHub6_Sym_Aligned" => ConnectedHub6_Sym_Aligned.fromSeq(seq2)
      case "ConnectedHub6_2_2_2" => ConnectedHub6_2_2_2.fromSeq(seq2)
      case "ConnectedHub6_1_2_2_1" => ConnectedHub6_1_2_2_1.fromSeq(seq2)
      case _ => throw new IllegalArgumentException(s"Unexpected node_type for ConnectedHub: $node_type")
    }
  }
}

trait ConnectedHub {
  def apply(topConnector: Connection, bottomConnector: Connection, powerBus: Connection, bodyRotParam: NamedParam)(implicit g: LowLevelGenerator): LowLevelAssembly
  val rotationAngle: Double
}

object ConnectedHub2_Sym_Long {
  def fromSeq(seq: DesignSeq): (ConnectedHub,DesignSeq) = {
    val (mainSegment, seq2) = MainSegment.fromSeq(seq)
    (ConnectedHub2_Sym_Long(mainSegment), seq2)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[ConnectedHub], DesignSeq) = {
    BFParser.fromSeqBF_1Main(seq, ConnectedHub2_Sym_Long(_))
  }
}

case class ConnectedHub2_Sym_Long(mainSegment: MainSegment) extends ConnectedHub {
  override val rotationAngle = 0.0
  val mainHub = MainHub2()
  val orient = Orient()
  override def apply(topConnector: Connection, bottomConnector: Connection,
                     powerBus: Connection, bodyRotParam: NamedParam)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()
    val c2 = g.createConnection()
    val oc = g.createConnection()
    mainHub(topConnector,bottomConnector,oc,c1,c2) ++
    orient(oc, bodyRotParam) ++
    mainSegment(c1,powerBus,Forward) ++
    mainSegment(c2,powerBus,Backward)
  }
}

object ConnectedHub2_Sym_Wide {
  def fromSeq(seq: DesignSeq): (ConnectedHub,DesignSeq) = {
    val (mainSegment, seq2) = MainSegment.fromSeq(seq)
    (ConnectedHub2_Sym_Wide(mainSegment), seq2)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[ConnectedHub], DesignSeq) = {
    BFParser.fromSeqBF_1Main(seq,ConnectedHub2_Sym_Wide(_))
  }
}

case class ConnectedHub2_Sym_Wide(mainSegment: MainSegment) extends ConnectedHub {
  override val rotationAngle = 90.0
  val mainHub = MainHub2()
  val orient = Orient()
  override def apply(topConnector: Connection, bottomConnector: Connection,
                     powerBus: Connection, bodyRotParam: NamedParam)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()
    val c2 = g.createConnection()
    val oc = g.createConnection()
    mainHub(topConnector,bottomConnector,oc,c1,c2) ++
    orient(oc, bodyRotParam) ++
    mainSegment(c1,powerBus,Right) ++
    mainSegment(c2,powerBus,Left)
  }
}

object ConnectedHub2_Asym {
  def fromSeq(seq: DesignSeq): (ConnectedHub,DesignSeq) = {
    val (frontSegment, seq2) = MainSegment.fromSeq(seq)
    val (rearSegment, seq3) = MainSegment.fromSeq(seq2)
    (ConnectedHub2_Asym(frontSegment, rearSegment), seq3)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[ConnectedHub], DesignSeq) = {
    BFParser.fromSeqBF_2Main(seq, ConnectedHub2_Asym(_,_))
  }
}

case class ConnectedHub2_Asym(frontSegment: MainSegment,
                              rearSegment: MainSegment) extends ConnectedHub {
  override val rotationAngle = 0.0
  val mainHub = MainHub2()
  val orient = Orient()
  override def apply(topConnector: Connection, bottomConnector: Connection,
                     powerBus: Connection, bodyRotParam: NamedParam)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()
    val c2 = g.createConnection()
    val oc = g.createConnection()
    mainHub(topConnector,bottomConnector,oc,c1,c2) ++
    orient(oc, bodyRotParam) ++
    frontSegment(c1,powerBus,Forward) ++
    rearSegment(c2,powerBus,Backward)
  }
}

object ConnectedHub3_Sym {
  def fromSeq(seq: DesignSeq): (ConnectedHub,DesignSeq) = {
    val (angle, seq2) = readKeyValue[Double](seq, "angle")
    val (mainSegment, seq3) = MainSegment.fromSeq(seq2)
    (ConnectedHub3_Sym(angle, mainSegment), seq3)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[ConnectedHub], DesignSeq) = {
    val (angle, seq2) = readKeyValue[Double](seq, "angle")
    BFParser.fromSeqBF_1Main(seq2, ConnectedHub3_Sym(angle, _))
  }
}

case class ConnectedHub3_Sym(angle: Double, mainSegment: MainSegment) extends ConnectedHub {
  val mainHub = MainHub3(angle)
  override val rotationAngle = mainHub.bigAngle
  val orient = Orient()
  override def apply(topConnector: Connection, bottomConnector: Connection,
                     powerBus: Connection, bodyRotParam: NamedParam)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()
    val c2 = g.createConnection()
    val c3 = g.createConnection()
    val oc = g.createConnection()
    mainHub(topConnector,bottomConnector,oc,c1,c2,c3) ++
    orient(oc, bodyRotParam) ++
    mainSegment(c1,powerBus,Forward) ++
    mainSegment(c2,powerBus,Right) ++
    mainSegment(c3,powerBus,Left)
  }
}

object ConnectedHub3_2_1 {
  def fromSeq(seq: DesignSeq): (ConnectedHub,DesignSeq) = {
    val (angle, seq2) = readKeyValue[Double](seq, "angle")
    val (centerSegment, seq3) = MainSegment.fromSeq(seq2)
    val (mainSegment, seq4) = MainSegment.fromSeq(seq3)
    (ConnectedHub3_2_1(angle, centerSegment, mainSegment), seq4)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[ConnectedHub], DesignSeq) = {
    val (angle, seq2) = readKeyValue[Double](seq, "angle")
    BFParser.fromSeqBF_2Main(seq2, ConnectedHub3_2_1(angle,_,_))
  }
}

case class ConnectedHub3_2_1(angle: Double, centerSegment: MainSegment,
                             mainSegment: MainSegment) extends ConnectedHub {
  val mainHub = MainHub3(angle)
  //override val rotationAngle = 360.0 - mainHub.bigAngle
  override val rotationAngle = mainHub.bigAngle
  val orient = Orient()
  override def apply(topConnector: Connection, bottomConnector: Connection,
                     powerBus: Connection, bodyRotParam: NamedParam)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()
    val c2 = g.createConnection()
    val c3 = g.createConnection()
    val oc = g.createConnection()
    mainHub(topConnector,bottomConnector,oc,c1,c2,c3) ++
    orient(oc, bodyRotParam) ++
    centerSegment(c1,powerBus,Forward) ++
    mainSegment(c2,powerBus,Right) ++
    mainSegment(c3,powerBus,Left)
  }
}

object ConnectedHub4_Sym {
  def fromSeq(seq: DesignSeq): (ConnectedHub,DesignSeq) = {
    val (mainSegment, seq2) = MainSegment.fromSeq(seq)
    (ConnectedHub4_Sym(mainSegment), seq2)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[ConnectedHub], DesignSeq) = {
    BFParser.fromSeqBF_1Main(seq, ConnectedHub4_Sym(_))
  }
}

case class ConnectedHub4_Sym(mainSegment: MainSegment) extends ConnectedHub {
  override val rotationAngle = 45.0
  val mainHub = MainHub4()
  val orient = Orient()
  override def apply(topConnector: Connection, bottomConnector: Connection,
                     powerBus: Connection, bodyRotParam: NamedParam)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()
    val c2 = g.createConnection()
    val c3 = g.createConnection()
    val c4 = g.createConnection()
    val oc = g.createConnection()
    mainHub(topConnector,bottomConnector,oc,c1,c2,c3,c4) ++
    orient(oc, bodyRotParam) ++
    mainSegment(c1,powerBus,Left) ++
    mainSegment(c2,powerBus,Right) ++
    mainSegment(c3,powerBus,Right) ++
    mainSegment(c4,powerBus,Left)
  }
}

object ConnectedHub4_Sym_Aligned {
  def fromSeq(seq: DesignSeq): (ConnectedHub,DesignSeq) = {
    val (mainSegment, seq2) = MainSegment.fromSeq(seq)
    (ConnectedHub4_Sym_Aligned(mainSegment), seq2)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[ConnectedHub], DesignSeq) = {
    BFParser.fromSeqBF_1Main(seq, ConnectedHub4_Sym_Aligned(_))
  }
}

case class ConnectedHub4_Sym_Aligned(mainSegment: MainSegment) extends ConnectedHub {
  override val rotationAngle = 0.0
  val mainHub = MainHub4()
  val orient = Orient()
  override def apply(topConnector: Connection, bottomConnector: Connection,
                     powerBus: Connection, bodyRotParam: NamedParam)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()
    val c2 = g.createConnection()
    val c3 = g.createConnection()
    val c4 = g.createConnection()
    val oc = g.createConnection()
    mainHub(topConnector,bottomConnector,oc,c1,c2,c3,c4) ++
      orient(oc, bodyRotParam) ++
      mainSegment(c1,powerBus,Forward) ++
      mainSegment(c2,powerBus,Right) ++
      mainSegment(c3,powerBus,Backward) ++
      mainSegment(c4,powerBus,Left)
  }
}

object ConnectedHub4_1_2_1 {
  def fromSeq(seq: DesignSeq): (ConnectedHub,DesignSeq) = {
    val (frontSegment, seq2) = MainSegment.fromSeq(seq)
    val (middleSegment, seq3) = MainSegment.fromSeq(seq2)
    val (rearSegment, seq4) = MainSegment.fromSeq(seq3)
    (ConnectedHub4_1_2_1(frontSegment, middleSegment, rearSegment), seq4)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[ConnectedHub], DesignSeq) = {
    BFParser.fromSeqBF_3Main(seq, ConnectedHub4_1_2_1(_,_,_))
  }
}

case class ConnectedHub4_1_2_1(frontSegment: MainSegment, middleSegment: MainSegment,
                               rearSegment: MainSegment) extends ConnectedHub {
  override val rotationAngle = 0.0
  val mainHub = MainHub4()
  val orient = Orient()
  override def apply(topConnector: Connection, bottomConnector: Connection,
                     powerBus: Connection, bodyRotParam: NamedParam)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()
    val c2 = g.createConnection()
    val c3 = g.createConnection()
    val c4 = g.createConnection()
    val oc = g.createConnection()
    mainHub(topConnector,bottomConnector,oc,c1,c2,c3,c4) ++
      orient(oc, bodyRotParam) ++
      frontSegment(c1,powerBus,Forward) ++
      middleSegment(c2,powerBus,Right) ++
      rearSegment(c3,powerBus,Backward) ++
      middleSegment(c4,powerBus,Left)
  }
}

object ConnectedHub4_2_2 {
  def fromSeq(seq: DesignSeq): (ConnectedHub,DesignSeq) = {
    val (frontSegment, seq2) = MainSegment.fromSeq(seq)
    val (rearSegment, seq3) = MainSegment.fromSeq(seq2)
    (ConnectedHub4_2_2(frontSegment, rearSegment), seq3)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[ConnectedHub], DesignSeq) = {
    BFParser.fromSeqBF_2Main(seq, ConnectedHub4_2_2(_,_))
  }
}

case class ConnectedHub4_2_2(frontSegment: MainSegment,
                             rearSegment: MainSegment) extends ConnectedHub {
  override val rotationAngle = 45.0
  val mainHub = MainHub4()
  val orient = Orient()
  override def apply(topConnector: Connection, bottomConnector: Connection,
                     powerBus: Connection, bodyRotParam: NamedParam)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()
    val c2 = g.createConnection()
    val c3 = g.createConnection()
    val c4 = g.createConnection()
    val oc = g.createConnection()
    mainHub(topConnector,bottomConnector,oc,c1,c2,c3,c4) ++
    orient(oc, bodyRotParam) ++
    frontSegment(c1,powerBus,Left) ++
    frontSegment(c2,powerBus,Right) ++
    rearSegment(c3,powerBus,Right) ++
    rearSegment(c4,powerBus,Left)
  }
}

object ConnectedHub5_Sym {
  def fromSeq(seq: DesignSeq): (ConnectedHub,DesignSeq) = {
    val (mainSegment, seq2) = MainSegment.fromSeq(seq)
    (ConnectedHub5_Sym(mainSegment), seq2)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[ConnectedHub], DesignSeq) = {
    BFParser.fromSeqBF_1Main(seq, ConnectedHub5_Sym(_))
  }

}

case class ConnectedHub5_Sym(mainSegment: MainSegment) extends ConnectedHub {
  override val rotationAngle = 0.0
  val mainHub = MainHub5()
  val orient = Orient()
  override def apply(topConnector: Connection, bottomConnector: Connection,
                     powerBus: Connection, bodyRotParam: NamedParam)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()
    val c2 = g.createConnection()
    val c3 = g.createConnection()
    val c4 = g.createConnection()
    val c5 = g.createConnection()
    val oc = g.createConnection()
    mainHub(topConnector,bottomConnector,oc,c1,c2,c3,c4,c5) ++
    orient(oc, bodyRotParam) ++
    mainSegment(c1,powerBus,Forward) ++
    mainSegment(c2,powerBus,Right) ++
    mainSegment(c3,powerBus,Right) ++
    mainSegment(c4,powerBus,Left) ++
    mainSegment(c5,powerBus,Left)
  }
}

object ConnectedHub5_4_1 {
  def fromSeq(seq: DesignSeq): (ConnectedHub,DesignSeq) = {
    val (centerSegment, seq2) = MainSegment.fromSeq(seq)
    val (mainSegment, seq3) = MainSegment.fromSeq(seq2)
    (ConnectedHub5_4_1(centerSegment, mainSegment), seq3)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[ConnectedHub], DesignSeq) = {
    BFParser.fromSeqBF_2Main(seq, ConnectedHub5_4_1(_,_))
  }
}

case class ConnectedHub5_4_1(centerSegment: MainSegment,
                             mainSegment: MainSegment) extends ConnectedHub {
  override val rotationAngle = 0.0
  val mainHub = MainHub5()
  val orient = Orient()
  override def apply(topConnector: Connection, bottomConnector: Connection,
                     powerBus: Connection, bodyRotParam: NamedParam)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()
    val c2 = g.createConnection()
    val c3 = g.createConnection()
    val c4 = g.createConnection()
    val c5 = g.createConnection()
    val oc = g.createConnection()
    mainHub(topConnector,bottomConnector,oc,c1,c2,c3,c4,c5) ++
    orient(oc, bodyRotParam) ++
    centerSegment(c1,powerBus,Forward) ++
    mainSegment(c2,powerBus,Right) ++
    mainSegment(c3,powerBus,Right) ++
    mainSegment(c4,powerBus,Left) ++
    mainSegment(c5,powerBus,Left)
  }
}

object ConnectedHub5_2_2_1 {
  def fromSeq(seq: DesignSeq): (ConnectedHub,DesignSeq) = {
    val (centerSegment, seq2) = MainSegment.fromSeq(seq)
    val (frontSegment, seq3) = MainSegment.fromSeq(seq2)
    val (rearSegment, seq4) = MainSegment.fromSeq(seq3)
    (ConnectedHub5_2_2_1(centerSegment, frontSegment, rearSegment), seq4)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[ConnectedHub], DesignSeq) = {
    BFParser.fromSeqBF_3Main(seq, ConnectedHub5_2_2_1(_,_,_))
  }
}

case class ConnectedHub5_2_2_1(centerSegment: MainSegment,
                               frontSegment: MainSegment,
                               rearSegment: MainSegment) extends ConnectedHub {
  override val rotationAngle = 0.0
  val mainHub = MainHub5()
  val orient = Orient()
  override def apply(topConnector: Connection, bottomConnector: Connection,
                     powerBus: Connection, bodyRotParam: NamedParam)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()
    val c2 = g.createConnection()
    val c3 = g.createConnection()
    val c4 = g.createConnection()
    val c5 = g.createConnection()
    val oc = g.createConnection()
    mainHub(topConnector,bottomConnector,oc,c1,c2,c3,c4,c5) ++
    orient(oc, bodyRotParam) ++
    centerSegment(c1,powerBus,Forward) ++
    frontSegment(c2,powerBus,Right) ++
    rearSegment(c3,powerBus,Right) ++
    rearSegment(c4,powerBus,Left) ++
    frontSegment(c5,powerBus,Left)
  }
}

object ConnectedHub6_Sym {
  def fromSeq(seq: DesignSeq): (ConnectedHub,DesignSeq) = {
    val (mainSegment, seq2) = MainSegment.fromSeq(seq)
    (ConnectedHub6_Sym(mainSegment), seq2)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[ConnectedHub], DesignSeq) = {
    BFParser.fromSeqBF_1Main(seq, ConnectedHub6_Sym(_))
  }
}

case class ConnectedHub6_Sym(mainSegment: MainSegment) extends ConnectedHub {
  override val rotationAngle = 30.0
  val mainHub = MainHub6()
  val orient = Orient()
  override def apply(topConnector: Connection, bottomConnector: Connection,
                     powerBus: Connection, bodyRotParam: NamedParam)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()
    val c2 = g.createConnection()
    val c3 = g.createConnection()
    val c4 = g.createConnection()
    val c5 = g.createConnection()
    val c6 = g.createConnection()
    val oc = g.createConnection()
    mainHub(topConnector,bottomConnector,oc,c1,c2,c3,c4,c5,c6) ++
      orient(oc, bodyRotParam) ++
      mainSegment(c1,powerBus,Left) ++
      mainSegment(c2,powerBus,Right) ++
      mainSegment(c3,powerBus,Right) ++
      mainSegment(c4,powerBus,Right) ++
      mainSegment(c5,powerBus,Left) ++
      mainSegment(c6,powerBus,Left)
  }
}

object ConnectedHub6_Sym_Aligned{
  def fromSeq(seq: DesignSeq): (ConnectedHub,DesignSeq) = {
    val (mainSegment, seq2) = MainSegment.fromSeq(seq)
    (ConnectedHub6_Sym_Aligned(mainSegment), seq2)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[ConnectedHub], DesignSeq) = {
    BFParser.fromSeqBF_1Main(seq, ConnectedHub6_Sym_Aligned(_))
  }
}

case class ConnectedHub6_Sym_Aligned(mainSegment: MainSegment) extends ConnectedHub {
  override val rotationAngle = 0.0
  val mainHub = MainHub6()
  val orient = Orient()
  override def apply(topConnector: Connection, bottomConnector: Connection,
                     powerBus: Connection, bodyRotParam: NamedParam)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()
    val c2 = g.createConnection()
    val c3 = g.createConnection()
    val c4 = g.createConnection()
    val c5 = g.createConnection()
    val c6 = g.createConnection()
    val oc = g.createConnection()
    mainHub(topConnector,bottomConnector,oc,c1,c2,c3,c4,c5,c6) ++
      orient(oc, bodyRotParam) ++
      mainSegment(c1,powerBus,Forward) ++
      mainSegment(c2,powerBus,Right) ++
      mainSegment(c3,powerBus,Right) ++
      mainSegment(c4,powerBus,Backward) ++
      mainSegment(c5,powerBus,Left) ++
      mainSegment(c6,powerBus,Left)
  }
}

object ConnectedHub6_2_2_2 {
  def fromSeq(seq: DesignSeq): (ConnectedHub,DesignSeq) = {
    val (frontSegment, seq2) = MainSegment.fromSeq(seq)
    val (middleSegment, seq3) = MainSegment.fromSeq(seq2)
    val (rearSegment, seq4) = MainSegment.fromSeq(seq3)
    (ConnectedHub6_2_2_2(frontSegment, middleSegment, rearSegment), seq4)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[ConnectedHub], DesignSeq) = {
    BFParser.fromSeqBF_3Main(seq, ConnectedHub6_2_2_2(_,_,_))
  }
}

case class ConnectedHub6_2_2_2(frontSegment: MainSegment, middleSegment: MainSegment,
                               rearSegment: MainSegment) extends ConnectedHub {
  override val rotationAngle = 30.0  // correct value?
  val mainHub = MainHub6()
  val orient = Orient()
  override def apply(topConnector: Connection, bottomConnector: Connection,
                     powerBus: Connection, bodyRotParam: NamedParam)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()
    val c2 = g.createConnection()
    val c3 = g.createConnection()
    val c4 = g.createConnection()
    val c5 = g.createConnection()
    val c6 = g.createConnection()
    val oc = g.createConnection()
    mainHub(topConnector,bottomConnector,oc,c1,c2,c3,c4,c5,c6) ++
      orient(oc, bodyRotParam) ++
      frontSegment(c1,powerBus,Left) ++
      frontSegment(c2,powerBus,Right) ++
      middleSegment(c3,powerBus,Right) ++
      rearSegment(c4,powerBus,Right) ++
      rearSegment(c5,powerBus,Left) ++
      middleSegment(c6,powerBus,Left)
  }
}

object ConnectedHub6_1_2_2_1 {
  def fromSeq(seq: DesignSeq): (ConnectedHub,DesignSeq) = {
    val (frontCenterSegment, seq2) = MainSegment.fromSeq(seq)
    val (frontSegment, seq3) = MainSegment.fromSeq(seq2)
    val (rearSegment, seq4) = MainSegment.fromSeq(seq3)
    val (rearCenterSegment, seq5) = MainSegment.fromSeq(seq4)
    (ConnectedHub6_1_2_2_1(frontCenterSegment, frontSegment, rearSegment, rearCenterSegment), seq5)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[ConnectedHub], DesignSeq) = {
    BFParser.fromSeqBF_4Main(seq, ConnectedHub6_1_2_2_1(_,_,_,_))
  }
}

case class ConnectedHub6_1_2_2_1(frontCenterSegment: MainSegment, frontSegment: MainSegment,
                                 rearSegment: MainSegment, rearCenterSegment: MainSegment) extends ConnectedHub {
  override val rotationAngle = 0.0
  val mainHub = MainHub6()
  val orient = Orient()
  override def apply(topConnector: Connection, bottomConnector: Connection,
                     powerBus: Connection, bodyRotParam: NamedParam)(implicit g: LowLevelGenerator):LowLevelAssembly = {
    val c1 = g.createConnection()
    val c2 = g.createConnection()
    val c3 = g.createConnection()
    val c4 = g.createConnection()
    val c5 = g.createConnection()
    val c6 = g.createConnection()
    val oc = g.createConnection()
    mainHub(topConnector,bottomConnector,oc,c1,c2,c3,c4,c5,c6) ++
      orient(oc,bodyRotParam) ++
      frontCenterSegment(c1,powerBus,Forward) ++
      frontSegment(c2,powerBus,Right) ++
      rearSegment(c3,powerBus,Right) ++
      rearCenterSegment(c4,powerBus,Backward) ++
      rearSegment(c5,powerBus,Left) ++
      frontSegment(c6,powerBus,Left)
  }
}

trait MainSegment {
  def apply(connector: Connection, powerBus: Connection, symmetryCategory: SymmetryCategory)
            (implicit g: LowLevelGenerator): LowLevelAssembly
}

object MainSegment {

  def fromSeqBF(seq: DesignSeq): (BFParseResult[MainSegment], DesignSeq) = {
    val (node_type, seq2) = readKeyValue[String](seq, "node_type")
    node_type match {
      case "PropArm" => PropArm.fromSeqBF(seq2)
      case "AngledPropArm" => AngledPropArm.fromSeqBF(seq2)
      case "WingArm" => WingArm.fromSeqBF(seq2)
      case "AngledWingArm" => AngledWingArm.fromSeqBF(seq2)
      case "RotatedArmSegment" => RotatedArmSegment.fromSeqBF(seq2)
      case "BendSegment" => BendSegment.fromSeqBF(seq2)
      case "DoubleBendSegment" => DoubleBendSegment.fromSeqBF(seq2)
      case "SidewaysBendSegment" => SidewaysBendSegment.fromSeqBF(seq2)
      case "SidewaysBendWithTopSegment" => SidewaysBendWithTopSegment.fromSeqBF(seq2)
      case "CrossSegment" => CrossSegment.fromSeqBF(seq2)
      case "BranchSegment_Sym" => BranchSegment_Sym.fromSeqBF(seq2)
      case "BranchWithTopSegment_Sym" => BranchWithTopSegment_Sym.fromSeqBF(seq2)
      case "BranchSegment_Asym" => BranchSegment_Asym.fromSeqBF(seq2)
      case "BranchWithTopSegment_Asym" => BranchWithTopSegment_Asym.fromSeqBF(seq2)
      case _ => throw new IllegalArgumentException(s"Unexpected node_type for MainSegment: $node_type")
    }
  }

  def fromSeq(seq: DesignSeq): (MainSegment,DesignSeq) = {
    val (node_type, seq2) = readKeyValue[String](seq, "node_type")
    node_type match {
      case "PropArm" => PropArm.fromSeq(seq2)
      case "AngledPropArm" => AngledPropArm.fromSeq(seq2)
      case "WingArm" => WingArm.fromSeq(seq2)
      case "AngledWingArm" => AngledWingArm.fromSeq(seq2)
      case "RotatedArmSegment" => RotatedArmSegment.fromSeq(seq2)
      case "BendSegment" => BendSegment.fromSeq(seq2)
      case "DoubleBendSegment" => DoubleBendSegment.fromSeq(seq2)
      case "SidewaysBendSegment" => SidewaysBendSegment.fromSeq(seq2)
      case "SidewaysBendWithTopSegment" => SidewaysBendWithTopSegment.fromSeq(seq2)
      case "CrossSegment" => CrossSegment.fromSeq(seq2)
      case "BranchSegment_Sym" => BranchSegment_Sym.fromSeq(seq2)
      case "BranchWithTopSegment_Sym" => BranchWithTopSegment_Sym.fromSeq(seq2)
      case "BranchSegment_Asym" => BranchSegment_Asym.fromSeq(seq2)
      case "BranchWithTopSegment_Asym" => BranchWithTopSegment_Asym.fromSeq(seq2)
      case _ => throw new IllegalArgumentException(s"Unexpected node_type for MainSegment: $node_type")
    }
  }
}

object PropArm {
  def fromSeq(seq: DesignSeq): (PropArm, DesignSeq) = {
    val (armLength, seq2) = readKeyValue[Double](seq, "armLength")
    val (motor, seq3) = Motor.fromSeq(seq2)
    val (prop, seq4) = Prop.fromSeq(seq3)
    val (flange, seq5) = FlangeWithSide.fromSeq(seq4)
    (PropArm(armLength, motor, prop, flange), seq5)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[MainSegment], DesignSeq) = {
    val (armLength, seq2) = readKeyValue[Double](seq, "armLength")
    BFParser.fromSeqBF_3(seq2, Motor.fromSeqBF(_), Prop.fromSeqBF(_), FlangeWithSide.fromSeqBF(_),
      PropArm(armLength,_,_,_))
  }
}

case class PropArm(armLength: Double,
                   motor: Motor,
                   prop: Prop,
                   flange: FlangeWithSide) extends MainSegment {
  val arm = Arm(armLength)
  override def apply(connector: Connection, powerBus: Connection, symmetryCategory: SymmetryCategory)
                     (implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()  // prop-motor
    val c2 = g.createConnection()  // flange-motor
    val c3 = g.createConnection()  // flange-arm
    val controlChannel = ControlChannelAuto
    arm(connector,c3) ++
      flange(c3,c2,symmetryCategory) ++             // side, top
      motor(c2,powerBus,c1,controlChannel) ++       // connector, powerIn, powerOut
      prop(c1,symmetryCategory)
  }
}

object AngledPropArm {
  def fromSeq(seq: DesignSeq): (AngledPropArm,DesignSeq) = {
    val (armLength, seq2) = readKeyValue[Double](seq, "armLength")
    val (motor, seq3) = Motor.fromSeq(seq2)
    val (prop, seq4) = Prop.fromSeq(seq3)
    (AngledPropArm(armLength, motor, prop), seq4)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[MainSegment], DesignSeq) = {
    val (armLength, seq2) = readKeyValue[Double](seq, "armLength")
    BFParser.fromSeqBF_2(seq2, Motor.fromSeqBF(_), Prop.fromSeqBF(_),
      AngledPropArm(armLength,_,_))
  }

}

case class AngledPropArm(armLength: Double,
                         motor: Motor,
                         prop: Prop) extends MainSegment {
  val flange = Flange()
  val arm = Arm(armLength)
  override def apply(connector: Connection, powerBus: Connection, symmetryCategory: SymmetryCategory)
                    (implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()  // prop-motor
    val c2 = g.createConnection()  // flange-motor
    val c3 = g.createConnection()  // flange-arm
    val controlChannel = ControlChannelAuto
    arm(connector,c3) ++
    flange(c2,c3) ++       // top, bottom
    motor(c2,powerBus,c1,controlChannel) ++       // connector, powerIn, powerOut
    prop(c1,symmetryCategory)
  }
}

object WingArm {
  def fromSeq(seq: DesignSeq): (WingArm, DesignSeq) = {
    val (armLength, seq2) = readKeyValue[Double](seq, "armLength")
    val (wing, seq3) = Wing.fromSeq(seq2)
    val (servo, seq4) = Servo.fromSeq(seq3)
    (WingArm(armLength, wing, servo), seq4)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[MainSegment], DesignSeq) = {
    val (armLength, seq2) = readKeyValue[Double](seq, "armLength")
    BFParser.fromSeqBF_2(seq2, Wing.fromSeqBF(_), Servo.fromSeqBF(_), WingArm(armLength,_,_))
  }
}

case class WingArm(armLength: Double,
                   wing: Wing,
                   servo: Servo) extends MainSegment {
  val arm = Arm(armLength)
  override def apply(connector: Connection, powerBus: Connection, symmetryCategory: SymmetryCategory)
                    (implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()
    val c2 = g.createConnection()
    g.ignore(powerBus) ++       // not needed, tell generator to ignore it
    arm(connector,c1) ++
    wing(c1,c2,symmetryCategory) ++
    servo(c2)
  }
}

object AngledWingArm {
  def fromSeq(seq: DesignSeq): (AngledWingArm,DesignSeq) = {
    val (arm1Length, seq2) = readKeyValue[Double](seq, "arm1Length")
    val (arm2Length, seq3) = readKeyValue[Double](seq2, "arm2Length")
    val (wing, seq4) = Wing.fromSeq(seq3)
    val (servo, seq5) = Servo.fromSeq(seq4)
    (AngledWingArm(arm1Length, arm2Length, wing, servo), seq5)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[MainSegment], DesignSeq) = {
    val (arm1Length, seq2) = readKeyValue[Double](seq, "arm1Length")
    val (arm2Length, seq3) = readKeyValue[Double](seq2, "arm2Length")
    BFParser.fromSeqBF_2(seq3, Wing.fromSeqBF(_), Servo.fromSeqBF(_),
      AngledWingArm(arm1Length,arm2Length,_,_))
  }
}

case class AngledWingArm(arm1Length: Double, arm2Length: Double,
                   wing: Wing,
                   servo: Servo) extends MainSegment {
  val wingArm = WingArm(arm2Length, wing, servo)
  val bendSegment = BendSegment(90.0, arm1Length, wingArm)
  override def apply(connector: Connection, powerBus: Connection, symmetryCategory: SymmetryCategory)
                    (implicit g: LowLevelGenerator): LowLevelAssembly = {
    bendSegment(connector, powerBus, symmetryCategory)
  }
}

object BendSegment {
  def fromSeq(seq: DesignSeq): (BendSegment,DesignSeq) = {
    val (angle, seq2) = readKeyValue[Double](seq, "angle")
    val (armLength, seq3) = readKeyValue[Double](seq2, "armLength")
    val (mainSegment, seq4) = MainSegment.fromSeq(seq3)
    (BendSegment(angle,armLength,mainSegment), seq4)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[MainSegment], DesignSeq) = {
    val (angle, seq2) = readKeyValue[Double](seq, "angle")
    val (armLength, seq3) = readKeyValue[Double](seq2, "armLength")
    BFParser.fromSeqBF_1Main(seq3, BendSegment(angle,armLength,_))
  }

}

case class BendSegment(angle: Double, armLength: Double, mainSegment: MainSegment) extends MainSegment {
  val bend = Bend(angle)
  val arm = RotatedArm(armLength, 90.0)
  //val arm2 = RotatedArm(50.0, -90.0)      // just a little stump here to rotate back to the original orientation...
  val arm2 = RotatedArm(50.0, 270.0)      // just a little stump here to rotate back to the original orientation...
  val endConn = Bend(0.0)                 // and another hub to get a female end connector as usual

  override def apply(connector: Connection, powerBus: Connection, symmetryCategory: SymmetryCategory)
                    (implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()
    val c2 = g.createConnection()
    val c3 = g.createConnection()
    val c4 = g.createConnection()
    arm(connector,c1,symmetryCategory) ++
    bend(c1,c2,symmetryCategory) ++
    arm2(c2,c3,symmetryCategory) ++
    endConn(c3,c4,symmetryCategory) ++
    mainSegment(c4,powerBus,symmetryCategory)
  }
}

object DoubleBendSegment {
  def fromSeq(seq: DesignSeq): (DoubleBendSegment,DesignSeq) = {
    val (angle, seq2) = readKeyValue[Double](seq, "angle")
    val (arm1Length, seq3) = readKeyValue[Double](seq2, "arm1Length")
    val (arm2Length, seq4) = readKeyValue[Double](seq3, "arm2Length")
    val (mainSegment, seq5) = MainSegment.fromSeq(seq4)
    (DoubleBendSegment(angle,arm1Length,arm2Length,mainSegment), seq5)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[MainSegment], DesignSeq) = {
    val (angle, seq2) = readKeyValue[Double](seq, "angle")
    val (arm1Length, seq3) = readKeyValue[Double](seq2, "arm1Length")
    val (arm2Length, seq4) = readKeyValue[Double](seq3, "arm2Length")
    BFParser.fromSeqBF_1Main(seq4, DoubleBendSegment(angle,arm1Length,arm2Length,_))
  }
}

case class DoubleBendSegment(angle: Double, arm1Length: Double, arm2Length: Double,
                             mainSegment: MainSegment) extends MainSegment {
  val bend1 = Bend(angle)
  val bend2 = Bend(360.0-angle)
  val arm1 = RotatedArm(arm1Length, 90.0)
  val arm2 = Arm(arm2Length)
  val arm3 = RotatedArm(50.0, 270.0)      // just a little stump here to rotate back to the original orientation...
  val endConn = Bend(0.0)               // and another hub to get a female end connector as usual

  override def apply(connector: Connection, powerBus: Connection, symmetryCategory: SymmetryCategory)
                    (implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()
    val c2 = g.createConnection()
    val c3 = g.createConnection()
    val c4 = g.createConnection()
    val c5 = g.createConnection()
    val c6 = g.createConnection()
    arm1(connector,c1,symmetryCategory) ++
    bend1(c1,c2,symmetryCategory) ++
    arm2(c2,c3) ++
    bend2(c3,c4,symmetryCategory) ++
    arm3(c4,c5,symmetryCategory) ++
    endConn(c5,c6,symmetryCategory) ++
    mainSegment(c6,powerBus,symmetryCategory)
  }
}

object RotatedArmSegment {
  def fromSeq(seq: DesignSeq): (RotatedArmSegment,DesignSeq) = {
    val (armLength, seq2) = readKeyValue[Double](seq, "armLength")
    val (rot, seq3) = readKeyValue[Double](seq2, "rot")
    val (mainSegment, seq4) = MainSegment.fromSeq(seq3)
    (RotatedArmSegment(armLength, rot, mainSegment), seq4)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[MainSegment], DesignSeq) = {
    val (armLength, seq2) = readKeyValue[Double](seq, "armLength")
    val (rot, seq3) = readKeyValue[Double](seq2, "rot")
    BFParser.fromSeqBF_1Main(seq3, RotatedArmSegment(armLength, rot, _))
  }
}

case class RotatedArmSegment(armLength: Double, rot: Double, mainSegment: MainSegment) extends MainSegment {
  val bend = Bend(0.0)
  val arm = RotatedArm(armLength, rot)

  override def apply(connector: Connection, powerBus: Connection, symmetryCategory: SymmetryCategory)
                    (implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()
    val c2 = g.createConnection()
    arm(connector,c1,symmetryCategory) ++
    bend(c1,c2,symmetryCategory) ++
    mainSegment(c2,powerBus,symmetryCategory)
  }
}

object SidewaysBendSegment {
  def fromSeq(seq: DesignSeq): (SidewaysBendSegment,DesignSeq) = {
    val (angle, seq2) = readKeyValue[Double](seq, "angle")
    val (armLength, seq3) = readKeyValue[Double](seq2, "armLength")
    val (mainSegment, seq4) = MainSegment.fromSeq(seq3)
    (SidewaysBendSegment(angle, armLength, mainSegment), seq4)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[MainSegment], DesignSeq) = {
    val (angle, seq2) = readKeyValue[Double](seq, "angle")
    val (armLength, seq3) = readKeyValue[Double](seq2, "armLength")
    BFParser.fromSeqBF_1Main(seq3, SidewaysBendSegment(angle, armLength, _))
  }
}

case class SidewaysBendSegment(angle: Double, armLength: Double, mainSegment: MainSegment) extends MainSegment {
  val bend = Bend(angle)
  val arm = Arm(armLength)

  override def apply(connector: Connection, powerBus: Connection, symmetryCategory: SymmetryCategory)
                    (implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()
    val c2 = g.createConnection()
    arm(connector,c1) ++
    bend(c1,c2,symmetryCategory) ++
    mainSegment(c2,powerBus,symmetryCategory)
  }
}

object SidewaysBendWithTopSegment {
  def fromSeq(seq: DesignSeq): (SidewaysBendWithTopSegment,DesignSeq) = {
    val (angle, seq2) = readKeyValue[Double](seq, "angle")
    val (armLength, seq3) = readKeyValue[Double](seq2, "armLength")
    val (mainSegment, seq4) = MainSegment.fromSeq(seq3)
    val (topSegment, seq5) = MainSegment.fromSeq(seq4)
    (SidewaysBendWithTopSegment(angle, armLength, mainSegment, topSegment), seq5)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[MainSegment], DesignSeq) = {
    val (angle, seq2) = readKeyValue[Double](seq, "angle")
    val (armLength, seq3) = readKeyValue[Double](seq2, "armLength")
    BFParser.fromSeqBF_2Main(seq3, SidewaysBendWithTopSegment(angle, armLength, _, _))
  }
}

case class SidewaysBendWithTopSegment(angle: Double, armLength: Double, mainSegment: MainSegment,
                                      topSegment: MainSegment) extends MainSegment {
  val bend = BendWithTop(angle)
  val arm = Arm(armLength)

  override def apply(connector: Connection, powerBus: Connection, symmetryCategory: SymmetryCategory)
                    (implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()
    val c2 = g.createConnection()
    val c3 = g.createConnection()
    arm(connector,c1) ++
    bend(c3,c1,c2,symmetryCategory) ++
    mainSegment(c2,powerBus,symmetryCategory) ++
    topSegment(c3,powerBus,symmetryCategory)
  }
}

object CrossSegment {
  def fromSeq(seq: DesignSeq): (CrossSegment,DesignSeq) = {
    val (armLength, seq2) = readKeyValue[Double](seq, "armLength")
    val (sideSegment, seq3) = MainSegment.fromSeq(seq2)
    val (centerSegment, seq4) = MainSegment.fromSeq(seq3)
    (CrossSegment(armLength, sideSegment, centerSegment), seq4)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[MainSegment], DesignSeq) = {
    val (armLength, seq2) = readKeyValue[Double](seq, "armLength")
    BFParser.fromSeqBF_2Main(seq2, CrossSegment(armLength, _, _))
  }
}

case class CrossSegment(armLength: Double, sideSegment: MainSegment, centerSegment: MainSegment) extends MainSegment {
  val hub = Cross()
  val arm = Arm(armLength)

  override def apply(connector: Connection, powerBus: Connection, symmetryCategory: SymmetryCategory)
                    (implicit g: LowLevelGenerator): LowLevelAssembly = {
    val arm2hub = g.createConnection()
    val hub2left = g.createConnection()
    val hub2right = g.createConnection()
    val hub2fwd = g.createConnection()
    arm(connector, arm2hub) ++
      hub(arm2hub,hub2left,hub2right,hub2fwd) ++
      sideSegment(hub2left, powerBus, symmetryCategory.getLeft()) ++
      sideSegment(hub2right, powerBus, symmetryCategory.getRight()) ++
      centerSegment(hub2fwd, powerBus, symmetryCategory.getForward())
  }
}

object BranchSegment_Sym {
  def fromSeq(seq: DesignSeq): (BranchSegment_Sym,DesignSeq) = {
    val (armLength, seq2) = readKeyValue[Double](seq, "armLength")
    val (angle, seq3) = readKeyValue[Double](seq2, "angle")
    val (mainSegment, seq4) = MainSegment.fromSeq(seq3)
    (BranchSegment_Sym(armLength, angle, mainSegment), seq4)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[MainSegment], DesignSeq) = {
    val (armLength, seq2) = readKeyValue[Double](seq, "armLength")
    val (angle, seq3) = readKeyValue[Double](seq2, "angle")
    BFParser.fromSeqBF_1Main(seq3, BranchSegment_Sym(armLength, angle, _))
  }
}

case class BranchSegment_Sym(armLength: Double, angle: Double, mainSegment: MainSegment) extends MainSegment {
  val branch = Branch(angle)
  val arm = Arm(armLength)

  override def apply(connector: Connection, powerBus: Connection, symmetryCategory: SymmetryCategory)
                    (implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()
    val c2 = g.createConnection()
    val c3 = g.createConnection()
    arm(connector,c1) ++
    branch(c1,c2,c3,symmetryCategory) ++
    mainSegment(c2,powerBus,symmetryCategory.getLeft()) ++
    mainSegment(c3,powerBus,symmetryCategory.getRight())
  }
}

object BranchSegment_Asym {
  def fromSeq(seq: DesignSeq): (BranchSegment_Asym,DesignSeq) = {
    val (armLength, seq2) = readKeyValue[Double](seq, "armLength")
    val (angle, seq3) = readKeyValue[Double](seq2, "angle")
    val (frontSegment, seq4) = MainSegment.fromSeq(seq3)
    val (rearSegment, seq5) = MainSegment.fromSeq(seq4)
    (BranchSegment_Asym(armLength, angle, frontSegment, rearSegment), seq5)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[MainSegment], DesignSeq) = {
    val (armLength, seq2) = readKeyValue[Double](seq, "armLength")
    val (angle, seq3) = readKeyValue[Double](seq2, "angle")
    BFParser.fromSeqBF_2Main(seq3, BranchSegment_Asym(armLength, angle, _, _))
  }
}

case class BranchSegment_Asym(armLength: Double, angle: Double, frontSegment: MainSegment, rearSegment: MainSegment) extends MainSegment {
  val branch = Branch(angle)
  val arm = Arm(armLength)

  override def apply(connector: Connection, powerBus: Connection, symmetryCategory: SymmetryCategory)
                    (implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()
    val c2 = g.createConnection()
    val c3 = g.createConnection()
    arm(connector,c1) ++
      branch(c1,c2,c3,symmetryCategory) ++
      frontSegment(c2,powerBus,symmetryCategory.getLeft()) ++
      rearSegment(c3,powerBus,symmetryCategory.getRight())
  }
}

object BranchWithTopSegment_Sym {
  def fromSeq(seq: DesignSeq): (BranchWithTopSegment_Sym,DesignSeq) = {
    val (armLength, seq2) = readKeyValue[Double](seq, "armLength")
    val (angle, seq3) = readKeyValue[Double](seq2, "angle")
    val (mainSegment, seq4) = MainSegment.fromSeq(seq3)
    val (topSegment, seq5) = MainSegment.fromSeq(seq4)
    (BranchWithTopSegment_Sym(armLength, angle, mainSegment, topSegment), seq5)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[MainSegment], DesignSeq) = {
    val (armLength, seq2) = readKeyValue[Double](seq, "armLength")
    val (angle, seq3) = readKeyValue[Double](seq2, "angle")
    BFParser.fromSeqBF_2Main(seq3, BranchWithTopSegment_Sym(armLength, angle, _, _))
  }
}

case class BranchWithTopSegment_Sym(armLength: Double, angle: Double, mainSegment: MainSegment,
                                    topSegment: MainSegment) extends MainSegment {
  val branch = BranchWithTop(angle)
  val arm = Arm(armLength)
  val branchTop = BranchTop(angle)

  override def apply(connector: Connection, powerBus: Connection, symmetryCategory: SymmetryCategory)
                    (implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()
    val c2 = g.createConnection()
    val c3 = g.createConnection()
    val c4 = g.createConnection()
    val c5 = g.createConnection()
    arm(connector,c1) ++
    branch(c4,c1,c2,c3,symmetryCategory) ++
    mainSegment(c2,powerBus,symmetryCategory.getLeft()) ++
    mainSegment(c3,powerBus,symmetryCategory.getRight()) ++
    branchTop(c4,c5,symmetryCategory) ++
    topSegment(c5,powerBus,symmetryCategory.getForward())
  }
}

object BranchWithTopSegment_Asym {
  def fromSeq(seq: DesignSeq): (BranchWithTopSegment_Asym,DesignSeq) = {
    val (armLength, seq2) = readKeyValue[Double](seq, "armLength")
    val (angle, seq3) = readKeyValue[Double](seq2, "angle")
    val (leftSegment, seq4) = MainSegment.fromSeq(seq3)
    val (rightSegment, seq5) = MainSegment.fromSeq(seq4)
    val (topSegment, seq6) = MainSegment.fromSeq(seq5)
    (BranchWithTopSegment_Asym(armLength, angle, leftSegment, rightSegment, topSegment), seq6)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[MainSegment], DesignSeq) = {
    val (armLength, seq2) = readKeyValue[Double](seq, "armLength")
    val (angle, seq3) = readKeyValue[Double](seq2, "angle")
    BFParser.fromSeqBF_3Main(seq3, BranchWithTopSegment_Asym(armLength, angle, _, _, _))
  }
}

case class BranchWithTopSegment_Asym(armLength: Double, angle: Double, leftSegment: MainSegment, rightSegment: MainSegment,
                                     topSegment: MainSegment) extends MainSegment {
  val branch = BranchWithTop(angle)
  val arm = Arm(armLength)
  val branchTop = BranchTop(angle)

  override def apply(connector: Connection, powerBus: Connection, symmetryCategory: SymmetryCategory)
                    (implicit g: LowLevelGenerator): LowLevelAssembly = {
    val c1 = g.createConnection()
    val c2 = g.createConnection()
    val c3 = g.createConnection()
    val c4 = g.createConnection()
    val c5 = g.createConnection()
    arm(connector,c1) ++
      branch(c4,c1,c2,c3,symmetryCategory) ++
      leftSegment(c2,powerBus,symmetryCategory.getLeft()) ++
      rightSegment(c3,powerBus,symmetryCategory.getRight()) ++
      branchTop(c4,c5,symmetryCategory) ++
      topSegment(c5,powerBus,symmetryCategory.getForward())
  }
}

// Adapter that goes on top of a branch, to fix a rotation issue
case class BranchTop(branchAngle: Double) {
  val bend = Bend(0.0)

  def apply(incomingConnector: Connection, outgoingConnector: Connection, symmetryCategory: SymmetryCategory)
                    (implicit g: LowLevelGenerator): LowLevelAssembly = {
    val arm = FixedRotationArm(50.0, symmetryCategory.branchTopRotation(branchAngle))
    val c1 = g.createConnection()
    arm(incomingConnector,c1) ++
      bend(c1,outgoingConnector,symmetryCategory)
  }
}

// --- Primitive components ---
// These correspond to components that exist in the component library.

object SingleBatteryFuselage {
  def fromSeq(seq: DesignSeq): (SingleBatteryFuselage,DesignSeq) = {
    val (length, seq1) = readKeyValue[Double](seq, "length")
    val (vertDiameter, seq2) = readKeyValue[Double](seq1, "vertDiameter")
    val (horzDiameter, seq3) = readKeyValue[Double](seq2, "horzDiameter")
    val (floorHeight, seq4) = readKeyValue[Double](seq3, "floorHeight")
    val (batteryX, seq5) = readKeyValue[Double](seq4, "batteryX")
    val (batteryY, seq6) = readKeyValue[Double](seq5, "batteryY")
    val (rpmX, seq7) = readKeyValue[Double](seq6, "rpmX")
    val (rpmY, seq8) = readKeyValue[Double](seq7, "rpmY")
    val (autoPilotX, seq9) = readKeyValue[Double](seq8, "autoPilotX")
    val (autoPilotY, seq10) = readKeyValue[Double](seq9, "autoPilotY")
    val (currentX, seq11) = readKeyValue[Double](seq10, "currentX")
    val (currentY, seq12) = readKeyValue[Double](seq11, "currentY")
    val (voltageX, seq13) = readKeyValue[Double](seq12, "voltageX")
    val (voltageY, seq14) = readKeyValue[Double](seq13, "voltageY")
    val (gpsX, seq15) = readKeyValue[Double](seq14, "gpsX")
    val (gpsY, seq16) = readKeyValue[Double](seq15, "gpsY")
    val (varioX, seq17) = readKeyValue[Double](seq16, "varioX")
    val (varioY, seq18) = readKeyValue[Double](seq17, "varioY")
    (SingleBatteryFuselage(length, vertDiameter, horzDiameter, floorHeight, batteryX, batteryY,
      rpmX, rpmY, autoPilotX, autoPilotY, currentX, currentY, voltageX, voltageY, gpsX, gpsY, varioX, varioY), seq18)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[SingleBatteryFuselage], DesignSeq) = {
    val (comp, seq2) = fromSeq(seq)
    (Complete(comp), seq2)
  }

}

case class SingleBatteryFuselage(length: Double, vertDiameter: Double, horzDiameter: Double, floorHeight: Double,
                                 batteryX: Double, batteryY: Double,
                                 rpmX: Double, rpmY: Double,
                                 autoPilotX: Double, autoPilotY: Double,
                                 currentX: Double, currentY: Double,
                                 voltageX: Double, voltageY: Double,
                                 gpsX: Double, gpsY: Double,
                                 varioX: Double, varioY: Double) {
  val debug = false

  def debugXOffset(offset: Double): Double = {
    if (debug) offset - 500.0 else offset
  }

  def apply(bottomConnector: Connection,
            batteryConnector: Connection,
            rpmConnector: Connection,
            autopilotConnector: Connection,
            currentConnector: Connection,
            voltageConnector: Connection,
            gpsConnector: Connection,
            varioConnector: Connection,
            bodyRotParam: NamedParam)
           (implicit g: LowLevelGenerator): LowLevelAssembly = {
    // Sanity check
    if (floorHeight > vertDiameter / 2.0){
      throw IllegalDesignException(s"Floor height must be =< fuselage vert diameter / 2!")
    }
    val componentName = g.createComponentInstanceName("Fuselage")
    val lengthParam = NamedParam("FUSELAGE_LENGTH", length, Some("fuselage_length"))
    val vertDiameterParam = NamedParam("FUSELAGE_VERT_DIAMETER", vertDiameter, Some("fuselage_vert"))
    val horzDiameterParam = NamedParam("FUSELAGE_HORZ_DIAMETER", horzDiameter, Some("fuselage_horz"))
    val floorHeightParam = NamedParam("FUSELAGE_FLOOR_HEIGHT", floorHeight, Some("fuselage_floor_height"))
    val batteryXParam = NamedParam("FUSELAGE_BATTERY_X_OFFSET", debugXOffset(batteryX), Some("fuselage_battery_x"))
    val batteryYParam = NamedParam("FUSELAGE_BATTERY_Y_OFFSET", batteryY, Some("fuselage_battery_y"))
    val rpmXParam = NamedParam("FUSELAGE_RPM_X_OFFSET", debugXOffset(rpmX), Some("fuselage_sensor_x"))
    val rpmYParam = NamedParam("FUSELAGE_RPM_Y_OFFSET", rpmY, Some("fuselage_sensor_y"))
    val autoPilotXParam = NamedParam("FUSELAGE_AUTOPILOT_X_OFFSET", debugXOffset(autoPilotX), Some("fuselage_sensor_x"))
    val autoPilotYParam = NamedParam("FUSELAGE_AUTOPILOT_Y_OFFSET", autoPilotY, Some("fuselage_sensor_y"))
    val currentXParam = NamedParam("FUSELAGE_CURRENT_X_OFFSET", debugXOffset(currentX), Some("fuselage_sensor_x"))
    val currentYParam = NamedParam("FUSELAGE_CURRENT_Y_OFFSET", currentY, Some("fuselage_sensor_y"))
    val voltageXParam = NamedParam("FUSELAGE_VOLTAGE_X_OFFSET", debugXOffset(voltageX), Some("fuselage_sensor_x"))
    val voltageYParam = NamedParam("FUSELAGE_VOLTAGE_Y_OFFSET", voltageY, Some("fuselage_sensor_y"))
    val gpsXParam = NamedParam("FUSELAGE_GPS_X_OFFSET", debugXOffset(gpsX), Some("fuselage_sensor_x"))
    val gpsYParam = NamedParam("FUSELAGE_GPS_Y_OFFSET", gpsY, Some("fuselage_sensor_y"))
    val varioXParam = NamedParam("FUSELAGE_VARIO_X_OFFSET", debugXOffset(varioX), Some("fuselage_sensor_x"))
    val varioYParam = NamedParam("FUSELAGE_VARIO_Y_OFFSET", varioY, Some("fuselage_sensor_y"))
    LowLevelAssembly(
      g.labeledComponentChoice(componentName,"UAV_Fuselage", "capsule_fuselage",
        Map(
        "BottomConnector" -> Some(bottomConnector),
        "FloorConnector1" -> Some(batteryConnector),
        "FloorConnector2" -> None,
        "FloorConnector3" -> Some(rpmConnector),
        "FloorConnector4" -> Some(autopilotConnector),
        "FloorConnector5" -> Some(currentConnector),
        "FloorConnector6" -> Some(voltageConnector),
        "FloorConnector7" -> Some(gpsConnector),
        "FloorConnector8" -> Some(varioConnector)
      )),
      List(
        lengthParam.connectToComponent(componentName, "FUSE_CYL_LENGTH"),
        vertDiameterParam.connectToComponent(componentName, "VERT_DIAMETER"),
        horzDiameterParam.connectToComponent(componentName, "HORZ_DIAMETER"),
        floorHeightParam.connectToComponent(componentName, "FLOOR_HEIGHT"),
        batteryXParam.connectToComponent(componentName, "FLOOR_CONNECTOR_1_DISP_LENGTH"),
        batteryYParam.connectToComponent(componentName, "FLOOR_CONNECTOR_1_DISP_WIDTH"),
        rpmXParam.connectToComponent(componentName, "FLOOR_CONNECTOR_3_DISP_LENGTH"),
        rpmYParam.connectToComponent(componentName, "FLOOR_CONNECTOR_3_DISP_WIDTH"),
        autoPilotXParam.connectToComponent(componentName, "FLOOR_CONNECTOR_4_DISP_LENGTH"),
        autoPilotYParam.connectToComponent(componentName, "FLOOR_CONNECTOR_4_DISP_WIDTH"),
        currentXParam.connectToComponent(componentName, "FLOOR_CONNECTOR_5_DISP_LENGTH"),
        currentYParam.connectToComponent(componentName, "FLOOR_CONNECTOR_5_DISP_WIDTH"),
        voltageXParam.connectToComponent(componentName, "FLOOR_CONNECTOR_6_DISP_LENGTH"),
        voltageYParam.connectToComponent(componentName, "FLOOR_CONNECTOR_6_DISP_WIDTH"),
        gpsXParam.connectToComponent(componentName, "FLOOR_CONNECTOR_7_DISP_LENGTH"),
        gpsYParam.connectToComponent(componentName, "FLOOR_CONNECTOR_7_DISP_WIDTH"),
        varioXParam.connectToComponent(componentName, "FLOOR_CONNECTOR_8_DISP_LENGTH"),
        varioYParam.connectToComponent(componentName, "FLOOR_CONNECTOR_8_DISP_WIDTH"),
        bodyRotParam.connectToComponent(componentName, "BOTTOM_CONNECTOR_ROTATION")
      )
    )
  }
}

object DualBatteryFuselage {
  def fromSeq(seq: DesignSeq): (DualBatteryFuselage,DesignSeq) = {
    val (length, seq1) = readKeyValue[Double](seq, "length")
    val (vertDiameter, seq2) = readKeyValue[Double](seq1, "vertDiameter")
    val (horzDiameter, seq3) = readKeyValue[Double](seq2, "horzDiameter")
    val (floorHeight, seq4) = readKeyValue[Double](seq3, "floorHeight")
    val (battery1X, seq5) = readKeyValue[Double](seq4, "battery1X")
    val (battery1Y, seq6) = readKeyValue[Double](seq5, "battery1Y")
    val (battery2X, seq7) = readKeyValue[Double](seq6, "battery2X")
    val (battery2Y, seq8) = readKeyValue[Double](seq7, "battery2Y")
    val (rpmX, seq9) = readKeyValue[Double](seq8, "rpmX")
    val (rpmY, seq10) = readKeyValue[Double](seq9, "rpmY")
    val (autoPilotX, seq11) = readKeyValue[Double](seq10, "autoPilotX")
    val (autoPilotY, seq12) = readKeyValue[Double](seq11, "autoPilotY")
    val (currentX, seq13) = readKeyValue[Double](seq12, "currentX")
    val (currentY, seq14) = readKeyValue[Double](seq13, "currentY")
    val (voltageX, seq15) = readKeyValue[Double](seq14, "voltageX")
    val (voltageY, seq16) = readKeyValue[Double](seq15, "voltageY")
    val (gpsX, seq17) = readKeyValue[Double](seq16, "gpsX")
    val (gpsY, seq18) = readKeyValue[Double](seq17, "gpsY")
    val (varioX, seq19) = readKeyValue[Double](seq18, "varioX")
    val (varioY, seq20) = readKeyValue[Double](seq19, "varioY")
    (DualBatteryFuselage(length, vertDiameter, horzDiameter, floorHeight, battery1X, battery1Y, battery2X, battery2Y,
      rpmX, rpmY, autoPilotX, autoPilotY, currentX, currentY, voltageX, voltageY, gpsX, gpsY, varioX, varioY), seq20)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[DualBatteryFuselage], DesignSeq) = {
    val (comp, seq2) = fromSeq(seq)
    (Complete(comp), seq2)
  }

}

case class DualBatteryFuselage(length: Double, vertDiameter: Double, horzDiameter: Double, floorHeight: Double,
                                 battery1X: Double, battery1Y: Double,
                                 battery2X: Double, battery2Y: Double,
                                 rpmX: Double, rpmY: Double,
                                 autoPilotX: Double, autoPilotY: Double,
                                 currentX: Double, currentY: Double,
                                 voltageX: Double, voltageY: Double,
                                 gpsX: Double, gpsY: Double,
                                 varioX: Double, varioY: Double) {
  val debug = false

  def debugXOffset(offset: Double): Double = {
    if (debug) offset - 500.0 else offset
  }

  def apply(bottomConnector: Connection,
            battery1Connector: Connection,
            battery2Connector: Connection,
            rpmConnector: Connection,
            autopilotConnector: Connection,
            currentConnector: Connection,
            voltageConnector: Connection,
            gpsConnector: Connection,
            varioConnector: Connection,
            bodyRotParam: NamedParam)
           (implicit g: LowLevelGenerator): LowLevelAssembly = {
    // Sanity check
    if (floorHeight > vertDiameter / 2.0){
      throw IllegalDesignException(s"Floor height must be =< fuselage vert diameter / 2!")
    }
    val componentName = g.createComponentInstanceName("Fuselage")
    val lengthParam = NamedParam("FUSELAGE_LENGTH", length, Some("fuselage_length"))
    val vertDiameterParam = NamedParam("FUSELAGE_VERT_DIAMETER", vertDiameter, Some("fuselage_vert"))
    val horzDiameterParam = NamedParam("FUSELAGE_HORZ_DIAMETER", horzDiameter, Some("fuselage_horz"))
    val floorHeightParam = NamedParam("FUSELAGE_FLOOR_HEIGHT", floorHeight, Some("fuselage_floor_height"))
    val battery1XParam = NamedParam("FUSELAGE_BATTERY1_X_OFFSET", debugXOffset(battery1X), Some("fuselage_battery_x"))
    val battery1YParam = NamedParam("FUSELAGE_BATTERY1_Y_OFFSET", battery1Y, Some("fuselage_battery_y"))
    val battery2XParam = NamedParam("FUSELAGE_BATTERY2_X_OFFSET", debugXOffset(battery2X), Some("fuselage_battery_x"))
    val battery2YParam = NamedParam("FUSELAGE_BATTERY2_Y_OFFSET", battery2Y, Some("fuselage_battery_y"))
    val rpmXParam = NamedParam("FUSELAGE_RPM_X_OFFSET", debugXOffset(rpmX), Some("fuselage_sensor_x"))
    val rpmYParam = NamedParam("FUSELAGE_RPM_Y_OFFSET", rpmY, Some("fuselage_sensor_y"))
    val autoPilotXParam = NamedParam("FUSELAGE_AUTOPILOT_X_OFFSET", debugXOffset(autoPilotX), Some("fuselage_sensor_x"))
    val autoPilotYParam = NamedParam("FUSELAGE_AUTOPILOT_Y_OFFSET", autoPilotY, Some("fuselage_sensor_y"))
    val currentXParam = NamedParam("FUSELAGE_CURRENT_X_OFFSET", debugXOffset(currentX), Some("fuselage_sensor_x"))
    val currentYParam = NamedParam("FUSELAGE_CURRENT_Y_OFFSET", currentY, Some("fuselage_sensor_y"))
    val voltageXParam = NamedParam("FUSELAGE_VOLTAGE_X_OFFSET", debugXOffset(voltageX), Some("fuselage_sensor_x"))
    val voltageYParam = NamedParam("FUSELAGE_VOLTAGE_Y_OFFSET", voltageY, Some("fuselage_sensor_y"))
    val gpsXParam = NamedParam("FUSELAGE_GPS_X_OFFSET", debugXOffset(gpsX), Some("fuselage_sensor_x"))
    val gpsYParam = NamedParam("FUSELAGE_GPS_Y_OFFSET", gpsY, Some("fuselage_sensor_y"))
    val varioXParam = NamedParam("FUSELAGE_VARIO_X_OFFSET", debugXOffset(varioX), Some("fuselage_sensor_x"))
    val varioYParam = NamedParam("FUSELAGE_VARIO_Y_OFFSET", varioY, Some("fuselage_sensor_y"))
    LowLevelAssembly(
      g.labeledComponentChoice(componentName, "UAV_Fuselage", "capsule_fuselage",
        Map(
        "BottomConnector" -> Some(bottomConnector),
        "FloorConnector1" -> Some(battery1Connector),
        "FloorConnector2" -> Some(battery2Connector),
        "FloorConnector3" -> Some(rpmConnector),
        "FloorConnector4" -> Some(autopilotConnector),
        "FloorConnector5" -> Some(currentConnector),
        "FloorConnector6" -> Some(voltageConnector),
        "FloorConnector7" -> Some(gpsConnector),
        "FloorConnector8" -> Some(varioConnector)
      )),
      List(
        lengthParam.connectToComponent(componentName, "FUSE_CYL_LENGTH"),
        vertDiameterParam.connectToComponent(componentName, "VERT_DIAMETER"),
        horzDiameterParam.connectToComponent(componentName, "HORZ_DIAMETER"),
        floorHeightParam.connectToComponent(componentName, "FLOOR_HEIGHT"),
        battery1XParam.connectToComponent(componentName, "FLOOR_CONNECTOR_1_DISP_LENGTH"),
        battery1YParam.connectToComponent(componentName, "FLOOR_CONNECTOR_1_DISP_WIDTH"),
        battery2XParam.connectToComponent(componentName, "FLOOR_CONNECTOR_2_DISP_LENGTH"),
        battery2YParam.connectToComponent(componentName, "FLOOR_CONNECTOR_2_DISP_WIDTH"),
        rpmXParam.connectToComponent(componentName, "FLOOR_CONNECTOR_3_DISP_LENGTH"),
        rpmYParam.connectToComponent(componentName, "FLOOR_CONNECTOR_3_DISP_WIDTH"),
        autoPilotXParam.connectToComponent(componentName, "FLOOR_CONNECTOR_4_DISP_LENGTH"),
        autoPilotYParam.connectToComponent(componentName, "FLOOR_CONNECTOR_4_DISP_WIDTH"),
        currentXParam.connectToComponent(componentName, "FLOOR_CONNECTOR_5_DISP_LENGTH"),
        currentYParam.connectToComponent(componentName, "FLOOR_CONNECTOR_5_DISP_WIDTH"),
        voltageXParam.connectToComponent(componentName, "FLOOR_CONNECTOR_6_DISP_LENGTH"),
        voltageYParam.connectToComponent(componentName, "FLOOR_CONNECTOR_6_DISP_WIDTH"),
        gpsXParam.connectToComponent(componentName, "FLOOR_CONNECTOR_7_DISP_LENGTH"),
        gpsYParam.connectToComponent(componentName, "FLOOR_CONNECTOR_7_DISP_WIDTH"),
        varioXParam.connectToComponent(componentName, "FLOOR_CONNECTOR_8_DISP_LENGTH"),
        varioYParam.connectToComponent(componentName, "FLOOR_CONNECTOR_8_DISP_WIDTH"),
        bodyRotParam.connectToComponent(componentName, "BOTTOM_CONNECTOR_ROTATION")
      )
    )
  }
}

case class MainHub2() {
  def apply(topConnector: Connection, bottomConnector: Connection, orientConnector: Connection,
             connector_1: Connection, connector_2: Connection)
            (implicit g: LowLevelGenerator): LowLevelAssembly = {
    val componentName = g.createComponentInstanceName("MainHub")
    LowLevelAssembly(
      g.labeledComponentChoice(componentName,"Para_Hub_2", "0394od_para_hub_2",
        Map(
        "Center_Connector" -> None,
//        "Top_Connector" -> Some(topConnector),
//        "Bottom_Connector" -> Some(bottomConnector),
        "Top_Connector" -> Some(bottomConnector),
        "Bottom_Connector" -> Some(topConnector),
        "Orient_Connector" -> Some(orientConnector),
        "Side_Connector_1" -> Some(connector_1),
        "Side_Connector_2" -> Some(connector_2)))
    )
  }
}

case class MainHub3(angle: Double) {
  val bigAngle = (360.0 - angle) / 2.0
  def apply(topConnector: Connection, bottomConnector: Connection, orientConnector: Connection,
             connector_1: Connection, connector_2: Connection, connector_3: Connection)
            (implicit g: LowLevelGenerator): LowLevelAssembly = {
    val componentName = g.createComponentInstanceName("MainHub")
    LowLevelAssembly(
      g.labeledComponentChoice(componentName,"Para_Hub_3","0394od_para_hub_3",
        Map(
          "Center_Connector" -> None,
          //        "Top_Connector" -> Some(topConnector),
          //        "Bottom_Connector" -> Some(bottomConnector),
          "Top_Connector" -> Some(bottomConnector),
          "Bottom_Connector" -> Some(topConnector),
          "Orient_Connector" -> Some(orientConnector),
          "Side_Connector_1" -> Some(connector_2),
          "Side_Connector_2" -> Some(connector_1),
          "Side_Connector_3" -> Some(connector_3))),
      List(TunableParam("MainHub", componentName, "ANGHORZCONN", bigAngle)))
  }
}

case class MainHub4() {
  def apply(topConnector: Connection, bottomConnector: Connection, orientConnector: Connection,
             connector_1: Connection, connector_2: Connection, connector_3: Connection, connector_4: Connection)
            (implicit g: LowLevelGenerator)= {
    val componentName = g.createComponentInstanceName("MainHub")
    LowLevelAssembly(
      g.labeledComponentChoice(componentName,"Para_Hub_4", "0394od_para_hub_4",
        Map(
        "Center_Connector" -> None,
//        "Top_Connector" -> Some(topConnector),
//        "Bottom_Connector" -> Some(bottomConnector),
        "Top_Connector" -> Some(bottomConnector),
        "Bottom_Connector" -> Some(topConnector),
        "Orient_Connector" -> Some(orientConnector),
        "Side_Connector_1" -> Some(connector_3),
        "Side_Connector_2" -> Some(connector_2),
        "Side_Connector_3" -> Some(connector_1),
        "Side_Connector_4" -> Some(connector_4)))
    )
  }
}

case class MainHub5() {
  def apply(topConnector: Connection, bottomConnector: Connection, orientConnector: Connection,
             connector_1: Connection, connector_2: Connection, connector_3: Connection,
             connector_4: Connection, connector_5: Connection)
            (implicit g: LowLevelGenerator) = {
    val componentName = g.createComponentInstanceName("MainHub")
    LowLevelAssembly(
      g.labeledComponentChoice(componentName, "Para_Hub_5","0394od_para_hub_5",
        Map(
        "Center_Connector" -> None,
        //        "Top_Connector" -> Some(topConnector),
        //        "Bottom_Connector" -> Some(bottomConnector),
        "Top_Connector" -> Some(bottomConnector),
        "Bottom_Connector" -> Some(topConnector),
        "Orient_Connector" -> Some(orientConnector),
        "Side_Connector_1" -> Some(connector_3),
        "Side_Connector_2" -> Some(connector_2),
        "Side_Connector_3" -> Some(connector_1),
        "Side_Connector_4" -> Some(connector_4),
        "Side_Connector_5" -> Some(connector_5)))
    )
  }
}

case class MainHub6() {
  def apply(topConnector: Connection, bottomConnector: Connection, orientConnector: Connection,
            connector_1: Connection, connector_2: Connection, connector_3: Connection,
            connector_4: Connection, connector_5: Connection, connector_6: Connection)
           (implicit g: LowLevelGenerator) = {
    val componentName = g.createComponentInstanceName("MainHub")
    LowLevelAssembly(
      g.labeledComponentChoice(componentName, "Para_Hub_6", "0394od_para_hub_6",
        Map(
        "Center_Connector" -> None,
        //        "Top_Connector" -> Some(topConnector),
        //        "Bottom_Connector" -> Some(bottomConnector),
        "Top_Connector" -> Some(bottomConnector),
        "Bottom_Connector" -> Some(topConnector),
        "Orient_Connector" -> Some(orientConnector),
        "Side_Connector_1" -> Some(connector_3),
        "Side_Connector_2" -> Some(connector_2),
        "Side_Connector_3" -> Some(connector_1),
        "Side_Connector_4" -> Some(connector_4),
        "Side_Connector_5" -> Some(connector_5),
        "Side_Connector_6" -> Some(connector_6)))
    )
  }
}

case class Flange() {
  def apply(topConnector: Connection, bottomConnector: Connection)
           (implicit g: LowLevelGenerator): LowLevelAssembly = {
    val componentName = g.createComponentInstanceName("Flange")
    LowLevelAssembly(
      g.labeledComponentChoice(componentName, "Flange", "0394_para_flange",
        Map(
          "TopConnector" -> Some(topConnector),
          "BottomConnector" -> Some(bottomConnector)))
    )
  }
}

object FlangeWithSide {
  def fromSeq(seq: DesignSeq): (FlangeWithSide, DesignSeq) = {
    val (offset, seq2) = readKeyValue[Double](seq, "offset")
    val (angle, seq3) = readKeyValue[Double](seq2, "angle")
    (FlangeWithSide(offset,angle), seq3)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[FlangeWithSide], DesignSeq) = {
    val (comp, seq2) = fromSeq(seq)
    (Complete(comp), seq2)
  }

}

case class FlangeWithSide(offset: Double, angle: Double) {
  val nameBase = createUniqueName("Flange")
  def apply(sideConnector: Connection, topConnector: Connection,
            //bottomConnector: Connection,
            symmetryCategory: SymmetryCategory)
           (implicit g: LowLevelGenerator): LowLevelAssembly = {
    //val finalOffset = symmetryCategory.adjustOffset(offset)
    val finalAngle = symmetryCategory.adjustAngle(angle)
    //val finalBottomAngle = symmetryCategory.adjustRotation(0.0)
    val componentName = g.createComponentInstanceName(nameBase)
    val finalAngleParam = NamedParam(componentName + "_CLOCK_ANGLE", finalAngle)
    //val finalBottomAngleParam = NamedParam(componentName + "_BOTTOM_ANGLE", finalBottomAngle)  // This does not seem to make any difference
    LowLevelAssembly(
      g.labeledComponentChoice(componentName, "Flange", "0394_para_flange",
        Map(
          "SideConnector" -> Some(sideConnector),
          "TopConnector" -> Some(topConnector)
          //"BottomConnector" -> bottomConnector
        )),
      List(
        //TunableParam(nameBase, componentName, "SIDEMOUNT_OFFSET", offset),
        finalAngleParam.connectToComponent(componentName, "SIDE_ANGLE")
        //finalBottomAngleParam.connectToComponent(componentName, "BOTTOM_ANGLE")
      )
    )
  }
}

// Not currently used
case class Support(length: Double) {
  val nameBase = createUniqueName("Support")
  def apply(connector: Connection)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    val componentName = g.createComponentInstanceName(nameBase)
    LowLevelAssembly(
      g.labeledComponentChoice(componentName, "Tube", "0394OD_para_tube",
        Map("BaseConnection" -> Some(connector))),  // ?? previously EndConnection1
      List(TunableParam(nameBase, componentName, "Length", length, Some("support_length")))
    )
  }
}

case class Arm(length: Double) {
  val nameBase = createUniqueName("Arm")
  def apply(endConnection1: Connection, endConnection2: Connection)
           (implicit g: LowLevelGenerator): LowLevelAssembly = {
    val componentName = g.createComponentInstanceName(nameBase)
    LowLevelAssembly(
      g.labeledComponentChoice(componentName, "Tube", "0394OD_para_tube",
        Map(
          "BaseConnection" -> Some(endConnection1),
          "EndConnection" -> Some(endConnection2))),
      List(TunableParam(nameBase, componentName, "Length", length, Some("arm_length")))
    )
  }
}

case class Arm3(length: Double) {
  val nameBase = createUniqueName("Arm3")
  def apply(endConnection1: Connection, endConnection2: Connection, offsetConnection: Connection)
            (implicit g: LowLevelGenerator): LowLevelAssembly = {
    val componentName = g.createComponentInstanceName(nameBase)
    LowLevelAssembly(
      g.labeledComponentChoice(componentName, "Tube", "0394OD_para_tube",
        Map(
          "BaseConnection" -> Some(endConnection1),
          "EndConnection" -> Some(endConnection2),
          "OffsetConnection2" -> Some(offsetConnection))),
      List(TunableParam(nameBase, componentName, "Length", length, Some("arm_length")))
    )
  }
}

case class Bend(angle: Double) {
  def apply(connector_1: Connection, connector_2: Connection, symmetryCategory: SymmetryCategory)
           (implicit g: LowLevelGenerator): LowLevelAssembly = {
    val finalAngle = (180.0 + symmetryCategory.adjustAngle(angle)) % 360.0
    val componentName = g.createComponentInstanceName("Bend")
    val finalAngleParam = NamedParam(componentName + "_CLOCK_ANGLE", finalAngle)
    LowLevelAssembly(
      g.labeledComponentChoice(componentName, "Para_Hub_2", "0394od_para_hub_2",
        Map(
          "Side_Connector_1" -> Some(connector_1),
          "Side_Connector_2" -> Some(connector_2))),
      List(
        finalAngleParam.connectToComponent(componentName, "ANGHORZCONN")
      )
    )
  }
}

case class BendWithTop(angle: Double) {
  def apply(topConnector: Connection, connector_1: Connection, connector_2: Connection,
            symmetryCategory: SymmetryCategory)
           (implicit g: LowLevelGenerator): LowLevelAssembly = {
    val finalAngle = (180.0 + symmetryCategory.adjustAngle(angle)) % 360.0
    val componentName = g.createComponentInstanceName("Bend")
    val finalAngleParam = NamedParam(componentName + "_CLOCK_ANGLE", finalAngle)
    LowLevelAssembly(
      g.labeledComponentChoice(componentName, "Para_Hub_2", "0394od_para_hub_2",
        Map(
          "Top_Connector" -> Some(topConnector),
          "Side_Connector_1" -> Some(connector_1),
          "Side_Connector_2" -> Some(connector_2))),
      List(
        finalAngleParam.connectToComponent(componentName, "ANGHORZCONN")
      )
    )
  }
}

case class Cross() {
  val angle = 90.0
  def apply(incomingConnector: Connection, leftConnector: Connection, rightConnector: Connection, forwardConnector: Connection)
           (implicit g: LowLevelGenerator): LowLevelAssembly = {
    val componentName = g.createComponentInstanceName("Cross")
    LowLevelAssembly(
      g.labeledComponentChoice(componentName, "Para_Hub_4", "0394od_para_hub_4",
        Map(
          "Side_Connector_1" -> Some(forwardConnector),
          "Side_Connector_2" -> Some(rightConnector),
          "Side_Connector_3" -> Some(incomingConnector),
          "Side_Connector_4" -> Some(leftConnector)
        )))
  }
}

case class Branch(angle: Double) {
  val nameBase = createUniqueName("Branch")
  val bigAngle = (360.0 - angle) / 2.0
  def apply(incomingConnector: Connection, leftConnector: Connection, rightConnector: Connection,
            symmetryCategory: SymmetryCategory)
           (implicit g: LowLevelGenerator): LowLevelAssembly = {
    val componentName = g.createComponentInstanceName(nameBase)
    LowLevelAssembly(
      g.labeledComponentChoice(componentName, "Para_Hub_3", "0394od_para_hub_3",
        Map(
            "Side_Connector_1" -> Some(if (symmetryCategory == Left) leftConnector else rightConnector),
            "Side_Connector_2" -> Some(incomingConnector),
            "Side_Connector_3" -> Some(if (symmetryCategory == Left) rightConnector else leftConnector)
        )),
      List(TunableParam(nameBase, componentName, "ANGHORZCONN", bigAngle))
    )
  }
}

case class BranchWithTop(angle: Double) {
  val nameBase = createUniqueName("Branch")
  val bigAngle = (360.0 - angle) / 2.0

  def apply(topConnector: Connection, incomingConnector: Connection, leftConnector: Connection, rightConnector: Connection,
            symmetryCategory: SymmetryCategory)
           (implicit g: LowLevelGenerator): LowLevelAssembly = {
    val componentName = g.createComponentInstanceName(nameBase)
    LowLevelAssembly(
      g.labeledComponentChoice(componentName,"Para_Hub_3","0394od_para_hub_3",
        Map(
          "Top_Connector" -> Some(topConnector),
          "Side_Connector_1" -> Some(if (symmetryCategory == Left) leftConnector else rightConnector),
          "Side_Connector_2" -> Some(incomingConnector),
          "Side_Connector_3" -> Some(if (symmetryCategory == Left) rightConnector else leftConnector)
        )),
      List(TunableParam(nameBase, componentName, "ANGHORZCONN", bigAngle))
    )
  }
}

case class RotatedArm(length: Double, rot: Double) {
  val nameBase = createUniqueName("RotatedArm")
  def apply(endConnection1: Connection, endConnection2: Connection, symmetryCategory: SymmetryCategory)
           (implicit g: LowLevelGenerator): LowLevelAssembly = {
    //val finalRot = symmetryCategory.adjustRotation(rot)
    val finalRot = symmetryCategory.adjustAngle(rot)
    val componentName = g.createComponentInstanceName(nameBase)
    val finalRotParam = NamedParam(componentName + "_ROT2", finalRot)
    LowLevelAssembly(
      g.labeledComponentChoice(componentName, "Tube", "0394OD_para_tube",
        Map(
          "BaseConnection" -> Some(endConnection1),
          "EndConnection" -> Some(endConnection2))),
      List(
        TunableParam(nameBase, componentName, "Length", length, Some("arm_length")),
        finalRotParam.connectToComponent(componentName, "END_ROT")  // ?? previously ROT2
        //finalRotParam.connectToComponent(componentName, "BASE_ROT")
      )
    )
  }
}

case class RotatedAngledArm(length: Double, rot: Double) {
  val nameBase = createUniqueName("RotatedAngledArm")
  def apply(endConnection1: Connection, endConnection2: Connection, symmetryCategory: SymmetryCategory)
           (implicit g: LowLevelGenerator): LowLevelAssembly = {
    //val finalRot = symmetryCategory.adjustRotation(symmetryCategory.adjustAngle(rot))
    val finalRot = symmetryCategory.adjustRotation(rot)
    val componentName = g.createComponentInstanceName(nameBase)
    val finalRotParam = NamedParam(componentName + "_ROT2", finalRot)
    LowLevelAssembly(
      g.labeledComponentChoice(componentName, "Tube", "0394OD_para_tube",
        Map(
          "BaseConnection" -> Some(endConnection1),
          "EndConnection" -> Some(endConnection2))),
      List(
        TunableParam(nameBase, componentName, "Length", length, Some("arm_length")),
        finalRotParam.connectToComponent(componentName, "END_ROT")  // ?? previously ROT2
        //finalRotParam.connectToComponent(componentName, "BASE_ROT")
      )
    )
  }
}

case class FixedRotationArm(length: Double, rot: Double) {
  val nameBase = createUniqueName("FixedRotationArm")
  def apply(endConnection1: Connection, endConnection2: Connection)
           (implicit g: LowLevelGenerator): LowLevelAssembly = {
    val componentName = g.createComponentInstanceName(nameBase)
    val rotParam = NamedParam(componentName + "_ROT", rot)
    LowLevelAssembly(
      g.labeledComponentChoice(componentName, "Tube", "0394OD_para_tube",
        Map(
          "BaseConnection" -> Some(endConnection1),
          "EndConnection" -> Some(endConnection2))),
      List(
        TunableParam(nameBase, componentName, "Length", length, Some("arm_length")),
        rotParam.connectToComponent(componentName, "END_ROT")
        //rotParam.connectToComponent(componentName, "BASE_ROT")
      )
    )
  }
}

case class Orient() {
  val nameBase = "Orient"  // we only have one, so make it less confusing
  def apply(connector: Connection, bodyRotParam: NamedParam)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    //val componentName = g.createComponentInstanceName(nameBase)
    val componentName = "Orient"  // Must have this name for Direct2CAD to work!
    LowLevelAssembly(
      g.labeledComponentChoice(componentName, "Orient", "Orient",
        Map("ORIENTCONN" -> Some(connector))),
      List(
        bodyRotParam.connectToComponent(componentName, "Z_ANGLE")
      ))
  }
}

object Battery {
  def fromSeq(seq: DesignSeq): (Battery, DesignSeq) = {
    val (batteryType, seq2) = readKeyValue[String](seq, "batteryType")
    (Battery(batteryType), seq2)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[Battery], DesignSeq) = {
    val (comp, seq2) = fromSeq(seq)
    (Complete(comp), seq2)
  }
}

// ??
case class Battery(batteryType: String) {
  def apply(connector: Connection, powerBus: Connection)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    LowLevelAssembly(
      g.componentChoice("Battery", batteryType, Map(
        "Bottom_Connector" -> Some(connector), // ??
        "PowerBus" -> Some(powerBus)))
    )
  }
}

object Servo {
  def fromSeq(seq: DesignSeq): (Servo, DesignSeq) = {
    val (servoType, seq2) = readKeyValue[String](seq, "servoType")
    (Servo(servoType), seq2)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[Servo], DesignSeq) = {
    val (comp, seq2) = fromSeq(seq)
    (Complete(comp), seq2)
  }
}

case class Servo(servoType: String) {
  def apply(connector: Connection)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    LowLevelAssembly(
      g.componentChoice("Servo", servoType,
        Map("Connector" -> Some(connector)))
    )
  }
}

case class Cargo(weight: Double) {
  val componentName = "Cargo"  // We only have one
  def apply(connector: Connection)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    val weightParam = NamedParam("CargoMass", weight)
    LowLevelAssembly(
      g.labeledComponentChoice("Cargo", componentName,"Cargo",
        Map("CargoConnector" -> Some(connector))),
      List(
        weightParam.connectToComponent(componentName, "WEIGHT")
      ))
  }
}

case class CargoCase(rot: Double) {
  val componentName = "CargoCase"  // We only have one
  def apply(hubConnector: Connection, cargoConnector: Connection)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    val rotParam = NamedParam("CargoCaseRotation", rot)
    LowLevelAssembly(
      g.labeledComponentChoice(componentName, "CargoCase", "CargoCase", Map(
        "CargoConnector" -> Some(cargoConnector),
        "Case2HubConnector" -> Some(hubConnector))),
      List(
        rotParam.connectToComponent(componentName, "Rotation")
      ))
  }
}

case class Sensor(sensorType: String) {
  val componentName = sensorType  // We only have one
  def apply(connector: Connection)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    LowLevelAssembly(
      g.componentChoice("Sensor", componentName,
        Map("SensorConnector" -> Some(connector))))
    // Also has a ROTATION property
  }
}

// Output_Voltage, Input_Voltage ??
case class BatteryController() {
  val nameBase = createUniqueName("BatteryController")
  def apply(motorPower: Connection, batteryPower: Connection)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    val componentName = g.createComponentInstanceName(nameBase)
    LowLevelAssembly(
      g.labeledComponentChoice(componentName, "BatteryController", "BatteryController",
        Map(
          "MotorPower" -> Some(motorPower),
          "BatteryPower" -> Some(batteryPower)
        ))
    )
  }
}

object Prop {
  def fromSeq(seq: DesignSeq): (Prop, DesignSeq) = {
    val (propType, seq2) = readKeyValue[String](seq, "propType")
    (Prop(propType), seq2)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[Prop], DesignSeq) = {
    val (comp, seq2) = fromSeq(seq)
    (Complete(comp), seq2)
  }
}

case class Prop(propType: String) {
  def apply(connector: Connection, symmetryCategory: SymmetryCategory)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    val componentName = g.createComponentInstanceName("Propeller")
    LowLevelAssembly(
      g.labeledComponentChoice(componentName, "Propeller", propType,
        Map(
          "MOTOR_CONNECTOR_CS_IN" -> Some(connector)
        )),
      List(
        symmetryCategory.getPropDirection().connectToComponent(componentName, "Direction"),
        symmetryCategory.getPropSpinType().connectToComponent(componentName, "Prop_type")
      )
    )
  }
}

object Motor {
  def fromSeq(seq: DesignSeq): (Motor, DesignSeq) = {
    val (motorType, seq2) = readKeyValue[String](seq, "motorType")
    (Motor(motorType), seq2)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[Motor], DesignSeq) = {
    val (comp, seq2) = fromSeq(seq)
    (Complete(comp), seq2)
  }
}

case class Motor(motorType: String) {
  def apply(connector: Connection, powerIn: Connection,
            powerOut: Connection, controlChannel: NamedParam)(implicit g: LowLevelGenerator): LowLevelAssembly = {
    val componentName = g.createComponentInstanceName("Motor")
    LowLevelAssembly(
      g.labeledComponentChoice(componentName, "Motor", motorType,
        Map(
          "Base_Connector" -> Some(connector),
          "MotorPower" -> Some(powerIn),
          "Prop_Connector" -> Some(powerOut))),
      List(
        controlChannel.connectToComponent(componentName, "CONTROL_CHANNEL")
      )
    )
  }
}

object Wing {
  def fromSeq(seq: DesignSeq): (Wing, DesignSeq) = {
    val (wingType, seq2) = readKeyValue[String](seq, "wingType")
    val (nacaProfile, seq3) = readKeyValue[String](seq2, "nacaProfile")
    val (span, seq4) = readKeyValue[Double](seq3, "span")
    val (chordInner, seq5) = readKeyValue[Double](seq4, "chordInner")
    val (chordOuter, seq6) = readKeyValue[Double](seq5, "chordOuter")
    val (taperOffset, seq7) = readKeyValue[Boolean](seq6, "taperOffset")
    val (aileronBias, seq8) = readKeyValue[Double](seq7, "aileronBias")
    val (flapBias, seq9) = readKeyValue[Double](seq8, "flapBias")
    val (load, seq10) = readKeyValue[Double](seq9, "load")
    val (offset, seq11) = readKeyValue[Double](seq10, "tubeOffset")
    val (rot, seq12) = readKeyValue[Double](seq11, "tubeRot")
    (Wing(wingType, nacaProfile, span, chordInner, chordOuter, taperOffset, aileronBias, flapBias, load, offset, rot), seq12)
  }

  def fromSeqBF(seq: DesignSeq): (BFParseResult[Wing], DesignSeq) = {
    val (comp, seq2) = fromSeq(seq)
    (Complete(comp), seq2)
  }
}

case class Wing(wingType: String, nacaProfile: String, span: Double, chordInner: Double, chordOuter: Double,
                taperOffset: Boolean, aileronBias: Double, flapBias: Double, load: Double, tubeOffset: Double,
                tubeRot: Double) {
  val nameBase = createUniqueName("Wing")
  val nacaProfileParam = NamedParam(nameBase + "_NACA_Profile", nacaProfile)
  val spanParam = NamedParam(nameBase + "_SPAN", span, Some("wing_span"))  // FIXME: We cannot optimize this, because tubeOffset depends on it
  val chordInnerParam = NamedParam(nameBase + "_CHORD_INNER", chordInner, Some("wing_chord_inner"))
  val chordOuterParam = NamedParam(nameBase + "_CHORD_OUTER", chordOuter, Some("wing_chord_outer"))
  val taperOffsetParam = NamedParam(nameBase + "_TAPER_OFFSET", taperOffsetToInt(taperOffset))
  val aileronBiasParam = NamedParam(nameBase + "_AILERON_BIAS", aileronBias, Some("wing_aileron_bias"))
  val flapBiasParam = NamedParam(nameBase + "_FLAP_BIAS", flapBias, Some("wing_flap_bias"))
  val loadParam = NamedParam(nameBase + "_LOAD", load, Some("wing_load"))

  def apply(tubeConnector: Connection, servoConnector: Connection, symmetryCategory: SymmetryCategory)
           (implicit g: LowLevelGenerator): LowLevelAssembly = {
    val componentName = g.createComponentInstanceName(nameBase)
    val tubeRotation = NamedParam(componentName + "_TUBE_ROT", symmetryCategory.adjustAngle(tubeRot))
    val finalTubeOffset = NamedParam(componentName + "_TUBE_OFFSET", symmetryCategory.adjustTubeOffset(tubeOffset, span))
    LowLevelAssembly(
      g.labeledComponentChoice(componentName,"Wing", wingType,
        Map(
          "Wing_Tube_Connector" -> Some(tubeConnector),
          "Wing_Servo_Connector" -> Some(servoConnector))),
        List(
          tubeRotation.connectToComponent(componentName, "TUBE_ROTATION"),
          finalTubeOffset.connectToComponent(componentName, "TUBE_OFFSET"),
          nacaProfileParam.connectToComponent(componentName, "NACA_Profile"),
          thicknessOfNacaProfile(nacaProfile).connectToComponent(componentName, "THICKNESS"),
          spanParam.connectToComponent(componentName, "SPAN"),
          aileronBiasParam.connectToComponent(componentName, "AILERON_BIAS"),
          chordInnerParam.connectToComponent(componentName, symmetryCategory.innerChord()),
          chordOuterParam.connectToComponent(componentName, symmetryCategory.outerChord()),
          flapBiasParam.connectToComponent(componentName, "FLAP_BIAS"),
          taperOffsetParam.connectToComponent(componentName, "TAPER_OFFSET"),
          loadParam.connectToComponent(componentName, "LOAD")
        )
    )
  }
}
