package de.robinthor.digiworldexplorer.detection
import org.junit.Assert.*
import org.junit.Test

class CellClassifierTest{
 private val w=1000;private val h=1400;private val b=GridBounds(100,300,900,1100)
 private fun canvas()=IntArray(w*h){0xff163040.toInt()}
 private fun IntArray.fill(cell:Cell,color:Int,inset:Double=.10){
  val cw=(b.right-b.left)/5;val ch=(b.bottom-b.top)/5
  val ix=(cw*inset).toInt();val iy=(ch*inset).toInt()
  for(y in b.top+cell.row*ch+iy until b.top+(cell.row+1)*ch-iy-(ch*.12).toInt())
   for(x in b.left+cell.col*cw+ix until b.left+(cell.col+1)*cw-ix)this[y*w+x]=color
 }
 /** Blob in der Zellmitte, Groesse als Anteil der Zelle. */
 private fun IntArray.blob(cell:Cell,color:Int,part:Double){
  val cw=(b.right-b.left)/5;val ch=(b.bottom-b.top)/5
  val cx=b.left+cell.col*cw+cw/2;val cy=b.top+cell.row*ch+ch/2
  val rx=(cw*part/2).toInt();val ry=(ch*part/2).toInt()
  for(y in cy-ry until cy+ry)for(x in cx-rx until cx+rx)this[y*w+x]=color
 }

 /** Das Sprite ist ein dunkler Koerper mit gelben Augen auf hell markiertem Feld. */
 @Test fun classifiesRelativeCellsAtArbitrarySize(){
  val p=canvas()
  p.fill(Cell(2,1),0xff2090d0.toInt());p.blob(Cell(2,1),0xff101820.toInt(),.55);p.blob(Cell(2,1),0xffffd000.toInt(),.20)
  p.fill(Cell(1,3),0xffff7800.toInt());p.fill(Cell(3,2),0xff7050b0.toInt())
  val c=CellClassifier.classify(w,h,p,b)
  assertTrue("player=${c.getValue(Cell(2,1)).player}",c.getValue(Cell(2,1)).player>.08)
  assertTrue(c.getValue(Cell(1,3)).orange>.06)
  assertTrue(c.getValue(Cell(3,2)).obstacle())
 }

 /**
  * Blendet das Spiel "Bewegung zum ausgewaehlten Ort ist nicht moeglich." ein, deckt weisse
  * Schrift mehrere Zellen ab. Fruehere Fassungen haben diesen Helligkeitsanteil als Spielfigur
  * gewertet - der Bot ist dann der Meldung hinterhergetippt statt der Figur.
  */
 @Test fun dialogTextIsNeitherPlayerNorItem(){
  val p=canvas()
  for(col in 0..4)p.fill(Cell(2,col),0xfff4f4f4.toInt())
  val c=CellClassifier.classify(w,h,p,b)
  for(col in 0..4){
   val s=c.getValue(Cell(2,col))
   assertTrue("player col=$col ist ${s.player}",s.player<.08)
   assertTrue("item col=$col ist ${s.item}",s.item<.06)
   assertTrue("text col=$col ist ${s.text}",s.text>.08)
  }
 }
 @Test fun pyramidTipIsNotObstacle(){assertFalse(CellScores(0.0,0.0,0.0,0.0,0.0,.26,0.0).obstacle())}
 @Test fun itemArtIsNotObstacle(){
  val s=CellScores(0.0,0.0,.09,0.0,.09,.60,0.0);assertFalse(s.obstacle())
 }
 @Test fun bottomWallDecorationIsIgnored(){
  val w=700;val h=1200;val b=GridBounds(50,300,650,900);val p=IntArray(w*h){0xff163040.toInt()};val cw=(b.right-b.left)/5;val ch=(b.bottom-b.top)/5
  for(col in 0..4)for(y in b.bottom-(ch*.38).toInt() until b.bottom)for(x in b.left+col*cw until b.left+(col+1)*cw)p[y*w+x]=if(col%2==0)0xff7050b0.toInt() else 0xff30d0b0.toInt()
  val cells=CellClassifier.classify(w,h,p,b)
  for(col in 0..4){val score=cells.getValue(Cell(4,col));assertFalse("wall obstacle col=$col",score.obstacle());assertTrue("wall item col=$col",score.item<.06)}
 }}
