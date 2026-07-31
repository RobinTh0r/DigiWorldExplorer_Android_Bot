package de.robinthor.digiworldexplorer.strategy
import de.robinthor.digiworldexplorer.detection.*
import kotlin.math.abs
object PlayerSelector{
 fun select(cells:Map<Cell,CellScores>,previous:Cell?,expected:Cell?,recentItems:Set<Cell>,minScore:Double=.08):Map.Entry<Cell,CellScores>?{
  val candidates=cells.entries.filter{it.value.player>=minScore}
  expected?.let{e->candidates.firstOrNull{it.key==e}?.let{return it}}
  // [recentItems] unterdrueckt Zellen, deren Item-Farbe als Spieler missdeutet werden koennte.
  // Die zuletzt bestaetigte Spielerzelle ist davon ausgenommen: die Figur kann nicht
  // verschwinden, und ihr eigenes Sprite faerbt die Zelle dauerhaft als vermeintliches Item ein.
  return candidates.filter{e->(e.key==previous||e.key !in recentItems)&&(previous==null||distance(e.key,previous)<=1)}.maxByOrNull{e->e.value.player+(if(e.key==previous).04 else 0.0)}
 }
 private fun distance(a:Cell,b:Cell)=abs(a.row-b.row)+abs(a.col-b.col)
}