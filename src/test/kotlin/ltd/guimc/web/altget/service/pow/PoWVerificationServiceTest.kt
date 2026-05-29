package ltd.guimc.web.altget.service.pow

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class PoWVerificationServiceTest {

    @Autowired
    private lateinit var powVerificationService: PoWVerificationService

    @Test
    fun `test verify with valid proof of work`() {
        // Example test case - you'll need to generate actual valid PoW data from the WASM client
        // The input format is: 128 hex chars (sbox) + data
        // For example:
        // sbox = "0001020304050607...3f" (64 bytes as 128 hex chars representing a permutation)
        // data = "exampleData"
        // sign = 64 hex chars (the result from WASM computation)
        // nonce = the nonce value found by WASM
        // difficulty = number of leading zeros required
        
        // This is a placeholder test - replace with actual valid PoW data
        val sbox = (0..63).joinToString("") { String.format("%02x", it) }
        val data = "testData"
        val input = sbox + data
        val sign = "a".repeat(64) // placeholder
        val nonce = 12345
        val difficulty = 4
        
        // Note: This will likely return false until you provide real PoW data from WASM
        val result = powVerificationService.verify(input, sign, nonce, difficulty)
        
        // In production, this should be assertTrue with valid PoW data
        println("Verification result: $result")
    }

    @Test
    fun `test verify with invalid input length`() {
        // Too short
        val result1 = powVerificationService.verify("short", "a".repeat(64), 100, 4)
        assertFalse(result1)
        
        // Too long
        val result2 = powVerificationService.verify("a".repeat(630), "a".repeat(64), 100, 4)
        assertFalse(result2)
    }

    @Test
    fun `test verify with invalid difficulty`() {
        val sbox = (0..63).joinToString("") { String.format("%02x", it) }
        val input = sbox + "testData"
        val sign = "a".repeat(64)
        
        // difficulty <= 0
        val result1 = powVerificationService.verify(input, sign, 100, 0)
        assertFalse(result1)
        
        // difficulty >= 64
        val result2 = powVerificationService.verify(input, sign, 100, 64)
        assertFalse(result2)
    }

    @Test
    fun `test verify with invalid sign length`() {
        val sbox = (0..63).joinToString("") { String.format("%02x", it) }
        val input = sbox + "testData"
        
        // Sign too short
        val result1 = powVerificationService.verify(input, "abc", 100, 4)
        assertFalse(result1)
        
        // Sign too long
        val result2 = powVerificationService.verify(input, "a".repeat(65), 100, 4)
        assertFalse(result2)
    }

    @Test
    fun `test verify with invalid nonce`() {
        val sbox = (0..63).joinToString("") { String.format("%02x", it) }
        val input = sbox + "testData"
        val sign = "a".repeat(64)
        
        // Negative nonce
        val result1 = powVerificationService.verify(input, sign, -1, 4)
        assertFalse(result1)
        
        // Nonce too large
        val result2 = powVerificationService.verify(input, sign, 100000001, 4)
        assertFalse(result2)
    }

    @Test
    fun `test verify with invalid sbox hex`() {
        // Invalid hex characters
        val invalidSbox = "g".repeat(128) // 'g' is not a valid hex char
        val input = invalidSbox + "testData"
        val sign = "a".repeat(64)
        
        val result = powVerificationService.verify(input, sign, 100, 4)
        assertFalse(result)
    }
}
