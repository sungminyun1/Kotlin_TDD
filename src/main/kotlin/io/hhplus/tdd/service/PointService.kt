package io.hhplus.tdd.service

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.PointHistory
import io.hhplus.tdd.point.TransactionType
import io.hhplus.tdd.point.UserPoint
import io.hhplus.tdd.point.dto.response.PointHistoryResponse
import io.hhplus.tdd.point.dto.response.UserPointResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

@Service
class PointService @Autowired constructor(
    private val pointHistoryTable: PointHistoryTable,
    private val userPointTable: UserPointTable
){
    private val log = LoggerFactory.getLogger(PointService::class.java)

    private val userLock: ConcurrentHashMap<Long, ReentrantLock> = ConcurrentHashMap()

    fun chargeUserPoint(
        userId: Long, amount: Long
    ) : UserPointResponse {
        log.debug("arrived charge " + System.nanoTime())
        lockForUser(userId)

        try{
            log.debug("start charge " + System.nanoTime())
            val userPoint = userPointTable.selectById(userId)
            userPoint.chargePoint(amount)

            val chargedPoint = userPointTable.insertOrUpdate(userId, userPoint.point)

            pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, chargedPoint.updateMillis)

            log.debug("end charge " + System.nanoTime())

            return UserPointResponse.of(chargedPoint)
        }finally {
            unlockForUser(userId)
        }


    }

    fun useUserPoint(
        userId: Long, amount: Long
    ): UserPointResponse {
        log.debug("arrived use" + System.currentTimeMillis())

        lockForUser(userId)

        try{
            log.debug("start use" + System.currentTimeMillis())
            val userPoint = userPointTable.selectById(userId)

            userPoint.usePoint(amount)

            val usedPoint = userPointTable.insertOrUpdate(userId, userPoint.point)


            pointHistoryTable.insert(userId, amount, TransactionType.USE, usedPoint.updateMillis)

            log.debug("end use" + System.currentTimeMillis())

            return UserPointResponse.of(usedPoint)
        }finally {
            unlockForUser(userId)
        }

    }

    fun getUserPointHistory(
        userId: Long
    ): List<PointHistoryResponse> {
        log.debug("arrived history " + System.currentTimeMillis())

        lockForUser(userId)
        try{
            log.debug("start history " + System.currentTimeMillis())
            val histories = pointHistoryTable.selectAllByUserId(userId)
            log.debug("end history " + System.currentTimeMillis())

            return histories.map { PointHistoryResponse.of(it) }
        }finally {
            unlockForUser(userId)
        }
    }

    fun getUserPoint(
        userId: Long
    ): UserPointResponse {
        log.debug("arrived get " + System.currentTimeMillis())

        lockForUser(userId)
        try{
            log.debug("start get " + System.currentTimeMillis())
            val userPoint = userPointTable.selectById(userId)
            log.debug("end get " + System.currentTimeMillis())

            return UserPointResponse.of(userPoint)
        }finally {
            unlockForUser(userId)
        }

    }

    private fun lockForUser(key: Long) {
        val lock = userLock.computeIfAbsent(key) { ReentrantLock(true) }
        log.debug("Is this lock fair? {}", lock.isFair())
        lock.lock()
    }

    private fun unlockForUser(key: Long) {
        userLock[key]?.unlock()
    }
}