/* Forum.kt */
// * 2651688427@qq.com
// 论坛的实现
/*
    * Forum是每个具体论坛的共同超类， 封装了所有论坛的共同属性和方法
* 每个论坛的差异部分在各个论坛派生类当中重写或者实现
* 目标是实现和平台无关， 在Windows或者Android上零修改移植
*/
package my.freeruok.simpleforums

import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.nio.charset.Charset

//* 所有论坛类的超类
abstract class Forum {
    abstract val name: String
    abstract val baseURL: String

    //* 从一段文本中生成一条消息， 参看Message数据类
    //* 这段文本也许是Jsoup元素或者JSONObject
    //* 功能是从JsoupElement或者JSONObject中返回Message对象
    //*第二个参数为true表明这是楼层， 如果为false表明 这是主题
    abstract val build: (Any, Boolean) -> Message

    open val charsetName: String = "UTF-8"

    //* 有些论坛强制登录
    open val isForce = false
    open var userName: String = ""
    open var userAuth: String = ""

    // 保存用户名和授权码
    fun saveUser(name: String, auth: String) {
        if (name.isNotEmpty() && auth.isNotEmpty()) {
            val (_, _, domain) = baseURL.split('/').map { it.replace(".", "_") }
            App.context.getSharedPreferences(USER_DATA, App.MOD_PRIVATE)
                .edit().putString("${domain}_name", name).putString("${domain}_auth", auth)
                .apply()
        }
    }

    // 恢复用户名和授权码
    fun loadUser() {
        val (_, _, domain) = baseURL.split('/').map { it.replace(".", "_") }
        val pref = App.context.getSharedPreferences(USER_DATA, App.MOD_PRIVATE)
        userName = pref.getString("${domain}_name", "") ?: ""
        userAuth = pref.getString("${domain}_auth", "") ?: ""
    }

    open var pageNumber: Int = 1

    //* 是否是移动设备
    open val isMoveable: Boolean = false
    open val cookie: MyCookie? = null

    //* 用于数据解析的时候提供值， 比如css类名， json字段名等等， 两队四个够用
    //*这里默认是爱盲论坛的状态, 其他论坛根据情况重写 即可
    open val threadQuery = "#threadlist" to "tbody"
    open val postQuery = "#postlist" to ".plhin"

    //用户是否在线
    open val isOnline: Boolean
        get() = userAuth.isNotEmpty()

    //*记录当前主题， 比如用户打开某个主题帖的时候获取它的详细楼层
    open var currentMessage = Message()

    //* 用户登录默认实现
    open fun login(name: String, password: String): Boolean {
        return false
    }

    //* 主题帖解析器默认实现， 这里是爱盲论坛
    //* 如果提供了子URL解析该url， 没有提供默认解析最新主题
    open fun parse(subURL: String = ""): List<Message> {
        // 生成url
        val url = if (subURL.isNotEmpty()) {
            subURL
        } else {
            "${baseURL}/forum.php?mod=guide&view=newthread&page=$pageNumber"
        }
        // 提交http请求
        val body = try {
            Util.fastHttp(url, isMoveable = isMoveable, cookie = cookie)
        } catch (e: Exception) {
            Log.d("exception", e.stackTraceToString())
            byteArrayOf()
        }
        // 开始解析html文档
        if (body.isNotEmpty()) {
            //选择解析参考值
            val (contentQuery, subQuery) = if (subURL.isNotEmpty()) {
                postQuery
            } else {
                threadQuery
            }
            // 解析第一层， 比如主题列表， 楼层列表等等
            val elements =
                Jsoup.parse(body.toString(Charset.forName(charsetName))).select(contentQuery)
            // 解析第二层， 具体每一个主题或楼层
            if (elements.isNotEmpty()) {
                return elements[0].select(subQuery).map {
                    // 具体生成Message对象的内容参看具体实现
                    build(it, subURL.isNotEmpty())
                    //过滤掉没有内容的Message对象 ， 比如主题列表的表头
                }.filter { it.content.isNotEmpty() }
            }
        }
        return listOf()
    }

    // 解析某一主题
    open fun parsePosts(pageNumber: Int): List<Message> {
        val url = "${baseURL}${currentMessage.url}&page=${pageNumber}"
        return parse(subURL = url)
    }

    // 回帖
    open fun post(content: String): Boolean {
        return false
    }

    // 获取论坛的所有板块
    open fun section(): Array<Section> {
        return arrayOf<Section>()
    }

    // 发布新主题, 成功返回主题ID， 失败返回0
    open fun thread(title: String, message: String, section: MutableSection): Int {
        return 0
    }
}

//* 爱盲论坛： www.aimang.net
class AMForum : Forum() {
    override val name: String = "爱盲论坛"
    override val baseURL: String = "https://www.aimang.net/"
    override val charsetName: String = "gbk"

    //* 生成Message对象， 在parse函数里调用， 这个函数可以说是整个parse函数的解析规则
    override val build: (Any, Boolean) -> Message = { dataObj, isSub ->
        if (dataObj is Element) {
            // 解析主题, 参看爱盲论坛的html源代码
            if (!isSub) {
                val e = dataObj.select(".xst")
                if (e.isNotEmpty()) {
                    val a = e[0]
                    val content = a.text()
                    val url = a.attr("href")
                    val (author, lastPost) = dataObj.select("cite").map { it.text() }
                    val (dateFmt, _, lastDateFmt) = dataObj.select("em").map { it.text() }
                    val (num) = dataObj.select(".num")
                    val (post) = num.select("a").map { it.text().toInt() + 1 }
                    val (view) = num.select("em").map { it.text().toInt() }
                    // 构造Message对象返回
                    Message(
                        content = content,
                        url = url,
                        author = author,
                        dateFmt = dateFmt,
                        lastPost = lastPost,
                        lastDateFmt = lastDateFmt,
                        post = post,
                        view = view
                    )
                } else {
                    Message()
                }
            } else {
                // 解析楼层
                val (content) = dataObj.select(".t_f").map { it.text() }
                val (author) = dataObj.select(".authi").map { it.text() }
                val (dateFmt) = dataObj.select("span[title]").map { it.text() }
                Message(content = content, author = author, dateFmt = dateFmt)
            }
        } else {
            Message()
        }
    }
}

//* 帮忙社区： bbs.abm365.cn
class BMForum : Forum() {
    override val name: String = "帮忙社区"
    override val baseURL: String = "http://bbs.abm365.cn/"
    override val threadQuery = ".content" to ".item"
    override val isMoveable: Boolean = true
    override val cookie: MyCookie? = MyCookie()

    // 帮忙社区用户登录
    override fun login(name: String, password: String): Boolean {
        val url = "${baseURL}api/user/login"
        // http请求报文， 除了用户名和密码其他直接硬编码
        val forms = mapOf(
            "mobile" to name,
            "password" to password,
            "renemberPassword" to "true",
            "randStr" to "",
            "ticket" to ""
        )
        val body =
            Util.fastHttp(
                url = url,
                querys = forms,
                isMoveable = true,
                contentType = CONTENT_TYPE_JSON,
                cookie = MyCookie()
            )
        // 错误检查
        if (body.isEmpty()) {
            return false
        }
        val resText = body.toString(Charset.forName(charsetName))
        val jsonObj = JSONObject(resText)
        if (jsonObj.getInt("code") == 0) {
            // 获取授权码
            val auth = jsonObj.getString("data")
            // 通过授权码获取用户名
            return setUser(auth)
        } else {
            return false
        }
    }

    // 通过授权码设置用户状态
    private fun setUser(auth: String): Boolean {
        // http请求头
        val headers = mapOf(
            "Authorization" to auth
        )
        val body = Util.fastHttp(
            url = "${baseURL}api/user",
            isMoveable = true,
            cookie = MyCookie(),
            headers = headers
        )
        // 错误检查
        if (body.isNotEmpty()) {
            val resText = body.toString(Charset.forName(charsetName))
            Log.d("帮忙社区", resText)
            val jsonObj = JSONObject(resText)
            if (jsonObj.getInt("code") == 0) {
                userAuth = auth
                userName = jsonObj.getJSONObject("data").getString("name")
                saveUser(userName, userAuth)
                return true
            }
        }
        return false
    }

    // 解析帮忙社区， 逻辑和爱盲一样只是目标从html换成了json
    override fun parse(subURL: String): List<Message> {
        val url = if (subURL.isNotEmpty()) {
            subURL
        } else {
            "${baseURL}api/post/new?pageNum=${pageNumber}&pageSize=30"
        }

        val body = Util.fastHttp(url = url, isMoveable = isMoveable, cookie = cookie)
        if (body.isNotEmpty()) {
            val jsonObj = JSONObject(body.toString(Charset.forName(charsetName)))
            if (jsonObj.getInt("code") == 0) {
                val messages = mutableListOf<Message>()
                // 帮忙社区的迷惑行为， 楼主楼层和其他楼层分别处理， 所以这里需要特殊处理， 解析json看蜻蜓社区的实现
                try {
                    val list = jsonObj.getJSONObject("data").getJSONArray("list")
                    for (i in 0 until list.length()) {
                        messages.add(build(list.getJSONObject(i), subURL.isNotEmpty()))
                    }
                } catch (e: JSONException) {
                    val jsonFloor = jsonObj.getJSONObject("data").put("floor", 0)
                    messages.add(build(jsonFloor, subURL.isNotEmpty()))
                }
                return messages
            }
        }
        return listOf()
    }

    // 解析楼层， 这里还是处理帮忙社区的特殊情况
    override fun parsePosts(pageNumber: Int): List<Message> {
        val piffle = if (pageNumber == 1) {
            parse(subURL = "${baseURL}api/post/${currentMessage.id}")
        } else {
            listOf()
        }
        val url =
            "${baseURL}api/reply?postId=${currentMessage.id}&pageNum=${pageNumber}&pageSize=20&authorOnly=false"
        return piffle + parse(subURL = url)
    }

    // 构造Message对象
    override val build: (Any, Boolean) -> Message = { dataObj, isSub ->
        if (dataObj is JSONObject) {
            if (!isSub) {
                // 参看帮忙社区返回的json数据
                Message(
                    id = dataObj.getLong("id"),
                    content = dataObj.getString("title"),
                    author = dataObj.getString("userName"),
                    dateFmt = dataObj.getString("createTimeCn"),
                    post = dataObj.getInt("replyCount") + 1,
                    view = dataObj.getInt("viewCount")
                )
            } else {
                Message(
                    content = dataObj.getString("body"),
                    author = dataObj.getString("userName"),
                    dateFmt = dataObj.getString("createTime"),
                    floor = dataObj.getInt("floor") + 1
                )
            }
        } else {
            Message()
        }
    }

    // 回帖
    override fun post(content: String): Boolean {
        val url = "${baseURL}api/reply"
        val forms = mapOf<String, Any>(
            "body" to content,
            "postId" to currentMessage.id,
            "top" to false
        )
        val body = Util.fastHttp(
            url = url,
            querys = forms,
            contentType = CONTENT_TYPE_JSON,
            headers = mapOf("Authorization" to userAuth)
        )
        if (body.isNotEmpty()) {
            val jsonObj = JSONObject(body.toString(Charset.forName(charsetName)))
            return jsonObj.getInt("code") == 0
        } else {
            return false
        }
    }

    // 获取论坛所有板块
    override fun section(): Array<Section> {
        val body = Util.fastHttp(url = "${baseURL}/api/section")
        if (body.isNotEmpty()) {
            val resText = body.toString(Charset.forName(charsetName))
            val jsonObj = JSONObject(resText)
            if (jsonObj.getInt("code") == 0) {
                val jsonAry = jsonObj.getJSONArray("data")
                val sections = mutableListOf<Section>()
                for (i in 0 until jsonAry.length()) {
                    val jsonItem = jsonAry.getJSONObject(i)
                    sections.add(
                        Section(
                            name = jsonItem.getString("name"),
                            id = jsonItem.getInt("id")
                        )
                    )
                }
                return sections.toTypedArray()
            }
        }
        return arrayOf<Section>()
    }

    // 发布新主题
    override fun thread(title: String, message: String, section: MutableSection): Int {
        // 除了标题和内容其他的直接硬编码
        val forms = mapOf(
            "title" to title,
            "sectionId" to section.first.id,
            "body" to message,
            "notify" to false,
            "rewardPoint" to 0,
            "viewPrice" to 0,
            "goodsPrice" to 0,
            "albumId" to 0,
            "linkList" to JSONArray("[]"),
            "imageList" to JSONArray("[]"),
            "audioList" to JSONArray("[]"),
            "videoList" to JSONArray("[]"),
            "fileList" to JSONArray("[]"),
            "richBody" to false,
            "replyForbidden" to false,
            "mediaDownload" to false,
            "needReply" to false,
            "secret" to false,
            "useCoupon" to false,
            "delTime" to "null",
            "postPrice" to 0,
            "mediaPrice" to -1,
            "attachmentPrice" to -1
        )
        val url = "${baseURL}post"
        val body = Util.fastHttp(
            url = url,
            querys = forms,
            headers = mapOf("Authorization" to userAuth),
            contentType = CONTENT_TYPE_JSON
        )
        if (body.isNotEmpty()) {
            val resText = body.toString(Charset.forName(charsetName))
            val jsonObj = JSONObject(resText)
            if (jsonObj.getInt("code") == 0) {
                return jsonObj.getInt("data")
            }
        }
        return 0
    }
}

//* 蜻蜓社区： www.qt.hk
open class QTForum : Forum() {
    override val name: String = "蜻蜓社区"
    override val baseURL: String = "http://www.qt.hk/"
    // 强制登录
    override val isForce: Boolean = true

    //* 网站要求所有请求必须携带这些字段, 详情： http://www.qt.hk/open.htm
    // 这里大家可以用我的appkey和seckey
    open val httpForms: Map<String, String> = mapOf(
        "appkey" to "ba4255f81de52b475ef4efe0f95cd49c",
        "format" to "json",
    )
    open val secKey = "seckey" to "7056ea52c2ed298d1c0e26bd0dedf023c18322ef"
    // 蜻蜓社区要求密码的md5值， 而争渡网不需要
    open val isMD5Password: Boolean = true

    // 蜻蜓社区和争渡网用户登录
    override fun login(name: String, password: String): Boolean {
        val forms = mapOf(
            "email" to name,
            "password" to if (isMD5Password) {
                password.md5()
            } else {
                password
            },
            secKey
        )
        val body = Util.fastHttp(url = baseURL + "user-login.htm", querys = httpForms + forms)
        return if (body.size != 0) {
            //* 解析json数据， 参看蜻蜓社区返回的相关json数据
            val jsonObj = JSONObject(body.toString(Charset.forName(charsetName)))
            if (jsonObj.getInt("status") == 1) {
                val userObj = jsonObj.getJSONObject("message").getJSONObject("user")
                userName = userObj.getString("username")
                userAuth = userObj.getString("auth")
                saveUser(userName, userAuth)
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    override val threadQuery = "postlist" to "threadlist"

    // 解析蜻蜓社区和争渡网
    override fun parse(subURL: String): List<Message> {
        val forms: Map<String, String> =
            mapOf("auth" to userAuth, "page" to pageNumber.toString()) + httpForms
        val url = if (subURL.isNotEmpty()) {
            subURL
        } else {
            "${baseURL}index-index.htm"
        }
        val body = Util.fastHttp(url = url, querys = forms)
        if (body.isNotEmpty()) {
            val jsonObj = JSONObject(body.toString(Charset.forName(charsetName)))
            if (jsonObj.getInt("status") == 1) {
                val messages: MutableList<Message> = mutableListOf()
                val itemName = if (subURL.isNotEmpty()) {
                    threadQuery.first
                } else {
                    threadQuery.second
                }
                val jsonAry = jsonObj.getJSONObject("message").getJSONArray(itemName)
                for (i in 0 until jsonAry.length()) {
                    val jsonItem = jsonAry.getJSONObject(i)
                    messages.add(build(jsonItem, subURL.isNotEmpty()))
                }
                return messages
            }
        }
        return listOf()
    }

    // 解析蜻蜓社区和争渡网楼层
    override fun parsePosts(pageNumber: Int): List<Message> {
        val url = "${baseURL}thread-${currentMessage.id}-page-${pageNumber}.htm"
        Log.d("url", url)
        return parse(subURL = url)
    }

    // 构造Message对象
    override val build: (Any, Boolean) -> Message = { dataObj, isSub ->
        if (dataObj is JSONObject) {
            if (!isSub) {
                Message(
                    id = dataObj.getLong("tid"),
                    content = dataObj.getString("subject"),
                    author = dataObj.getString("username"),
                    dateLine = dataObj.getLong("dateline"),
                    dateFmt = dataObj.getString("dateline_fmt"),
                    post = dataObj.getInt("posts"),
                    view = dataObj.getInt("views"),
                    lastDate = dataObj.getLong("lastpost"),
                    lastDateFmt = dataObj.getString("lastpost_fmt"),
                    lastPost = dataObj.getString("lastusername")
                )
            } else {
                Message(
                    id = dataObj.getLong("tid"),
                    content = dataObj.getString("message"),
                    author = dataObj.getString("username"),
                    floor = dataObj.getInt("floor"),
                    dateLine = dataObj.getLong("dateline"),
                    dateFmt = dataObj.getString("dateline_fmt")
                )
            }
        } else {
            Message()
        }
    }

    // 蜻蜓社区和争渡网回帖
    override fun post(content: String): Boolean {
        val url = "${baseURL}post-post.htm"
        val forms = mapOf<String, Any>(
            "tid" to MainActivity.forum.currentMessage.id,
            "message" to content,
            "auth" to userAuth,
            secKey
        )
        val body = Util.fastHttp(url = url, querys = httpForms + forms)
        if (body.isNotEmpty()) {
            val jsonObj = JSONObject(body.toString(Charset.forName(charsetName)))
            return jsonObj.getInt("status") == 1
        } else {
            return false
        }
    }

    // 获取蜻蜓社区和争渡网论坛所有板块
    //* 注意争渡网还需要获取二级板块， 三四级板块可以忽略
    // 详细参看相关网站返回的json数据
    override fun section(): Array<Section> {
        val forms = mapOf("auth" to userAuth)
        val body = Util.fastHttp(url = "${baseURL}index-forumlist.htm", querys = httpForms + forms)
        if (body.isNotEmpty()) {
            val resText = body.toString(Charset.forName(charsetName))
            val jsonObj = JSONObject(resText)
            if (jsonObj.getInt("status") == 1) {
                val jsonAry = jsonObj.getJSONArray("message")
                val firstSections = mutableListOf<Section>()
                for (i in 0 until jsonAry.length()) {
                    val jsonItem = jsonAry.getJSONObject(i)
                    val section = Section(
                        name = jsonItem.getString("name"),
                        id = jsonItem.getInt("fid")
                    )
                    val jsonSecond = jsonItem.getJSONObject("types").getJSONArray("typeid1")
                    val secondSections = mutableListOf<Section>()
                    for (j in 1 until jsonSecond.length()) {
                        val secondObj = jsonSecond.getJSONObject(j)
                        secondSections.add(
                            Section(
                                secondObj.getString("name"),
                                secondObj.getInt("id")
                            )
                        )
                    }
                    if (secondSections.isNotEmpty()) {
                        section.subSections = secondSections.toTypedArray()
                    }
                    firstSections.add(section)
                }
                return firstSections.toTypedArray()
            }
        }
        return arrayOf<Section>()
    }

    // 蜻蜓社区和争渡网发布新主题
    override fun thread(title: String, message: String, section: MutableSection): Int {
        val forms = mapOf(
            "title" to title,
            "message" to message,
            "fid" to section.first.id,
            "typeid1" to section.second.id,
            "auth" to userAuth,
            secKey
        )
        val url = "${baseURL}post-thread.htm"
        val body = Util.fastHttp(url = url, querys = httpForms + forms)
        if (body.isNotEmpty()) {
            val resText = body.toString(Charset.forName(charsetName))
            val jsonObj = JSONObject(resText)
            if (jsonObj.getInt("status") == 1) {
                return jsonObj.getJSONObject("message").getInt("tid")
            }
        }
        return 0
    }
}

//* 争渡网： www.zd.hk 直接从蜻蜓社区继承即可
class ZDForum : QTForum() {
    override val name: String = "争渡网"
    override val baseURL: String = "http://www.zd.hk/"
    override val httpForms = mapOf(
        "appkey" to "488f3f5f9d",
        "format" to "json"
    )
    override val secKey = "seckey" to "57dfd28547"
    override val isMD5Password: Boolean = false
}
