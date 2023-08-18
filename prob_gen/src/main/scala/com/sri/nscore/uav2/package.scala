package com.sri.nscore

package object uav2 {

  //  trait TopBottomCategory {
  //    def propDir(): NamedParam
  //    def propType(): NamedParam
  //  }
  //
  //  object Top extends TopBottomCategory {
  //    override def propDir(): NamedParam = PosPropDir
  //    override def propType(): NamedParam = PosPropType
  //  }
  //
  //  object Bottom extends TopBottomCategory {
  //    override def propDir(): NamedParam = PosPropDir
  //    override def propType(): NamedParam = NegPropType
  //  }

  trait SymmetryCategory {
    def adjustOffset(offset: Double): Double

    def adjustAngle(angle: Double): Double

    def adjustRotation(rot: Double): Double

    def branchTopRotation(branchAngle: Double): Double

    def getForward(): SymmetryCategory

    def getBackward(): SymmetryCategory

    def getRight(): SymmetryCategory

    def getLeft(): SymmetryCategory

//    def getLeftConnection(frontConnection: Connection, rearConnection: Connection): Connection
//
//    def getRightConnection(frontConnection: Connection, rearConnection: Connection): Connection
//
//    def getTopConnection(topConnection: Connection): Option[Connection]
//
//    def getBottomConnection(topConnection: Connection): Option[Connection]

    //def tubeRotation(): Double
    def adjustTubeOffset(offset: Double, span: Double): Double

    def innerChord(): String

    def outerChord(): String

    def getPropDirection(): NamedParam

    def getPropSpinType(): NamedParam

//    def upwardForwardPropDir(): NamedParam
//
//    def upwardForwardPropType(): NamedParam
//
//    def downwardBackPropDir(): NamedParam
//
//    def downwardBackPropType(): NamedParam
  }

  object Left extends SymmetryCategory {
    override def adjustOffset(offset: Double): Double = -offset

    override def adjustAngle(angle: Double): Double = 360.0 - angle

    override def adjustRotation(rot: Double): Double = (rot + 180.0) % 360.0

    override def branchTopRotation(branchAngle: Double): Double = branchAngle

    //override def adjustRotation(rot:Double): Double = 360.0 - rot
//    override def getLeftConnection(frontConnection: Connection, rearConnection: Connection): Connection = rearConnection
//
//    override def getRightConnection(frontConnection: Connection, rearConnection: Connection): Connection = frontConnection
//
//    override def getTopConnection(topConnection: Connection): Option[Connection] = None
//
//    override def getBottomConnection(topConnection: Connection): Option[Connection] = Some(topConnection)

    //override def tubeRotation(): Double = 180.0
    override def adjustTubeOffset(offset: Double, span: Double): Double = span - offset

    override def innerChord(): String = "CHORD_1"

    override def outerChord(): String = "CHORD_2"

//    override def upwardForwardPropDir(): NamedParam = PosPropDir
//
//    override def upwardForwardPropType(): NamedParam = PosPropType
//
//    override def downwardBackPropDir(): NamedParam = NegPropDir
//
//    override def downwardBackPropType(): NamedParam = PosPropType

    override def getForward(): SymmetryCategory = Left

    override def getBackward(): SymmetryCategory = Right

    override def getRight(): SymmetryCategory = Forward

    override def getLeft(): SymmetryCategory = Backward

    override def getPropDirection(): NamedParam = PosPropDir

    override def getPropSpinType(): NamedParam = PosPropType
  }

  object Right extends SymmetryCategory {
    override def adjustOffset(offset: Double): Double = offset

    override def adjustAngle(angle: Double): Double = angle

    override def adjustRotation(rot: Double): Double = rot

    override def branchTopRotation(branchAngle: Double): Double = 0.0

//    override def getLeftConnection(frontConnection: Connection, rearConnection: Connection): Connection = frontConnection
//
//    override def getRightConnection(frontConnection: Connection, rearConnection: Connection): Connection = rearConnection
//
//    override def getTopConnection(topConnection: Connection): Option[Connection] = Some(topConnection)
//
//    override def getBottomConnection(topConnection: Connection): Option[Connection] = None

    //override def tubeRotation(): Double = 0.0
    override def adjustTubeOffset(offset: Double, span: Double): Double = offset

    override def innerChord(): String = "CHORD_2"

    override def outerChord(): String = "CHORD_1"

//    override def upwardForwardPropDir(): NamedParam = NegPropDir
//
//    override def upwardForwardPropType(): NamedParam = NegPropType
//
//    override def downwardBackPropDir(): NamedParam = PosPropDir
//
//    override def downwardBackPropType(): NamedParam = NegPropType

    override def getForward(): SymmetryCategory = Right

    override def getBackward(): SymmetryCategory = Left

    override def getRight(): SymmetryCategory = Backward

    override def getLeft(): SymmetryCategory = Forward

    override def getPropDirection(): NamedParam = NegPropDir

    override def getPropSpinType(): NamedParam = NegPropType
  }

  object Forward extends SymmetryCategory {
    override def adjustOffset(offset: Double): Double = 0.0

    override def adjustAngle(angle: Double): Double = 0.0

    override def adjustRotation(rot: Double): Double = rot // ??

    override def branchTopRotation(branchAngle: Double): Double = 0.0

    // Same as Right for now
//    override def getLeftConnection(frontConnection: Connection, rearConnection: Connection): Connection = frontConnection
//
//    override def getRightConnection(frontConnection: Connection, rearConnection: Connection): Connection = rearConnection
//
//    override def getTopConnection(topConnection: Connection): Option[Connection] = Some(topConnection)
//
//    override def getBottomConnection(topConnection: Connection): Option[Connection] = None

    //override def tubeRotation(): Double = 0.0
    override def adjustTubeOffset(offset: Double, span: Double): Double = offset

    override def innerChord(): String = "CHORD_2"

    override def outerChord(): String = "CHORD_1"

//    override def upwardForwardPropDir(): NamedParam = NegPropDir
//
//    override def upwardForwardPropType(): NamedParam = NegPropType
//
//    override def downwardBackPropDir(): NamedParam = PosPropDir
//
//    override def downwardBackPropType(): NamedParam = NegPropType

    override def getForward(): SymmetryCategory = Forward

    override def getBackward(): SymmetryCategory = Backward

    override def getRight(): SymmetryCategory = Right

    override def getLeft(): SymmetryCategory = Left

    override def getPropDirection(): NamedParam = NegPropDir

    override def getPropSpinType(): NamedParam = NegPropType
  }

  object Backward extends SymmetryCategory {
    override def adjustOffset(offset: Double): Double = 0.0

    override def adjustAngle(angle: Double): Double = 0.0

    override def adjustRotation(rot: Double): Double = rot // ??

    override def branchTopRotation(branchAngle: Double): Double = branchAngle

    // Same as Left for now
//    override def getLeftConnection(frontConnection: Connection, rearConnection: Connection): Connection = frontConnection
//
//    override def getRightConnection(frontConnection: Connection, rearConnection: Connection): Connection = rearConnection
//
//    override def getTopConnection(topConnection: Connection): Option[Connection] = None
//
//    override def getBottomConnection(topConnection: Connection): Option[Connection] = Some(topConnection)

    //override def tubeRotation(): Double = 180.0
    override def adjustTubeOffset(offset: Double, span: Double): Double = span - offset

    override def innerChord(): String = "CHORD_1"

    override def outerChord(): String = "CHORD_2"

//    override def upwardForwardPropDir(): NamedParam = PosPropDir
//
//    override def upwardForwardPropType(): NamedParam = PosPropType
//
//    override def downwardBackPropDir(): NamedParam = NegPropDir
//
//    override def downwardBackPropType(): NamedParam = PosPropType

    override def getForward(): SymmetryCategory = Backward

    override def getBackward(): SymmetryCategory = Forward

    override def getRight(): SymmetryCategory = Right

    override def getLeft(): SymmetryCategory = Left

    override def getPropDirection(): NamedParam = NegPropDir

    override def getPropSpinType(): NamedParam = PosPropType
  }

  val PosPropDir = NamedParam("PosPropDir", 1)
  val PosPropType = NamedParam("PosPropType", 1)
  val NegPropDir = NamedParam("NegPropDir", -1)
  val NegPropType = NamedParam("NegPropType", -1)
  val ControlChannelAuto = NamedParam("ControlChannelAuto", -1)

  def thicknessOfNacaProfile(nacaProfile: String): NamedParam = {
    val value = nacaProfile.substring(2).toInt
    NamedParam("NACA_" + nacaProfile + "_THICKNESS", value)
  }

  def taperOffsetToInt(taperOffset: Boolean): Int = {
    if (taperOffset) 0 else 1
  }

}