package com.viaibot.ai.repository

import com.viaibot.ai.entity.Document
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface VectorStoreRepository : JpaRepository<Document, UUID> {

    @Query(
        """
        SELECT metadata->>'filename' as filename, COUNT(metadata->>'filename') as cnt
        FROM vector_store
        GROUP BY metadata->>'filename'
        """,
        nativeQuery = true
    )
    fun countGroupedByFilename(): List<Map<String, Any>>

    @Modifying
    @Transactional
    @Query(
        """
            DELETE FROM vector_store
            WHERE metadata->>'filename' = :filename
        """,
        nativeQuery = true
    )
    fun deleteByFilename(@Param("filename") filename: String)

    @Query(
        """
    SELECT metadata->>'filename' AS filename,
           COUNT(metadata->>'filename') AS cnt
    FROM vector_store
    WHERE (:query IS NULL OR metadata->>'filename' ILIKE CONCAT('%', :query, '%'))
    GROUP BY metadata->>'filename'
    """,
        nativeQuery = true
    )
    fun countGroupedByFilenameFiltered(@Param("query") query: String?): List<Map<String, Any>>
}