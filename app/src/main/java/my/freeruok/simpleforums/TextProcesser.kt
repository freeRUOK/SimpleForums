/* TextProcesser.kt */
// * 2651688427@qq.com
// * 处理文本， 从文本中提取网址， 手机号qq号等等
package my.freeruok.simpleforums

import java.util.regex.Pattern

object TextProcesser {
    enum class TextType {
        WEB_URL, PHONE_NUMBER, EMAIL_ADDRESS, IP_ADDRESS, MEDIA_URL
    }

    private val patterns = mapOf<TextType, Pattern>(
        TextType.EMAIL_ADDRESS to Pattern.compile("[a-zA-Z0-9_\\.\\+\\-]+@[a-zA-Z0-9_\\.\\+\\-]+"),
        TextType.IP_ADDRESS to Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"),
        TextType.WEB_URL to Pattern.compile("((http|ftp|https):\\/\\/)*[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?"),
        TextType.PHONE_NUMBER to Pattern.compile("\\d{5,14}")
    )

    fun parseText(text: CharSequence): MutableSet<Pair<TextType, String>> {
        val results = mutableSetOf<Pair<TextType, String>>()
        var match = patterns[TextType.WEB_URL]!!.matcher(text)
        while (match.regionStart() < match.regionEnd()) {
            var isMatch = false
            for ((key, value) in patterns) {
                match = match.usePattern(value)
                if (match.lookingAt()) {
                    results.add(key to match.group())
                    match.region(match.end(), match.regionEnd())
                    isMatch = true
                }
            }
            if ((!isMatch) && match.regionStart() < match.regionEnd()) {
                match.region(match.regionStart() + 1, match.regionEnd())
            }
        }
        return results
    }


}
