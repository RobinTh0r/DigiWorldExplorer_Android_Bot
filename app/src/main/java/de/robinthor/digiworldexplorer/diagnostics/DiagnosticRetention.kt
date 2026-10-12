package de.robinthor.digiworldexplorer.diagnostics

/** Retain recent evidence, not just the beginning of a run. Pure policy for tests. */
internal data class EvidenceFile(val name:String,val modified:Long,val bytes:Long)
internal fun evidenceToRemove(files:List<EvidenceFile>,maxFiles:Int,maxBytes:Long):Set<String> {
    require(maxFiles>0 && maxBytes>0)
    var bytes=0L
    return files.sortedWith(compareByDescending<EvidenceFile>{it.modified}.thenByDescending{it.name})
        .mapIndexedNotNull { index,file ->
            // Keep the newest file even if it alone exceeds the budget; writing code
            // caps image dimensions independently. Never delete arbitrary user files.
            bytes+=file.bytes.coerceAtLeast(0)
            file.name.takeIf { index>=maxFiles || index>0 && bytes>maxBytes }
        }.toSet()
}
