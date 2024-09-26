package io.hhplus.tdd.database.dto

import io.hhplus.tdd.point.TransactionType

data class PointHistorySaveDto(
    val userId: Long,
    val type: TransactionType,
    val amount: Long,
    val timeMillis: Long,
)
