package ltd.guimc.web.altget.service.pow

import cn.hutool.crypto.digest.DigestUtil
import org.springframework.stereotype.Service

/**
 * PoW (Proof of Work) Verification Service
 * 
 * This service verifies the proof of work based on the custom algorithm from WASM code.
 * 
 * Algorithm:
 * 1. Parse sbox (substitution box) from hex string (128 hex chars = 64 bytes)
 * 2. Generate tireSeed = SHA256(data + difficulty)
 * 3. Swipe tireSeed using sbox
 * 4. Verify sign by:
 *    - Un-swipe the provided sign
 *    - Check if it matches SHA256(nonce + tireSeed) with uppercase transformations
 * 5. Verify final hash: SHA256(data + sign) starts with difficulty zeros
 */
@Service
class PoWVerificationService {

    /**
     * Verify the proof of work
     * 
     * @param input Full input string: 128 hex chars (sbox) + data
     * @param sign The swiped sign (64 hex chars)
     * @param nonce The nonce value
     * @param difficulty The difficulty level (number of leading zeros)
     * @return true if verification passes, false otherwise
     */
    fun verify(input: String, sign: String, nonce: Int, difficulty: Int): Boolean {
        try {
            // Validate input length
            if (input.length < 129) return false
            if (input.length > 629) return false
            
            // Validate difficulty
            if (difficulty !in 1..<64) return false
            
            // Validate sign length
            if (sign.length != 64) return false
            
            // Validate nonce
            if (nonce !in 0..100000000) return false
            
            // Parse sbox (first 128 hex chars)
            val sboxHex = input.substring(0, 128)
            val sbox = parseSbox(sboxHex) ?: return false
            
            // Extract data (remaining chars after sbox)
            val data = input.substring(128)
            
            // Generate tireSeed
            val seedInput = "$data$difficulty"
            var tireSeed = sha256Hex(seedInput)
            
            // Swipe tireSeed
            tireSeed = swipeBox(tireSeed, sbox) ?: return false
            
            // Un-swipe the provided sign to get original sign
            val signOriginal = unSwipeBox(sign, sbox) ?: return false
            
            // Verify sign: should match SHA256(nonce + tireSeed) with transformations
            val expectedSign = generateSign(nonce, tireSeed)
            if (signOriginal != expectedSign) return false
            
            // Verify final hash
            val candidate = "$data$signOriginal"
            val hash = sha256Hex(candidate)
            
            // Check if hash starts with difficulty zeros
            val target = "0".repeat(difficulty)
            return hash.startsWith(target)
            
        } catch (_: Exception) {
            return false
        }
    }
    
    /**
     * Parse sbox from hex string
     * 
     * @param hex 128 hex characters
     * @return IntArray of 64 elements representing byte indices (0-63), or null on error
     */
    private fun parseSbox(hex: String): IntArray? {
        if (hex.length != 128) return null
        
        val sbox = IntArray(64)
        
        for (i in 0 until 64) {
            val hi = hexCharToValue(hex[2 * i])
            val lo = hexCharToValue(hex[2 * i + 1])
            
            if (hi < 0 || lo < 0) return null
            
            sbox[i] = (hi shl 4) or lo
        }
        
        return sbox
    }
    
    /**
     * Convert hex character to value
     */
    private fun hexCharToValue(c: Char): Int {
        return when (c) {
            in '0'..'9' -> c - '0'
            in 'a'..'f' -> 10 + (c - 'a')
            in 'A'..'F' -> 10 + (c - 'A')
            else -> -1
        }
    }
    
    /**
     * Swipe box operation: rearrange string characters according to sbox indices
     * 
     * @param data 64-character hex string
     * @param sbox Array of 64 indices
     * @return Swiped string or null on error
     */
    private fun swipeBox(data: String, sbox: IntArray): String? {
        if (data.length != 64) return null
        if (sbox.size != 64) return null
        
        val result = CharArray(64)
        
        for (i in 0 until 64) {
            val index = sbox[i]
            if (index < 0 || index >= 64) return null
            result[i] = data[index]
        }
        
        return String(result)
    }
    
    /**
     * Un-swipe box operation: reverse the swipe operation
     * 
     * @param data 64-character hex string (swiped)
     * @param sbox Array of 64 indices
     * @return Un-swiped string or null on error
     */
    private fun unSwipeBox(data: String, sbox: IntArray): String? {
        if (data.length != 64) return null
        if (sbox.size != 64) return null
        
        val result = CharArray(64)
        
        for (i in 0 until 64) {
            val index = sbox[i]
            if (index < 0 || index >= 64) return null
            result[index] = data[i]
        }
        
        return String(result)
    }
    
    /**
     * Generate sign with uppercase transformations
     * 
     * @param nonce The nonce value
     * @param tireSeed The tire seed (64 hex chars)
     * @return Sign with uppercase transformations applied
     */
    private fun generateSign(nonce: Int, tireSeed: String): String {
        // Create input: nonce_str + tireSeed
        val tmp = "$nonce$tireSeed"
        val sign = sha256Hex(tmp)
        
        // Apply uppercase transformations
        // For each position i (i != 0), if nonce % i == 0 and char is lowercase, convert to uppercase
        val signChars = sign.toCharArray()
        
        for (i in 1 until 64) {
            if (nonce % i == 0 && signChars[i] in 'a'..'z') {
                signChars[i] = signChars[i] - 32 // Convert to uppercase
            }
        }
        
        return String(signChars)
    }
    
    /**
     * Calculate SHA256 hash and return as hex string
     * 
     * @param input Input string
     * @return 64-character hex string (lowercase)
     */
    private fun sha256Hex(input: String): String {
        return DigestUtil.sha256Hex(input)
    }
}
