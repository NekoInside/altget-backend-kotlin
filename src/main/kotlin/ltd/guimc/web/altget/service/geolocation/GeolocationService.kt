package ltd.guimc.web.altget.service.geolocation

import cn.hutool.http.HttpRequest
import cn.hutool.json.JSONObject
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.*

@Service
class GeolocationService {
    private val cache = ConcurrentHashMap<String, GeolocationInfo>()
    private val executorService: ExecutorService = Executors.newFixedThreadPool(4)
    fun getGeolocation(ip: String): GeolocationInfo {
        // Check cache first
        val cached = cache[ip]
        if (cached != null && !cached.isExpired) {
            return cached
        }

        // Fetch from API with timeout
        try {
            val future = executorService.submit(Callable { fetchGeolocation(ip) })
            val info = future.get(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
            cache[ip] = info
            return info
        } catch (_: TimeoutException) {
            return GeolocationInfo("", "", "未知地区")
        } catch (_: Exception) {
            return GeolocationInfo("", "", "未知地区")
        }
    }

    private fun fetchGeolocation(ip: String): GeolocationInfo {
        try {
            val apiUrl = "https://get.geojs.io/v1/ip/geo/" + URLEncoder.encode(ip, StandardCharsets.UTF_8) + ".json"
            val response = HttpRequest.get(apiUrl)
                .timeout(TIMEOUT_SECONDS * 1000)
                .execute()

            if (response.isOk()) {
                val dataJson = JSONObject(response.body())
                val city = dataJson.getStr("city", "")
                val region = dataJson.getStr("region", "")
                val countryName = dataJson.getStr("country", "")
                return GeolocationInfo(city, region, countryName)
            } else {
                return GeolocationInfo("", "", "未知地区")
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to fetch geolocation data for IP: $ip", e)
        }
    }

    fun formatLocationMessage(info: GeolocationInfo): String {
        return if (info.region.isNullOrBlank()) {
            info.countryName
        } else if (info.city.isBlank()) {
            "${info.region}, ${info.countryName}"
        } else {
            "${info.city}, ${info.region}, ${info.countryName}"
        }
    }

    class GeolocationInfo(val city: String, val region: String?, val countryName: String) {
        private val timestamp: Long = System.currentTimeMillis()

        val isExpired: Boolean
            get() = System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MINUTES * 60 * 1000L
    }

    companion object {
        private const val TIMEOUT_SECONDS = 3
        private const val CACHE_EXPIRY_MINUTES = 60
    }
}
