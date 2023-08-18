package com.sri.nscore

import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import com.github.tototoshi.csv.CSVReader

import java.io.FileReader

class ComponentLibrary(val componentSpec: String) {

  var components: Map[String, ComponentTypeSpec] = loadComponentTypeSpecs(componentSpec)
    .map(c => (c.component_type,c)).toMap

//  def getCadPart(ciName: String): String = {
//    components.get(ciName) match {
//      case None => throw new IllegalArgumentException(s"No such component instance: ${ciName}!")
//      case Some(ciSpec) => ciSpec.cad_part
//    }
//  }

  def getSingleComponent(typename: String): String = {
    components.get(typename) match {
      case None => throw new IllegalArgumentException(s"No such component instance: ${typename}!")
      case Some(spec) =>
        if (spec.component_choices.size > 1)
          throw new IllegalArgumentException(s"Type has more than one component: $typename!")
        spec.component_choices(0)
    }
  }

  def loadComponentTypeSpecs(jsonFile: String): List[ComponentTypeSpec] = {
    println(s"Reading $jsonFile")
    implicit val formats = Serialization.formats(NoTypeHints)
    val specs = Serialization.read[List[ComponentTypeSpec]](new FileReader(jsonFile))
    specs
  }

  def containsComponent(compType: String, compName: String): Boolean = {
//    components(compType).component_choices match {
//      case Some(comps) => comps.contains(compName)
//      case None => false
//    }
    components(compType).component_choices.contains(compName)
  }

  def getComponentNames(typename: String): List[String] = {
    components.get(typename) match {
      case None => throw new IllegalArgumentException(s"No such component type: ${typename}!")
      case Some(spec) => {
//        ciSpec.component_choices match {
//          case None => throw new IllegalArgumentException(s"${ciName} has no component choices!")
//          case Some(choices: List[String]) => choices
//        }
        spec.component_choices
      }
    }
  }

}
