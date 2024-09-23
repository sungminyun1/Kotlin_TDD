package io.hhplus.tdd.point

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class UserPointTest {

    @Test
    @DisplayName("포인트를 충전한다")
    fun testChargePoint() {
        //given
        val userId = 10L
        val userPoint = UserPoint(userId, 10, 0)

        //when
        userPoint.chargePoint(30)

        //then
        assertThat(userPoint.currentPoint).isEqualTo(40)
    }
}