package com.ntg.lmd.mainscreen.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntg.lmd.mainscreen.domain.model.ActiveUser
import com.ntg.lmd.mainscreen.domain.usecase.GetActiveUsersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AgentsState(
    val isLoading: Boolean = false,
    val agents: List<ActiveUser> = emptyList(),
    val error: String? = null,
    val currentUserId: String? = null,
)

class ActiveAgentsViewModel(
    private val getActiveUsers: GetActiveUsersUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(AgentsState())
    val state: StateFlow<AgentsState> = _state

    fun load() {
        if (_state.value.isLoading) return
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val (list, currentId) = getActiveUsers()
                _state.value =
                    AgentsState(
                        isLoading = false,
                        agents = list,
                        currentUserId = currentId,
                    )
            } catch (t: Throwable) {
                _state.value = _state.value.copy(isLoading = false, error = t.message ?: "Failed")
            }
        }
    }
}
