package io.hhplus.tdd.service

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.UserPoint
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest
class PointServiceTest {

    private lateinit var userPointTable: UserPointTable
    private lateinit var pointHistoryTable: PointHistoryTable
    private lateinit var pointService: PointService

    @BeforeEach
    fun setUp() {
        userPointTable = UserPointTable()
        pointHistoryTable = PointHistoryTable()
        pointService = PointService(pointHistoryTable, userPointTable)
    }

    @Test
    @DisplayName("포인트를 충전한다")
    fun chargePoint() {
        //given
        val userId: Long = 10L;
        val initPoint = userPointTable.insertOrUpdate(userId, 10L)

        //when
        val chargedPoint = pointService.chargeUserPoint(userId, 100L);

        //then
        assertThat(chargedPoint).isEqualTo(UserPoint(userId, 110L, 0L))
    }

    @Test
    @DisplayName("기존에 저장된 유저 포인트가 없다면 0점으로 시작한다")
    fun chargePointWithoutInitData() {
        //given
        val userId: Long = 10L;

        //when
        val chargedPoint = pointService.chargeUserPoint(userId, 100L);

        //then
        assertThat(chargedPoint).isEqualTo(UserPoint(userId, 100L, 0L))
    }

    @Test
    @DisplayName("유저 포인트를 사용한다")
    fun usePoint() {
        //given
        val userId: Long = 10L;
        val initPoint = userPointTable.insertOrUpdate(userId, 10L)

        //when
        val usedPoint = pointService.useUserPoint(userId, 5L)

        //then
        assertThat(usedPoint).isEqualTo(UserPoint(userId, 5L, 0L))
    }

    @Test
    @DisplayName("유저 포인트 사용시 잔액이 부족하면 오류가 발생한다")
    fun usePointWithLessPoint() {
        //given
        val userId: Long = 10L;
        val initPoint = userPointTable.insertOrUpdate(userId, 10L)

        //when then
        assertThatThrownBy { pointService.useUserPoint(userId, 20L) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("잔여 포인트가 부족합니다")

    }
}