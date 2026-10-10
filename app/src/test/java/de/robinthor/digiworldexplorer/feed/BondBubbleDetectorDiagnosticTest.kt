package de.robinthor.digiworldexplorer.feed

import de.robinthor.digiworldexplorer.vision.PixelFrame
import de.robinthor.digiworldexplorer.vision.GameViewport
import de.robinthor.digiworldexplorer.vision.NormalizedRect
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.imageio.ImageIO

class BondBubbleDetectorDiagnosticTest {
    private fun frame(name: String): PixelFrame {
        val image = ImageIO.read(requireNotNull(javaClass.getResource("/$name")))
        return PixelFrame(image.width, image.height) { x, y -> image.getRGB(x, y) }
    }

    @Test fun `oneplus bubble target is the bubble and never the figure underneath`() {
        val bubble = BondBubbleDetector.observe(frame("oneplus_bond_home_bubble.jpg"))
        assertNotNull(bubble)
        val target=BondBubbleDetector.tapTarget(bubble!!)!!
        assertTrue(target.x>bubble.center.x)
        assertTrue(target.y<bubble.center.y)
        assertTrue(target.x in bubble.bounds.left..bubble.bounds.right)
        assertTrue(target.y in bubble.bounds.top..bubble.bounds.bottom)
    }

    @Test fun `partner popup cannot authorize a collection tap`() {
        assertNull(BondBubbleDetector.detect(frame("oneplus_bond_partner_popup.jpg")))
    }

    @Test fun `moving bubbles use upper right interior instead of a fixed screen point`() {
        for(name in listOf("oneplus_bond_home_bubble.jpg","bond_home_food.png",
            "live_bubble_moving_left.jpg","live_bubble_moving_right.jpg",
            "live_bubble_projectile_before.jpg","live_bubble_projectile_after.jpg")) {
            val reading=requireNotNull(BondBubbleDetector.observe(frame(name))) {name}
            val target=requireNotNull(BondBubbleDetector.tapTarget(reading)) {name}
            assertTrue(name,target.x>reading.center.x && target.y<reading.center.y)
            assertTrue(name,target.x<reading.bounds.right && target.y>reading.bounds.top)
        }
    }

    @Test fun `synthetic scales and black margins keep detection and input in the same viewport`() {
        val source=ImageIO.read(javaClass.getResource("/oneplus_bond_home_bubble.jpg"))
        for((width,height) in listOf(1080 to 2340,1080 to 2400,720 to 1612,1440 to 3200)) {
            for(padded in listOf(false,true)) {
                val left=if(padded)width/20 else 0
                val top=if(padded)height/30 else 0
                val gameWidth=width-left*2
                val gameHeight=height-top*2
                val image=java.awt.image.BufferedImage(width,height,java.awt.image.BufferedImage.TYPE_INT_RGB)
                image.createGraphics().apply {
                    drawImage(source,left,top,gameWidth,gameHeight,null)
                    dispose()
                }
                val pixels=PixelFrame(width,height,image::getRGB)
                val reading=requireNotNull(BondBubbleDetector.observe(pixels)) {"$width/$height padded=$padded"}
                val target=requireNotNull(BondBubbleDetector.tapTarget(reading))
                val (x,y)=reading.viewport.pixel(target)
                // Source bubble is right of the figure; the tap must remain in the
                // transformed white-panel region, not shift down to the central sprite.
                assertTrue("x=$x",x in left+(gameWidth*.49).toInt()..left+(gameWidth*.57).toInt())
                assertTrue("y=$y",y in top+(gameHeight*.37).toInt()..top+(gameHeight*.42).toInt())
                assertTrue(target.x>reading.center.x && target.y<reading.center.y)
            }
        }
    }

    @Test fun `figure band cannot be passed as a bubble tap target`() {
        val figure=BondBubbleReading(NormalizedRect(.40,.53,.60,.65),GameViewport.fit(1080,2340))
        assertNull(BondBubbleDetector.tapTarget(figure))
    }

    @Test fun `vanished bubble does not produce a stored or center fallback target`() {
        val source=ImageIO.read(javaClass.getResource("/oneplus_bond_home_bubble.jpg"))
        val reading=requireNotNull(BondBubbleDetector.observe(PixelFrame(source.width,source.height,source::getRGB)))
        val image=java.awt.image.BufferedImage(source.width,source.height,java.awt.image.BufferedImage.TYPE_INT_RGB)
        image.createGraphics().apply {
            drawImage(source,0,0,null)
            color=java.awt.Color.BLACK
            val (left,top)=reading.viewport.pixel(de.robinthor.digiworldexplorer.vision.NormalizedPoint(reading.bounds.left,reading.bounds.top))
            fillRect(left-5,top-5,(reading.bounds.width*reading.viewport.width).toInt()+12,
                (reading.bounds.height*reading.viewport.height).toInt()+12)
            dispose()
        }
        assertNull(BondBubbleDetector.observe(PixelFrame(image.width,image.height,image::getRGB)))
    }
}
