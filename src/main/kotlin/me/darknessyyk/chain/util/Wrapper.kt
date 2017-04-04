package me.darknessyyk.chain.util

import java.util.*

class ByteArrayWrapper(b: ByteArray) {

    private val contents: ByteArray = b.copyOf()

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }

        val otherB = other as ByteArrayWrapper
        val b = otherB.contents
        if (contents.size != b.size)
            return false
        return b.indices.none { contents[it] != b[it] }
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(contents)
    }
}

