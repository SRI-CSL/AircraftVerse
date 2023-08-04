package com.sri.nscore

import scala.util.Random
import scala.collection.mutable

class SWRIGenerator(val componentLibrary: ComponentLibrary) {

  val MAX_CONTROL_CHANNELS = 13

  // abstract method for specific generators to override
  //def design(): () => List[Component]

  // --- Internal stuff ---

  var random = new Random

  var i = 0
  var c = 0

  var compIndices = mutable.Map[String,Int]()

  def createConnection(): Connection = {
    i += 1
    Connection(i)
  }

  def createControlChannel(): Int = {
    c += 1
    if (c > MAX_CONTROL_CHANNELS){
      throw IllegalDesignException(s"Too many control channels. Aborting design.")
    }
    c
  }

//  // Converts internal param format to external format
  def convertParams(typename: String, params: Map[String,String]): List[ParamSpec] = {
//    val paramSpecs = componentLibrary.components(typename).params.map(ps => (ps.param,ps.unit)).toMap
//    (for ( (param,value) <- params) yield {
//      ParamSpec(param, value, paramSpecs(param))
//    }).toList
    (for ( (p,v) <- params ) yield {
      ParamSpec(p, v, None)
    }).toList
  }
//
//  def getParamSpec(origCiName: String, param: String): ParamSpec = {
//    componentLibrary.components(origCiName).params.filter(_.param == param).head
//  }

  // This is a bit ugly. Consider cleanup
//  def expandParam(tunableParam: TunableParam, componentDefs: List[ComponentDef]): ParamDef = {
//    val relevantComponentDefs = componentDefs.filter(cDef => cDef.new_ci_name.startsWith(tunableParam.componentNameBase))
//    val paramName = tunableParam.componentNameBase + "_" + tunableParam.componentParam
//    val origCiName = relevantComponentDefs.head.old_ci_name
//    val paramSpec = getParamSpec(origCiName, tunableParam.componentParam)
//    ParamDef(paramName, tunableParam.componentParam, tunableParam.value.toString, paramSpec.unit, relevantComponentDefs.map(_.new_ci_name))
//  }

  def expandParams(params: List[TunableParam]): List[ParamDef] = {
    val paramsByName = params.groupBy(_.param).toList
    for ((param,tparams) <- paramsByName) yield {
      val paramName = param.name
      val value = param.value.toString
      //val unit = param.unit
      val opt = param.optimization_category
      val paramTargetDefs = for (tp <- tparams) yield {
        ParamTargetDef(tp.componentName, tp.componentParam)
      }
      ParamDef(paramName, value, paramTargetDefs, opt)
    }
  }

  def countComponents(compDefs: List[ComponentDef], compType: String): Int = {
    compDefs.count(cd => cd.component_type == compType)
  }

  def checkConstraint(ineq: (Int,Int)=>Boolean, constraint: Option[Int], value: Int, msgGen: Int=>String): Option[String] = {
    constraint match {
      case None => None
      case Some(min) =>
        if (ineq(value,min)) Some(msgGen(min)) else None
    }
  }

  def checkMinConstraint(minValue: Option[Int], value: Int, compType: String): Option[String] = {
    checkConstraint(_<_, minValue, value, cstr=>s"Too few ${compType}s ($value < $cstr)")
  }

  def checkMaxConstraint(maxValue: Option[Int], value: Int, compType: String): Option[String] = {
    checkConstraint(_>_, maxValue, value, cstr=>s"Too many ${compType}s ($value > $cstr)")
  }

  def generate(designTree: HighLevelDesign, minProps: Option[Int] = None, maxProps: Option[Int] = None,
               minWings: Option[Int] = None, maxWings: Option[Int] = None,
               bidirectionalConnections: Boolean = true): Design = {
    i = 0
    c = 0
    compIndices.clear()
    //val comps = design()()

    val assembly = designTree.apply(this)
    val comps = assembly.components

    // Now, we convert to a form that is more directly usable
    val compDefs = (for (c <- comps if c.typename != "Ignore") yield {
      //val params = convertParams(c.typename, c.params)
      ComponentDef(c.name, c.typename, c.component)
    }).sortBy(_.component_instance)

    // Check the prop and wing constraints
    val numProps = countComponents(compDefs, "Propeller")
    val numWings = countComponents(compDefs, "Wing")
    val problems = List(
      checkMinConstraint(minProps, numProps, "Propeller"),
      checkMaxConstraint(maxProps, numProps, "Propeller"),
      checkMinConstraint(minWings, numWings, "Wing"),
      checkMaxConstraint(maxWings, numWings, "Wing"),
    ).flatten
    if (!problems.isEmpty) throw IllegalDesignException(problems.mkString("; "))

    //println(s"component names: ${compDefs.map(_.new_ci_name)}")

    // (component, port, conn) triples
    val connTriples = comps.flatMap(_.getConnections())

    // Just the connections
    val conns = connTriples.map(_._3).toSet

    // now we need to self-join on the conn
    val connTriplesByConn = connTriples.groupBy(_._3)

    var connDefs = conns.flatMap(c => {
      connTriplesByConn.get(c) match {
        case None => Nil
        case Some(pairs) => {
          if (pairs.size >= 2) {
            val from = pairs.head
            if (from._1 != "Ignore") {
              for (to <- pairs.tail if to._1 != "Ignore") yield {
                if (to._1 == "Orient"){  // always put Orient on the from-side to work around Direct2CAD bug.
                  ConnectionDef(to._1, to._2, from._1, from._2)
                }
                else {
                  ConnectionDef(from._1, from._2, to._1, to._2)
                }
              }
            }
            else {
              Nil
            }
          }
          else {
            println(s"WARNING! Connector ${c} has less than two connected components: ${pairs}")
            Nil
          }
        }
      }
    }).toList.sortBy(_.from_ci)

    // Add connections in the other direction
    if (bidirectionalConnections){
      val oppositeConns = for (connDef <- connDefs) yield {
        ConnectionDef(connDef.to_ci, connDef.to_conn, connDef.from_ci, connDef.from_conn)
      }
      connDefs = connDefs ++ oppositeConns
    }

    // Sanity check the params against the component library
    for (param <- assembly.parameters){
      val compName = param.componentName
      val paramName = param.componentParam
      val compDefMap = compDefs.groupBy(_.component_instance)
      if (!compDefMap.contains(compName)){
        throw new IllegalArgumentException(s"Design does not contain component $compName")
      }
      val cis = compDefMap(compName)
      if (cis.isEmpty){
        throw new IllegalArgumentException(s"Design does not contain component $compName")
      }
      val ci = cis(0)
      val compType = ci.component_type
      if (!componentLibrary.components.contains(compType)){
        throw new IllegalArgumentException(s"Component library does not contain type $compType")
      }
      val compSpec = componentLibrary.components(compType)
      if (!compSpec.params.contains(paramName)){
        throw new IllegalArgumentException(s"Component type $compType does not contain parameter $paramName!")
      }
    }

    val paramDefs = expandParams(assembly.parameters).sortBy(_.parameter_name)

    val result = Design(Map(), designTree.name, paramDefs, compDefs, connDefs)
    checkDesign(result)
    result
  }

  // Check the produced design for known issues (e.g. disconnected islands)
  def checkDesign(design: Design): Unit = {
    checkConnected(design)
  }

  def checkConnected(design: Design): Unit = {
    val allComponents = design.components.map(_.component_instance)
    val firstComponent = allComponents(0)
    val connectedPairs = design.connections.map(c => (c.from_ci, c.to_ci) ) ++
      design.connections.map(c => (c.to_ci, c.from_ci) )
    val connectedByComp = connectedPairs.groupBy(_._1).view.mapValues(_.map(_._2)).toMap
    checkConnected(Set(firstComponent), allComponents.toSet - firstComponent, connectedByComp) match {
      case None => ;
      case Some((s1,s2)) => throw new Exception(s"Design is invalid due to disconnected islands of components:\n${s1}\n${s2}")
    }
  }

  // Returns a pair of two disconnected component sets if such exists, returns None if everything is connected
  def checkConnected(connectedSet: Set[String],
                     otherComponents: Set[String],
                     connections: Map[String,List[String]]): Option[(Set[String],Set[String])] = {
    // Follow connections one step from the current set
    val connected =
      connectedSet.flatMap(from_ci => {
        connections.get(from_ci) match {
          case None => None
          case Some(conns) => conns
        }
      })
    val newConnected = connected -- connectedSet
    if (newConnected.isEmpty){
      if (otherComponents.isEmpty){
        None
      }
      else {
        Some((connectedSet, otherComponents))  // We found two islands
      }
    }
    else {
      checkConnected(connectedSet ++ newConnected, otherComponents -- newConnected, connections) // check the next step
    }
  }

  def createComponentInstanceName(typeName: String): String = {
    val idx = compIndices.get(typeName) match {
      case None => {
        compIndices(typeName) = 1
        1
      }
      case Some(i) => {
        val j = i+1
        compIndices(typeName) = j
        j
      }
    }
    typeName + "_" + idx
  }

  val ignoreComponent = Component("Ignore","Ignore","Ignore",Nil)

  // A special component to ignore connections (e.g. unused power bus) and avoid warnings
  def ignore(connection: Connection): LowLevelAssembly = {
    LowLevelAssembly(Component("Ignore","Ignore","Ignore",Map("Ignore" -> connection)))
  }

  def filterConnections(rawConnections: Map[String,Option[Connection]]): Map[String,Connection] = {
    rawConnections.flatMap(conn => {
      conn._2 match {
        case Some(c) => Some((conn._1, c))
        case None => None
      }
    })
  }

  def _labeledComponent(ciName: String, typename: String, componentChoice: String,
                        connections: Map[String,Option[Connection]]): Component = {
    componentLibrary.components.get(typename) match {
      case None => throw new IllegalArgumentException(s"No such component type: ${typename}")
      case Some(compTypeSpec) => {
        // Check the connections
        for (conn <- connections.map(_._1)){
          if (!compTypeSpec.connectors.contains(conn)){
            throw new IllegalArgumentException(s"Type ${typename} does not contain connector ${conn}")
          }
        }
        // Check the component choice
        if (!compTypeSpec.component_choices.contains(componentChoice)){
          throw new IllegalArgumentException(s"Type ${typename} does not contain component $componentChoice")
        }
        // Check the params
//        for (param <- params.map(_._1)){
//          if (!ciSpec.params.contains(param)){
//            throw new IllegalArgumentException(s"Type ${typename} does not contain param ${param}")
//          }
//        }
        //val name = createComponentName(nameBase)
        Component(ciName, typename, componentChoice, filterConnections(connections))
      }
    }
  }

  // Deprecated. Better to use labeledComponentChoice, because most types have more than
  // one choice now (and sometimes they add more choices later).
  def labeledComponent(ciName: String, typename: String,
                       connections: Map[String,Option[Connection]]): Component = {
    //val cad_part = componentLibrary.getCadPart(ciName)
    val component = componentLibrary.getSingleComponent(typename)
    _labeledComponent(ciName, typename, component, connections)
  }

  def component(typename: String,
                connections: Map[String,Option[Connection]]): Component = {
    //val cad_part = componentLibrary.getCadPart(ciName)
    val component = componentLibrary.getSingleComponent(typename)
    _labeledComponent(createComponentInstanceName(typename), typename, component, connections)
  }

  def labeledComponentChoice(ciName: String, typename: String, componentChoice: String,
                             connections: Map[String,Option[Connection]]): Component = {
    //val cad_part = componentLibrary.getCadPart(ciName)
    _labeledComponent(ciName, typename, componentChoice, connections)
  }

  def componentChoice(componentType: String, componentChoice: String,
                      connections: Map[String,Option[Connection]]): Component = {
    //val cad_part = componentLibrary.getCadPart(ciName)
    _labeledComponent(createComponentInstanceName(componentType), componentType, componentChoice, connections)
  }

}
