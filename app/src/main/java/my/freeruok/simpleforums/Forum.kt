/* Forum.kt */
// * 2651688427@qq.com
// 论坛的实现
/*
    * Forum是每个具体论坛的共同超类， 封装了所有论坛的共同属性和方法
* 每个论坛的差异部分在各个论坛派生类当中重写或者实现
* 目标是实现和平台无关， 在Windows或者Android上零修改移植
*/
package my.freeruok.simpleforums

import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URLEncoder
import java.nio.charset.Charset
import java.time.Instant

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
        val (_, _, domain) = baseURL.split('/').map { it.replace(".", "_") }
        App.context.getSharedPreferences(USER_DATA, App.MOD_PRIVATE)
            .edit().putString("${domain}_name", name).putString("${domain}_auth", auth)
            .apply()
    }

    // 恢复用户名和授权码
    fun loadUser() {
        val (_, _, domain) = baseURL.split('/').map { it.replace(".", "_") }
        val pref = App.context.getSharedPreferences(USER_DATA, App.MOD_PRIVATE)
        if (baseURL == "https://www.aimang.net/") {
            val authCookie = CookiesStorage().loadForRequest(baseURL.toHttpUrl())
                .find { it.name.contains("_auth") }
            val curTime = Instant.now().epochSecond
            if (authCookie != null && authCookie.expiresAt <= curTime) {
                return
            }
        }
        userName = pref.getString("${domain}_name", "") ?: ""
        userAuth = pref.getString("${domain}_auth", "") ?: ""
    }

    open var pageNumber: Int = 1

    //* 是否是移动设备
    open val isMoveable: Boolean = false
    open val cookie: CookiesStorage = CookiesStorage()

    //* 用于数据解析的时候提供值， 比如css类名， json字段名等等， 两队四个够用
//*这里默认是爱盲论坛的状态, 其他论坛根据情况重写 即可
    open val threadQuery = "#threadlist" to "tbody"
    open val postQuery = "#postlist" to ".plhin"

    //用户是否在线
    open val isOnline: Boolean
        get() = userAuth.isNotEmpty()

    //*记录当前用户浏览的主题， 比如用户打开某个主题帖的时候获取它的详细楼层
    open var currentMessage = Message()

    //* 用户登录默认实现
    open fun login(name: String, password: String): Boolean {
        val url =
            "${baseURL}member.php?mod=logging&action=login&loginsubmit=yes&handlekey=login&loginhash=LeDDv&inajax=1"
        val forms = mapOf<String, Any>(
            "username" to URLEncoder.encode(name, charsetName),
            "password" to password,
            "cookietime" to "2592000"
        )
        val response = Util.fastResponse(url, querys = forms, cookie = CookiesStorage())
        if (response.isSuccessful) {
            val content = response.body?.bytes() ?: byteArrayOf()
            val resText = String(content, Charset.forName(charsetName))
            val result = Regex("username\\'\\:\\'(\\S+?)\\'").find(resText)
            if (result != null) {
                userName = result.groupValues[1]
                userAuth = "aimang_net_login" // 此值无意义， 随便填充完事
                saveUser(userName, userAuth)
                return true
            }
        }
        return false
    }

    //* 主题帖解析器默认实现， 这里是爱盲论坛
//* 默认解析最新主题
    open fun parse(
        subURL: String = "",
        isSub: Boolean = false,
        vararg args: Pair<String, Any>
    ): List<Message> {
        // 生成url
        val url = if (subURL.isNotEmpty()) {
            subURL
        } else {
            val orderMode = orderModes.getOrDefault(currentOrder, "newthread")
            "${baseURL}/forum.php?mod=guide&view=$orderMode&page=$pageNumber"
        }
        // 提交http请求
        val body = try {
            Util.fastBinaryContent(url, isMoveable = isMoveable, cookie = cookie)
        } catch (e: Exception) {
            byteArrayOf()
        }
        // 开始解析html文档
        if (body.isNotEmpty()) {
            //选择解析参考值
            val (contentQuery, subQuery) = if (isSub) {
                postQuery
            } else if (keyword.isNotEmpty()) {
                "#threadlist" to ".pbw"
            } else {
                threadQuery
            }
            // 解析第一层， 比如主题列表， 楼层列表等等
            val htmlDoc = Jsoup.parse(body.toString(Charset.forName(charsetName)))
            val elements = htmlDoc.select(contentQuery)
            if (isSub) {
                val (form) = htmlDoc.select("#fastpostform").map { it }
                postForm = form
            }
            // 解析第二层， 具体每一个主题或楼层
            if (elements.isNotEmpty()) {
                return elements[0].select(subQuery).map {
                    // 具体生成Message对象的内容参看具体实现
                    build(it, isSub)
                    //过滤掉没有内容的Message对象 ， 比如主题列表的表头
                }.filter { it.content.isNotEmpty() }
            }
        }
        return listOf()
    }

    // 解析某一主题
    open fun parsePosts(pageNumber: Int): List<Message> {
        val url =
            "${baseURL}forum.php?mod=viewthread&tid=${currentMessage.tid}&extra=&page=${pageNumber}"
        return parse(subURL = url, isSub = true)
    }

    protected var keyword: String = ""
    abstract val searchURL: String
    open fun parseSearch(kw: String): List<Message> {
        val urlStr = URLEncoder.encode(kw, charsetName)
        if (keyword != urlStr) {
            pageNumber = 1
            keyword = urlStr
        }
        return parse(subURL = searchURL, isSub = false, "keyword" to keyword)
    }

    // 保存当前主题的回帖表单备用
    private var postForm = Jsoup.parse("<form action=\"\"></form>").root()

    // 回帖， 爱盲论坛
    open fun post(content: String): Message {
        val url = "${baseURL}${postForm.attr("action")}"
        if (url == baseURL) {
            return Message()
        }
        val (formHash) = postForm.select("input[name=formhash]").map { it.attr("value") }
        val forms = mapOf<String, Any>(
            "message" to URLEncoder.encode(content, charsetName),
            "posttime" to Instant.now().epochSecond.toString(),
            "formhash" to formHash,
            "usesig" to 1,
            "subject" to ""
        )
        val response =
            Util.fastResponse(url = url, querys = forms, cookie = cookie, isRedirect = false)
        if (response.code == 301) {
            return Message(
                content = content,
                author = userName,
                floor = currentMessage.postCount,
                dateFmt = "刚刚"
            )
        }
        return Message()
    }

    // 获取论坛的所有板块， 爱盲论坛
    open fun section(): Array<Section> {
        val buf = Util.fastBinaryContent(url = "${baseURL}forum.php", cookie = cookie)
        if (buf.isNotEmpty()) {
            val resText = String(buf, Charset.forName(charsetName))
            return mutableListOf<Section>().apply {
                Jsoup.parse(resText).select(".fl_tb").forEach { ele ->
                    val sl = ele.select("h2").map {
                        val (a) = it.select("a")
                        val result = Regex("fid=(\\d+)").find(a.attr("href"))?.groupValues
                        if (result != null) {
                            val id = result[1].toInt()
                            Section(name = it.text(), id = id)
                        } else {
                            Section(name = it.text(), id = 0)
                        }
                    }
                    addAll(sl)
                }
            }.toTypedArray()
        }
        return arrayOf()
    }

    // 发布新主题, 成功返回主题ID， 失败返回0， 爱盲论坛
    open fun thread(title: String, message: String, section: MutableSection): Int {
        var url = "${baseURL}forum.php?mod=forumdisplay&fid=${section.first.id}"
        val buf = Util.fastBinaryContent(url = url, cookie = cookie)
        if (buf.isNotEmpty()) {
            val resText = String(buf, Charset.forName(charsetName))
            val (formElement) = Jsoup.parse(resText).select("#fastpostform")
            url = "${baseURL}${formElement.attr("action")}"
            val (formHash) = formElement.select("input[name=formhash]").map { it.attr("value") }
            val forms = mapOf<String, Any>(
                "subject" to URLEncoder.encode(title, charsetName),
                "message" to URLEncoder.encode(message, charsetName),
                "formhash" to formHash,
                "posttime" to Instant.now().epochSecond,
                "usesig" to 1,
            )
            val response =
                Util.fastResponse(url = url, querys = forms, cookie = cookie, isRedirect = false)
            if (response.code == 301) {
                val result =
                    Regex("tid=(\\d+)").find(response.headers["location"].toString())?.groupValues
                if (result != null) {
                    return result[1].toInt()
                }
            }
        }
        return 0
    }

    // 生成媒体项目
    open fun buildResources(mediaData: Any): MutableMap<String, TextProcesser.TextType> {
        val iText = mediaData.toString()
        val results = mutableMapOf<String, TextProcesser.TextType>().apply {
            this.putAll(TextProcesser.parseText(iText).associate { it.second to it.first })
            if (mediaData is Element) {
                mediaData.select("audio,video").forEach {
                    val url = it.attr("src")
                    if (url.isNotEmpty()) {
                        this[url] = TextProcesser.TextType.MEDIA_URL
                    } else {
                        val source = it.select("source")
                        if (source.isNotEmpty()) {
                            this[source[0].attr("src")] = TextProcesser.TextType.MEDIA_URL
                        }
                    }
                }
            }
        }
        return results
    }

    // 处理排序加载
    abstract val orderModes: Map<String, String>
    var currentOrder: String = loadOrderMode()
        set(value) {
            if (field != value) {
                field = value
                saveOrderMode()
            }
        }

    fun loadOrderMode(): String {
        /*
        return App.context.getSharedPreferences(USER_DATA, App.MOD_PRIVATE)
            .getString("$ORDER_MODE_KEY-$name", ORDER_MODE_NEW_THREAD) ?: ""
            */
        return ""

    }

    fun saveOrderMode() {
        App.context.getSharedPreferences(USER_DATA, App.MOD_PRIVATE).edit()
            .putString("$ORDER_MODE_KEY-$name", currentOrder).apply()
    }
}

//* 爱盲论坛： www.aimang.net
class AMForum : Forum() {
    override val name: String = "爱盲论坛"
    override val baseURL: String = "https://www.aimang.net/"
    override val searchURL: String
        get() = "${baseURL}search.php?mod=forum&searchid=${searchId}&orderby=lastpost&ascdesc=desc&searchsubmit=yes&kw=${keyword}&page=${pageNumber}"
    private var searchId: String = ""

    override val charsetName: String = "gbk"

    override val orderModes: Map<String, String> =
        mapOf(ORDER_MODE_NEW_THREAD to "newthread", ORDER_MODE_LAST_THREAD to "hotthread")

    //* 生成Message对象， 在parse函数里调用， 这个函数可以说是整个parse函数的解析规则， 爱盲论坛
    override val build: (Any, Boolean) -> Message = { dataObj, isSub ->
        if (dataObj is Element) {
            // 解析主题, 参看爱盲论坛的html源代码
            if (!isSub) {
                val e = dataObj.select(".xst")
                if (e.isNotEmpty()) {
                    val a = e[0]
                    val content = a.text()
                    val url = a.attr("href")
                    val tid: Long = parseTid(url)
                    val (author, lastPost) = dataObj.select("cite").map { it.text() }
                    val (dateFmt, _, lastDateFmt) = dataObj.select("em").map { it.text() }
                    val (num) = dataObj.select(".num")
                    val (post) = num.select("a").map { it.text().toInt() + 1 }
                    val (view) = num.select("em").map { it.text().toInt() }
                    // 构造Message对象返回
                    Message(
                        tid = tid,
                        content = content,
                        url = url,
                        author = author,
                        dateFmt = dateFmt,
                        lastPost = lastPost,
                        lastDateFmt = lastDateFmt,
                        postCount = post,
                        viewCount = view
                    )
                } else {
                    buildSearch(dataObj)
                }
            } else {
                // 解析楼层
                val mainBody = dataObj.select(".t_f")
                val contentSrc = mainBody[0].text()
                val (floor, content) = parseFloorAndContent(contentSrc)
                val resources = buildResources(mainBody[0])
                val (author) = dataObj.select(".authi").map { it.text() }
                val (dateFmt) = dataObj.select("span[title]").map { it.text() }
                Message(
                    tid = currentMessage.tid,
                    content = content,
                    floor = floor,
                    author = author,
                    dateFmt = dateFmt,
                    resources = resources
                )
            }
        } else {
            Message()
        }
    }

    // 解析爱盲论坛搜索结果， 用正则表达式简化了
    private fun buildSearch(ele: Element): Message {
        if (keyword.isEmpty()) {
            return Message()
        }
        return try {
            val (h3) = ele.select(".xs3")
            val (a) = h3.select("a[title]")
            val url = a.attr("href")
            val tid = parseTid(url)
            val title = a.attr("title")
            val result =
                Regex("(.*)\\s作者：(\\S+)\\s发表时间\\:(.*)\\s浏览：(\\d+)次\\s回复：(\\d+)\\s最后回复：(\\S+)\\s最后回复时间\\:(.*)").find(
                    title
                )
            val values = if (result != null) {
                result.groupValues.subList(0, 4) to result.groupValues.subList(4, 8)
            } else {
                listOf<String>() to listOf<String>()
            }
            val (_, content, author, dateLine_fmt) = values.first
            val (viewCount, postCount, lastPost, lastDateLine_fmt) = values.second

            Message(
                tid = tid,
                url = url,
                content = content,
                author = author,
                dateFmt = dateLine_fmt,
                lastPost = lastPost,
                lastDateFmt = lastDateLine_fmt,
                viewCount = viewCount.toInt(),
                postCount = postCount.toInt()
            )
        } catch (e: Exception) {
            Message()
        }
    }

    // 解析帖子tid
    private fun parseTid(url: String): Long {
        val tid: Long = try {
            (Regex("tid=(\\d+)").find(url)?.groupValues ?: listOf(
                "0",
                "0"
            ))[1].toLong()
        } catch (e: Exception) {
            0L
        }
        return tid
    }

    private val floors = mapOf("楼主" to 1, "沙发" to 2, "板凳" to 3, "地板" to 4)

    // 爱盲论坛特殊处理， 返回楼层号和内容
    private fun parseFloorAndContent(src: String): Pair<Int, String> {
        val floor = floors.getOrDefault(
            src.substring(0, 2),
            Regex("\\d+").matchAt(src, 0)?.value?.toInt() ?: 0
        )

        val t = src.indexOf("说：")
        val content = src.substring(t + 2)
        return floor to content
    }

    override fun parseSearch(kw: String): List<Message> {
        val urlStr = URLEncoder.encode(kw, charsetName)
        if (keyword != urlStr) {
            pageNumber = 1
            keyword = urlStr
            val url = "${baseURL}search.php?searchsubmit=yes"
            val forms = mapOf<String, Any>(
                "mod" to "forum",
                "srchtype" to "title",
                "srchtxt" to keyword,
                "srhfid" to ""
            )
            val response = Util.fastResponse(
                url,
                querys = forms,
                isRedirect = false,
                cookie = cookie
            )
            if (response.code == 302) {
                searchId = parseSearchId(response.headers)
            }
        }
        return parse(subURL = searchURL, isSub = false)
    }

    // 从http头里解析searchid值
    private fun parseSearchId(headers: Headers): String {
        val url = headers["location"]
        if (url != null) {
            val result = Regex("searchid=(\\S+)&").find(url)
            if (result != null) {
                return result.groupValues[1]
            }
        }
        return ""
    }
}

//* 帮忙社区： bbs.abm365.cn
class BMForum : Forum() {
    override val name: String = "帮忙社区"
    override val baseURL: String = "http://bbs.abm365.cn/"
    override val searchURL: String
        get() = "${baseURL}api/post/search?keyword=${keyword}&pageSize=20&pageNum=${pageNumber}"

    override val orderModes: Map<String, String> =
        mapOf(
            ORDER_MODE_NEW_THREAD to "new",
            ORDER_MODE_LAST_THREAD to "new-reply",
            ORDER_MODE_ESSENCE_THREAD to "essence"
        )

    override val threadQuery = ".content" to ".item"
    override val isMoveable: Boolean = true

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
            Util.fastBinaryContent(
                url = url,
                querys = forms,
                isMoveable = true,
                contentType = CONTENT_TYPE_JSON,
                cookie = cookie
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
        val body = Util.fastBinaryContent(
            url = "${baseURL}api/user",
            isMoveable = true,
            cookie = CookiesStorage(),
            headers = headers
        )
        // 错误检查
        if (body.isNotEmpty()) {
            val resText = body.toString(Charset.forName(charsetName))

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
    override fun parse(
        subURL: String,
        isSub: Boolean,
        vararg args: Pair<String, Any>
    ): List<Message> {
        val url = if (subURL.isNotEmpty()) {
            subURL
        } else {
            val orderMode = orderModes.getOrDefault(currentOrder, "new")
            "${baseURL}api/post/$orderMode?pageNum=${pageNumber}&pageSize=30"
        }

        val body = Util.fastBinaryContent(url = url, isMoveable = isMoveable, cookie = cookie)
        if (body.isNotEmpty()) {
            val jsonObj = JSONObject(body.toString(Charset.forName(charsetName)))
            if (jsonObj.getInt("code") == 0) {
                val messages = mutableListOf<Message>()
                // 帮忙社区的迷惑行为， 楼主楼层和其他楼层分别处理， 所以这里需要特殊处理， 解析json看蜻蜓社区的实现
                try {
                    val list = jsonObj.getJSONObject("data").getJSONArray("list")
                    for (i in 0 until list.length()) {
                        messages.add(build(list.getJSONObject(i), isSub))
                    }
                } catch (e: JSONException) {
                    val jsonFloor = jsonObj.getJSONObject("data").put("floor", 0)
                    messages.add(build(jsonFloor, isSub))
                }
                return messages
            }
        }
        return listOf()
    }

    // 解析楼层， 这里还是处理帮忙社区的特殊情况
    override fun parsePosts(pageNumber: Int): List<Message> {
        val piffle = if (pageNumber == 1) {
            parse(subURL = "${baseURL}api/post/${currentMessage.tid}", isSub = true)
        } else {
            listOf()
        }
        val url =
            "${baseURL}api/reply?postId=${currentMessage.tid}&pageNum=${pageNumber}&pageSize=20&authorOnly=false"
        return piffle + parse(subURL = url, isSub = true)
    }

    // 构造Message对象
    override val build: (Any, Boolean) -> Message = { dataObj, isSub ->
        if (dataObj is JSONObject) {
            if (!isSub) {
                // 参看帮忙社区返回的json数据
                Message(
                    tid = dataObj.getLong("id"),
                    content = dataObj.getString("title"),
                    author = dataObj.getString("userName"),
                    dateFmt = dataObj.getString("createTimeCn"),
                    postCount = dataObj.getInt("replyCount") + 1,
                    viewCount = dataObj.getInt("viewCount")
                )
            } else {
                Message(
                    tid = currentMessage.tid,
                    content = dataObj.getString("body"),
                    author = dataObj.getString("userName"),
                    dateFmt = dataObj.getString("createTime"),
                    floor = dataObj.getInt("floor") + 1,
                    resources = buildResources(dataObj)
                )
            }
        } else {
            Message()
        }
    }

    // 回帖
    override fun post(content: String): Message {
        val url = "${baseURL}api/reply"
        val forms = mapOf<String, Any>(
            "body" to content,
            "postId" to currentMessage.tid,
            "top" to false
        )
        val body = Util.fastBinaryContent(
            url = url,
            querys = forms,
            contentType = CONTENT_TYPE_JSON,
            headers = mapOf("Authorization" to userAuth)
        )
        if (body.isNotEmpty()) {
            val jsonObj = JSONObject(body.toString(Charset.forName(charsetName)))
            if (jsonObj.getInt("code") == 0) {
                return Message(
                    content = content,
                    author = userName,
                    dateFmt = "刚刚",
                    floor = currentMessage.postCount + 1
                )
            }
        }
        return Message()
    }

    // 获取论坛所有板块
    override fun section(): Array<Section> {
        val body = Util.fastBinaryContent(url = "${baseURL}/api/section")
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
        return arrayOf()
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
        val url = "${baseURL}api/post"
        val body = Util.fastBinaryContent(
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

    override fun buildResources(mediaData: Any): MutableMap<String, TextProcesser.TextType> {
        if (mediaData is JSONObject) {
            val resources = mutableMapOf<String, TextProcesser.TextType>()
            val ele = Jsoup.parse(mediaData.getString("body")).root()
            resources += super.buildResources(ele)
            arrayOf("audioList", "videoList").forEach {
                if (mediaData.has(it)) {
                    resources += parseMediaData(mediaData.getJSONArray(it), "fileName", "desc")
                }
            }
            return resources
        }
        return mutableMapOf()
    }

    private fun parseMediaData(
        mediaDate: JSONArray,
        vararg keywords: String
    ): MutableMap<String, TextProcesser.TextType> {
        val results = mutableMapOf<String, TextProcesser.TextType>()
        for (i in 0 until mediaDate.length()) {
            val dat = mediaDate.getJSONObject(i)
            if (!dat.getBoolean("isPaid")) {
                for (kw in keywords) {
                    try {
                        val url = dat.getString("url")
                        results.put(url, TextProcesser.TextType.MEDIA_URL)
                        break
                    } catch (e: JSONException) {
                        continue
                    }
                }
            }
        }
        return results
    }
}

//* 蜻蜓社区： www.qt.hk
open class QTForum : Forum() {
    override val name: String = "蜻蜓社区"
    override val baseURL: String = "http://www.qt.hk/"
    override val searchURL: String
        get() = "${baseURL}search-index.htm"

    override val orderModes: Map<String, String> =
        mapOf(
            ORDER_MODE_NEW_THREAD to "tid",
            ORDER_MODE_LAST_THREAD to "lastpost",
            ORDER_MODE_ESSENCE_THREAD to "digest"
        )

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
        val body =
            Util.fastBinaryContent(url = baseURL + "user-login.htm", querys = httpForms + forms)
        return if (body.isNotEmpty()) {
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
    override fun parse(
        subURL: String,
        isSub: Boolean,
        vararg args: Pair<String, Any>
    ): List<Message> {
        val forms: Map<String, String> =
            mapOf(
                "auth" to userAuth,
                "page" to pageNumber.toString()
            ) + httpForms + args.map { it.first to it.second.toString() }
        val url = if (subURL.isNotEmpty()) {
            subURL
        } else {
            val orderMode = orderModes.getOrDefault(currentOrder, "tid")
            "${baseURL}index-index.htm?orderby=$orderMode"
        }

        val body = Util.fastBinaryContent(url = url, querys = forms)
        if (body.isNotEmpty()) {
            val jsonObj = JSONObject(body.toString(Charset.forName(charsetName)))
            if (jsonObj.getInt("status") == 1) {
                val messages: MutableList<Message> = mutableListOf()
                val itemName = if (isSub) {
                    threadQuery.first
                } else {
                    threadQuery.second
                }
                val jsonAry = jsonObj.getJSONObject("message").getJSONArray(itemName)
                for (i in 0 until jsonAry.length()) {
                    val jsonItem = jsonAry.getJSONObject(i)
                    messages.add(build(jsonItem, isSub))
                }
                return messages
            }
        }
        return listOf()
    }

    // 解析蜻蜓社区和争渡网楼层
    override fun parsePosts(pageNumber: Int): List<Message> {
        val url = "${baseURL}thread-${currentMessage.tid}-page-${pageNumber}.htm"
        return parse(subURL = url, isSub = true)
    }

    // 构造Message对象
    override val build: (Any, Boolean) -> Message = { dataObj, isSub ->
        if (dataObj is JSONObject) {
            if (!isSub) {
                Message(
                    tid = dataObj.getLong("tid"),
                    content = dataObj.getString("subject"),
                    author = dataObj.getString("username"),
                    dateLine = dataObj.getLong("dateline"),
                    dateFmt = dataObj.getString("dateline_fmt"),
                    postCount = dataObj.getInt("posts"),
                    viewCount = dataObj.getInt("views"),
                    lastDate = dataObj.getLong("lastpost"),
                    lastDateFmt = dataObj.getString("lastpost_fmt"),
                    lastPost = dataObj.getString("lastusername")
                )
            } else {
                val content = dataObj.getString("message")
                val resources = buildResources(Jsoup.parse(content).root())
                Message(
                    tid = dataObj.getLong("tid"),
                    content = content,
                    author = dataObj.getString("username"),
                    floor = dataObj.getInt("floor"),
                    dateLine = dataObj.getLong("dateline"),
                    dateFmt = dataObj.getString("dateline_fmt"),
                    resources = resources
                )
            }
        } else {
            Message()
        }
    }

    // 蜻蜓社区和争渡网回帖
    override fun post(content: String): Message {
        val url = "${baseURL}post-post.htm"
        val forms = mapOf<String, Any>(
            "tid" to MainActivity.forum.currentMessage.tid,
            "message" to content,
            "auth" to userAuth,
            secKey
        )
        val body = Util.fastBinaryContent(url = url, querys = httpForms + forms)
        if (body.isNotEmpty()) {
            val jsonObj = JSONObject(body.toString(Charset.forName(charsetName)))
            if (jsonObj.getInt("status") == 1)
                return Message(
                    content = content,
                    author = userName,
                    dateFmt = "刚刚",
                    floor = currentMessage.postCount + 1
                )
        }
        return Message()
    }

    // 获取蜻蜓社区和争渡网论坛所有板块
    //* 注意争渡网还需要获取二级板块， 三四级板块可以忽略
    // 详细参看相关网站返回的json数据
    override fun section(): Array<Section> {
        val forms = mapOf("auth" to userAuth)
        val body =
            Util.fastBinaryContent(
                url = "${baseURL}index-forumlist.htm",
                querys = httpForms + forms
            )
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
        return arrayOf()
    }

    // 蜻蜓社区和争渡网发布新主题
    override fun thread(title: String, message: String, section: MutableSection): Int {
        val forms = mapOf(
            "subject" to title,
            "message" to message,
            "fid" to section.first.id,
            "typeid1" to section.second.id,
            "typeid2" to 0,
            "typeid3" to 0,
            "typeid4" to 0,
            "auth" to userAuth,
            secKey
        )
        val url = "${baseURL}post-thread.htm"
        val body = Util.fastBinaryContent(url = url, querys = httpForms + forms)
        if (body.isNotEmpty()) {
            val resText = body.toString(Charset.forName(charsetName))

            val jsonObj = JSONObject(resText)
            if (jsonObj.getInt("status") == 1) {
                return jsonObj.getJSONObject("message").getJSONObject("thread").getInt("tid")
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
