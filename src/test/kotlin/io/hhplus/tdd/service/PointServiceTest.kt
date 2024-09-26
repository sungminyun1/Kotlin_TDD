package io.hhplus.tdd.service

import io.hhplus.tdd.database.PointHistoryRepositoryImpl
import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointRepositoryImpl
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.PointHistory
import io.hhplus.tdd.point.TransactionType
import io.hhplus.tdd.point.UserPoint
import io.hhplus.tdd.point.dto.response.PointHistoryResponse
import io.hhplus.tdd.point.dto.response.UserPointResponse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test



class PointServiceTest {

    private lateinit var fakeUserPointTable: UserPointTable
    private lateinit var fakePointHistoryTable: PointHistoryTable
    private lateinit var pointService: PointService

    @BeforeEach
    fun setUp() {
        fakeUserPointTable = FakeUserPointTable()
        fakePointHistoryTable = FakePointHistoryTable()
        pointService = PointService(
            PointHistoryRepositoryImpl(fakePointHistoryTable),
            UserPointRepositoryImpl(fakeUserPointTable),)
    }

    @Test
    @DisplayName("포인트를 충전한다")
    fun chargePoint() {
        //given
        val userId: Long = 10L;
        val initPoint = fakeUserPointTable.insertOrUpdate(userId, 10L)

        //when
        val chargedPoint = pointService.chargeUserPoint(userId, 100L);

        //then
        assertThat(chargedPoint.point).isEqualTo(110L)
    }

    @Test
    @DisplayName("기존에 저장된 유저 포인트가 없다면 0점으로 시작한다")
    fun chargePointWithoutInitData() {
        //given
        val userId: Long = 10L;

        //when
        val chargedPoint = pointService.chargeUserPoint(userId, 100L);

        //then
        assertThat(chargedPoint.point).isEqualTo(100L)
    }

    @Test
    @DisplayName("유저 포인트를 사용한다")
    fun usePoint() {
        //given
        val userId: Long = 10L;
        val initPoint = fakeUserPointTable.insertOrUpdate(userId, 10L)

        //when
        val usedPoint = pointService.useUserPoint(userId, 5L)

        //then
        assertThat(usedPoint.point).isEqualTo(5L)
    }

    @Test
    @DisplayName("유저 포인트 사용시 잔액이 부족하면 오류가 발생한다")
    fun usePointWithLessPoint() {
        //given
        val userId: Long = 10L;
        val initPoint = fakeUserPointTable.insertOrUpdate(userId, 10L)

        //when then
        assertThatThrownBy { pointService.useUserPoint(userId, 20L) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("잔여 포인트가 부족합니다")

    }

    @Test
    @DisplayName("새로운 유저의 포인트를 충전하지 않고 사용하려하면 잔액이 부족하다")
    fun usePointWithoutInit() {
        //given
        val userId: Long = 10L;

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
        assertThat(histories.get(0).id).isEqualTo(1)
        assertThat(histories.get(0).amount).isEqualTo(10)
        assertThat(histories.get(0).type).isEqualTo(TransactionType.CHARGE)
        assertThat(histories.get(0).userId).isEqualTo(userId)
        assertThat(histories.get(1).id).isEqualTo(2)
        assertThat(histories.get(1).amount).isEqualTo(20)
        assertThat(histories.get(1).type).isEqualTo(TransactionType.CHARGE)
        assertThat(histories.get(1).userId).isEqualTo(userId)
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
        assertThat(histories.get(0).id).isEqualTo(1)
        assertThat(histories.get(0).amount).isEqualTo(100)
        assertThat(histories.get(0).type).isEqualTo(TransactionType.CHARGE)
        assertThat(histories.get(0).userId).isEqualTo(userId)
        assertThat(histories.get(1).id).isEqualTo(2)
        assertThat(histories.get(1).amount).isEqualTo(20)
        assertThat(histories.get(1).type).isEqualTo(TransactionType.USE)
        assertThat(histories.get(1).userId).isEqualTo(userId)
    }

    @Test
    @DisplayName("유저 포인트를 조회한다")
    fun getUserPoint() {
        //given
        val userId: Long = 10L;
        val initPoint = fakeUserPointTable.insertOrUpdate(userId, 10L)

        //when
        val userPoint = pointService.getUserPoint(userId)

        //then
        assertThat(userPoint.point).isEqualTo(10)
        assertThat(userPoint.id).isEqualTo(userId)
    }

    @Test
    @DisplayName("유저 포인트를 조회한다. 없다면 기본값은 0 이다")
    fun getUserPointWithoutInitData() {
        //given
        val userId: Long = 10L;

        //when
        val userPoint = pointService.getUserPoint(userId)

        //then
        assertThat(userPoint.point).isEqualTo(0)
        assertThat(userPoint.id).isEqualTo(userId)
    }

    @Test
    @DisplayName("유저 포인트를 충전하면 충전된 포인트가 조회된다")
    fun getUserPointWithCharge() {
        //given
        val userId: Long = 10L;
        val initPoint = fakeUserPointTable.insertOrUpdate(userId, 10L)
        val chargedPoint = pointService.chargeUserPoint(userId, 100L)

        //when
        val userPoint = pointService.getUserPoint(userId)

        //then
        assertThat(userPoint.point).isEqualTo(110)
    }

    @Test
    @DisplayName("유저 포인트를 사용하면 사용된 포인트가 조회된다")
    fun getUserPointWithUse() {
        //given
        val userId: Long = 10L;
        val initPoint = fakeUserPointTable.insertOrUpdate(userId, 100L)
        val usedPoint = pointService.useUserPoint(userId, 30L)

        //when
        val userPoint = pointService.getUserPoint(userId)

        //then
        assertThat(userPoint.point).isEqualTo(70)
    }

    class FakePointHistoryTable: PointHistoryTable() {
        private val table = mutableListOf<PointHistory>()
        private var cursor: Long = 1L

        override fun insert(
            id: Long,
            amount: Long,
            transactionType: TransactionType,
            updateMillis: Long
        ): PointHistory {
            val history = PointHistory(
                id = cursor++,
                userId = id,
                amount = amount,
                type = transactionType,
                timeMillis = updateMillis,
            )
            table.add(history)
            return history
        }

        override fun selectAllByUserId(userId: Long): List<PointHistory> {
            return table.filter { it.userId == userId }
        }
    }

    class FakeUserPointTable: UserPointTable() {
        private val table = HashMap<Long, UserPoint>()

        override fun selectById(id: Long): UserPoint {
            return table[id] ?: UserPoint(id = id, point = 0, updateMillis = System.currentTimeMillis())
        }

        override fun insertOrUpdate(id: Long, amount: Long): UserPoint {
            val userPoint = UserPoint(id = id, point = amount, updateMillis = System.currentTimeMillis())
            table[id] = userPoint
            return userPoint
        }
    }
}
