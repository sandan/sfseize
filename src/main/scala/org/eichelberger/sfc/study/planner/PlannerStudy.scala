package org.eichelberger.sfc.study.planner

import org.eichelberger.sfc.SpaceFillingCurve
import org.eichelberger.sfc.SpaceFillingCurve._
import org.eichelberger.sfc.planners.{QuadTreePlanner, SquareQuadTreePlanner}
import org.eichelberger.sfc.utils.Timing
import org.eichelberger.sfc._

object PlannerStudy extends App {
  trait IndexCounter extends SpaceFillingCurve {
    var numIndexed: Long = 0L

    abstract override def index(point: OrdinalVector): OrdinalNumber = {
      numIndexed += 1
      super.index(point)
    }
  }

  class NewZ(precisions: OrdinalVector) extends ZCurve(precisions) with IndexCounter

  class OldH(precisions: OrdinalVector) extends OldCompactHilbertCurve(precisions) with IndexCounter

  class NewH(precisions: OrdinalVector) extends NewCompactHilbertCurve(precisions) with IndexCounter

  val precisions = OrdinalVector(20, 30)

  val pointQuery = Query(Seq(
    OrdinalRanges(OrdinalPair(3, 3)),
    OrdinalRanges(OrdinalPair(19710507, 19710507))
  ))
  val smallQuery = Query(Seq(
    OrdinalRanges(OrdinalPair(1970, 2001)),
    OrdinalRanges(OrdinalPair(423, 828))
  ))
  val mediumQuery = Query(Seq(
    OrdinalRanges(OrdinalPair(19704, 20018)),
    OrdinalRanges(OrdinalPair(4230, 8281))
  ))
  val bigQuery = Query(Seq(
    OrdinalRanges(OrdinalPair(2, 28), OrdinalPair(101, 159)),
    OrdinalRanges(OrdinalPair(19710507, 20010423))
  ))
  val query = smallQuery

  val z = new NewZ(precisions)
  val h0 = new OldH(precisions)
  val h1 = new NewH(precisions)

  val numWarmup = 2
  val numEval = 5

  (1 to numWarmup) foreach { i =>
    val (rZ, msZ) = Timing.time(() => z.getRangesCoveringQuery(query))
    println(s"warmup Z $i in $msZ ms...")
    val (rH0, msH0) = Timing.time(() => h0.getRangesCoveringQuery(query))
    println(s"warmup H0 $i in $msH0 ms...")
    val (rH1, msH1) = Timing.time(() => h1.getRangesCoveringQuery(query))
    println(s"warmup H1 $i in $msH1 ms...")

    if (i == 1) {
      val zSize = rZ.toList.size
      val zCells = rZ.toList.map(_.size).sum
      println(s"\n  Number of Z ranges:  $zSize\n               cells:  $zCells")

      val rListH0 = rH0.toList
      val h0Size = rListH0.size
      val h0Cells = rListH0.toList.map(_.size).sum
      println(s"\n  Number of H0 ranges:  $h0Size\n                cells:  $h0Cells")

      val rListH1 = rH1.toList
      val h1Size = rListH1.size
      val h1Cells = rListH1.toList.map(_.size).sum
      println(s"\n  Number of H1 ranges:  $h1Size\n                cells:  $h1Cells")

      println(s"\n  Number of Z evaluations:  ${z.numIndexed}")
      println(s"  Number of H0 evaluations:  ${h0.numIndexed}")
      println(s"  Number of H1 evaluations:  ${h1.numIndexed}")
    }

    System.out.flush()
    require(rH0.toList == rH1.toList, s"Old/new ranges do not match!\n  old:  ${rH0.toList}\n  new:  ${rH1.toList}")
  }

  val (msSumZ, msSumH0, msSumH1) = (1 to numEval).foldLeft((0L, 0L, 0L))((acc, i) => acc match {
    case (soFarZ, soFarH0, soFarH1) =>
      val (_, msZ) = Timing.time(() => z.getRangesCoveringQuery(query))
      println(s"evaluated Z $i in $msZ ms...")
      val (rH0, msH0) = Timing.time(() => h0.getRangesCoveringQuery(query))
      println(s"evaluated H0 $i in $msH0 ms...")
      val (rH1, msH1) = Timing.time(() => h1.getRangesCoveringQuery(query))
      println(s"evaluated H1 $i in $msH1 ms...")

      (soFarZ + msZ, soFarH0 + msH0, soFarH1 + msH1)
  })
  val msZ = msSumZ / numEval.toDouble / 1000.0
  val msH0 = msSumH0 / numEval.toDouble / 1000.0
  val msH1 = msSumH1 / numEval.toDouble / 1000.0

  println(s"[PLANNER TIMING STUDY]")
  println(s"\nNumber of warm-up trials:  $numWarmup")
  println(s"Number of evaluation trials:  $numEval")
  println(s"\nZ mean planning time:  ${msZ.formatted("%1.4f")} seconds")
  println(s"H0 mean planning time:  ${msH0.formatted("%1.4f")} seconds")
  println(s"H1 mean planning time:  ${msH1.formatted("%1.4f")} seconds")
}