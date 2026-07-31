package de.robinthor.digiworldexplorer.detection

data class Cell(val row:Int,val col:Int)
data class CellScores(
 val player:Double,val orange:Double,val pink:Double,val green:Double,
 val item:Double,val pyramid:Double,val highlight:Double,
 /** Anteil heller, entsaettigter Pixel - praktisch nur Dialogschrift. Diente frueher als
  *  Spielermerkmal und hat dabei jede Fehlermeldung zur Spielfigur erklaert. */
 val text:Double=0.0,
){fun obstacle():Boolean=pyramid>.30&&item<=.06}

object CellClassifier{
 /** Abtastschritt in Pixeln. Alle Kennzahlen sind Flaechenanteile, ein Gitter aus jedem zweiten
  *  Pixel liefert dieselben Werte bei einem Viertel der Arbeit. Bei rund 32000 Pixeln je Zelle
  *  war das Vollbild-Abtasten der teuerste Schritt der gesamten Analyse. */
 private const val STEP=2
 fun classify(width:Int,height:Int,argb:IntArray,b:GridBounds):Map<Cell,CellScores>{
  require(argb.size==width*height)
  val out=mutableMapOf<Cell,CellScores>()
  val cw=(b.right-b.left)/5.0;val ch=(b.bottom-b.top)/5.0
  for(row in 0..4)for(col in 0..4){
   val x0=(b.left+col*cw+cw*.07).toInt().coerceIn(0,width-1)
   val x1=(b.left+(col+1)*cw-cw*.07).toInt().coerceIn(x0+1,width)
   val y0=(b.top+row*ch+ch*.07).toInt().coerceIn(0,height-1)
   val y1=(b.top+(row+1)*ch-ch*(if(row==4).42 else .11)).toInt().coerceIn(y0+1,height)
   val playerY1=(b.top+(row+1)*ch-ch*.11).toInt().coerceIn(y0+1,height)
   var n=0;var orange=0;var pink=0;var green=0;var red=0;var neutral=0;var yellow=0;var dark=0;var hi=0;var pyramid=0
   for(y in y0 until y1 step STEP)for(x in x0 until x1 step STEP){val p=argb[y*width+x];val r=p shr 16 and 255;val g=p shr 8 and 255;val bl=p and 255;n++
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
   // Das Sprite ist ein dunkler Koerper mit gelben Augen. Frueher zaehlten hier zusaetzlich
   // helle und rote Flaechen - beides trifft auf die Dialogschrift zu und hat die Verfolgung
   // auf die Fehlermeldung gezogen, statt auf die Figur.
   val shadow=if(q(dark)>.20&&q(yellow)>.008&&q(hi)>.20&&q(orange)<.04)q(yellow)*8 else 0.0
   // Die Krallen-Aufsammelbelohnung ist ein satt rotes Symbol, kein orangenes wie die
   // Energiekugel - ohne den eigenen Rotanteil wurde sie nie als Item erkannt und die Figur
   // ist einfach daran vorbeigelaufen.
   val os=q(orange);val ps=q(pink);val gs=q(green);val rs=q(red);val item=maxOf(os,ps,gs,rs)
   // Nur in der untersten Reihe weicht das Spielerfenster vom Abtastfenster ab (42% statt 11%
   // Rand unten). Fuer alle anderen Reihen ist der zweite Durchlauf Wort fuer Wort derselbe
   // Bereich und damit reine Doppelarbeit.
   val extra=if(row==4)playerScore(width,argb,x0,x1,y0,playerY1) else 0.0
   out[Cell(row,col)]=CellScores(maxOf(shadow,extra),os,ps,gs,item,q(pyramid),q(hi),q(neutral))
  }
  return out
 }
 private fun playerScore(width:Int,argb:IntArray,x0:Int,x1:Int,y0:Int,y1:Int):Double{
  var n=0;var yellow=0;var dark=0;var hi=0;var orange=0
  for(y in y0 until y1 step STEP)for(x in x0 until x1 step STEP){val p=argb[y*width+x];val r=p shr 16 and 255;val g=p shr 8 and 255;val b=p and 255;n++;if(r>180&&g>55&&g<190&&b<100)orange++;if(r>190&&g>140&&b<80)yellow++;if(r<65&&g<65&&b<75)dark++;if(b>120&&g>90&&b>r+25)hi++}
  fun q(v:Int):Double{return v/n.coerceAtLeast(1).toDouble()}
  return if(q(dark)>.20&&q(yellow)>.008&&q(hi)>.20&&q(orange)<.04)q(yellow)*8 else 0.0
 }}
