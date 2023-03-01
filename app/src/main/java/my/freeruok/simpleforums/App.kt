/* App.kt */
// * 2651688427@qq.com
// 整个应用程序的全局上下文
package my.freeruok.simpleforums

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

/// 全局上下文
/// 方便在应用程序的任何部分获取上下文对象
class App : Application() {
    companion object {
        // 不要在activity； service上用此技巧， 有内存泄露的风险
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
        var MOD_PRIVATE: Int = 0

        @Volatile
        var isLoading: Boolean = false
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        MOD_PRIVATE = MODE_PRIVATE
    }
}
