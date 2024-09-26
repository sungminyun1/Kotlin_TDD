package io.hhplus.tdd.point

class UserPoint(
    val id: Long,
    point: Long,
    val updateMillis: Long,
) {

    private val MAXIMUM_POINT = 1_000_000L

    var point: Long = point
        set(value) {
            if( value < 0 )  throw RuntimeException("잔여 포인트가 부족합니다")
            if( value > MAXIMUM_POINT) throw RuntimeException("포인트는 100만포인트를 초과하여 보유할 수 없습니다")
            field = value
        }

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
        this.point -= point
    }
}
