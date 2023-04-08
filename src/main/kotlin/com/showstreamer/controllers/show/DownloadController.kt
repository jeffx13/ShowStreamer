package com.showstreamer.controllers.show

import com.showstreamer.utils.Downloader
import com.showstreamer.ShowStreamer
import com.showstreamer.controllers.MainController
import com.showstreamer.parsers.Video
import impl.org.controlsfx.skin.TaskProgressViewSkin
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.collections.ListChangeListener
import javafx.concurrent.Task
import javafx.concurrent.WorkerStateEvent
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.Initializable
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import org.controlsfx.control.TaskProgressView
import org.json.JSONObject
import org.kordamp.ikonli.javafx.FontIcon
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URL
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


class DownloadController() :Initializable {

    lateinit var playButton: Button
    lateinit var downloadButton: Button
    lateinit var cancelAllButton: Button
    lateinit var openDirectoryButton: Button
    lateinit var browseDirectoryButton: Button
    private val urlRegex = """((http|https)://([\w_-]+(?:(?:\.[\w_-]+)+))([\w.,@?^=%&:/~+#-]*[\w@?^=%&/~+#-])?)""".toRegex()
    private val refererRegex = "[rR]eferer:(.*?)\"".toRegex()
    //region FXML
    lateinit var downloadTaskProgressView: TaskProgressView<DownloadTask>
    lateinit var downloadTabPlayButton: Button
    lateinit var directDownloadButton: Button
    lateinit var directDownloadRefererTextField: TextField
    lateinit var urlPasteButton: Button
    lateinit var directDownloadUrlTextField: TextField
    lateinit var directDownloadDirectoryBrowseButton: Button
    lateinit var directDownloadDirectoryOpenButton: Button
    lateinit var directDownloadDirectoryTextField: TextField
    lateinit var directDownloadFileNameTextField: TextField
    lateinit var directDownloadFolderNameTextField: TextField
    //endregion
    val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
    fun addToDownloadQueue(videos:List<Video>){
        videos.forEach {
            Platform.runLater {
                val task = DownloadTask(it)
                executor.submit(task)
                downloadTaskProgressView.tasks.add(task)
            }
        }
    }
    fun onUrlPaste(event: ActionEvent?) {
        if(clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)){
            val contents = clipboard.getData(DataFlavor.stringFlavor).toString()
            if(contents.startsWith('{')){
                val json = JSONObject(contents)
                directDownloadFileNameTextField.text=json.getString("site").substringAfterLast("-")
                directDownloadUrlTextField.text = json.getString("url")
                directDownloadRefererTextField.text = json.getString("referer")
                directDownloadFolderNameTextField.text = json.getString("title")
            }else{
                if(event?.source==urlPasteButton){
                    val url = urlRegex.find(contents)?.value
                    directDownloadUrlTextField.text = url ?: contents
                }

            }
        }
    }
    fun onRefererPaste() {
        val contents:String
        try { contents=clipboard.getData(DataFlavor.stringFlavor).toString()
        }catch (e: UnsupportedFlavorException){ return }
        val foundReferer=refererRegex.find(contents)?.groupValues?.get(1)
        directDownloadRefererTextField.text= foundReferer ?: ""
    }
    fun setPoolSize(newSize:Int){
        val size = if(newSize>5)5 else newSize
        executor.corePoolSize = size
        executor.maximumPoolSize = size
    }
    val executor = ThreadPoolExecutor(3,3,60,
        TimeUnit.SECONDS, LinkedBlockingQueue() )
    private fun parseInputs(): Video {
        val url =directDownloadUrlTextField.text
        directDownloadButton.isDisable = true
        var showName = directDownloadFolderNameTextField.text
        var episodeName = directDownloadFileNameTextField.text
        if (showName.isEmpty()) showName = "Sine nomine"
        if (episodeName.isEmpty()) episodeName = (1..5).map { (0..9).random() }.joinToString("")
        return Video(episodeName, showName, url, directDownloadRefererTextField.text, true, quality = null)
    }
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        directDownloadDirectoryTextField.text= MainController.downloadWorkDir
        initTaskProgressView()
        setButtonActions()
    }
    private fun initTaskProgressView(){
        val taskHandler: EventHandler<WorkerStateEvent> = EventHandler<WorkerStateEvent> { evt ->
            val source=evt.source
            when(evt.eventType){
                WorkerStateEvent.WORKER_STATE_SUCCEEDED ->{downloadTaskProgressView.tasks.remove(source)}
                WorkerStateEvent.WORKER_STATE_CANCELLED->{downloadTaskProgressView.tasks.remove(source)}
                WorkerStateEvent.WORKER_STATE_FAILED->{downloadTaskProgressView.tasks.remove(source)}
            }
        }
        downloadTaskProgressView.tasks.addListener(ListChangeListener { c ->
            while (c.next()) {
                if (c.wasAdded()) {
                    for (task in c.addedSubList) {
                        task.addEventHandler<WorkerStateEvent>(
                            WorkerStateEvent.ANY,
                            taskHandler
                        )
                    }
                } else if (c.wasRemoved()) {
                    for (task in c.addedSubList) {
                        task.removeEventHandler<WorkerStateEvent>(
                            WorkerStateEvent.ANY,
                            taskHandler
                        )
                    }
                }
            }
        })
        downloadTaskProgressView.skin=object: TaskProgressViewSkin<DownloadTask>(downloadTaskProgressView){
            init {
                this.skinnable.tasks
                val borderPane = BorderPane()
                borderPane.styleClass.add("box")

                val listView: ListView<DownloadTask> = ListView()
                listView.setPrefSize(500.0, 400.0)
                listView.placeholder = Label("No tasks running")
                listView.setCellFactory { TaskCell() }
                listView.isFocusTraversable = false

                Bindings.bindContent(listView.items, skinnable.tasks)
                listView.items.addListener(ListChangeListener {
                    it.next()
                })
                borderPane.center = listView

                children.add(listView)
            }
        }
    }
    private fun setButtonActions(){
        browseDirectoryButton.setOnAction { MainController.browseDirectory(directDownloadDirectoryTextField.text) }
        openDirectoryButton.setOnAction { MainController.openDirectory(directDownloadDirectoryTextField.text) }
        downloadButton.setOnAction {
            val video = parseInputs()
            addToDownloadQueue(listOf(video))
        }
        playButton.setOnAction {
            MainController.mpvController.loadPlaylist(listOf(parseInputs()))
        }
        cancelAllButton.setOnAction {
            downloadTaskProgressView.tasks.forEach {
                Platform.runLater{
                    it.cancel()
                }
            }
        }
    }


}
class TaskCell : ListCell<DownloadTask?>() {
    private val progressBar: ProgressBar = ProgressBar()
    private val titleText: Label = Label().apply {style="-fx-font-size:14;-fx-text-fill:white;" }
    private val messageText: Label = Label().apply {style="-fx-font-size:14;-fx-text-fill:white;" }
    private val cancelButton: Button
    private var task: DownloadTask? = null
    private val borderPane: BorderPane
    init {
        progressBar.maxWidth = Double.MAX_VALUE
        progressBar.maxHeight = 8.0
        style="-fx-background-color:transparent;"
        cancelButton = Button("Cancel")
        cancelButton.tooltip = Tooltip("Cancel Task")
        cancelButton.setOnAction { evt ->
            task?.cancel()
        }
        val vbox = VBox()
        vbox.spacing = 4.0
        vbox.children.add(titleText)
        vbox.children.add(progressBar)
        vbox.children.add(messageText)
        BorderPane.setAlignment(cancelButton, Pos.CENTER)
        BorderPane.setMargin(cancelButton, Insets(0.0, 0.0, 0.0, 4.0))
        borderPane = BorderPane()
        borderPane.center = vbox
        borderPane.right = cancelButton
        contentDisplay = ContentDisplay.GRAPHIC_ONLY
    }
    override fun updateItem(task: DownloadTask?, empty: Boolean) {
        super.updateItem(task, empty)
        this.task = task
        if (empty || task == null) {
            graphic = null
        } else {
            progressBar.progressProperty().bind(task.progressProperty())
            titleText.text="${task.video.showName} ${task.video.name}"
            messageText.prefWidthProperty().bind(listView.widthProperty())
            messageText.textProperty().bind(task.messageProperty())
            cancelButton.disableProperty().bind(
                Bindings.not(task.runningProperty())
            )

            borderPane.left = null

            graphic = borderPane
        }
    }
}
class DownloadTask(val video: Video):Task<Int>(){
    private var process: Process? = null
    private lateinit var folder:File
    private var videoFile: File? = null
    override fun call(): Int {
        val folderName = "${MainController.downloadWorkDir}/${video.showName.replace(':',' ')}"
        if(File("${folderName}/${video.name}.mp4").exists()){
            return 1
        }
        folder=File("${folderName}/${video.name}")
        if(!folder.isDirectory){
            folder.mkdir()
        }
        process = Downloader.download(folderName,video) ?: return 0

        videoFile=File("${folderName}/${video.name}")
        val reader = BufferedReader(InputStreamReader(process!!.inputStream))
        var messageLine = ""
        val retryCounts = 3
        try{
            while(process!!.isAlive){
                messageLine = reader.readLine() ?: continue
                if(messageLine=="Retry Count $retryCounts/15"){destroyProcess();break}
                if(messageLine.isEmpty() || !messageLine[0].isDigit())continue
                messageLine=messageLine.substring(13)
                updateMessage(messageLine)
                if (messageLine.contains("B/s @")){
                    val percent=(messageLine.split('(')[1].split('%')[0]).toDouble()
                    updateProgress(percent,100.0)
                }
            }
        }catch (e:Exception){
            println(e)
        }
        return 1
    }
    private fun destroyProcess(){
        process?.destroyForcibly()
        if(videoFile?.isDirectory == true)videoFile?.deleteRecursively()
    }
    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        destroyProcess()
        return super.cancel(mayInterruptIfRunning)
    }
}












































class DownloadQueueListCell : ListCell<Video>(){
    private val cancelIcon = FontIcon("mdoal-cancel_presentation").apply {  iconSize=20;iconColor= Color.WHITE }
    private val progressBar= ProgressBar(0.0).apply { padding = Insets(5.0) }
    private val cancelButton = Button("Cancel").apply { graphic= cancelIcon }
    private val borderPane = BorderPane()
    private val itemLabel = Label()

    init {
        borderPane.right=cancelButton
        borderPane.bottom=progressBar
        borderPane.left=itemLabel
        borderPane.prefWidthProperty().bind(this.widthProperty())
        background= Background(BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY))
//        itemLabel.style="-fx-"
    }
    override fun updateItem(item: Video?, empty: Boolean) {
        super.updateItem(item, empty)
        if(empty || item == null){
            text=null
            graphic=null
        }else{
            prefWidthProperty().bind(this.listView.widthProperty().divide(1.1))
            borderPane.prefWidthProperty().bind(this.listView.widthProperty().divide(1.1))
            progressBar.progressProperty().bind(item.downloadProgress)
            itemLabel.text= "${item.showName} ${item.name}"

            graphic=borderPane
            cancelButton.setOnAction { item.shouldStopDownloading.value=true
                listView.items.remove(item)
            }

        }
    }
}