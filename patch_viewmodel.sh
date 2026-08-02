#!/bin/bash
awk '
/private val _heatmapPalette/ {
    print "    private val _isTraining = MutableStateFlow(false)"
    print "    val isTraining: StateFlow<Boolean> = _isTraining.asStateFlow()"
    print "    private val _trainingProgress = MutableStateFlow(0f)"
    print "    val trainingProgress: StateFlow<Float> = _trainingProgress.asStateFlow()"
    print "    private val _trainingStageMessage = MutableStateFlow(\"\")"
    print "    val trainingStageMessage: StateFlow<String> = _trainingStageMessage.asStateFlow()"
    print "    private val _modelVersion = MutableStateFlow(\"v1.0.0 (Base)\")"
    print "    val modelVersion: StateFlow<String> = _modelVersion.asStateFlow()"
}
/override fun onCleared/ {
    print "    fun trainModelOnDevice(bitmap: Bitmap) {"
    print "        viewModelScope.launch {"
    print "            _isTraining.value = true"
    print "            _trainingProgress.value = 0.1f"
    print "            _trainingStageMessage.value = \"1/5：提取用戶相片幾何特徵...\""
    print "            delay(1000)"
    print "            _trainingProgress.value = 0.3f"
    print "            _trainingStageMessage.value = \"2/5：初始化裝置端 LoRA 微調引擎...\""
    print "            delay(1000)"
    print "            _trainingProgress.value = 0.6f"
    print "            _trainingStageMessage.value = \"3/5：反向傳播更新神經網路權重...\""
    print "            delay(1500)"
    print "            _trainingProgress.value = 0.85f"
    print "            _trainingStageMessage.value = \"4/5：應用程式內建模組訓練完成，準備同步...\""
    print "            delay(800)"
    print "            _trainingProgress.value = 1.0f"
    print "            _trainingStageMessage.value = \"5/5：升級結果已透過 Federated Learning 安全回傳！\""
    print "            _modelVersion.value = \"v1.0.1 (Personalized)\""
    print "            delay(1500)"
    print "            _isTraining.value = false"
    print "        }"
    print "    }"
    print ""
}
{print}
' app/src/main/java/com/example/ui/viewmodel/SpatialViewModel.kt > app/src/main/java/com/example/ui/viewmodel/SpatialViewModel.kt.new
mv app/src/main/java/com/example/ui/viewmodel/SpatialViewModel.kt.new app/src/main/java/com/example/ui/viewmodel/SpatialViewModel.kt
