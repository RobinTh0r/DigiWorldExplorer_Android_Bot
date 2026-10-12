package de.robinthor.digiworldexplorer.diagnostics

import org.junit.Assert.*
import org.junit.Test

class DiagnosticRetentionTest {
    @Test fun fiftyFramesKeepTheEndOfLongRun() {
        val files=(1..80).map {EvidenceFile("frame-$it",it.toLong(),100)}
        val removed=evidenceToRemove(files,50,100_000)
        assertEquals((1..30).map {"frame-$it"}.toSet(),removed)
        assertFalse("The failure at the end must survive", "frame-80" in removed)
    }
    @Test fun byteBudgetIsIndependentOfImageCount() {
        assertEquals(setOf("older"),evidenceToRemove(listOf(EvidenceFile("older",1,600),EvidenceFile("latest",2,600)),50,1000))
    }
    @Test fun newestEvidenceIsKeptEvenIfSingleFileExceedsBudget() {
        assertEquals(setOf("older"),evidenceToRemove(listOf(EvidenceFile("older",1,20),EvidenceFile("latest",2,2000)),50,1000))
    }
    @Test fun equalTimestampsAreDeterministicAndNothingOutsideListIsDeleted() {
        assertEquals(setOf("a"),evidenceToRemove(listOf(EvidenceFile("a",1,1),EvidenceFile("b",1,1)),1,100))
    }
}
