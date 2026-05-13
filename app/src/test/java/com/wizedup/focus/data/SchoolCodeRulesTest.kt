package com.wizedup.focus.data

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class SchoolCodeRulesTest {

    @Test
    fun validate_normalizesCaseAndTrim() {
        assertThat(SchoolCodeRules.validate("  demo-001  ")).isEqualTo("DEMO-001")
    }

    @Test
    fun validate_rejectsTooShort() {
        assertThrows(IllegalArgumentException::class.java) {
            SchoolCodeRules.validate("AB")
        }
    }

    @Test
    fun validate_rejectsInvalidChars() {
        assertThrows(IllegalArgumentException::class.java) {
            SchoolCodeRules.validate("DEMO_001")
        }
    }
}
