package io.hhplus.tdd.point

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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
        assertThat(userPoint.point).isEqualTo(40)
    }

    @Test
    @DisplayName("포인트를 사용한다")
    fun testUsePoint() {
        //given
        val userId = 10L
        val userPoint = UserPoint(userId, 10, 0)

        //when
        userPoint.usePoint(5)

        //then
        assertThat(userPoint.point).isEqualTo(5)
    }

    @Test
    @DisplayName("포인트를 사용한다시 잔액이 부족하면 오류가 발생한다")
    fun testUsePointWithLessPoint() {
        //given
        val userId = 10L
        val userPoint = UserPoint(userId, 10, 0)

        //when then
        assertThatThrownBy { userPoint.usePoint(15) }
            .isInstanceOf(RuntimeException::class.java)
    }
}