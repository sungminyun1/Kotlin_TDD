package io.hhplus.tdd.service

import io.hhplus.tdd.database.dto.PointHistorySaveDto
import io.hhplus.tdd.point.PointHistory

interface PointHistoryRepository {
    fun selectAllByUser(userId: Long): List<PointHistory>

    fun save(pointHistorySaveDto: PointHistorySaveDto): PointHistory
}