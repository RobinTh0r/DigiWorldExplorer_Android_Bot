package de.robinthor.digiworldexplorer.detection
import org.junit.Assert.*
import org.junit.Test

class CellClassifierTest{
 @Test fun classifiesRelativeCellsAtArbitrarySize(){
  val w=1000;val h=1400;val b=GridBounds(100,300,900,1100);val p=IntArray(w*h){0xff163040.toInt()}
  fun fill(cell:Cell,color:Int){val cw=(b.right-b.left)/5;val ch=(b.bottom-b.top)/5
   for(y in b.top+cell.row*ch+15 until b.top+(cell.row+1)*ch-20)for(x in b.left+cell.col*cw+15 until b.left+(cell.col+1)*cw-15)p[y*w+x]=color}
  fill(Cell(2,1),0xffd02020.toInt());fill(Cell(1,3),0xffff7800.toInt());fill(Cell(3,2),0xff7050b0.toInt())
  val c=CellClassifier.classify(w,h,p,b)
  assertTrue(c.getValue(Cell(2,1)).player>.08)
  assertTrue(c.getValue(Cell(1,3)).orange>.06)
  assertTrue(c.getValue(Cell(3,2)).obstacle())
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
