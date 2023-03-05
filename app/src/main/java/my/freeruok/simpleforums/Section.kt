/* Section.kt */
// * 2651688427@qq.com

package my.freeruok.simpleforums

data class Section(
    val name: String = "",
    val id: Int = 0,
    var subSections: Array<Section> = arrayOf()
) {
    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {
        return id == (other as Section).id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

data class MutableSection(var first: Section, var second: Section)
