package com.saadm.zenith.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LicenceNoticeTest {
    @Test
    fun everyNoticeHasItsLicenceFile() {
        notices.forEach { assertTrue(it.file, File("src/main/assets/licenses/${it.file}").isFile) }
    }

    @Test
    fun reflowJoinsWrappedLinesAndKeepsParagraphs() {
        assertEquals("One two\n\nThree", reflowLicence("  One\n  two\n \n\nThree\n"))
    }
}
