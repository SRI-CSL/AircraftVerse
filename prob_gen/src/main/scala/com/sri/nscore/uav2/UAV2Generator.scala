package com.sri.nscore.uav2

import com.sri.nscore._

trait UAV2Generator {

  def generateUAVDesign(name: String): UAV2Design = {
    nameIndices.clear()
    val hub = generateConnectedHub()
    val fuselage = generateFuselageWithComponents()
    UAV2Design(name, hub, fuselage)
  }

  def generateFuselageWithComponents(): FuselageWithComponents

  def generateSingleBatteryFuselageWithComponents(): SingleBatteryFuselageWithComponents = {
    val battery = generateBattery()
    val fuselage = generateSingleBatteryFuselage()
    SingleBatteryFuselageWithComponents(battery, fuselage)
  }

  def generateDualBatteryFuselageWithComponents(): DualBatteryFuselageWithComponents = {
    val battery = generateBattery()
    val fuselage = generateDualBatteryFuselage()
    DualBatteryFuselageWithComponents(battery, fuselage)
  }

  def generateSingleBatteryFuselage(): SingleBatteryFuselage = {
    val length = generateFuselageLength()
    val vertDiameter = generateFuselageVertDiameter()
    val horzDiameter = generateFuselageHorzDiameter()
    val floorHeight = Math.min(generateFuselageFloorHeight(), vertDiameter / 2.0)
    val batteryX = generateBatteryOffsetX()
    val batteryY = generateBatteryOffsetY()
    val rpmX = generateRpmOffsetX()
    val rpmY = generateRpmOffsetY()
    val autoPilotX = generateAutoPilotOffsetX()
    val autoPilotY = generateAutoPilotOffsetY()
    val currentX = generateCurrentOffsetX()
    val currentY = generateCurrentOffsetY()
    val voltageX = generateVoltageOffsetX()
    val voltageY = generateVoltageOffsetY()
    val gpsX = generateGPSOffsetX()
    val gpsY = generateGPSOffsetY()
    val varioX = generateVariometerOffsetX()
    val varioY = generateVariometerOffsetY()
    SingleBatteryFuselage(length, vertDiameter, horzDiameter, floorHeight, batteryX, batteryY, rpmX, rpmY,
      autoPilotX, autoPilotY, currentX, currentY, voltageX, voltageY, gpsX, gpsY, varioX, varioY)
  }

  def generateDualBatteryFuselage(): DualBatteryFuselage = {
    val length = generateFuselageLength()
    val vertDiameter = generateFuselageVertDiameter()
    val horzDiameter = generateFuselageHorzDiameter()
    val floorHeight = Math.min(generateFuselageFloorHeight(), vertDiameter / 2.0)
    val battery1X = generateBatteryOffsetX()
    val battery1Y = generateBatteryOffsetY()
    val battery2X = generateBatteryOffsetX()
    val battery2Y = generateBatteryOffsetY()
    val rpmX = generateRpmOffsetX()
    val rpmY = generateRpmOffsetY()
    val autoPilotX = generateAutoPilotOffsetX()
    val autoPilotY = generateAutoPilotOffsetY()
    val currentX = generateCurrentOffsetX()
    val currentY = generateCurrentOffsetY()
    val voltageX = generateVoltageOffsetX()
    val voltageY = generateVoltageOffsetY()
    val gpsX = generateGPSOffsetX()
    val gpsY = generateGPSOffsetY()
    val varioX = generateVariometerOffsetX()
    val varioY = generateVariometerOffsetY()
    DualBatteryFuselage(length, vertDiameter, horzDiameter, floorHeight, battery1X, battery1Y, battery2X, battery2Y, rpmX, rpmY,
      autoPilotX, autoPilotY, currentX, currentY, voltageX, voltageY, gpsX, gpsY, varioX, varioY)
  }

  def generateConnectedHub2_Sym_Long(): ConnectedHub2_Sym_Long = {
    val mainSegment = generateMainSegment(true)
    ConnectedHub2_Sym_Long(mainSegment)
  }

  def generateConnectedHub2_Sym_Wide(): ConnectedHub2_Sym_Wide = {
    val mainSegment = generateMainSegment()
    ConnectedHub2_Sym_Wide(mainSegment)
  }

  def generateConnectedHub2_Asym(): ConnectedHub2_Asym = {
    val mainSegment1 = generateMainSegment(true)
    val mainSegment2 = generateMainSegment(true)
    ConnectedHub2_Asym(mainSegment1, mainSegment2)
  }

  def generateConnectedHub3_Sym(): ConnectedHub3_Sym = {
    val mainSegment = generateMainSegment(true)
    ConnectedHub3_Sym(120.0, mainSegment)
  }

  def generateConnectedHub3_2_1(): ConnectedHub3_2_1 = {
    val centerSegment = generateMainSegment(true)
    val angle = generateBranchAngle()
    val rearSegment = generateMainSegment()
    ConnectedHub3_2_1(angle, centerSegment, rearSegment)
  }

  def generateConnectedHub4_Sym(): ConnectedHub4_Sym = {
    val mainSegment = generateMainSegment()
    ConnectedHub4_Sym(mainSegment)
  }

  def generateConnectedHub4_Sym_Aligned(): ConnectedHub4_Sym_Aligned = {
    val mainSegment = generateMainSegment(true)
    ConnectedHub4_Sym_Aligned(mainSegment)
  }

  def generateConnectedHub4_2_2(): ConnectedHub4_2_2 = {
    val frontSegment = generateMainSegment()
    val rearSegment = generateMainSegment()
    ConnectedHub4_2_2(frontSegment, rearSegment)
  }

  def generateConnectedHub4_1_2_1(): ConnectedHub4_1_2_1 = {
    val frontSegment = generateMainSegment(true)
    val middleSegment = generateMainSegment()
    val rearSegment = generateMainSegment(true)
    ConnectedHub4_1_2_1(frontSegment, middleSegment, rearSegment)
  }

  def generateConnectedHub5_Sym(): ConnectedHub5_Sym = {
    val mainSegment = generateMainSegment(true)
    ConnectedHub5_Sym(mainSegment)
  }

  def generateConnectedHub5_4_1(): ConnectedHub5_4_1 = {
    val centerSegment = generateMainSegment(true)
    val mainSegment = generateMainSegment()
    ConnectedHub5_4_1(centerSegment, mainSegment)
  }

  def generateConnectedHub5_2_2_1(): ConnectedHub5_2_2_1 = {
    val centerSegment = generateMainSegment(true)
    val frontSegment = generateMainSegment()
    val rearSegment = generateMainSegment()
    ConnectedHub5_2_2_1(centerSegment, frontSegment, rearSegment)
  }

  def generateConnectedHub6_Sym(): ConnectedHub6_Sym = {
    val mainSegment = generateMainSegment()
    ConnectedHub6_Sym(mainSegment)
  }

  def generateConnectedHub6_Sym_Aligned(): ConnectedHub6_Sym_Aligned = {
    val mainSegment = generateMainSegment(true)
    ConnectedHub6_Sym_Aligned(mainSegment)
  }

  def generateConnectedHub6_2_2_2(): ConnectedHub6_2_2_2 = {
    val frontSegment = generateMainSegment()
    val middleSegment = generateMainSegment()
    val rearSegment = generateMainSegment()
    ConnectedHub6_2_2_2(frontSegment, middleSegment, rearSegment)
  }

  def generateConnectedHub6_1_2_2_1(): ConnectedHub6_1_2_2_1 = {
    val frontCenterSegment = generateMainSegment(true)
    val frontSegment = generateMainSegment()
    val rearSegment = generateMainSegment()
    val rearCenterSegment = generateMainSegment(true)
    ConnectedHub6_1_2_2_1(frontCenterSegment, frontSegment, rearSegment, rearCenterSegment)
  }

  def generateRotatedArmSegment(): RotatedArmSegment = {
    val armLength = generateRotatedArmLength()
    val rot = generateArmRotation()
    val mainSegment = generateMainSegment()
    RotatedArmSegment(armLength, rot, mainSegment)
  }

  def generatePropArm(center: Boolean = false): PropArm = {
    val armLength = generateArmLength()
    val (propType, motorType) = generatePropMotorTypes()
    val prop = generateProp(propType)
    val motor = generateMotor(motorType)
    val flange = generateFlangeWithSide()
    //val support = generateSupport()
    PropArm(armLength, motor, prop, flange)
  }

  def generateAngledPropArm(): AngledPropArm = {
    val armLength = generateArmLength()
    val (propType, motorType) = generatePropMotorTypes()
    val prop = generateProp(propType)
    val motor = generateMotor(motorType)
    AngledPropArm(armLength, motor, prop)
  }

  def generateBranchSegment_Sym(center: Boolean = false): BranchSegment_Sym = {
    val armLength = generateArmLength()
    val angle = generateBranchAngle()
    val mainSegment = generateMainSegment(center)
    BranchSegment_Sym(armLength, angle, mainSegment)
  }

  def generateBranchWithTopSegment_Sym(center: Boolean = false): BranchWithTopSegment_Sym = {
    val armLength = generateArmLength()
    val angle = generateBranchAngle()
    val mainSegment = generateMainSegment(center)
    val topSegment = generateMainSegment(center)
    BranchWithTopSegment_Sym(armLength, angle, mainSegment, topSegment)
  }

  def generateCrossSegment(center: Boolean = false): CrossSegment = {
    val armLength = generateArmLength()
    val sideSegment = generateMainSegment(!center)
    val centerSegment = generateMainSegment(center)
    CrossSegment(armLength, sideSegment, centerSegment)
  }

  def generateBranchSegment_Asym(): BranchSegment_Asym = {
    val armLength = generateArmLength()
    val angle = generateBranchAngle()
    val branchAngle = (360.0 - angle) / 2.0
    val leftSegment = generateMainSegment()
    val rightSegment = generateMainSegment()
    BranchSegment_Asym(armLength, angle, leftSegment, rightSegment)
  }

  def generateBranchWithTopSegment_Asym(): BranchWithTopSegment_Asym = {
    val armLength = generateArmLength()
    val angle = generateBranchAngle()
    val leftSegment = generateMainSegment()
    val rightSegment = generateMainSegment()
    val topSegment = generateMainSegment()
    BranchWithTopSegment_Asym(armLength, angle, leftSegment, rightSegment, topSegment)
  }

  def generateWingArm(): WingArm = {
    val armLength = generateArmLength()
    val wing = generateWing()
    val servo = generateServo()
    WingArm(armLength, wing, servo)
  }

  def generateAngledWingArm(): AngledWingArm = {
    val arm1Length = generateArmLength()
    val arm2Length = generateRiserLength()
    val wing = generateWing()
    val servo = generateServo()
    AngledWingArm(arm1Length, arm2Length, wing, servo)
  }

  def generateSupport(): Support = {
    val length = generateSupportLength()
    Support(length)
  }

  def generateSidewaysBendSegment(): SidewaysBendSegment = {
    val armLength = generateArmLength()
    val angle = generateBendAngle()
    val mainSegment = generateMainSegment()
    SidewaysBendSegment(angle, armLength, mainSegment)
  }

  def generateSidewaysBendWithTopSegment(): SidewaysBendWithTopSegment = {
    val armLength = generateArmLength()
    val angle = generateBendAngle()
    val mainSegment = generateMainSegment()
    val topSegment = generateMainSegment()
    SidewaysBendWithTopSegment(angle, armLength, mainSegment, topSegment)
  }

  def generateBendSegment(center: Boolean = false): BendSegment = {
    val angle = generateBendAngle()
    val armLength = generateArmLength()
    val mainSegment = generateMainSegment(center)
    BendSegment(angle, armLength, mainSegment)
  }

  def generateDoubleBendSegment(center: Boolean = false): DoubleBendSegment = {
    val angle = generateBendAngle()
    val arm1length = generateArmLength()
    val arm2length = generateArmLength()
    val mainSegment = generateMainSegment(center)
    DoubleBendSegment(angle, arm1length, arm2length, mainSegment)
  }

  def generateFlangeWithSide(center: Boolean = false): FlangeWithSide = {
    val offset = generateFlangeOffset()
    val angle = if (center) 0.0 else generateFlangeAngle()
    FlangeWithSide(offset, angle)
  }

  def generateWing(): Wing = {
    val wingType = generateWingType()
    val nacaProfile = generateNACAProfile()
    val span = generateWingSpan()
    val chordInner = generateWingChordInner()
    val chordOuter = generateWingChordOuter()
    val taperOffset = generateWingTaperOffset()
    val aileronBias = generateWingAileronBias()
    val flapBias = generateWingFlapBias()
    val load = generateWingLoad()
    val offset = generateWingTubeOffset()
    val rot = generateWingAngle()
    Wing(wingType, nacaProfile, span, chordInner, chordOuter, taperOffset, aileronBias, flapBias, load, offset, rot)
  }

  def generateProp(propType: String): Prop = {
    //val propType = generatePropType()
    Prop(propType)
  }

  def generateMotor(motorType: String): Motor = {
    //val motorType = generateMotorType()
    Motor(motorType)
  }

  def generateBattery(): Battery = {
    val batteryType = generateBatteryType()
    Battery(batteryType)
  }

  def generateServo(): Servo = {
    val servoType = generateServoType()
    Servo(servoType)
  }

  def generateNACAProfile(): String

  def generatePropMotorTypes(): (String, String)

  //  def generatePropType(): String
  //
  //  def generateMotorType(): String
  //
  //  def generateESCType(): String

  def generateFuselageLength(): Double

  def generateFuselageVertDiameter(): Double

  def generateFuselageHorzDiameter(): Double

  def generateFuselageFloorHeight(): Double

  def generateBatteryOffsetY(): Double

  def generateBatteryOffsetX(): Double

  def generateRpmOffsetX(): Double

  def generateRpmOffsetY(): Double

  def generateAutoPilotOffsetX(): Double

  def generateAutoPilotOffsetY(): Double

  def generateCurrentOffsetX(): Double

  def generateCurrentOffsetY(): Double

  def generateVoltageOffsetX(): Double

  def generateVoltageOffsetY(): Double

  def generateGPSOffsetX(): Double

  def generateGPSOffsetY(): Double

  def generateVariometerOffsetX(): Double

  def generateVariometerOffsetY(): Double

  def generateWingType(): String

  def generateWingTubeOffset(): Double

  def generateWingOffset(): Double

  def generateWingSpan(): Double

  def generateWingAileronBias(): Double

  def generateWingChordInner(): Double

  def generateWingChordOuter(): Double

  def generateWingFlapBias(): Double

  def generateWingTaperOffset(): Boolean

  def generateWingLoad(): Double

  def generateFlangeAngle(): Double

  def generateFlangeOffset(): Double

  def generateSupportLength(): Double

  def generateRiserLength(): Double

  def generateArmLength(): Double

  def generateRotatedArmLength(): Double

  def generateArmRotation(): Double

  def generateBranchAngle(): Double

  def generateBendAngle(): Double

  def generateWingAngle(): Double

  def generateServoType(): String

  def generateBatteryType(): String

  def generateMainSegment(center: Boolean = false): MainSegment

  def generateConnectedHub(): ConnectedHub

}
