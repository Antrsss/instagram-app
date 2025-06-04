package com.example.instagramapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.instagramapp.models.Profile
import com.example.instagramapp.repos.ProfileRepository
import com.example.instagramapp.repos.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Empty)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    fun searchProfiles(query: String) {
        viewModelScope.launch {
            _searchState.value = SearchState.Loading
            try {
                val profiles = profileRepository.searchProfiles(query)
                _searchState.value = if (profiles.isEmpty()) {
                    SearchState.Empty
                } else {
                    SearchState.Success(profiles)
                }
            } catch (e: Exception) {
                _searchState.value = SearchState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class SearchState {
    object Empty : SearchState()
    object Loading : SearchState()
    data class Success(val profiles: List<Profile>) : SearchState()
    data class Error(val message: String) : SearchState()
}