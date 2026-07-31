package de.robinthor.digiworldexplorer.detection
import org.junit.Assert.*
import org.junit.Test
class CalibrationValidatorTest{
 private fun s(p:Double)=CellScores(p,0.0,0.0,0.0,0.0,0.0,0.0)
 @Test fun rejectsWhiteAppScreen(){assertFalse(CalibrationValidator.plausible((0..4).flatMap{r->(0..4).map{c->Cell(r,c)}}.associateWith{s(1.0)}))}
 @Test fun acceptsSinglePlayerBoard(){assertTrue(CalibrationValidator.plausible(mapOf(Cell(2,1) to s(.12),Cell(0,0) to s(.01))))}
}