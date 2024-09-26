package io.hhplus.tdd.service

import io.hhplus.tdd.database.PointHistoryTable
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
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@ActiveProfiles("test")
@SpringBootTest
class PointServiceTest {

    private lateinit var userPointTable: UserPointTable
    private lateinit var pointHistoryTable: PointHistoryTable
    private lateinit var pointService: PointService
    private lateinit var executorService: ExecutorService

    @BeforeEach
    fun setUp() {
        userPointTable = UserPointTable()
        pointHistoryTable = PointHistoryTable()
        pointService = PointService(pointHistoryTable, userPointTable)
        executorService = Executors.newFixedThreadPool(5)
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
        val initPoint = userPointTable.insertOrUpdate(userId, 10L)

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
        val initPoint = userPointTable.insertOrUpdate(userId, 10L)

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
        val initPoint = userPointTable.insertOrUpdate(userId, 10L)

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
        val initPoint = userPointTable.insertOrUpdate(userId, 10L)
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
        val initPoint = userPointTable.insertOrUpdate(userId, 100L)
        val usedPoint = pointService.useUserPoint(userId, 30L)

        //when
        val userPoint = pointService.getUserPoint(userId)

        //then
        assertThat(userPoint.point).isEqualTo(70)
    }

    @Test
    @DisplayName("동시에 충전 요청을 할 경우 올바르게 값이 충전된다")
    fun testConcurrentRequest() {
        //given
        val numberOfThread = 5
        val startLatch = CountDownLatch(1)
        val finishLatch = CountDownLatch(numberOfThread)
        val userId = 10L;
        userPointTable.insertOrUpdate(userId, 100L)

        repeat(numberOfThread) {
            executorService.submit {
                try {
                    startLatch.await()

                    val response = pointService.chargeUserPoint(userId, 100L)

                } finally {
                    finishLatch.countDown()
                }
            }
        }

        startLatch.countDown()

        executorService.shutdown()
        executorService.awaitTermination(5, TimeUnit.SECONDS)

        //when
        val userPoint = pointService.getUserPoint(userId)
        val userHistory = pointService.getUserPointHistory(userId)

        //then
        assertThat(userPoint.point).isEqualTo(600L)
        assertThat(userHistory.size).isEqualTo(5)

    }

    @Test
    @DisplayName("동시에 충전과 사용 요청을 할 경우 순서대로 올바르게 사용된다")
    fun testConcurrentRequestWithUse() {
        //given
        val numberOfThread = 4
        val startLatch = CountDownLatch(1)
        val finishLatch = CountDownLatch(numberOfThread)
        val userId = 10L;

        executorService.submit {
            try {
                startLatch.await()

                val response = pointService.chargeUserPoint(userId, 100L)

            } finally {
                finishLatch.countDown()
            }
        }

        executorService.submit {
            try {
                startLatch.await()
                Thread.sleep(1)

                val response = pointService.useUserPoint(userId, 100L)

            } finally {
                finishLatch.countDown()
            }
        }

        executorService.submit {
            try {
                startLatch.await()
                Thread.sleep(2)
                val response = pointService.chargeUserPoint(userId, 100L)

            } finally {
                finishLatch.countDown()
            }
        }

        executorService.submit {
            try {
                startLatch.await()
                Thread.sleep(3)
                val response = pointService.useUserPoint(userId, 100L)

            } finally {
                finishLatch.countDown()
            }
        }

        startLatch.countDown()

        executorService.shutdown()
        executorService.awaitTermination(5, TimeUnit.SECONDS)

        //when
        val userPoint = pointService.getUserPoint(userId)
        val userHistory = pointService.getUserPointHistory(userId)

        //then
        assertThat(userPoint.point).isEqualTo(0)
        assertThat(userHistory.size).isEqualTo(4)
        assertThat(userHistory.get(0).type).isEqualTo(TransactionType.CHARGE)
        assertThat(userHistory.get(1).type).isEqualTo(TransactionType.USE)
        assertThat(userHistory.get(2).type).isEqualTo(TransactionType.CHARGE)
        assertThat(userHistory.get(3).type).isEqualTo(TransactionType.USE)
    }

    @Test
    @DisplayName("동시의 여러 충전 요청 중 현재 포인트를 조회하면 올바르게 조회해야한다")
    fun testGetPointWithConcurrentRequest() {
        //given
        val numberOfThread = 5
        val startLatch = CountDownLatch(1)
        val finishLatch = CountDownLatch(numberOfThread)
        val userId = 10L;
        lateinit var searchedPoint: UserPointResponse

        executorService.submit {
            try {
                startLatch.await()

                val response = pointService.chargeUserPoint(userId, 100L)

            } finally {
                finishLatch.countDown()
            }
        }

        executorService.submit {
            try {
                startLatch.await()
                Thread.sleep(1)

                val response = pointService.chargeUserPoint(userId, 100L)

            } finally {
                finishLatch.countDown()
            }
        }

        executorService.submit {
            try {
                startLatch.await()
                Thread.sleep(2)

                searchedPoint = pointService.getUserPoint(userId)

            } finally {
                finishLatch.countDown()
            }
        }

        executorService.submit {
            try {
                startLatch.await()
                Thread.sleep(3)
                val response = pointService.chargeUserPoint(userId, 100L)

            } finally {
                finishLatch.countDown()
            }
        }

        executorService.submit {
            try {
                startLatch.await()
                Thread.sleep(4)
                val response = pointService.chargeUserPoint(userId, 100L)

            } finally {
                finishLatch.countDown()
            }
        }

        startLatch.countDown()

        executorService.shutdown()
        executorService.awaitTermination(5, TimeUnit.SECONDS)

        //when
        val userHistory = pointService.getUserPointHistory(userId)

        //then
        assertThat(searchedPoint.point).isEqualTo(200)
        assertThat(userHistory.size).isEqualTo(4)
        assertThat(userHistory.get(0).type).isEqualTo(TransactionType.CHARGE)
        assertThat(userHistory.get(1).type).isEqualTo(TransactionType.CHARGE)
        assertThat(userHistory.get(2).type).isEqualTo(TransactionType.CHARGE)
        assertThat(userHistory.get(3).type).isEqualTo(TransactionType.CHARGE)
    }

    @Test
    @DisplayName("동시의 여러 요청 중 이력을 조회하면 올바르게 조회해와야한다")
    fun testGetHistoryWithConcurrentRequest() {
        //given
        val numberOfThread = 5
        val startLatch = CountDownLatch(1)
        val finishLatch = CountDownLatch(numberOfThread)
        val userId = 10L;
        lateinit var searchedHistory: List<PointHistoryResponse>

        executorService.submit {
            try {
                startLatch.await()

                val response = pointService.chargeUserPoint(userId, 100L)

            } finally {
                finishLatch.countDown()
            }
        }

        executorService.submit {
            try {
                startLatch.await()
                Thread.sleep(1)

                val response = pointService.useUserPoint(userId, 100L)

            } finally {
                finishLatch.countDown()
            }
        }

        executorService.submit {
            try {
                startLatch.await()
                Thread.sleep(2)

                val response  = pointService.chargeUserPoint(userId, 100L)

            } finally {
                finishLatch.countDown()
            }
        }

        executorService.submit {
            try {
                startLatch.await()
                Thread.sleep(3)
                searchedHistory = pointService.getUserPointHistory(userId)

            } finally {
                finishLatch.countDown()
            }
        }

        executorService.submit {
            try {
                startLatch.await()
                Thread.sleep(4)
                val response = pointService.useUserPoint(userId, 100L)

            } finally {
                finishLatch.countDown()
            }
        }

        startLatch.countDown()

        executorService.shutdown()
        executorService.awaitTermination(5, TimeUnit.SECONDS)

        //when


        //then
        assertThat(searchedHistory.size).isEqualTo(3)
        assertThat(searchedHistory.get(0).type).isEqualTo(TransactionType.CHARGE)
        assertThat(searchedHistory.get(1).type).isEqualTo(TransactionType.USE)
        assertThat(searchedHistory.get(2).type).isEqualTo(TransactionType.CHARGE)
    }
}