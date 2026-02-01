package com.example.forenscan.data.models.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.net.Uri
import com.example.forenscan.data.models.viewmodel.HistoryItem

class FSViewModel : ViewModel() {

    // 2. Add 'history' for the Attack Reconstruction list
    val history = MutableLiveData<List<HistoryItem>>()

    // 3. Add 'isExportEnabled' for the Timeline toggle
    val isExportEnabled = MutableLiveData<Boolean>(false)

    // 4. Add 'timelineExportStatus' for the Share Sheet
    val timelineExportStatus = MutableLiveData<Uri?>()

    fun setExportFeature(status: Boolean) {
        isExportEnabled.value = status
    }

    fun exportTimeline(format: String) {
        // Logic for triggering file generation
    }
}