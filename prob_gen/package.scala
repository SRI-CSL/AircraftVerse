package com.sri.nscore

package object uam {

  trait TopBottomCategory {
    def propDir(): NamedParam
    def propType(): NamedParam
  }

  object Top extends TopBottomCategory {
    override def propDir(): NamedParam = PosPropDir
    override def propType(): NamedParam = PosPropType
  }

  object Bottom extends TopBottomCategory {
    override def propDir(): NamedParam = PosPropDir
    override def propType(): NamedParam = NegPropType
  }

  trait LeftRightSymmetryCategory {
    def batteryWingConnector(): String
    def wingInnerConnector(): String
    def wingOuterConnector(): String
    def wingBatteryConnector(): String
    def innerChord(): String
    def outerChord(): String
    def mountSide(): NamedParam
    def armRotation(): NamedParam
    def upwardForwardPropDir(): NamedParam
    def upwardForwardPropType(): NamedParam
    def downwardBackPropDir(): NamedParam
    def downwardBackPropType(): NamedParam
  }

  object Left extends LeftRightSymmetryCategory {
    override def batteryWingConnector(): String = "Battery_Connector_1_out"
    override def wingInnerConnector(): String = "Connector_1"
    override def wingOuterConnector(): String = "Connector_2"
    override def wingBatteryConnector(): String = "Battery_Connector_1"
    override def innerChord(): String = "CHORD_1"
    override def outerChord(): String = "CHORD_2"
    override def mountSide(): NamedParam = NamedParam("LeftMountSide", "1")
    override def armRotation(): NamedParam = NamedParam("LeftArmRotation", 90.0)
    override def upwardForwardPropDir(): NamedParam = PosPropDir
    override def upwardForwardPropType(): NamedParam = PosPropType
    override def downwardBackPropDir(): NamedParam = NegPropDir
    override def downwardBackPropType(): NamedParam = PosPropType
  }

  object Right extends LeftRightSymmetryCategory {
    override def batteryWingConnector(): String = "Battery_Connector_2_out"
    override def wingInnerConnector(): String = "Connector_2"
    override def wingOuterConnector(): String = "Connector_1"
    override def wingBatteryConnector(): String = "Battery_Connector_2"
    override def innerChord(): String = "CHORD_2"
    override def outerChord(): String = "CHORD_1"
    override def mountSide(): NamedParam = NamedParam("RightMountSide", "2")
    override def armRotation(): NamedParam = NamedParam("RightArmRotation", 270.0)
    override def upwardForwardPropDir(): NamedParam = NegPropDir
    override def upwardForwardPropType(): NamedParam = NegPropType
    override def downwardBackPropDir(): NamedParam = PosPropDir
    override def downwardBackPropType(): NamedParam = NegPropType
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

  // Returns an array of booleans of length numChoices, with at least one
  // true, and the rest true with prob p
  def atLeastOne(numChoices: Int, p: Double): Array[Boolean] = {
    val booleans = Array(true) ++ (2 to numChoices).map(_ => categoricalPick( (p,true), (1-p,false))).toArray
    val startIndex = uniformListPick((0 until numChoices).toList)
    (0 until numChoices).map(i => booleans(((startIndex + i)) % numChoices)).toArray

    //    uniformPick(true,false),
    //      uniformPick(true,false),
    //      uniformPick(true,false))
    //    val startIndex = uniformPick(0,1,2,3)
    //    val reorderedBooleans = Array(booleans(startIndex),
    //      booleans((startIndex+1) % 4),
    //      booleans((startIndex+2) % 4),
    //      booleans((startIndex+3) % 4),
    //    )
  }

  def taperOffsetToInt(taperOffset: Boolean): Int = {
    if (taperOffset) 0 else 1
  }

  case class BatteryFitInfo(actualVoltage: Double, seriesCount: Int, parallelCount: Int)

  def checkBatteryWingFit(baseVoltage: Double, baseVolume: Double, voltageRequest: Double,
                          volumePercent: Double, chord1: Double, chord2: Double, span: Double, thickness: Double): BatteryFitInfo = {
    batteryFitsWing(baseVoltage, baseVolume, voltageRequest, volumePercent, chord1, chord2, span, thickness) match {
      case None => throw IllegalDesignException(s"Battery does not fit wing!")
      case Some(r) => r
    }
  }

  def batteryFitsWing(baseVoltage: Double, baseVolume: Double, voltageRequest: Double,
                      volumePercent: Double, chord1: Double, chord2: Double, span: Double, thickness: Double): Option[BatteryFitInfo] = {
    val volumeRatio = volumePercent / 100.0
    val seriesCount = (voltageRequest / baseVoltage).ceil.toInt
    val minPackVolume = seriesCount * baseVolume
    val A = chord1/2.0
    val B = thickness/100.0*A
    val C = chord2/2.0
    val D = thickness/100.0*C
    val maxVolume = 1.0/6*span*(A*B+C*D+((A+C)*(B+D)))
    val availableVolume = maxVolume * volumeRatio
    if (minPackVolume > availableVolume) {
      None
    }
    else {
      val parallelCount = (availableVolume / minPackVolume).floor.toInt
      //val megaPackCapacity = baseCapacity * parallelCount
      val actualVoltage = baseVoltage * seriesCount
      Some(BatteryFitInfo(actualVoltage, seriesCount, parallelCount))
    }
  }

}
