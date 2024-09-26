package io.hhplus.tdd.database

import io.hhplus.tdd.database.dto.PointHistorySaveDto
import io.hhplus.tdd.point.PointHistory
import io.hhplus.tdd.service.PointHistoryRepository
import org.springframework.stereotype.Repository

@Repository
class PointHistoryRepositoryImpl(
    private val pointHistoryTable: PointHistoryTable
) : PointHistoryRepository {
    override fun selectAllByUser(userId: Long): List<PointHistory> {
        return pointHistoryTable.selectAllByUserId(userId)
    }

    override fun save(pointHistorySaveDto: PointHistorySaveDto): PointHistory {
        return pointHistoryTable.insert(
            id = pointHistorySaveDto.userId,
            amount = pointHistorySaveDto.amount,
            transactionType = pointHistorySaveDto.type,
            updateMillis = pointHistorySaveDto.timeMillis
        )
    }
}