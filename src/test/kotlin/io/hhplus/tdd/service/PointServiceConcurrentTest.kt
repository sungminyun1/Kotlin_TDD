package io.hhplus.tdd.service

import io.hhplus.tdd.database.PointHistoryRepositoryImpl
import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointRepositoryImpl
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.TransactionType
import io.hhplus.tdd.point.dto.response.PointHistoryResponse
import io.hhplus.tdd.point.dto.response.UserPointResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
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
class PointServiceConcurrentTest {

    private lateinit var userPointTable: UserPointTable
    private lateinit var pointHistoryTable: PointHistoryTable
    private lateinit var pointService: PointService
    private lateinit var executorService: ExecutorService

    @BeforeEach
    fun setUp() {
        userPointTable = UserPointTable()
        pointHistoryTable = PointHistoryTable()

        pointService = PointService(
            PointHistoryRepositoryImpl(pointHistoryTable),
            UserPointRepositoryImpl(userPointTable),
        )

        executorService = Executors.newFixedThreadPool(5)
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