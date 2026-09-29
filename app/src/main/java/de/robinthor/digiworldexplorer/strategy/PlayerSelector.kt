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
  val nearby=candidates.filter{e->(e.key==previous||e.key !in recentItems)&&(previous==null||distance(e.key,previous)<=1)}
  // Large partner sprites (notably Botamon/Botemon-class figures) spill into a neighbouring
  // cell. The neighbour may briefly have the larger raw colour score although no move happened.
  // Keep the previously proven cell while it still carries most of the best nearby evidence;
  // an explicitly expected destination above always wins after a commanded move.
  previous?.let { old ->
   val oldEntry=nearby.firstOrNull{it.key==old}
   val best=nearby.maxOfOrNull{it.value.player}?:0.0
   if(oldEntry!=null&&oldEntry.value.player>=best*.68)return oldEntry
  }
  return nearby.maxByOrNull{e->e.value.player}
 }
 private fun distance(a:Cell,b:Cell)=abs(a.row-b.row)+abs(a.col-b.col)
}
