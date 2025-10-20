package com.viaibot.ai.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

@Entity
@Table(name = "vector_store")
data class Document(

    @Id
    @Column(name = "id", columnDefinition = "uuid", nullable = false)
    val id: UUID,

    @Column(name = "content", columnDefinition = "text")
    val content: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    val metadata: Map<String, Any>
)