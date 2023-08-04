package com.sri.nscore.uav2

import com.sri.nscore.SWRIGenerator

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}

object UAV2Tree2SwriCLI {

  def processDesignDir(swriGenerator: SWRIGenerator, outDir: Path): Unit = {
    println(s"\nProcessing design in $outDir")
    try {
      val outPathTree = outDir.resolve("design_tree.json")
      val treeFile = outPathTree.toFile
      val outPathSwri = outDir.resolve("design_swri.json")

      val jsonStr = scala.reflect.io.File(treeFile).slurp()
      val tree = UAV2Design.fromJson(jsonStr)
      val jsonStrTree = tree.toJson()
      Files.write(outPathTree, jsonStrTree.getBytes(StandardCharsets.UTF_8))
      val jsonStrSwri = swriGenerator.generate(tree).toJson()
      Files.write(outPathSwri, jsonStrSwri.getBytes(StandardCharsets.UTF_8))
      println("Success!")
    } catch {
      case e: Exception => println(s"Error: ${e.getMessage}")
    }
  }

  def main(args: Array[String]): Unit = {
    if (args.size < 1)
      throw new IllegalArgumentException("I need at least one argument (output directory)")
    val outdir = args(0)
    val num = if (args.size > 1) args(1).toInt else 1

    val compLib = new UAV2ComponentLibrary("./data/components_v2.json",
      "./data/Aero_Corpus.csv", "./data/prop_motor_pairing.csv")
    val swriGenerator = new SWRIGenerator(compLib)
    val outDir = Paths.get(outdir)

    val outPathTree = outDir.resolve("design_tree.json")
    val treeFile = outPathTree.toFile
    if (treeFile.exists()){
      processDesignDir(swriGenerator, outDir)
    }
    else {
      // Not a design dir, process all sub-dirs
      val designDirFile = outDir.toFile
      if (!designDirFile.exists()) {
        throw new IllegalArgumentException(s"Design dir does not exists: $designDirFile")
      }
      for (subdir <- designDirFile.listFiles()) {
        val subdirPath = subdir.toPath
        val subdirDesignTreeFile = subdirPath.resolve("design_tree.json").toFile
        if (subdirDesignTreeFile.exists()) {
          processDesignDir(swriGenerator, subdirPath)
        }
      }
    }
  }
}
