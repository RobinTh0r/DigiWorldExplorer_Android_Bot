package de.robinthor.digiworldexplorer.detection

data class Cell(val row:Int,val col:Int)
data class CellScores(
 val player:Double,val orange:Double,val pink:Double,val green:Double,
 val item:Double,val pyramid:Double,val highlight:Double,
){fun obstacle():Boolean=pyramid>.30&&item<=.06}

object CellClassifier{
 fun classify(width:Int,height:Int,argb:IntArray,b:GridBounds):Map<Cell,CellScores>{
  require(argb.size==width*height)
  val out=mutableMapOf<Cell,CellScores>()
  val cw=(b.right-b.left)/5.0;val ch=(b.bottom-b.top)/5.0
  for(row in 0..4)for(col in 0..4){
   val x0=(b.left+col*cw+cw*.07).toInt().coerceIn(0,width-1)
   val x1=(b.left+(col+1)*cw-cw*.07).toInt().coerceIn(x0+1,width)
   val y0=(b.top+row*ch+ch*.07).toInt().coerceIn(0,height-1)
   val y1=(b.top+(row+1)*ch-ch*(if(row==4).42 else .11)).toInt().coerceIn(y0+1,height)
   var n=0;var orange=0;var pink=0;var green=0;var red=0;var neutral=0;var yellow=0;var dark=0;var hi=0;var pyramid=0
   for(y in y0 until y1)for(x in x0 until x1){val p=argb[y*width+x];val r=p shr 16 and 255;val g=p shr 8 and 255;val bl=p and 255;n++
    val o=r>180&&g>55&&g<190&&bl<100;if(o)orange++
    if(r>170&&bl>140&&g<170&&r>g+40)pink++
    if(g>130&&r<150&&g>r+35&&g>bl+25)green++
    if(r>110&&r>g+45&&r>bl+25&&!o)red++
    val max=maxOf(r,g,bl);val min=minOf(r,g,bl);if(min>165&&max-min<65)neutral++
    if(r>190&&g>140&&bl<80)yellow++;if(r<65&&g<65&&bl<75)dark++
    if(bl>120&&g>90&&bl>r+25)hi++
    if(bl>70&&r>45&&bl>g+10)pyramid++
   }
   fun q(v:Int):Double { return v/n.toDouble() }
   val shadow=if(q(dark)>.20&&q(yellow)>.008&&q(hi)>.20&&q(orange)<.04)q(yellow)*8 else 0.0
   val os=q(orange);val ps=q(pink);val gs=q(green);val item=maxOf(os,ps,gs)
   out[Cell(row,col)]=CellScores(maxOf(q(red),q(neutral),shadow),os,ps,gs,item,q(pyramid),q(hi))
  }
  return out
 }
}
