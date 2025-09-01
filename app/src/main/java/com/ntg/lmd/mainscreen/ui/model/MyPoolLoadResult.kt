package com.ntg.lmd.mainscreen.ui.model

sealed class MyPoolLoadResult {
    data object Appended : MyPoolLoadResult()

    data class EndReached(
        val pageAt: Int,
    ) : MyPoolLoadResult()

    data class NoChange(
        val pageAt: Int,
    ) : MyPoolLoadResult()

    data class Error(
        val throwable: Throwable,
    ) : MyPoolLoadResult()
}
