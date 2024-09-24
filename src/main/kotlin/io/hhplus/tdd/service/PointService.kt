package io.hhplus.tdd.service

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.PointHistory
import io.hhplus.tdd.point.TransactionType
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

        val chargedPoint = userPointTable.insertOrUpdate(userId, userPoint.point)

        pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, chargedPoint.updateMillis)

        return chargedPoint
    }

    fun useUserPoint(
        userId: Long, amount: Long
    ): UserPoint {
        val userPoint = userPointTable.selectById(userId)
        userPoint.usePoint(amount)

        val usedPoint = userPointTable.insertOrUpdate(userId, userPoint.point)

        pointHistoryTable.insert(userId, amount, TransactionType.USE, usedPoint.updateMillis)

        return usedPoint
    }

    fun getUserPointHistory(
        userId: Long
    ): List<PointHistory> {
        return pointHistoryTable.selectAllByUserId(userId)
    }

    fun getUserPoint(
        userId: Long
    ): UserPoint {
        return userPointTable.selectById(userId)
    }
}