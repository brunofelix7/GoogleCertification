package me.brunofelix.googlecertapp.ui.taskdetails

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.brunofelix.googlecertapp.data.Task
import me.brunofelix.googlecertapp.data.TaskRepository
import me.brunofelix.googlecertapp.extensions.cancelNotification
import me.brunofelix.googlecertapp.extensions.cancelWorker
import me.brunofelix.googlecertapp.extensions.scheduleWorker
import me.brunofelix.googlecertapp.utils.AppConstants
import me.brunofelix.googlecertapp.utils.AppProvider

class TaskDetailsViewModel constructor(
    private val repository: TaskRepository,
    private val dispatcher: CoroutineDispatcher,
    private val provider: AppProvider
): ViewModel() {

    private val _liveData = MutableLiveData<TaskDetailsUiState>()
    val liveData: LiveData<TaskDetailsUiState> get() = _liveData

    fun findTaskById(id: Long) {
        viewModelScope.launch(dispatcher) {
            val task = repository.findById(id)

            if (task != null) {
                withContext(Dispatchers.Main) {
                    _liveData.value = TaskDetailsUiState.OnFound(task)
                }
            } else {
                withContext(Dispatchers.Main) {
                    _liveData.value = TaskDetailsUiState.Error(AppConstants.NOT_FOUND_ERROR)
                }
            }
        }
    }

    fun updateTask(task: Task) {
        _liveData.value = TaskDetailsUiState.Loading

        viewModelScope.launch(dispatcher) {
            val result = repository.insert(task)

            if (result > 0) {
                provider.context().cancelNotification(notificationId = task.id.toInt())
                provider.context().cancelWorker(workerTag = task.id)
                provider.context().scheduleWorker(task = task, workerTag = result)

                withContext(Dispatchers.Main) {
                    _liveData.value = TaskDetailsUiState.OnUpdated
                }
            } else {
                withContext(Dispatchers.Main) {
                    _liveData.value = TaskDetailsUiState.Error(AppConstants.GENERIC_ERROR)
                }
            }
        }
    }

    fun deleteTask(task: Task) {
        _liveData.value = TaskDetailsUiState.Loading

        viewModelScope.launch(dispatcher) {
            repository.delete(task)

            provider.context().cancelWorker(workerTag = task.id)
            provider.context().cancelNotification(notificationId = task.id.toInt())

            withContext(Dispatchers.Main) {
                _liveData.value = TaskDetailsUiState.OnDeleted
            }
        }
    }
}
