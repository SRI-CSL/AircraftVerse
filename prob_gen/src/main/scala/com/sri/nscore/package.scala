package com.sri

import org.json4s.NoTypeHints
import org.json4s.native.Serialization

import scala.collection.mutable
import scala.util.{Failure, Random, Success, Try}

package object nscore {

  case class IllegalDesignException(message: String) extends Exception(message)

  // Returning a Try[T] wrapper
  //  @annotation.tailrec
  def tryUntilSuccess[T](fn: => T): Try[T] = {
    Try {
      fn
    } match {
      case Success(r) => Success(r)
      case Failure(IllegalDesignException(msg)) => {
        println(msg + " -- trying again")
        tryUntilSuccess(fn)
      }
      case Failure(e) => {
        e.printStackTrace()
        Failure(e)
      }
    }
  }

  trait HighLevelDesign {
    val name: String
    val validNamePat = "[a-zA-Z0-9_]+"
    def apply(implicit g: LowLevelGenerator): LowLevelAssembly
  }


  type DesignSeq = List[(String,Any)]

  def readKeyValue[T](seq: DesignSeq, expectedKey: String): (T,DesignSeq) = {
    if (seq.isEmpty)
      throw new IllegalArgumentException(s"Expected $expectedKey, got empty list")
    val (k,v) = seq.head
    //println(s"Read $k : $v")
    if (k.toLowerCase() != expectedKey.toLowerCase())
      throw new IllegalArgumentException(s"Expected key $expectedKey, got $k")
    val s = v.asInstanceOf[T]
    (s,seq.tail)
  }

  var nameIndices = mutable.Map[String,Int]()

  def createUniqueName(typeName: String): String = {
    val idx = nameIndices.get(typeName) match {
      case None => {
        nameIndices(typeName) = 1
        1
      }
      case Some(i) => {
        val j = i+1
        nameIndices(typeName) = j
        j
      }
    }
    typeName + "_" + idx
  }


  var random = new Random

  def categoricalPick[T](choices: (Double,T)*): T = {
    def categoricalPick[T](r: Double, choices: Seq[(Double,T)]): T = {
      if (choices.size == 1){
        choices.head._2
      }
      else {
        val prob = choices.head._1
        if (r < prob){
          choices.head._2
        }
        else {
          categoricalPick(r - prob, choices.tail)
        }
      }
    }

    val r = random.nextDouble()
    categoricalPick(r, choices)
  }

  def uniformPick[T](choices: T*): T = {
    uniformListPick(choices.toList)
  }

  def uniformListPick[T](choices: List[T]): T = {
    choices(random.nextInt(choices.length))
  }

  def uniform(lower:Double, upper:Double): Double = {
    random.nextDouble() * (upper-lower) + lower
  }

  // Return the necessary Bend angle to get from the current angle to the
  // desired angle
  def bendAngleTo(currentAngle:Double, desiredAngle:Double): Double = {
    desiredAngle - currentAngle
  }

  case class Connection(counter: Int) {
    override def toString: String = s"c${counter}"
  }

  // type Conn = Option[Connection]

  // trait Component

  // A parameter that is connected to all components with the given name base
  case class NamedParam(name: String, value: Any, optimization_category: Option[String] = None) { // unit: Option[String] = None,
    def connectToComponent(componentName: String, componentParam: String): TunableParam = {
      TunableParam(this, componentName, componentParam)
    }
    def doubleValue(): Double = value.asInstanceOf[Double]
    def intValue(): Int = value.asInstanceOf[Int]
    def stringValue(): String = value.asInstanceOf[String]
  }

  object TunableParam {
    def apply(nameBase: String, componentName: String, componentParam: String, value: Any,  // unit: Option[String] = None,
              optimization_category: Option[String] = None): TunableParam = {
      NamedParam(s"${nameBase}_$componentParam", value, optimization_category).connectToComponent(componentName, componentParam)
    }
  }

  case class TunableParam(param: NamedParam, componentName: String, componentParam: String)

  case class Component(name: String,
                       typename: String,
                       component: String,
                       connections: Iterable[(String,Connection)]){

    override def toString: String = {
      s"${name}: ${typename} - ${component}, ${connections.toMap}"
    }

    // Returns (component, port, conn) triples
    def getConnections(): Iterable[(String,String,Connection)] =
      connections.map(c => (name, c._1, c._2))
//      connections.flatMap(conn => {
//        conn._2 match {
//          case Some(c) => Some(name, conn._1, c)
//          case None => None
//        }
//      })
  }

  case class LowLevelAssembly(components: List[Component], parameters: List[TunableParam]){
    def ++(that: LowLevelAssembly): LowLevelAssembly = {
      LowLevelAssembly(components ++ that.components, parameters ++ that.parameters)
    }
  }

  object LowLevelAssembly {
    def empty(): LowLevelAssembly = {
      LowLevelAssembly(List(), List())
    }

    def apply(component: Component): LowLevelAssembly = {
      LowLevelAssembly(List(component), List())
    }

    def apply(component: Component, parameters: List[TunableParam]): LowLevelAssembly = {
      LowLevelAssembly(List(component), parameters)
    }
  }

  //  trait PropDirection {
//    def nameFor(propName: String): String
//    def reverse(): PropDirection
//    def isReversible(): Boolean
//  }
//
//  object AnyProp extends PropDirection {
//    override def nameFor(propName:String): String = propName
//    override def reverse(): PropDirection = this
//    override def isReversible(): Boolean = false
//  }
//
//  object E extends PropDirection {
//    override def nameFor(propName:String): String = propName + "E"
//    override def reverse(): PropDirection = EP
//    override def isReversible(): Boolean = true
//  }
//
//  object EP extends PropDirection {
//    override def nameFor(propName:String): String = propName + "EP"
//    override def reverse(): PropDirection = E
//    override def isReversible(): Boolean = true
//  }

  case class ParamDef(parameter_name: String, value: String, // unit: Option[String],
                      component_properties: List[ParamTargetDef], optimization: Option[String])

  case class ParamTargetDef(component_name: String, component_property: String)

  case class ComponentDef(component_instance: String, component_type: String, component_choice: String){
    override def toString:String = s"${component_instance}: ${component_type} - ${component_choice}"
  }

  case class ConnectionDef(from_ci: String, from_conn: String, to_ci: String, to_conn: String){
    override def toString:String = s"${from_ci}/${from_conn}<->${to_ci}/${to_conn}"
  }

  // This is to re-organize the design, print it more nicely, etc
  case class Design(extra: Map[String,String], name: String, parameters: List[ParamDef], components: List[ComponentDef], connections: List[ConnectionDef]){
    override def toString:String = s"${name}\ncomponents: ${components.mkString(", ")}\nconnections: ${connections.mkString(", ")}"

    def toJson(): String = {
      implicit val formats = Serialization.formats(NoTypeHints)
      val jsonStr = Serialization.writePretty(this)
      jsonStr
    }

  }

  case class ParamSpec(param: String, value: String, unit: Option[String])

  case class ComponentTypeSpec(component_type: String,
                               component_choices: List[String],
                               connectors: List[String],
                               params: List[String])
}
