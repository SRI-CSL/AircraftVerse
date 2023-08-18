package com.sri.nscore.uav2

import com.github.tototoshi.csv.CSVReader
import com.sri.nscore.{ComponentLibrary, ComponentTypeSpec}

class UAV2ComponentLibrary(componentSpec: String, nacaFile: String, propMotorFile: String)
  extends ComponentLibrary(componentSpec = componentSpec){

  // Set up the left/right wing types
//  val wingCompSpec = components("Wing")
//  val leftWings = wingCompSpec.component_choices.filter(c => c.startsWith("left_")).map(c => c.substring(5))
//  val LeftWingCompSpec = ComponentTypeSpec("Left_Wing", leftWings, wingCompSpec.connectors, wingCompSpec.params)
//  val rightWings = wingCompSpec.component_choices.filter(c => c.startsWith("right_")).map(c => c.substring(5))
//  val RightWingCompSpec = ComponentTypeSpec("Right_Wing", rightWings, wingCompSpec.connectors, wingCompSpec.params)

  // Set up the special "ReversiblePropeller" type
  // These are all the Propellers that have E/EP variants.
//  val propCompSpec = components("Propeller")
//  val eProps = propCompSpec.component_choices.filter(c => c.endsWith("E")).map(c => c.substring(0,c.length-1))
//  val epProps = propCompSpec.component_choices.filter(c => c.endsWith("EP")).map(c => c.substring(0,c.length-2))
//  val revProps = (eProps ++ epProps).toSet.toList
//  val RevPropCompSpec = ComponentTypeSpec("ReversiblePropeller", revProps, propCompSpec.connectors, propCompSpec.params)
//
//  components += ("Left_Wing" -> LeftWingCompSpec)
//  components += ("Right_Wing" -> RightWingCompSpec)
//  components += ("ReversiblePropeller" -> RevPropCompSpec)

  // Add hacks for broken corpus
  //val oldBatteryConfig = components("Battery")
  //val newBatteryConfig = oldBatteryConfig.copy(connectors = oldBatteryConfig.connectors :+ "Bottom_Connector")
  //components += ("Battery"  -> newBatteryConfig)

  val nacaProfiles: List[String] = loadNACAProfiles(nacaFile)

  val propMotorPairs: List[(String,String)] = loadPropMotorPairs(propMotorFile)

  def loadPropMotorPairs(triplesFile: String) :List[(String, String)] = {
    val reader = CSVReader.open(triplesFile)
    val data = reader.all()
    reader.close()
    val pairs = data.map(row => (row(1), row(2))).tail // skip header row and keep only relevant columns
    // Remove entries that are not in the component library
    val filteredTriples = pairs.filter(s => {
      val prop = s._1
      val motor = s._2
      val hasProp = containsComponent("Propeller",prop)
      val hasMotor = containsComponent("Motor",motor)
//      val hasESC = containsComponent("ESC",esc)
//      if (!hasProp){
//        println(s"Propeller ${prop} is not a reversible prop")
//      }
//      if (!hasMotor){
//        println(s"Motor ${motor} is not in the component library")
//      }
//      if (!hasESC){
//        println(s"ESC ${esc} is not in the component library")
//      }
      hasProp && hasMotor
    })
    println(s"CSV has ${pairs.size} pairs, kept ${filteredTriples.size} valid pairs")
    filteredTriples
  }

  private def loadNACAProfiles(nacaFile: String): List[String] = {
    val reader = CSVReader.open(nacaFile)
    val data = reader.all()
    reader.close()
    for (row <- data.tail) yield {
      val nacaProfile = row(0).substring(5) // last 4 digits
      nacaProfile
    }
  }

}
