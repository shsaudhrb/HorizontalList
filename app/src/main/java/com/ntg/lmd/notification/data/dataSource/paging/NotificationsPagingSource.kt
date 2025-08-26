package com.ntg.lmd.notification.data.dataSource.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.ntg.lmd.notification.domain.model.NotificationFilter
import com.ntg.lmd.notification.domain.repository.NotificationRepository
import com.ntg.lmd.notification.ui.model.NotificationUi
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class NotificationsPagingSource(
    private val repo: NotificationRepository,
    private val filter: NotificationFilter,
    private val pageSize: Int,
) : PagingSource<Int, NotificationUi>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, NotificationUi> =
        try {
            val offset = params.key ?: 0
            val limit = params.loadSize
            val raw = repo.loadPage(offset, limit, filter)

            val items =
                raw.map {
                    NotificationUi(
                        id = it.id,
                        message = it.message,
                        timestampMs = it.timestampMs,
                        type = it.type,
                    )
                }

            val nextKey = if (items.size < limit) null else offset + items.size
            val prevKey = if (offset == 0) null else (offset - pageSize).coerceAtLeast(0)

            LoadResult.Page(
                data = items,
                prevKey = prevKey,
                nextKey = nextKey,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            LoadResult.Error(e)
        } catch (e: HttpException) {
            LoadResult.Error(e)
        } catch (e: IllegalArgumentException) {
            LoadResult.Error(e)
        }

    override fun getRefreshKey(state: PagingState<Int, NotificationUi>): Int? {
        val anchor = state.anchorPosition ?: return null
        val anchorPage = state.closestPageToPosition(anchor)
        return anchorPage?.prevKey?.plus(pageSize) ?: anchorPage?.nextKey?.minus(pageSize)
    }
}
