package org.jellyfin.androidtv.ui.home

import android.annotation.SuppressLint
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.QueryType
import org.jellyfin.androidtv.ui.browsing.BrowseRowDef
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import androidx.leanback.widget.Row
import androidx.leanback.widget.Presenter
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.constant.ImageType
import org.jellyfin.androidtv.ui.card.LegacyImageCardView
import androidx.leanback.widget.BaseCardView
import timber.log.Timber

class HomeFragmentWatchAgainRow(
    private val userRepository: UserRepository,
    private val api: ApiClient
) : HomeFragmentRow {

    private val noInfoCardPresenter = object : CardPresenter(false, ImageType.THUMB, 110) {
        init {
            setHomeScreen(true)
            setUniformAspect(true)
        }

        override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any?) {
            super.onBindViewHolder(viewHolder, item)

            (viewHolder.view as? LegacyImageCardView)?.let { cardView ->
                cardView.cardType = BaseCardView.CARD_TYPE_MAIN_ONLY
            }
        }
    }

    @SuppressLint("StringFormatInvalid")
	override fun addToRowsAdapter(
        context: Context,
        cardPresenter: CardPresenter,
        rowsAdapter: MutableObjectAdapter<Row>
    ) {
        val userId = userRepository.currentUser.value?.id
        if (userId == null) {
            Timber.e("User not available, cannot load watch again items")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get recently watched items (movies and TV shows)
                val recentlyWatchedQuery = GetItemsRequest(
                    userId = userId,
                    includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
                    sortOrder = setOf(SortOrder.DESCENDING),
                    sortBy = setOf(ItemSortBy.DATE_PLAYED),
                    limit = 20,
                    recursive = true,
                    fields = setOf(
                        ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
                        ItemFields.OVERVIEW,
                        ItemFields.CHILD_COUNT,
                        ItemFields.MEDIA_STREAMS,
                        ItemFields.MEDIA_SOURCES
                    )
                )

                // Get historically watched items (sorting by play count)
                val historicallyWatchedQuery = GetItemsRequest(
                    userId = userId,
                    includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
                    sortOrder = setOf(SortOrder.DESCENDING),
                    sortBy = setOf(ItemSortBy.PLAY_COUNT),
                    limit = 20,
                    recursive = true,
                    fields = setOf(
                        ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
                        ItemFields.OVERVIEW,
                        ItemFields.CHILD_COUNT,
                        ItemFields.MEDIA_STREAMS,
                        ItemFields.MEDIA_SOURCES
                    )
                )

                Timber.d("Loading watch again rows")

                withContext(Dispatchers.Main) {
                    // Create "Recently Watched" row
                    val recentlyWatchedRowDef = BrowseRowDef(
                        context.getString(R.string.recently_watched),
                        recentlyWatchedQuery,
                        15, // chunkSize
                        false, // preferParentThumb
                        true, // staticHeight
                        arrayOf()
                    )
                    val recentlyWatchedRow = HomeFragmentBrowseRowDefRow(recentlyWatchedRowDef)
                    recentlyWatchedRow.addToRowsAdapter(context, noInfoCardPresenter, rowsAdapter)

                    // Create "Watch Again" (historically watched) row
                    val watchAgainRowDef = BrowseRowDef(
                        context.getString(R.string.watch_again),
                        historicallyWatchedQuery,
                        15, // chunkSize
                        false, // preferParentThumb
                        true, // staticHeight
                        arrayOf()
                    )
                    val watchAgainRow = HomeFragmentBrowseRowDefRow(watchAgainRowDef)
                    watchAgainRow.addToRowsAdapter(context, noInfoCardPresenter, rowsAdapter)
                }

            } catch (e: Exception) {
                Timber.e(e, "Error loading watch again items: ${e.message}")
            }
        }
    }
}