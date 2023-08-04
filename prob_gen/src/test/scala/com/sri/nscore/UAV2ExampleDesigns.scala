package com.sri.nscore

import com.sri.nscore.uav2._
import org.scalatest.FunSuite

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

class UAV2ExampleDesigns extends FunSuite {

  val componentLibrary = new UAV2ComponentLibrary("./data/components_v2.json",
    "./data/Aero_Corpus.csv", "./data/prop_motor_pairing.csv")
  val treeGenerator = new DefaultUAV2Generator(componentLibrary)
  val swriGenerator = new SWRIGenerator(componentLibrary)

  val outBase = Paths.get("src/test/resources/designs/uav2/")

  def saveDesign(treeDesign: UAV2Design): Unit = {
    val outPath = outBase.resolve(treeDesign.name)
    Files.createDirectories(outPath)
    val jsonStrTree = treeDesign.toJson()
    val outPathTree = outPath.resolve("design_tree.json")
    Files.write(outPathTree, jsonStrTree.getBytes(StandardCharsets.UTF_8))
    val swriDesign = swriGenerator.generate(treeDesign)
    val outPathSwri = outPath.resolve("design_swri.json")
    Files.write(outPathSwri, swriDesign.toJson().getBytes(StandardCharsets.UTF_8))
    nameIndices.clear()
  }

  test("QuadBend design") {
    val propArm = treeGenerator.generatePropArm()
    val angle = uniform(45.0, 315.0)
    val armLength = uniform(50.0, 500.0)
    val leg = BendSegment(angle, armLength, propArm)
    val hub = ConnectedHub4_Sym(leg)
    val plate = treeGenerator.generateFuselageWithComponents()
    val treeDesign = UAV2Design("QuadBend", hub, plate)
    saveDesign(treeDesign)
  }

  test("Twin design") {
    val wingArm = treeGenerator.generateAngledWingArm()
    val hub = ConnectedHub2_Sym_Wide(wingArm)
    val plate = treeGenerator.generateFuselageWithComponents()
    val treeDesign = UAV2Design("Twin", hub, plate)
    saveDesign(treeDesign)
  }

  test("DoubleBend test") {
    val wingArm = treeGenerator.generateAngledWingArm()
    val doubleBendSegment = DoubleBendSegment(45.0, 100.0, 100.0, wingArm)
    val hub = ConnectedHub2_Sym_Wide(doubleBendSegment)
    val plate = treeGenerator.generateFuselageWithComponents()
    val treeDesign = UAV2Design("DoubleBend", hub, plate)
    saveDesign(treeDesign)
  }

  test("Bend test") {
    val wingArm = treeGenerator.generateWingArm()
    val bendSegment = BendSegment(45.0, 100.0, wingArm)
    val hub = ConnectedHub2_Sym_Wide(bendSegment)
    val plate = treeGenerator.generateFuselageWithComponents()
    val treeDesign = UAV2Design("Bend", hub, plate)
    saveDesign(treeDesign)
  }

  test("Branch test") {
    val rear = treeGenerator.generateAngledWingArm()
    val front = treeGenerator.generatePropArm()
    val branchSegment = BranchSegment_Asym(100.0, 90.0, front, rear)
    val hub = ConnectedHub2_Sym_Wide(branchSegment)
    val plate = treeGenerator.generateFuselageWithComponents()
    val treeDesign = UAV2Design("Branch", hub, plate)
    saveDesign(treeDesign)
  }

  test("BranchWithTop test") {
    val leftBranch = treeGenerator.generateAngledPropArm()
    val rightBranch = treeGenerator.generatePropArm()
    val topSegment = treeGenerator.generateWingArm()
    val branchSegment = BranchWithTopSegment_Asym(100.0, 17.0, leftBranch, rightBranch, topSegment)
    val hub = ConnectedHub2_Sym_Wide(branchSegment)
    val plate = treeGenerator.generateFuselageWithComponents()
    val treeDesign = UAV2Design("BranchWithTop", hub, plate)
    saveDesign(treeDesign)
  }

  test("BendWithTop test") {
    val propArm = treeGenerator.generatePropArm()
    val topSegment = treeGenerator.generateAngledPropArm()
    val branchSegment = SidewaysBendWithTopSegment(30.0, 100.0, propArm, topSegment)
    val hub = ConnectedHub2_Sym_Wide(branchSegment)
    val plate = treeGenerator.generateFuselageWithComponents()
    val treeDesign = UAV2Design("BendWithTop", hub, plate)
    saveDesign(treeDesign)
  }

  test("Twin prop design") {
    val propArm = treeGenerator.generatePropArm()
    val hub = ConnectedHub2_Sym_Wide(propArm)
    val plate = treeGenerator.generateFuselageWithComponents()
    val treeDesign = UAV2Design("TwinProp", hub, plate)
    saveDesign(treeDesign)
  }

  test("RotatedArm test") {
    //val wingArm = treeGenerator.generateWingArm()
    val propArm = treeGenerator.generatePropArm()
    val len = treeGenerator.generateRotatedArmLength()
    val rot = 35.0
    val rotArm = RotatedArmSegment(len, rot, propArm)
    val hub = ConnectedHub2_Sym_Wide(rotArm)
    val plate = treeGenerator.generateFuselageWithComponents()
    val treeDesign = UAV2Design("RotatedArm", hub, plate)
    saveDesign(treeDesign)
  }

  test("3-2-1 design") {
    val sideArm = treeGenerator.generateAngledWingArm()
    val frontArm = treeGenerator.generateAngledPropArm()
    val hub = ConnectedHub3_2_1(30.0,frontArm,sideArm)
    val plate = treeGenerator.generateFuselageWithComponents()
    val treeDesign = UAV2Design("Tri_21", hub, plate)
    saveDesign(treeDesign)
  }

  test("Quad Sym design") {
    val component = treeGenerator.generateAngledWingArm()
    val hub = ConnectedHub4_Sym(component)
    val plate = treeGenerator.generateFuselageWithComponents()
    val treeDesign = UAV2Design("QuadCopter_sym", hub, plate)
    saveDesign(treeDesign)
  }

  test("Quad Sym Aligned design") {
    val component = treeGenerator.generatePropArm()
    val hub = ConnectedHub4_Sym_Aligned(component)
    val plate = treeGenerator.generateFuselageWithComponents()
    val treeDesign = UAV2Design("QuadCopter_sym_align", hub, plate)
    saveDesign(treeDesign)
  }

  test("QuadCopter 2-2 design") {
    //val front = treeGenerator.generatePropArm()
    val front = treeGenerator.generateAngledWingArm()
    val rear = treeGenerator.generateAngledWingArm()
    val hub = ConnectedHub4_2_2(front,rear)
    val plate = treeGenerator.generateFuselageWithComponents()
    val treeDesign = UAV2Design("QuadCopter_2_2", hub, plate)
    saveDesign(treeDesign)
  }

  test("QuadCopter 1-2-1 design") {
    //val front = treeGenerator.generatePropArm()
    val front = treeGenerator.generateAngledPropArm()
    val middle = treeGenerator.generateAngledWingArm()
    val rear = treeGenerator.generateAngledPropArm()
    val hub = ConnectedHub4_1_2_1(front,middle,rear)
    val plate = treeGenerator.generateFuselageWithComponents()
    val treeDesign = UAV2Design("QuadCopter_121", hub, plate)
    saveDesign(treeDesign)
  }

  test("QuadSpider design") {
    val propArm = treeGenerator.generatePropArm()
    val angle = uniform(45.0, 135.0)
    val arm1length = uniform(50.0, 500.0)
    val arm2length = uniform(50.0, 500.0)
    val leg = DoubleBendSegment(angle, arm1length, arm2length, propArm)
    val hub = ConnectedHub4_Sym(leg)
    val plate = treeGenerator.generateFuselageWithComponents()
    val treeDesign = UAV2Design("QuadSpider_reconstructed", hub, plate)
    saveDesign(treeDesign)
  }

  /* These are broken in the corpus.

  test("Penta design") {
    //val main = treeGenerator.generateBranchWithTopSegment_Asym()
    val front = treeGenerator.generateAngledPropArm()
    val middle = treeGenerator.generatePropArm()
    val rear = treeGenerator.generateAngledWingArm()
    val hub = ConnectedHub5_2_2_1(front,middle,rear)
    val plate = treeGenerator.generateFuselageWithComponents()
    val treeDesign = UAV2Design("Penta", hub, plate)
    saveDesign(treeDesign)
  }

  test("Penta 4-1 design") {
    //val main = treeGenerator.generateBranchWithTopSegment_Asym()
    val front = treeGenerator.generateAngledPropArm()
    val other = treeGenerator.generateAngledWingArm()
    val hub = ConnectedHub5_4_1(front,other)
    val plate = treeGenerator.generateFuselageWithComponents()
    val treeDesign = UAV2Design("Penta_41", hub, plate)
    saveDesign(treeDesign)
  }

  test("Penta 2-2-1 design") {
    val center = treeGenerator.generateAngledPropArm()
    val front = treeGenerator.generatePropArm()
    val rear = treeGenerator.generateAngledWingArm()
    val hub = ConnectedHub5_2_2_1(center,front,rear)
    val plate = treeGenerator.generateFuselageWithComponents()
    val treeDesign = UAV2Design("Penta_221", hub, plate)
    saveDesign(treeDesign)
  }
   */

  test("Hex_sym design") {
    val component = treeGenerator.generateAngledWingArm()
    val hub = ConnectedHub6_Sym(component)
    val plate = treeGenerator.generateFuselageWithComponents()
    val treeDesign = UAV2Design("Hex_sym", hub, plate)
    saveDesign(treeDesign)
  }

  test("Hex_sym_aligned design") {
    val component = treeGenerator.generateAngledWingArm()
    val hub = ConnectedHub6_Sym_Aligned(component)
    val plate = treeGenerator.generateFuselageWithComponents()
    val treeDesign = UAV2Design("Hex_sym_align", hub, plate)
    saveDesign(treeDesign)
  }

  test("Hex_222 design") {
    //val front = treeGenerator.generatePropArm()
    val front = treeGenerator.generateAngledWingArm()
    val middle = treeGenerator.generateAngledWingArm()
    //val rear = treeGenerator.generateAngledPropArm()
    val rear = treeGenerator.generateAngledWingArm()
    val hub = ConnectedHub6_2_2_2(front,middle,rear)
    val plate = treeGenerator.generateFuselageWithComponents()
    val treeDesign = UAV2Design("Hex_222", hub, plate)
    saveDesign(treeDesign)
  }

  test("Hex 1-2-2-1 design") {
    val frontProp = treeGenerator.generateAngledPropArm()
    val frontCenter = frontProp
//    val front = treeGenerator.generatePropArm()
//    val rear = treeGenerator.generateAngledWingArm()
//    val rearProp = treeGenerator.generateAngledPropArm()
//    val rearCenter = rearProp
    val front = treeGenerator.generateAngledWingArm()
    val rear = treeGenerator.generateAngledWingArm()
    val rearProp = treeGenerator.generatePropArm()
    val rearCenter = rearProp
    val hub = ConnectedHub6_1_2_2_1(frontCenter, front, rear, rearCenter)
    val plate = treeGenerator.generateFuselageWithComponents()
    val treeDesign = UAV2Design("Hex_1221", hub, plate)
    saveDesign(treeDesign)
  }

  test("Hex 1-2-2-1 ortho design") {
    val frontProp = treeGenerator.generateAngledPropArm()
    val frontCenter = frontProp
    val front = treeGenerator.generatePropArm()
    val frontBend = SidewaysBendSegment(bendAngleTo(60.0,0.0), 100.0, front)
    val rear = treeGenerator.generateAngledWingArm()
    val rearBend = SidewaysBendSegment(bendAngleTo(120.0,90.0), 100.0, rear)
    val rearProp = treeGenerator.generateAngledPropArm()
    val rearCenter = rearProp
    val hub = ConnectedHub6_1_2_2_1(frontCenter, frontBend, rearBend, rearCenter)
    val plate = treeGenerator.generateFuselageWithComponents()
    val treeDesign = UAV2Design("Hex_1221_ortho", hub, plate)
    saveDesign(treeDesign)
  }

  test("QuadWing design") {
    val frontPropArm = treeGenerator.generatePropArm()
    val front = BranchSegment_Sym(300.0, 180.0, frontPropArm)
    val middle = treeGenerator.generateAngledWingArm()
    val rearPropArm = treeGenerator.generatePropArm()
    val tailSideWing = treeGenerator.generateAngledWingArm()
    val tailRudder = treeGenerator.generatePropArm()
    val tailSegment = BranchWithTopSegment_Sym(400.0, 180.0, tailSideWing, tailRudder)
    val rear = CrossSegment(100.0, rearPropArm, tailSegment)
    val hub = ConnectedHub4_1_2_1(front,middle,rear)
    val fuselage = treeGenerator.generateFuselageWithComponents()
    tryUntilSuccess{
      val treeDesign = UAV2Design("QuadWing", hub, fuselage)
      saveDesign(treeDesign)
    }
  }

}
