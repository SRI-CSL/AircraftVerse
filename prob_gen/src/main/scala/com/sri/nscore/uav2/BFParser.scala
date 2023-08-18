package com.sri.nscore.uav2
import util.control.Breaks._
import com.sri.nscore.DesignSeq

/**
 * Helper functions and types for breadth-first seq2tree parsing.
 */

/**
 * When we parse to a given depth, we either get back
 * - a final object of the expected type T, and the remaining token sequence
 * - a function that we can call with the next higher depth to parse deeper
 * @tparam T expected result type
 */
sealed trait BFParseResult[T] {
  def parse(seq: DesignSeq): (BFParseResult[T], DesignSeq)
}
case class Complete[T](result: T) extends BFParseResult[T] {
  override def parse(seq: DesignSeq): (BFParseResult[T], DesignSeq) = (this, seq)
}
case class Incomplete[T](continue: (DesignSeq)=>(BFParseResult[T], DesignSeq)) extends BFParseResult[T] {
  override def parse(seq: DesignSeq): (BFParseResult[T], DesignSeq) = continue(seq)
}

object BFParser {

  // Iteratively parse a result until it is complete.
  // Should only be called from the top level
  def parseBF[T](result: BFParseResult[T], seq: DesignSeq): T = {
    result match {
      case Complete(result) =>
        if (!seq.isEmpty)
          throw new IllegalArgumentException(s"Extra entries in sequence: $seq")
        else result
      case r: Incomplete[T] =>
        val (nextRes, nextSeq) = r.parse(seq)
        parseBF(nextRes,nextSeq)
    }
  }

  // Parsing methods for components with 1-4 child components
  // For these, we have to pass the parsers for the child components, and the
  // constructor for the final component

  def fromSeqBF_1[T1,TR](seq: DesignSeq,
                        part1InitParser: DesignSeq=>(BFParseResult[T1], DesignSeq),
                        constr: (T1)=>TR): (BFParseResult[TR], DesignSeq) = {
    def parseDeeper(s: DesignSeq,
                    part1: BFParseResult[T1]): (BFParseResult[TR], DesignSeq) = {
      val (part1Next, s2) = part1.parse(s)
      part1Next match {
        case Complete(part1Final) =>
          val comp = constr(part1Final)
          (Complete(comp), s2)
        case _ =>
          val func = (s) => parseDeeper(s, part1Next)
          (Incomplete(func), s2)
      }
    }

    val cont = (s:DesignSeq) => {
      val part1 = Incomplete(part1InitParser)
      parseDeeper(s, part1)
    }

    (Incomplete(cont), seq)
  }

  def fromSeqBF_2[T1,T2,TR](seq: DesignSeq,
                               part1InitParser: DesignSeq=>(BFParseResult[T1], DesignSeq),
                               part2InitParser: DesignSeq=>(BFParseResult[T2], DesignSeq),
                               constr: (T1, T2)=>TR): (BFParseResult[TR], DesignSeq) = {
    def parseDeeper(s: DesignSeq,
                    part1: BFParseResult[T1],
                    part2: BFParseResult[T2]): (BFParseResult[TR], DesignSeq) = {
      val (part1Next, s2) = part1.parse(s)
      val (part2Next, s3) = part2.parse(s2)
      (part1Next, part2Next) match {
        case (Complete(part1Final), Complete(part2Final)) =>
          val comp = constr(part1Final, part2Final)
          (Complete(comp), s3)
        case _ =>
          val func = (s) => parseDeeper(s, part1Next, part2Next)
          (Incomplete(func), s3)
      }
    }

    val cont = (s:DesignSeq) => {
      val part1 = Incomplete(part1InitParser)
      val part2 = Incomplete(part2InitParser)
      parseDeeper(s, part1, part2)
    }

    (Incomplete(cont), seq)
  }

  def fromSeqBF_3[T1,T2,T3,TR](seq: DesignSeq,
                               part1InitParser: DesignSeq=>(BFParseResult[T1], DesignSeq),
                               part2InitParser: DesignSeq=>(BFParseResult[T2], DesignSeq),
                               part3InitParser: DesignSeq=>(BFParseResult[T3], DesignSeq),
                               constr: (T1, T2, T3)=>TR): (BFParseResult[TR], DesignSeq) = {
    def parseDeeper(s: DesignSeq,
                    part1: BFParseResult[T1],
                    part2: BFParseResult[T2],
                    part3: BFParseResult[T3]): (BFParseResult[TR], DesignSeq) = {
      val (part1Next, s2) = part1.parse(s)
      val (part2Next, s3) = part2.parse(s2)
      val (part3Next, s4) = part3.parse(s3)
      (part1Next, part2Next, part3Next) match {
        case (Complete(part1Final), Complete(part2Final), Complete(part3Final)) =>
          val comp = constr(part1Final, part2Final, part3Final)
          (Complete(comp), s4)
        case _ =>
          val func = (s) => parseDeeper(s, part1Next, part2Next, part3Next)
          (Incomplete(func), s4)
      }
    }

    val cont = (s:DesignSeq) => {
      val part1 = Incomplete(part1InitParser)
      val part2 = Incomplete(part2InitParser)
      val part3 = Incomplete(part3InitParser)
      parseDeeper(s, part1, part2, part3)
    }

    (Incomplete(cont), seq)
  }

  def fromSeqBF_4[T1,T2,T3,T4,TR](seq: DesignSeq,
                               part1InitParser: DesignSeq=>(BFParseResult[T1], DesignSeq),
                               part2InitParser: DesignSeq=>(BFParseResult[T2], DesignSeq),
                               part3InitParser: DesignSeq=>(BFParseResult[T3], DesignSeq),
                               part4InitParser: DesignSeq=>(BFParseResult[T4], DesignSeq),
                               constr: (T1, T2, T3, T4)=>TR): (BFParseResult[TR], DesignSeq) = {
    def parseDeeper(s: DesignSeq,
                    part1: BFParseResult[T1],
                    part2: BFParseResult[T2],
                    part3: BFParseResult[T3],
                    part4: BFParseResult[T4]): (BFParseResult[TR], DesignSeq) = {
      val (part1Next, s2) = part1.parse(s)
      val (part2Next, s3) = part2.parse(s2)
      val (part3Next, s4) = part3.parse(s3)
      val (part4Next, s5) = part4.parse(s4)
      (part1Next, part2Next, part3Next, part4Next) match {
        case (Complete(part1Final), Complete(part2Final), Complete(part3Final), Complete(part4Final)) =>
          val comp = constr(part1Final, part2Final, part3Final, part4Final)
          (Complete(comp), s5)
        case _ =>
          val func = (s) => parseDeeper(s, part1Next, part2Next, part3Next, part4Next)
          (Incomplete(func), s5)
      }
    }

    val cont = (s:DesignSeq) => {
      val part1 = Incomplete(part1InitParser)
      val part2 = Incomplete(part2InitParser)
      val part3 = Incomplete(part3InitParser)
      val part4 = Incomplete(part4InitParser)
      parseDeeper(s, part1, part2, part3, part4)
    }

    (Incomplete(cont), seq)
  }

  // Convenience methods for the case where all children are MainSegment

  def fromSeqBF_1Main[T](seq: DesignSeq, constr: MainSegment=>T): (BFParseResult[T], DesignSeq) = {
    fromSeqBF_1(seq, MainSegment.fromSeqBF(_), constr)
  }

  def fromSeqBF_2Main[T](seq: DesignSeq, constr: (MainSegment, MainSegment)=>T): (BFParseResult[T], DesignSeq) = {
    fromSeqBF_2(seq, MainSegment.fromSeqBF(_), MainSegment.fromSeqBF(_), constr)
  }

  def fromSeqBF_3Main[T](seq: DesignSeq, constr: (MainSegment, MainSegment, MainSegment)=>T): (BFParseResult[T], DesignSeq) = {
    fromSeqBF_3(seq, MainSegment.fromSeqBF(_), MainSegment.fromSeqBF(_), MainSegment.fromSeqBF(_), constr)
  }

  def fromSeqBF_4Main[T](seq: DesignSeq, constr: (MainSegment, MainSegment, MainSegment, MainSegment)=>T): (BFParseResult[T], DesignSeq) = {
    fromSeqBF_4(seq, MainSegment.fromSeqBF(_), MainSegment.fromSeqBF(_), MainSegment.fromSeqBF(_), MainSegment.fromSeqBF(_), constr)
  }

}
