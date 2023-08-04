package com.sri.nscore

import com.sri.nscore.uav2.{BendSegment, ConnectedHub2_Asym, ConnectedHub2_Sym_Long, ConnectedHub2_Sym_Wide, ConnectedHub3_2_1, ConnectedHub3_Sym, ConnectedHub4_1_2_1, ConnectedHub4_2_2, ConnectedHub4_Sym, ConnectedHub4_Sym_Aligned, ConnectedHub5_2_2_1, ConnectedHub5_4_1, ConnectedHub5_Sym, ConnectedHub6_1_2_2_1, ConnectedHub6_2_2_2, DefaultUAV2Generator, UAV2ComponentLibrary, UAV2Design}
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.scalatest.FunSuite

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

class BFTest extends FunSuite {

  val componentLibrary = new UAV2ComponentLibrary("./data/components_v2.json",
    "./data/Aero_Corpus.csv", "./data/prop_motor_pairing.csv")
  val treeGenerator = new DefaultUAV2Generator(componentLibrary)
  val swriGenerator = new SWRIGenerator(componentLibrary)
  val outBase = Paths.get("src/test/resources/designs/uav2/")
  implicit val formats = Serialization.formats(NoTypeHints)

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

  def parseDesign(name: String): Unit = {
    val outPath = outBase.resolve(name)
    val designFilename = "design_seq_bf.json"
    val designSeqFile = outPath.resolve(designFilename).toFile
    val jsonStr = scala.reflect.io.File(designSeqFile).slurp()
    val designSeq = Serialization.read[DesignSeq](jsonStr)
    //println(s"Read design sequence:\n${designSeq.mkString("\n")}")
    val design = UAV2Design.fromSeqBF(designSeq)
    //println(design)
    val designTreeFilename = "design_tree_reconstructed_from_bf.json"
    val designTreeFile = outPath.resolve(designTreeFilename)
    val jsonStrTree = design.toJson()
    Files.write(designTreeFile, jsonStrTree.getBytes(StandardCharsets.UTF_8))
  }

  test("BFTest design") {
    val angle = uniform(45.0, 315.0)
    val propArm1 = treeGenerator.generateSidewaysBendWithTopSegment()
    val propArm2 = treeGenerator.generatePropArm()
    val propArm3 = treeGenerator.generatePropArm()
    val propArm4 = treeGenerator.generatePropArm()
    val hub = ConnectedHub6_2_2_2(propArm1, propArm2, propArm3)
    val fuselage = treeGenerator.generateDualBatteryFuselageWithComponents()
    val treeDesign = UAV2Design("BFTest", hub, fuselage)
    saveDesign(treeDesign)
  }

  test("Parse BF design") {
    parseDesign("BFTest")
  }

}
