package io.hhplus.tdd.service

import io.hhplus.tdd.point.UserPoint

interface UserPointRepository {
    fun findByUserId(userId: Long): UserPoint

    fun save(userPoint: UserPoint): UserPoint
}