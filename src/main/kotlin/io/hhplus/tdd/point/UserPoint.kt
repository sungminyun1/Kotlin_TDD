package io.hhplus.tdd.point

data class UserPoint(
    val id: Long,
    var point: Long,
    val updateMillis: Long,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserPoint

        if (id != other.id) return false
        if (point != other.point) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + point.hashCode()
        return result
    }

    fun chargePoint(point: Long) {
        this.point += point
    }

    fun usePoint(point: Long) {
        if(this.point < point) throw RuntimeException("잔여 포인트가 부족합니다")
        this.point -= point
    }
}
