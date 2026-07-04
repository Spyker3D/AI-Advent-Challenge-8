package com.aiassistant.core.domain.repository

import com.aiassistant.core.domain.invariant.Invariant

interface InvariantRepository {
    suspend fun getInvariants(): List<Invariant>
    suspend fun saveInvariants(invariants: List<Invariant>)
}
