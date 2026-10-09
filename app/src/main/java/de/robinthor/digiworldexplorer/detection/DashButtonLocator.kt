package de.robinthor.digiworldexplorer.detection

/**
 * Finds the blue action with the green Dash icon below the board. In game 1.5 the upper-right
 * blue button is a broom action and must never be used as the Dash target.
 */
object DashButtonLocator{
 private const val STEP=2

 fun locate(width:Int,height:Int,argb:IntArray,bounds:GridBounds):Pair<Float,Float>?{
  val gridWidth=bounds.right-bounds.left
  val gridHeight=bounds.bottom-bounds.top
  if(gridWidth<=0||gridHeight<=0)return null
  val x0=(bounds.left+gridWidth*.42).toInt().coerceIn(0,width-1)
  val y0=(bounds.bottom+gridHeight*.18).toInt().coerceIn(0,height-1)
  val x1=minOf(width,bounds.right+gridWidth/8)
  val y1=minOf(height,(bounds.bottom+gridHeight*.88).toInt())
  if(x1<=x0||y1<=y0)return null
  val sw=(x1-x0+STEP-1)/STEP;val sh=(y1-y0+STEP-1)/STEP
  val blue=BooleanArray(sw*sh)
  for(gy in 0 until sh)for(gx in 0 until sw){
   val x=x0+gx*STEP;val y=y0+gy*STEP
   if(x>=width||y>=height)continue
   val p=argb[y*width+x];val r=p shr 16 and 255;val g=p shr 8 and 255;val b=p and 255
   blue[gy*sw+gx]=b>195&&g>140&&r<140&&b>r+80
  }
  val seen=BooleanArray(blue.size);val queue=IntArray(blue.size)
  data class Candidate(val x:Float,val y:Float)
  val candidates=mutableListOf<Candidate>()
  for(start in blue.indices){
   if(!blue[start]||seen[start])continue
   var head=0;var tail=0;queue[tail++]=start;seen[start]=true
   var minX=sw;var maxX=0;var minY=sh;var maxY=0
   while(head<tail){
    val at=queue[head++];val gx=at%sw;val gy=at/sw
    minX=minOf(minX,gx);maxX=maxOf(maxX,gx);minY=minOf(minY,gy);maxY=maxOf(maxY,gy)
    for(dy in -1..1)for(dx in -1..1){
     val nx=gx+dx;val ny=gy+dy
     if(nx !in 0 until sw||ny !in 0 until sh)continue
     val next=ny*sw+nx
     if(blue[next]&&!seen[next]){seen[next]=true;queue[tail++]=next}
    }
   }
   val boxW=(maxX-minX+1)*STEP;val boxH=(maxY-minY+1)*STEP
   val aspect=boxW.toDouble()/boxH.coerceAtLeast(1)
   val cx=x0+(minX+maxX)*STEP/2f;val cy=y0+(minY+maxY)*STEP/2f
   val relativeX=(cx-bounds.left)/gridWidth
   val relativeY=(cy-bounds.bottom)/gridHeight
   if(tail<120||boxW !in (gridWidth*.10).toInt()..(gridWidth*.36).toInt()||
      boxH !in (gridWidth*.10).toInt()..(gridWidth*.36).toInt()||aspect !in .7..1.4||
      relativeX !in .50f.. .78f||relativeY !in .30f.. .86f)continue
   val radius=(minOf(boxW,boxH)*.30f).toInt().coerceAtLeast(1)
   var green=0;var samples=0
   for(y in (cy.toInt()-radius).coerceAtLeast(0)..(cy.toInt()+radius).coerceAtMost(height-1) step STEP)
    for(x in (cx.toInt()-radius).coerceAtLeast(0)..(cx.toInt()+radius).coerceAtMost(width-1) step STEP){
     val p=argb[y*width+x];val r=p shr 16 and 255;val g=p shr 8 and 255;val b=p and 255
     samples++
     if(g>155&&g>r+35&&g>b+15)green++
    }
   if(green>=maxOf(18,samples/20))candidates+=Candidate(cx,cy)
  }
  return candidates.singleOrNull()?.let{it.x to it.y}
 }

}
