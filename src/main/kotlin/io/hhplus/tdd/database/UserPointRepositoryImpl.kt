package io.hhplus.tdd.database

import io.hhplus.tdd.point.UserPoint
import io.hhplus.tdd.service.UserPointRepository
import org.springframework.stereotype.Repository

@Repository
class UserPointRepositoryImpl(
    private val userPointTable: UserPointTable
) : UserPointRepository  {
    override fun findByUserId(userId: Long): UserPoint {
        return userPointTable.selectById(userId)
    }

    override fun save(userPoint: UserPoint): UserPoint {
        return userPointTable.insertOrUpdate(
            id= userPoint.id,
            amount = userPoint.point
        )
    }
}