package com.dressed.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dressed.app.DressedApplication
import com.dressed.app.data.BorrowedLibraryRepository
import com.dressed.app.data.local.BorrowedItemEntity
import com.dressed.app.data.local.BorrowedLibraryListRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibrariesViewModel(
    application: Application,
    private val borrowedLibraryRepository: BorrowedLibraryRepository,
) : AndroidViewModel(application) {

    val libraryRows: Flow<List<BorrowedLibraryListRow>> =
        borrowedLibraryRepository.observeLibraryListRows()

    fun itemsForLibrary(libraryId: String): Flow<List<BorrowedItemEntity>> =
        borrowedLibraryRepository.observeItems(libraryId)

    private val _pendingOpenImportedLibrary = MutableStateFlow<Pair<String, String>?>(null)
    val pendingOpenImportedLibrary: StateFlow<Pair<String, String>?> =
        _pendingOpenImportedLibrary.asStateFlow()

    fun consumePendingOpenImportedLibrary() {
        _pendingOpenImportedLibrary.value = null
    }

    fun importLibraryFromUri(uri: Uri, onDone: (errorMessage: String?) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                borrowedLibraryRepository.importFromUri(getApplication(), uri)
            }
            result.fold(
                onSuccess = { outcome ->
                    _pendingOpenImportedLibrary.value = outcome.libraryId to outcome.sharerName
                    onDone(null)
                },
                onFailure = { e -> onDone(e.message ?: "Import failed") },
            )
        }
    }

    fun removeLibrary(libraryId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                borrowedLibraryRepository.removeLibrary(libraryId)
            }
        }
    }

    companion object {
        fun factory(app: DressedApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass == LibrariesViewModel::class.java)
                    return LibrariesViewModel(app, app.borrowedLibraryRepository) as T
                }
            }
    }
}
