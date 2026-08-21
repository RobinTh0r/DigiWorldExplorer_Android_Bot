package de.robinthor.digiworldexplorer.detection
import kotlin.math.max
object PreviewClassifier{
 fun classify(width:Int,height:Int,argb:IntArray,b:GridBounds):Map<Cell,CellScores>{
  val cw=(b.right-b.left)/5.0;val ch=(b.bottom-b.top)/5.0;val x0=(b.right+cw*.05).toInt().coerceIn(0,width-1);val x1=(b.right+cw*.93).toInt().coerceIn(x0+1,width);if(x1-x0<cw*.35)return emptyMap()
  return (0..4).associateWith{row->val y0=(b.top+row*ch+ch*.07).toInt().coerceIn(0,height-1);val y1=(b.top+(row+1)*ch-ch*(if(row==4).42 else .11)).toInt().coerceIn(y0+1,height);score(width,argb,x0,x1,y0,y1,Cell(row,5))}.values.associateBy{it.first}.mapValues{it.value.second}
 }
 private fun score(w:Int,p:IntArray,x0:Int,x1:Int,y0:Int,y1:Int,cell:Cell):Pair<Cell,CellScores>{var n=0;var o=0;var pink=0;var green=0;var red=0;var neutral=0;var yellow=0;var dark=0;var hi=0;var pyramid=0
  for(y in y0 until y1)for(x in x0 until x1){val v=p[y*w+x];val r=v shr 16 and 255;val g=v shr 8 and 255;val b=v and 255;n++;val orange=r>180&&g>55&&g<190&&b<100;if(orange)o++;if(r>170&&b>140&&g<170&&r>g+40)pink++;if(g>130&&r<150&&g>r+35&&g>b+25)green++;if(r>110&&r>g+45&&r>b+25&&!orange)red++;val mx=maxOf(r,g,b);val mn=minOf(r,g,b);if(mn>165&&mx-mn<65)neutral++;if(r>190&&g>140&&b<80)yellow++;if(r<65&&g<65&&b<75)dark++;if(b>120&&g>90&&b>r+25)hi++;if(b>70&&r>45&&b>g+10)pyramid++}
  fun q(v:Int):Double { return v/max(1,n).toDouble() };val os=q(o);val ps=q(pink);val gs=q(green);val yellowClaw=if(q(yellow)>.008&&q(dark)<.08&&q(hi)<.25&&os<.06)q(yellow)*7 else 0.0;val item=maxOf(os,ps,gs,q(red)*4.0,yellowClaw);val shadow=if(q(dark)>.20&&q(yellow)>.008&&q(hi)>.20&&os<.04)q(yellow)*8 else 0.0;return cell to CellScores(maxOf(q(red),q(neutral),shadow),os,ps,gs,item,q(pyramid),q(hi))}
}