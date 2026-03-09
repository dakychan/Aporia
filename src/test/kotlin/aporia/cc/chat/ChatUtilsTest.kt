package aporia.cc.chat

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

/**
 * Unit tests for ChatUtils functionality.
 */
class ChatUtilsTest {
    
    @BeforeEach
    fun setup() {
        /**
         * Reset prefix to default before each test.
         */
        ChatUtils.setPrefix("^")
    }
    
    /**
     * Test formatCommand() with default prefix.
     */
    @Test
    fun testFormatCommandWithDefaultPrefix() {
        val result = ChatUtils.formatCommand("help")
        assertEquals("^help", result, "formatCommand should prepend default prefix")
    }
    
    /**
     * Test formatCommand() with custom prefix.
     */
    @Test
    fun testFormatCommandWithCustomPrefix() {
        ChatUtils.setPrefix(".")
        val result = ChatUtils.formatCommand("help")
        assertEquals(".help", result, "formatCommand should prepend custom prefix")
    }
    
    /**
     * Test formatCommand() with empty command.
     */
    @Test
    fun testFormatCommandWithEmptyCommand() {
        val result = ChatUtils.formatCommand("")
        assertEquals("^", result, "formatCommand should return just prefix for empty command")
    }
    
    /**
     * Test formatCommand() dynamically updates with prefix changes.
     */
    @Test
    fun testFormatCommandDynamicPrefixUpdate() {
        ChatUtils.setPrefix("^")
        val result1 = ChatUtils.formatCommand("info")
        assertEquals("^info", result1, "formatCommand should use first prefix")
        
        ChatUtils.setPrefix("!")
        val result2 = ChatUtils.formatCommand("info")
        assertEquals("!info", result2, "formatCommand should use updated prefix")
    }
    
    /**
     * Test formatCommand() with multi-character prefix.
     */
    @Test
    fun testFormatCommandWithMultiCharPrefix() {
        ChatUtils.setPrefix(">>")
        val result = ChatUtils.formatCommand("config")
        assertEquals(">>config", result, "formatCommand should work with multi-character prefix")
    }
}
