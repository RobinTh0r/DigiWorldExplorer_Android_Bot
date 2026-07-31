package de.robinthor.digiworldexplorer.detection

/**
 * Findet den blauen Dash-Knopf unterhalb des Spielfelds. Absolute Pixelkoordinaten waeren
 * geraete- und panelabhaengig, deshalb wird der Knopf ueber seine Farbe gesucht: er ist die
 * einzige grosse, kraeftig cyan-blaue Flaeche unterhalb des Rasters. Der dunkelblaue
 * Ueberschriftentext faellt durch die Gruen-Untergrenze heraus.
 */
object DashButtonLocator{
 private const val MIN_SAMPLES=350
 private const val STEP=2

 fun locate(width:Int,height:Int,argb:IntArray,bounds:GridBounds):Pair<Float,Float>?{
  val y0=(bounds.bottom+ (bounds.bottom-bounds.top)/20).coerceIn(0,height-1)
  if(y0>=height-10)return null
  var sumX=0L;var sumY=0L;var n=0
  var y=y0
  while(y<height){
   var x=0
   while(x<width){
    val p=argb[y*width+x];val r=p shr 16 and 255;val g=p shr 8 and 255;val b=p and 255
    if(b>195&&g>140&&r<140&&b>r+80){sumX+=x;sumY+=y;n++}
    x+=STEP
   }
   y+=STEP
  }
  if(n<MIN_SAMPLES)return null
  return (sumX.toDouble()/n).toFloat() to (sumY.toDouble()/n).toFloat()
 }
}
