package io.hhplus.tdd.service

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.PointHistory
import io.hhplus.tdd.point.TransactionType
import io.hhplus.tdd.point.UserPoint
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

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

    @Test
    @DisplayName("유저 포인트 충전시 HistoryTable 에 저장된다")
    fun historyTableUseTest() {
        //given
        val userId: Long = 10L;
        pointService.chargeUserPoint(userId, 10L)
        pointService.chargeUserPoint(userId, 20L)

        //when
        val histories = pointService.getUserPointHistory(userId)

        assertThat(histories).hasSize(2)
        assertThat(histories.get(0)).isEqualTo(PointHistory(1, userId, TransactionType.CHARGE, 10, 0))
        assertThat(histories.get(1)).isEqualTo(PointHistory(2, userId, TransactionType.CHARGE, 20, 0))
    }

    @Test
    @DisplayName("유저 포인트 사용시 HistoryTable 에 저장된다")
    fun historyTableChargeTest() {
        //given
        val userId: Long = 10L;
        pointService.chargeUserPoint(userId, 100L)
        pointService.useUserPoint(userId, 20L)

        //when
        val histories = pointService.getUserPointHistory(userId)

        assertThat(histories).hasSize(2)
        assertThat(histories.get(0)).isEqualTo(PointHistory(1, userId, TransactionType.CHARGE, 100, 0))
        assertThat(histories.get(1)).isEqualTo(PointHistory(2, userId, TransactionType.USE, 20, 0))
    }

    @Test
    @DisplayName("유저 포인트를 조회한다")
    fun getUserPoint() {
        //given
        val userId: Long = 10L;
        val initPoint = userPointTable.insertOrUpdate(userId, 10L)

        //when
        val userPoint = pointService.getUserPoint(userId)

        //then
        assertThat(userPoint).isEqualTo(initPoint)
    }

    @Test
    @DisplayName("유저 포인트를 조회한다. 없다면 기본값은 0 이다")
    fun getUserPointWithoutInitData() {
        //given
        val userId: Long = 10L;

        //when
        val userPoint = pointService.getUserPoint(userId)

        //then
        assertThat(userPoint).isEqualTo(UserPoint(userId, 0L, 0L))
    }

    @Test
    @DisplayName("유저 포인트를 충전하면 충전된 포인트가 조회된다")
    fun getUserPointWithCharge() {
        //given
        val userId: Long = 10L;
        val initPoint = userPointTable.insertOrUpdate(userId, 10L)
        val chargedPoint = pointService.chargeUserPoint(userId, 100L)

        //when
        val userPoint = pointService.getUserPoint(userId)

        //then
        assertThat(userPoint).isEqualTo(UserPoint(userId, 110L, 0L))
    }

    @Test
    @DisplayName("유저 포인트를 사용하면 사용된 포인트가 조회된다")
    fun getUserPointWithUse() {
        //given
        val userId: Long = 10L;
        val initPoint = userPointTable.insertOrUpdate(userId, 100L)
        val usedPoint = pointService.useUserPoint(userId, 30L)

        //when
        val userPoint = pointService.getUserPoint(userId)

        //then
        assertThat(userPoint).isEqualTo(UserPoint(userId, 70L, 0L))
    }
}