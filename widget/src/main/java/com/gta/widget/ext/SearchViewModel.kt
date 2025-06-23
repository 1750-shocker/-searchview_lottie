package com.jidouauto.appshop.feature.search

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.jidouauto.appshop.R
import com.jidouauto.appshop.base.mvvm.BaseViewModel
import com.jidouauto.appshop.extension.appHandleShow
import com.jidouauto.appshop.feature.OrderStatus
import com.jidouauto.appshop.feature.ViewStatus
import com.jidouauto.appshop.feature.bean.*
import com.jidouauto.appshop.helper.*
import com.jidouauto.base.BuildParam
import com.jidouauto.base.IndexBeanWrapper
import com.jidouauto.base.RefreshEmit
import com.jidouauto.lib.rxhelper.transformer.LifecycleTransformer
import com.jidouauto.logger.Logger
import com.jidouauto.market.api.data.MarketRepository
import com.jidouauto.market.api.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import java.util.*
import javax.inject.Inject

@SuppressLint("CheckResult")
@HiltViewModel
class SearchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val buildParam: BuildParam,
    private val marketRepository: MarketRepository,
    private val downloadHelper: DownloadHelper,
    private val packageManagerDelegate: PackageManagerDelegate,
    private val paySuccessDelegate: PaySuccessDelegate,
) : BaseViewModel() {

    private val searchList = mutableListOf<Any>()

    private var searchJob: Job? = null

    private val refreshFlow = MutableStateFlow(RefreshEmit())
    private val searchQuery = MutableSharedFlow<String?>()
    private val _searchType = MutableStateFlow(SearchActivity.TYPE_DEFAULT)
    internal val searchType = _searchType.asStateFlow()

    private var lastSearchResultTime = 0L

    private val _viewState = MutableStateFlow<ViewState>(ViewState.Idle)
    internal val viewState: StateFlow<ViewState> = _viewState.asStateFlow()

    private val nativeAppChangedItem =
        MutableStateFlow<IndexBeanWrapper<SearchNativeAppUIData>?>(null)

    init {
        AppStateDelegate.getAppStateFlowable()
            .observeOn(AndroidSchedulers.mainThread())
            .compose(LifecycleTransformer.bind(lifecycleObservable))
            .map { packageBean: AppUIBean ->
                Logger.info("receive download packageBean ${packageBean.name}, process ${packageBean.percent()}")
                val index =
                    searchList.indexOfFirst { it is SearchNativeAppUIData && packageBean.packageName == it.data.packageName }
                if (index >= 0) {
                    val nativeAppUiData = searchList[index] as SearchNativeAppUIData
                    copyStatusValue(nativeAppUiData.data, packageBean)
                    nativeAppChangedItem.value = (IndexBeanWrapper(index, nativeAppUiData))
                    true
                }
                false
            }.subscribe()

        packageManagerDelegate
            .getInstallStatusFlowable()
            .observeOn(AndroidSchedulers.mainThread())
            .compose(LifecycleTransformer.bind(lifecycleObservable))
            .map { packageName: String ->
                val index =
                    searchList.indexOfFirst { it is SearchNativeAppUIData && packageName == it.data.packageName }
                if (index >= 0) {
                    val nativeAppUiData = searchList[index] as SearchNativeAppUIData
                    nativeAppUiData.data.downloadAndInstallViewStatus =
                        ViewStatus.VIEW_DEFAULT_OPERATION
                    downloadHelper.checkAppDownloadStatus(nativeAppUiData.data)
                    nativeAppChangedItem.value = (IndexBeanWrapper(index, nativeAppUiData))
                    true
                }
                false
            }
            .subscribe()

        paySuccessDelegate
            .getPaySuccessFlowable()
            .observeOn(AndroidSchedulers.mainThread())
            .compose(LifecycleTransformer.bind(lifecycleObservable))
            .map { packageName: String ->
                val index =
                    searchList.indexOfFirst { it is SearchNativeAppUIData && packageName == it.data.packageName }
                if (index >= 0) {
                    val nativeAppUiData = searchList[index] as SearchNativeAppUIData
                    nativeAppUiData.data.active = OrderStatus.ORDER_PAID
                    nativeAppChangedItem.value = (IndexBeanWrapper(index, nativeAppUiData))
                    true
                }
                false
            }
            .subscribe()

        subscribeQueryInput()
    }

    fun searchInputChanged(searchTxt: String) {
        viewModelScope.launch {
            searchQuery.emit(searchTxt)
        }
    }

    fun searchTypeChanged(type: Int) {
        viewModelScope.launch {
            if (type == SearchActivity.TYPE_SEARCH) {
                if (_searchType.value == SearchActivity.TYPE_SEARCH_RESULT) {
                    // 不允许搜索结果后100ms内转为搜索过程
                    if (System.currentTimeMillis() - lastSearchResultTime > 100L) {
                        _searchType.emit(type)
                    }
                } else {
                    _searchType.emit(type)
                }
            } else if (type == SearchActivity.TYPE_SEARCH_RESULT) {
                lastSearchResultTime = System.currentTimeMillis()
                _searchType.emit(type)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            refreshFlow.emit(RefreshEmit())
        }
    }

    fun handle(
        activity: Activity? = null,
        item: AppUIBean,
        operationSuccessCallback: OnOperateSuccessListener
    ) {
        downloadHelper.appHandle(
            item,
            true,
            operationSuccessCallback
        ) { type, appHandleCallback, appIdleHandleCallback ->
            activity?.appHandleShow(type, appHandleCallback, appIdleHandleCallback)
        }
    }

    fun nativeAppChangedItem(): StateFlow<IndexBeanWrapper<SearchNativeAppUIData>?> =
        nativeAppChangedItem

    private fun subscribeQueryInput() {
        viewModelScope.launch {
            combine(
                refreshFlow,
                searchQuery,
                _searchType,
            ) { _, searchQuery, searchType ->
                mutableListOf<Any?>(searchQuery, searchType)
            }.debounce(500)
                .filterNotNull()
                .filter { it.isNotEmpty() }
                .collect { list ->
                    searchJob?.cancel()
                    val query = list[0]?.let { it as String } ?: ""
                    val type = list[1]?.let { it as Int } ?: SearchActivity.TYPE_DEFAULT

                    if (query.trim().isEmpty()) {
                        _viewState.emit(ViewState.Idle)
                        return@collect
                    }

                    searchList.clear()
                    _viewState.emit(ViewState.Loading)
                    searchJob = viewModelScope.launch {
                        try {
                            val resultList = mutableListOf<Any>()
                            val adaptiveApps = marketRepository.search(
                                query,
                                type.toString()
                            ).await()

                            if (type == SearchActivity.TYPE_SEARCH_RESULT) {
                                val nativeApps = mutableListOf<PackageBean>()
                                val quickApps = mutableListOf<QuickAppData>()
                                adaptiveApps.forEach { adaptiveAppData ->
                                    when (adaptiveAppData.type) {
                                        AdaptiveAppData.TYPE_LOCAL_APP -> {
                                            adaptiveAppData.toPackageBean()
                                                ?.let { nativeApps.add(it) }
                                        }

                                        AdaptiveAppData.TYPE_QUICK_APP -> {
                                            adaptiveAppData.toQuickAppData()
                                                ?.let { quickApps.add(it) }
                                        }
                                    }
                                }

                                if (nativeApps.isNotEmpty()) {
                                    val nativeAppDatas: List<SearchNativeAppUIData> =
                                        nativeApps.map { it.toAppStateBean() }
                                            .map {
                                                downloadHelper.checkAppDownloadStatus(it)
                                                SearchNativeAppUIData(
                                                    it,
                                                    true
                                                )
                                            }
                                    resultList.run {
                                        add(SearchHeaderUIData(context.getString(R.string.jdo_app_shop_search_result_local_apps)))
                                        addAll(nativeAppDatas)
                                    }
                                }
                                if (quickApps.isNotEmpty()) {
                                    val quickAppDatas: List<SearchQuickAppUIData> =
                                        quickApps.map { it.toQuickAppUIData() }
                                            .map {
                                                SearchQuickAppUIData(
                                                    it,
                                                    true
                                                )
                                            }
                                    resultList.run {
                                        add(SearchHeaderUIData(context.getString(R.string.jdo_app_shop_search_result_quick_apps)))
                                        addAll(quickAppDatas)
                                    }
                                }
                            } else {
                                adaptiveApps.forEach { adaptiveAppData ->
                                    when (adaptiveAppData.type) {
                                        AdaptiveAppData.TYPE_LOCAL_APP -> {
                                            adaptiveAppData.toPackageBean()
                                                ?.let { it.toAppStateBean() }
                                                ?.let {
                                                    downloadHelper.checkAppDownloadStatus(it)
                                                    SearchNativeAppUIData(
                                                        it,
                                                        false
                                                    )
                                                }
                                                ?.let {
                                                    resultList.add(it)
                                                }
                                        }

                                        AdaptiveAppData.TYPE_QUICK_APP -> {
                                            adaptiveAppData.toQuickAppData()
                                                ?.let { it.toQuickAppUIData() }
                                                ?.let {
                                                    SearchQuickAppUIData(
                                                        it,
                                                        false
                                                    )
                                                }
                                                ?.let {
                                                    if (it.data.quickAppType != QuickAppType.XM.name) {
                                                        resultList.add(it)
                                                    }
                                                }
                                        }
                                    }
                                }
                            }

                            viewModelScope.launch {
                                if (resultList.isEmpty()) {
                                    Logger.info(
                                        "searchList is empty"
                                    )
                                    _viewState.emit(ViewState.Empty)
                                } else {
                                    parseSearchUIDataList(type, resultList).let {
                                        Logger.info(
                                            "searchResultList size: ${it.size}"
                                        )
                                        _viewState.emit(ViewState.SearchList(it))
                                        searchList.addAll(it)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Logger.warning(e)
                            if (e is kotlinx.coroutines.CancellationException) {
                                Logger.warning("searchJob: ignore CancellationException")
                            } else {
                                _viewState.emit(ViewState.Error(e))
                            }
                        }
                    }
                }
        }
    }

    private fun parseSearchUIDataList(searchType: Int, searchUIDataList: List<Any>): List<Any> {
        val searchResult = arrayListOf<Any>()
        if (searchType == SearchActivity.TYPE_SEARCH) {
            searchResult.addAll(searchUIDataList.take(SearchActivity.SEARCH_SUGGEST_NUM).apply {
                forEach {
                    if (it is ISearchUI) {
                        it.layoutPosition = 0
                        it.isInTextMode = false
                    }
                }
            })
            searchResult.addAll(searchUIDataList.drop(SearchActivity.SEARCH_SUGGEST_NUM).apply {
                forEach {
                    if (it is ISearchUI) {
                        it.layoutPosition = 0
                        it.isInTextMode = true
                    }
                }
            })
        } else if (searchType == SearchActivity.TYPE_SEARCH_RESULT) {
            var headerIndex = 0
            searchUIDataList.forEachIndexed { index, item ->
                when (item) {
                    is SearchHeaderUIData -> {
                        headerIndex = index
                    }

                    is SearchNativeAppUIData -> {
                        item.layoutPosition =
                            (index - headerIndex - 1) % SearchActivity.getAppItemRowCount(buildParam.isAudi())
                        item.data.pos = index - headerIndex - 1
                        item.isInTextMode = false
                    }

                    is SearchQuickAppUIData -> {
                        item.layoutPosition =
                            (index - headerIndex - 1) % SearchActivity.getQuickAppItemRowCount(
                                buildParam.isAudi()
                            )
                        item.data.dataIndex = index - headerIndex
                        item.isInTextMode = false
                    }
                }
                searchResult.add(item)
            }
        }
        return searchResult
    }

    internal sealed class ViewState {
        data class SearchList(val apps: List<Any>) : ViewState()
        data class Error(val error: java.lang.Exception) : ViewState()
        object Loading : ViewState()
        object Empty : ViewState()
        object Idle : ViewState()
    }
}