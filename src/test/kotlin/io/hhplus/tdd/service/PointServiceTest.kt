package io.hhplus.tdd.service

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.UserPoint
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class PointServiceTest @Autowired constructor(
    private val pointService: PointService,
    private val pointHistoryTable: PointHistoryTable,
    private val userPointTable: UserPointTable
) {

    /**
     * memo: UserPoint 에 대한 검증을 equals 를 통해 하고싶음.
     * 이때 updateMills 까지 비교해야할까?
     * 인데 안하기로함. 수정할 수 없는 UserPointTable 에서 insert 하는 시점에 updateMills 를 지정하기때문
     * 내가 직접 제어가 안됨
     */
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
}