package de.robinthor.digiworldexplorer.detection
object CalibrationValidator{
 fun plausible(cells:Map<Cell,CellScores>):Boolean{val scores=cells.values.map{it.player};return (scores.maxOrNull()?:0.0)>=.08&&scores.count{it>.50}<=2}
}