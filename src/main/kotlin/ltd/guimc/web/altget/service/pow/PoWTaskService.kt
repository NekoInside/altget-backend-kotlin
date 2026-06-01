package ltd.guimc.web.altget.service.pow

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.math.floor

@Service
class PoWTaskService(private val poWVerificationService: PoWVerificationService) {
    private val consideringTaskTime = 120_000L
    private val defaultDifficulties = object : HashMap<String, Int>() {
        init {
            put("fetch", 2)
            put("reset-password", 3)
            put("new-api", 1)
        }
    }


    private val taskMap = HashMap<String, PoWTask>()
    private val srcAddrTaskTime = HashMap<String, MutableList<Long>>()

    fun expiredTime(difficulty: Int): Long {
        return when (difficulty) {
            in 0..3 -> 120_000
            4 -> 180_000
            5 -> 300_000
            else -> 600_000
        }
    }

    fun createTask(target: String, srcIp: String): PoWTask {
        var difficulty = defaultDifficulties[target] ?: 2
        if (difficulty <= 0) difficulty = 1
        val recentCreatedTaskTime = srcAddrTaskTime[srcIp] ?: emptyList()
        val now = System.currentTimeMillis()
        val recentCreatedTaskCount = recentCreatedTaskTime.count { it > now - consideringTaskTime }
        difficulty += floor(recentCreatedTaskCount / 2.0).toInt()
        srcAddrTaskTime[srcIp] ?: srcAddrTaskTime.put(srcIp, mutableListOf())
        srcAddrTaskTime[srcIp]!!.add(now)
        while (srcAddrTaskTime.size > 6)  {
            srcAddrTaskTime[srcIp]!!.removeAt(0)
        }
        val taskId = UUID.randomUUID().toString().replace("-", "")
        val data = generateSwipeBox() + UUID.randomUUID().toString().replace("-", "")
        val task = PoWTask(target, difficulty, now, data)
        taskMap[taskId] = task
        return task
    }

    fun generateSwipeBox(): String {
        // generate a random list of 64 unique integers from 0 to 63
        val list = (0..63).toMutableList()
        list.shuffle()
        // convert to hex string
        var hexString = ""
        list.forEach {
            hexString += String.format("%02x", it)
        }
        return hexString
    }

    fun validateTask(taskId: String, target: String, result: String): Boolean {
        if (!taskMap.containsKey(taskId)) return false
        try {
            val task = taskMap[taskId]
            if (task == null || task.target != target) return false
            val sign = result.substring(0..63)
            val nonce = result.substring(64).toInt()
            return poWVerificationService.verify(task.data, sign, nonce, task.difficulty)
        } catch (_: Exception) {
            return false
        } finally {
            taskMap.remove(taskId)
        }
    }

    @Scheduled(fixedDelay = 60_000)
    fun cleanExpiredTasks() {
        val now = System.currentTimeMillis()
        val expiredTaskIds = taskMap.filter { now - it.value.timestamp > expiredTime(it.value.difficulty) }.keys
        expiredTaskIds.forEach { taskMap.remove(it) }
    }

    data class PoWTask(
        val target: String,
        val difficulty: Int,
        val timestamp: Long,
        val data: String
    )
}