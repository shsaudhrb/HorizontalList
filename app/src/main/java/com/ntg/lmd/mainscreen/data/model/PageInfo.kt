package com.ntg.lmd.mainscreen.data.model

data class PageInfo(
    val page: Int? = null,
    val totalPages: Int? = null,
    val nextPage: Int? = null,
    val hasMore: Boolean? = null,
    val cursor: String? = null,
    val nextCursor: String? = null,
)
