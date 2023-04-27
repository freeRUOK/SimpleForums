/* CookieStorage.kt */
// * 2651688427@qq.com
// 实现网站cookie
package my.freeruok.simpleforums

import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class CookiesStorage : CookieJar {
    private val cookieStorage =
        PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(App.context))

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieStorage.loadForRequest(url)
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookieStorage.saveFromResponse(url, cookies)
    }
}
