package com.sri.nscore.uav2

import com.sri.nscore.{ComponentLibrary, DesignSeq, SWRIGenerator}
import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import de.halcony.argparse._

object UAV2Seq2TreeCLI {

  val compLib = new UAV2ComponentLibrary("./data/components_v2.json",
    "./data/Aero_Corpus.csv", "./data/prop_motor_pairing.csv")
  implicit val formats = Serialization.formats(NoTypeHints)
  val swriGenerator = new SWRIGenerator(compLib)

  def processDesign(designDirPath: Path, rename: Boolean, bf: Boolean): Unit = {
    println(s"\nProcessing design in $designDirPath")
    try {
      val designFilename = if (bf) "design_seq_bf.json" else "design_seq.json"
      val designSeqFile = designDirPath.resolve(designFilename).toFile
      val jsonStr = scala.reflect.io.File(designSeqFile).slurp()
      val designSeq = Serialization.read[DesignSeq](jsonStr)
      //println(s"Read design sequence:\n${designSeq.mkString("\n")}")
      val design = if (bf) UAV2Design.fromSeqBF(designSeq) else UAV2Design.fromSeq(designSeq)
      //println(design)
      val designTreeFilename = if (rename) "design_tree_reconstructed.json" else "design_tree.json"
      val designTreeFile = designDirPath.resolve(designTreeFilename)
      val jsonStrTree = design.toJson()
      Files.write(designTreeFile, jsonStrTree.getBytes(StandardCharsets.UTF_8))
      val swriFilename = if (rename) "design_swri_reconstructed.json" else "design_swri.json"
      val outPathSwri = designDirPath.resolve(swriFilename)
      val jsonStrSwri = swriGenerator.generate(design).toJson()
      Files.write(outPathSwri, jsonStrSwri.getBytes(StandardCharsets.UTF_8))
      println("Success!")
    } catch {
      case e: Exception => println(s"Error: ${e.getMessage}")
    }
  }

  def main(args: Array[String]): Unit = {
    val parser = Parser("UAV2 sequence to tree parser", "Convert UAV2 sequence to tree form")
      .addPositional("design-dir","design directory")
      .addFlag("bf","bf","breadth-first","use breadth-first sequence (design_seq_bf.json)")
      .addFlag("rename","r","rename","rename output file to design_tree_reconstructed.json")

    val pargs = parser.parse(args)
    val designdir = pargs.getValue[String]("design-dir")
    val rename = pargs.getValue[Boolean]("rename")
    val bf = pargs.getValue[Boolean]("bf")
    val designdirPath = Paths.get(designdir)
    val designFilename = if (bf) "design_seq_bf.json" else "design_seq.json"
    val designSeqFile = designdirPath.resolve(designFilename).toFile

    if (designSeqFile.exists()) {
      processDesign(designdirPath, rename, bf)
    }
    else {
      // Process all subdirectories
      val designDirFile = designdirPath.toFile
      if (!designDirFile.exists()) {
        throw new IllegalArgumentException(s"Design dir does not exists: $designDirFile")
      }
      for (subdir <- designDirFile.listFiles()) {
        val subdirPath = subdir.toPath
        val subdirDesignTreeFile = subdirPath.resolve(designFilename).toFile
        if (subdirDesignTreeFile.exists()) {
          processDesign(subdirPath, rename, bf)
        }
      }
    }
  }
}
