package com.sri.nscore.uav2

import com.sri.nscore.{categoricalPick, uniform, uniformListPick, uniformPick}

class DefaultUAV2Generator(componentLibrary: UAV2ComponentLibrary) extends UAV2Generator {

  override def generateConnectedHub(): ConnectedHub = {
    uniformPick(
      //generateConnectedHub2_Sym_Long _,   // This one is not very good
      generateConnectedHub2_Sym_Wide _,
      //generateConnectedHub2_Asym _,       // This one is not very good
      //generateConnectedHub3_Sym _,        // This one is a bit broken, and not that interesting
      generateConnectedHub3_2_1 _,
      generateConnectedHub4_Sym _,
      generateConnectedHub4_Sym_Aligned _,
      generateConnectedHub4_2_2 _,
      generateConnectedHub4_1_2_1 _,
      // hub5 is broken, see https://git.isis.vanderbilt.edu/SwRI/uam-uav-models/-/issues/9
      //generateConnectedHub5_Sym _,
      //generateConnectedHub5_4_1 _,
      //generateConnectedHub5_2_2_1 _,
      generateConnectedHub6_Sym _,
      generateConnectedHub6_Sym_Aligned _,
      generateConnectedHub6_2_2_2 _,
      generateConnectedHub6_1_2_2_1 _,
    )()
  }

  override def generateMainSegment(center: Boolean = false): MainSegment = {
    if (center) generateCenterSegment() else {
      categoricalPick(
        (0.7, categoricalPick(
          (0.5, () => generatePropArm()),
          (0.2, generateAngledPropArm _),
          (0.2, generateAngledWingArm _),
          (0.1, generateWingArm _),
        )),
        (0.3, uniformPick(
          () => generateBendSegment(),
          () => generateDoubleBendSegment(),
          generateSidewaysBendSegment _,
          generateSidewaysBendWithTopSegment _,
          ()=>generateBranchSegment_Sym(),
          ()=>generateBranchWithTopSegment_Sym(),
          ()=>generateCrossSegment(),
          generateBranchSegment_Asym _,
          generateBranchWithTopSegment_Asym _,
        ))
      )()
    }
  }

  def generateCenterSegment(): MainSegment = {
    categoricalPick(
      (0.7, categoricalPick(
        (0.2, ()=>generatePropArm(true)),
        (0.5, generateAngledPropArm _),
        //(0.3, generateWingArm _)  // TODO: Vertical wing (rudder)
      )),
      (0.3, uniformPick(
        ()=>generateCrossSegment(true),
        ()=>generateBendSegment(true),
        ()=>generateDoubleBendSegment(true),
        ()=>generateBranchSegment_Sym(true),
        ()=>generateBranchWithTopSegment_Sym(true)
      ))
    )()
  }

  override def generateFuselageWithComponents(): FuselageWithComponents = {
    uniformPick(
      generateSingleBatteryFuselageWithComponents _,
      generateDualBatteryFuselageWithComponents _
    )()
  }

  override def generateArmRotation(): Double = {
    categoricalPick(
      (0.8, uniformPick(90.0, 180.0, 270.0)),
      (0.2, uniform(0.0, 260.0))
    )
  }

  override def generateArmLength(): Double = {
    uniform(50.0, 500.0)
  }

  override def generateRotatedArmLength(): Double = {
    uniform(50.0, 100.0)
  }

  override def generateSupportLength(): Double = {
    uniform(10.0, 300.0)
  }

  override def generateBendAngle(): Double = {
    categoricalPick(
      (0.7, -90.0),
      (0.3, uniform(-135.0, 135.0))
    )
  }

  override def generateBranchAngle(): Double = {
    categoricalPick(
      (0.4, 120.0),  // 3-way symmetry
      (0.4, 180.0),  // T-joint
      (0.2, uniform(45.0, 180.0))
    )
  }

  override def generateFlangeOffset(): Double = {
    uniform(-50.0, 50.0)
  }

  override def generateFlangeAngle(): Double = {
    categoricalPick(
      (0.7, 0.0),
      (0.2, uniformPick(90.0, 180.0, 270.0)),
      (0.1, uniform(0.0,360.0))
    )
  }

  override def generateWingType(): String = {
    uniformListPick(componentLibrary.getComponentNames("Wing"))
  }

  override def generateWingChordInner(): Double = {
    uniform(100.0, 500.0)
  }

  override def generateWingChordOuter(): Double = {
    uniform(50.0, 200.0)
  }

  override def generateWingTubeOffset(): Double = {
    uniform(10.0, 90.0)
  }

  override def generateWingOffset(): Double = {
    uniform(10.0, 90.0)
  }

  override def generateWingSpan(): Double = {
    uniform(200.0, 2000.0)
  }

  override def generateWingAngle(): Double = {
    categoricalPick(
      (0.5, 270.0),
      (0.5, uniform(220.0, 320.0))
    )
  }

  override def generateBatteryOffsetX(): Double = {
    uniform(-100.0,100.0)
    //-500  // for debugging
  }

  override def generateBatteryOffsetY(): Double = {
    0.0
    //uniform(0.0,120.0)
  }



//  override def generatePropType(): String = {
//    uniformListPick(componentLibrary.getComponentNames("ReversiblePropeller"))
//  }

//  override def generateMotorType(): String = {
//    uniformListPick(componentLibrary.getComponentNames("Motor"))
//  }

//  override def generateESCType(): String = {
//    uniformListPick(componentLibrary.getComponentNames("ESC"))
//  }

  override def generateRpmOffsetX(): Double = 0.0

  override def generateRpmOffsetY(): Double = 0.0

  override def generateAutoPilotOffsetX(): Double = 0.0

  override def generateAutoPilotOffsetY(): Double = 0.0

  override def generateCurrentOffsetX(): Double = 0.0

  override def generateCurrentOffsetY(): Double = 0.0

  override def generateVoltageOffsetX(): Double = 0.0

  override def generateVoltageOffsetY(): Double = 0.0

  override def generateGPSOffsetX(): Double = 0.0

  override def generateGPSOffsetY(): Double = 0.0

  override def generateVariometerOffsetX(): Double = 0.0

  override def generateVariometerOffsetY(): Double = 0.0

  override def generateNACAProfile(): String = {
    uniformListPick(componentLibrary.nacaProfiles)
  }

  override def generatePropMotorTypes(): (String,String) = {
    uniformListPick(componentLibrary.propMotorPairs)
  }

  override def generateServoType(): String = {
    uniformListPick(componentLibrary.getComponentNames("Servo"))
  }

  override def generateBatteryType(): String = {
    uniformListPick(componentLibrary.getComponentNames("Battery"))
  }

  override def generateRiserLength(): Double = {
    uniform(50.0, 200.0)
  }

  override def generateFuselageLength(): Double = {
    uniform(50.0, 200.0)
  }

  override def generateFuselageVertDiameter(): Double = {
    uniform(50.0, 200.0)
  }

  override def generateFuselageHorzDiameter(): Double = {
    uniform(120.0, 200.0)
  }

  override def generateFuselageFloorHeight(): Double = {
    uniform(10.0, 100.0)
  }

  override def generateWingAileronBias(): Double = {
    uniform(0.1, 1.0)  // default is 0.5
  }

  //  override def generateWingThickness(): Double = {
  //    uniform(6.0, 18.0)  // default is 12.0
  //  }

  override def generateWingFlapBias(): Double = {
    uniform(0.1, 1.0)  // default is 0.5
  }

  override def generateWingTaperOffset(): Boolean = {
    uniformPick(true, false)
  }

  override def generateWingLoad(): Double = {
    uniform(100.0, 1000.0)
  }

}