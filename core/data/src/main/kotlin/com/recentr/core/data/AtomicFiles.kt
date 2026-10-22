package com.recentr.core.data

import java.io.File

/** Writes [text] next to this file first, then renames: a crash mid-write never leaves half a file. */
internal fun File.writeAtomically(text: String) {
    parentFile?.mkdirs()
    val temp = File(parentFile, "$name.tmp")
    temp.writeText(text)
    if (!temp.renameTo(this)) {
        delete()
        temp.renameTo(this)
    }
}
