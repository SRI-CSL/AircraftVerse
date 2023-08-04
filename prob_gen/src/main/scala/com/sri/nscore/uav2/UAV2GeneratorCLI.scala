package com.sri.nscore.uav2

import com.sri.nscore.{SWRIGenerator, tryUntilSuccess}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.util.UUID
import de.halcony.argparse._

object UAV2GeneratorCLI {

  def main(args: Array[String]): Unit = {
//    if (args.size < 1)
//      throw new IllegalArgumentException("I need at least one argument (output directory)")
//    val outdir = args(0)
//    val num = if (args.size > 1) args(1).toInt else 1

    val parser = Parser("UAV2 generator CLI parser", "Generate random UAV2 designs")
      .addPositional("outdir","output directory")
      .addOptional("num-designs","n","num-designs",Some("1"),"number of designs to generate")
      .addOptional("min-props","p", "min-props",Some("1"),"min number of propellers")
      .addOptional("min-wings","w","min-wings",Some("0"),"min number of wings")
      .addOptional("max-props","P","max-props",Some("16"),"max number of propellers")
      .addOptional("max-wings","W","max-wings",Some("12"),"max number of wings")

    val pargs = parser.parse(args)
    val outdir = pargs.getValue[String]("outdir")
    val numDesigns = pargs.getValue[String]("num-designs").toInt
    val minProps = pargs.getValue[String]("min-props").toInt
    val minWings = pargs.getValue[String]("min-wings").toInt
    val maxProps = pargs.getValue[String]("max-props").toInt
    val maxWings = pargs.getValue[String]("max-wings").toInt

    val compLib = new UAV2ComponentLibrary("./data/components_v2.json",
      "./data/Aero_Corpus.csv", "./data/prop_motor_pairing.csv")
    val treeGenerator = new DefaultUAV2Generator(compLib)
    val swriGenerator = new SWRIGenerator(compLib)

    (1 to numDesigns).foreach(n => {
      tryUntilSuccess { // designs are validated post-hoc
        val dirname = s"design_$n"
        val outDir = Paths.get(outdir).resolve(dirname)
        Files.createDirectories(outDir)

        val randomID = UUID.randomUUID().toString.replaceAll("-","")
        val design_name = s"${dirname}_$randomID"
        val outPathTree = outDir.resolve("design_tree.json")
        val tree = treeGenerator.generateUAVDesign(design_name)
        val jsonStrTree = tree.toJson()
        Files.write(outPathTree, jsonStrTree.getBytes(StandardCharsets.UTF_8))

        val outPathSwri = outDir.resolve("design_swri.json")
        val jsonStrSwri = swriGenerator.generate(tree, minProps = Some(minProps), maxProps = Some(maxProps),
          minWings = Some(minWings), maxWings = Some(maxWings)).toJson()
        Files.write(outPathSwri, jsonStrSwri.getBytes(StandardCharsets.UTF_8))
      }
    })
  }
}
