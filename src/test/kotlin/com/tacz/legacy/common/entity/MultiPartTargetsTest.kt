package com.tacz.legacy.common.entity

import org.junit.Assert.*
import org.junit.Test

class MultiPartTargetsTest {
    open class Base
    class Host : Base()

    /** 与寄生虫模组 EntityBody 相同的形态：直接继承基类、用 parent 字段持有本体 */
    open class BodyPart : Base() {
        @Suppress("unused")
        protected var parent: Host? = null
    }

    class SubBodyPart : BodyPart()

    class OwnedThing : Base() {
        @Suppress("unused")
        private var owner: Host? = null
    }

    class ParentNotEntity : Base() {
        @Suppress("unused")
        private var parentName: String = ""
    }

    @Test
    fun `parent field of entity type is found`() {
        val field = MultiPartTargets.findParentField(BodyPart::class.java, Base::class.java)
        assertNotNull(field)
        assertEquals("parent", field!!.name)
        val part = BodyPart()
        val host = Host()
        field.set(part, host)
        assertSame(host, field.get(part))
    }

    @Test
    fun `parent field declared in superclass is found`() {
        assertEquals("parent", MultiPartTargets.findParentField(SubBodyPart::class.java, Base::class.java)?.name)
    }

    @Test
    fun `fields not named parent or not of entity type are ignored`() {
        assertNull(MultiPartTargets.findParentField(OwnedThing::class.java, Base::class.java))
        assertNull(MultiPartTargets.findParentField(ParentNotEntity::class.java, Base::class.java))
        assertNull(MultiPartTargets.findParentField(Host::class.java, Base::class.java))
    }
}
