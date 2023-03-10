package com.kunkunapp.allvideodowloader.viewModel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.kunkunapp.allvideodowloader.database.AppDatabase
import com.kunkunapp.allvideodowloader.database.Download
import com.kunkunapp.allvideodowloader.database.DownloadsRepository
import com.kunkunapp.allvideodowloader.work.DeleteWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {

    val loadState: MutableLiveData<DownloadState> = MutableLiveData(DownloadState.INIT)

     fun updateLoading(loadState: DownloadState) {
        this.loadState.postValue(loadState)
    }

    private val repository: DownloadsRepository
    val allDownloads: LiveData<List<Download>>

    init {
        val downloadsDao = AppDatabase.getDatabase(application).downloadsDao()
        repository = DownloadsRepository(downloadsDao)
        allDownloads = repository.allDownloads
    }

    fun insert(word: Download) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(word)
    }

    fun update(word: Download) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(word)
    }

    fun delete(word: Download) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(word)
    }

     fun startDelete(id: Long, context: Context) {
        val workTag = "tag_$id"
        val workManager = WorkManager.getInstance(context.applicationContext!!)
        val state = workManager.getWorkInfosByTag(workTag).get()?.getOrNull(0)?.state

        if (state === WorkInfo.State.RUNNING || state === WorkInfo.State.ENQUEUED) {
            return
        }

        val workData = workDataOf(
            DeleteWorker.fileIdKey to id
        )
        val workRequest = OneTimeWorkRequestBuilder<DeleteWorker>()
            .addTag(workTag)
            .setInputData(workData)
            .build()

        workManager.enqueueUniqueWork(
            workTag,
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

     fun viewContent(path: String, context: Context) {
        val intent = Intent(Intent.ACTION_VIEW)
        val uri = Uri.parse(path)
        val downloadedFile = DocumentFile.fromSingleUri(context, uri)!!
        if (!downloadedFile.exists()) {
            Toast.makeText(context, "file not found", Toast.LENGTH_SHORT).show()
            return
        }
        val mimeType = context.contentResolver.getType(uri) ?: "*/*"
        intent.setDataAndType(uri, mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (intent.resolveActivity(context.packageManager) != null) {
            ContextCompat.startActivity(context, intent, null)
        } else {
            Toast.makeText(context, "app not found", Toast.LENGTH_SHORT).show()
        }
    }


}

enum class DownloadState {
    INIT, DOWNLOADING, COMPLETED, CANCELED, FAILED
}