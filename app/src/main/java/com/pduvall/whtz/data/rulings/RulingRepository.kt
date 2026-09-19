package com.pduvall.whtz.data.rulings

import com.pduvall.whtz.data.local.dao.RulingDao
import javax.inject.Inject
import javax.inject.Singleton

/** Offline ruling lookups (bundled in the `rulings` table). */
@Singleton
class RulingRepository @Inject constructor(
    private val rulingDao: RulingDao,
) {
    /** Of the given oracle ids, those that have at least one ruling (for cheap action gating). */
    suspend fun oracleIdsWithRulings(oracleIds: List<String>): Set<String> =
        rulingDao.oracleIdsWithRulings(oracleIds).toSet()

    /** Display lines for a card's rulings, one bullet per ruling (with its date when known). */
    suspend fun printableLines(oracleId: String): List<String> =
        rulingDao.forOracle(oracleId).map { r ->
            val date = r.publishedAt?.takeIf { it.isNotBlank() }
            if (date != null) "• ($date) ${r.comment}" else "• ${r.comment}"
        }
}
