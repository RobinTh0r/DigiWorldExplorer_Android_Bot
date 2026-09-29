package de.robinthor.digiworldexplorer.purchase

import javax.imageio.ImageIO
import org.junit.Assert.*
import org.junit.Test

class SummonV4DiagnosticRegressionTest {
    @Test fun purpleTicketResultFromDiagnosticIsAffordableInV4() {
        val image = ImageIO.read(javaClass.getResource("/oneplus_summon_purple_result.jpg"))
        val result = RewardPurchaseDetector.detect(image.width, image.height, image::getRGB)
        assertTrue(result.recognized)
        assertTrue(result.affordable)
        assertEquals(image.width * .665f, result.tapX, .01f)
        assertEquals(image.height * .96f, result.tapY, .01f)
    }

    @Test fun partnerScreenFromDiagnosticDoesNotAuthorizeSummon() {
        val image = ImageIO.read(javaClass.getResource("/oneplus_partner_during_summon_test.jpg"))
        assertFalse(RewardPurchaseDetector.detect(image.width, image.height, image::getRGB).recognized)
    }
}
