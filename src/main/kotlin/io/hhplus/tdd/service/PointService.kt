package io.hhplus.tdd.service

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.PointHistory
import io.hhplus.tdd.point.UserPoint
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PointService @Autowired constructor(
    private val pointHistoryTable: PointHistoryTable,
    private val userPointTable: UserPointTable
){

    fun chargeUserPoint(
        userId: Long, amount: Long
    ) : UserPoint {

        val userPoint = userPointTable.selectById(userId)
        userPoint.chargePoint(amount)

        return userPointTable.insertOrUpdate(userId, userPoint.currentPoint)
    }
}