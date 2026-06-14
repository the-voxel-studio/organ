package fr.studio.voxel.organ

import fr.studio.voxel.organ.domain.ValidateEmailUseCase
import fr.studio.voxel.organ.domain.ValidatePasswordUseCase
import fr.studio.voxel.organ.ui.components.formatDeletedDate
import org.junit.Test
import org.junit.Assert.*

class ValidationUnitTest {

    private val validateEmail = ValidateEmailUseCase()
    private val validatePassword = ValidatePasswordUseCase()

    @Test
    fun validateEmail_correctEmails_returnsTrue() {
        assertTrue(validateEmail("test@example.com"))
        assertTrue(validateEmail("user.name+tag@domain.co.uk"))
        assertTrue(validateEmail("123@domain.org"))
    }

    @Test
    fun validateEmail_incorrectEmails_returnsFalse() {
        assertFalse(validateEmail("plainaddress"))
        assertFalse(validateEmail("#@%^%#$@#$@#.com"))
        assertFalse(validateEmail("@domain.com"))
        assertFalse(validateEmail("Joe Smith <email@domain.com>"))
        assertFalse(validateEmail("email.domain.com"))
        assertFalse(validateEmail("email@domain@domain.com"))
    }

    @Test
    fun validatePassword_correctPassword_returnsTrue() {
        assertTrue(validatePassword("password123"))
        assertTrue(validatePassword("12345678")) // Exactly 8 characters
    }

    @Test
    fun validatePassword_incorrectPassword_returnsFalse() {
        assertFalse(validatePassword("1234567")) // 7 characters
        assertFalse(validatePassword(""))
    }

    @Test
    fun formatDeletedDate_correctUtcDate_returnsFormattedDate() {
        val originalTimeZone = java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))
            val input = "2026-06-14T08:27:00"
            val expected = "14/06/2026 08:27"
            assertEquals(expected, formatDeletedDate(input))
        } finally {
            java.util.TimeZone.setDefault(originalTimeZone)
        }
    }

    @Test
    fun formatDeletedDate_nullOrBlank_returnsDash() {
        assertEquals("-", formatDeletedDate(null))
        assertEquals("-", formatDeletedDate(""))
    }

    @Test
    fun formatDeletedDate_invalidFormat_returnsSubstring() {
        assertEquals("invalid_da", formatDeletedDate("invalid_date_format"))
    }
}
