/* MyCookie.kt */
// * 2651688427@qq.com
// 实现网站cookie
package my.freeruok.simpleforums

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class MyCookie : CookieJar {
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookieValue =
            "eyJhbGciOiJIUzUxMiJ9.eyJjcmVhdGVkIjoxNjc0OTI0NzQxOTkzLCJleHAiOjE2NzU1Mjk1NDEsInVzZXJJZCI6MTc1ODF9._VVMTM6TUzzNulxWBtN7if86mQfjFod0WGAu522IAXSg38c9KQ1EWsA6ulU1kM7oCW6w5viqkpNTogvBLuZkzA"
        return listOf(
            Cookie.Builder().domain("bbs.abm365.cn").name("JD-BBS-UID")
                .value(cookieValue).build()
        )
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {

    }
}
