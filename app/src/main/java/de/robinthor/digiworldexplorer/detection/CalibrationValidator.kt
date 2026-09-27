package de.robinthor.digiworldexplorer.detection
object CalibrationValidator{
 fun plausible(cells:Map<Cell,CellScores>):Boolean{
  val scores=cells.values.map{it.player}
  // A menu/result page sampled through stale DWS bounds often contains one dark block and used to
  // pass as a player forever. Real board cells do not contain app-wide text across several cells;
  // rejecting that pattern lets the calibrated grid release as soon as DWS is left.
  val textCovered=cells.values.count{it.text>.08}
  return textCovered<3&&(scores.maxOrNull()?:0.0)>=.08&&scores.count{it>.50}<=2
 }
}
