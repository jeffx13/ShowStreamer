package com.showstreamer.controllers.show

import com.showstreamer.ShowStreamer
import com.showstreamer.controllers.MainController
import com.showstreamer.mpv.*
import com.showstreamer.parsers.Video
import com.showstreamer.utils.substringBetween
import com.showstreamer.utils.unixTime
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.ptr.LongByReference
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.binding.StringBinding
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.fxml.Initializable
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.BorderPane
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.stage.StageStyle
import java.awt.*
import java.io.File
import java.net.URL
import java.util.*
import kotlin.math.roundToInt
import kotlin.properties.Delegates


class MpvController:Initializable {

    lateinit var mpvBorderPane: BorderPane
    lateinit var mpvStage:Stage
    private val screenSize: Dimension = Toolkit.getDefaultToolkit().screenSize
    private val childStagePane=BorderPane()
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        initStage()
//        initmpvv2()

        setEventFilters()
        initMpvHistory()
    }
    //region mpv
    lateinit var mpv: MPV
    var handle by Delegates.notNull<Long>()
    var isInitialised = false
    lateinit var hwnd:WinDef.HWND
    lateinit var longByReference:LongByReference
    private fun command(vararg args:String){
        with(mpv.mpv_command(handle, arrayOf(*args))){
            if(this!=0){
                this.checkError()
                arrayOf(*args).forEach { print("$it ") }
            }
        }
    }
    private fun setProperty(name:String,value:String)=mpv.mpv_set_property_string(handle,name,value).checkError()
    private fun Int.checkError() = if(this!=0) MPV_ERROR.getName(this).also { println(it) } else null
    //endregion
    //region player properties
    val currentVideo= object:SimpleObjectProperty<Video?>(null){
        override fun asString(): StringBinding {
            return Bindings.`when`(this.isNull).then("Currently Playing").otherwise(
               Bindings.createStringBinding({"Currently Playing: ${this.value?.showName} ${this.value?.name}"},this)
            )
        }
    }
    private var loopShouldStop=false
    var currentSpeed=1.0f
    var isPausedProperty = SimpleBooleanProperty(false)
    private var isFullScreen=SimpleBooleanProperty(false)
    private var duration = SimpleDoubleProperty(0.0)
    private var timePos = SimpleDoubleProperty(0.0)
    private var isMuted =false
    private var volume = 50
    private var skipOP: Int? = null
    private var skipED: Int? = null
    private var prev_time_remaining =0
    var currentPlaylist= SimpleObjectProperty(mutableListOf<Video>())
    var currentIndex = SimpleIntegerProperty(-1)
    private var outroRange = SimpleObjectProperty<ClosedFloatingPointRange<Double>?>(null)
    private var introRange = SimpleObjectProperty<ClosedFloatingPointRange<Double>?>(null)
    private var lastLoop: Thread? = null
    //endregion
    //region stage setup
    private fun initStage(){

        mpvStage = Stage(StageStyle.TRANSPARENT)
        val scene = Scene(childStagePane, Color.BLACK)

        childStagePane.style="-fx-background-color:transparent;"
        val mpvLogo=ImageView(Image("/images/mpv_logo.png"))
        childStagePane.center=mpvLogo
        mpvLogo.fitHeightProperty().bind(mpvStage.heightProperty().divide(10))
        mpvLogo.fitWidthProperty().bind(mpvLogo.fitHeightProperty())

        mpvStage.apply {
            Platform.runLater {
                mpvStage.opacity=0.0
                scene.fill=Color.TRANSPARENT
                mpvStage.isResizable=false
                mpvBorderPane.prefWidthProperty().bind(mpvStage.widthProperty())
                mpvBorderPane.prefHeightProperty().bind(mpvStage.heightProperty())
                mpvStage.initOwner(ShowStreamer.primaryStage)
                mpvStage.title="MPV"
                mpvStage.scene = scene
                mpvStage.show()
                initMpv()
            }
                hide()
        }
    }

    private fun initMpv(){
        mpv = MPV.INSTANCE
        handle=mpv.mpv_create()
        hwnd = User32.INSTANCE.FindWindow(null, "MPV")
        longByReference = LongByReference(Pointer.nativeValue(hwnd.pointer))
        mpv.mpv_set_option(handle, "wid", MPV_FORMAT.INT64.id, longByReference.pointer).checkError()
        setMpvOptions()
        mpv.mpv_initialize(handle)
    }
    private fun setMpvOptions(){
//        mpv.mpv_set_option_string(handle, "config-dir", "D:\\Games\\MPV\\mpv").checkError()
//        mpv.mpv_set_option_string(handle, "config", "yes").checkError()
//        mpv.mpv_set_option_string(handle,"osd-level","3").checkError()
//        mpv.mpv_set_option_string(handle,"osd-border-size","100").checkError()
        mpv.mpv_set_option_string(handle, "keep-open", "yes").checkError()
        mpv.mpv_set_option_string(handle,"osd-border-color","#FF0000")
        mpv.mpv_set_option_string(handle,"osd-bar-align-x","-1").checkError()
        mpv.mpv_set_option_string(handle,"osd-bar-align-y","1").checkError()
        mpv.mpv_set_option_string(handle,"osd-bar-w","99.45").checkError()
        mpv.mpv_set_option_string(handle,"idle","yes").checkError()
        mpv.mpv_set_option_string(handle,"osd-font","Bitstream Vera Sans").checkError()
        mpv.mpv_set_option_string(handle,"osc","yes").checkError()
        mpv.mpv_set_option_string(handle, "input-conf", "D:\\Games\\MPV\\mpv\\input.conf").checkError()
        mpv.mpv_set_option_string(handle, "input-vo-keyboard", "yes").checkError()
        mpv.mpv_initialize(handle).checkError()
        //TODO() SET AUTOSKIP VALUE
        setVolume(volume)
    }
    fun hide(){
        Platform.runLater {
            mpvStage.opacity=0.0
            mpvBorderPane.requestFocus()
            mpvStage.width=0.0
            mpvStage.height=0.0
            mpvStage.isIconified=true
            mpvStage.x = -100.0
            mpvStage.y=-100.0
            mpvStage.scene.fill=Color.TRANSPARENT

            childStagePane.style="-fx-background-color: black;"
        }
    }
    fun show() {
        Platform.runLater {
            mpvBorderPane.requestFocus()
            mpvStage.isIconified=false
            rebasePlayer()
            mpvStage.opacity=1.0
        }
    }
    fun rebasePlayer(){
        val lVboxBounds= ShowStreamer.mainController.leftVbox.localToScreen(ShowStreamer.mainController.leftVbox.boundsInLocal)
        val rVboxBounds= ShowStreamer.mainController.rightVbox.localToScreen(ShowStreamer.mainController.rightVbox.boundsInLocal)
        mpvStage.x = lVboxBounds.maxX
        mpvStage.y = lVboxBounds.minY
        mpvStage.width=rVboxBounds.minX-lVboxBounds.maxX
        mpvStage.height=rVboxBounds.height
    }
    //endregion
    //region handle events
    private fun startEventLoop(index:Int) {
        loopShouldStop=false
        while(mpvStage.isIconified){
            Thread.sleep(1000)
            if(loopShouldStop){
                return
            }
        }
        command("playlist-play-index","$index")
        while (!loopShouldStop) {
//             println(mpv.mpv_get_property_string(handle,"start")?.getString(0))
            val event = mpv.mpv_wait_event(handle, 0.01)
            if (event?.event_id == MPV_EVENT.NONE.id) continue
            if (event != null) {
                event.error.checkError()
                val id = event.event_id
//                val event_name=MPV_EVENT.getName(id)
//                if(id !in (20..22))println(event_name)
                when(id){
                    MPV_EVENT.PROPERTY_CHANGE.id-> {if(handlePropertyChange(event))break}
                    MPV_EVENT.FILE_LOADED.id ->{}//if(skipOP!=null){seek(skipOP!!)}
                    MPV_EVENT.END_FILE.id -> {
                        val event_end_file = MPV_EVENT_END_FILE(event.data!!)
                        // println(MPV_END_FILE_REASON.getName(event_end_file.reason!!))
                    }
                    MPV_EVENT.SHUTDOWN.id -> break
                }
            }
        }
    }
    private fun setEventFilters(){
        Platform.runLater {
            ShowStreamer.primaryStage.addEventFilter(MouseEvent.MOUSE_PRESSED) {
                Platform.runLater {
                    if(!handleTabSwitch(it)){
                        if (checkBounds(it)) { mpvStage.fireEvent(it) }
                    }
                }
            }
            ShowStreamer.primaryStage.addEventFilter(MouseEvent.MOUSE_MOVED){
                Platform.runLater {
                    if(checkBounds(it)){
                        mpvStage.fireEvent(it)
                    }
                }

            }
            MainController.settingsController.autoSkipToggleGroup.selectedToggleProperty().addListener { observable, oldValue, newValue ->
                if(newValue == MainController.settingsController.autoSkipAlways){
                    skipOP=MainController.settingsController.autoskipOPEnd.text.toInt()
                    skipED=MainController.settingsController.autoskipED.text.toInt()
                }else{
                    skipOP=null
                    skipED=null
                }
            }
            mpvStage.addEventFilter(KeyEvent.KEY_PRESSED){handleKeyPress(it)}
            mpvStage.addEventFilter(MouseEvent.MOUSE_PRESSED){
                if(handleTabSwitch(it))return@addEventFilter
                if(it.clickCount==2){
                    setFullScreen(!isFullScreen.value)
                }else if(it.y>mpvStage.height*0.959){
                    val offset = if(isFullScreen.value)7.0 else 3.0
                    val percent=(it.x-offset)/(mpvStage.width-offset)*100
                    command("seek",percent.toString(),"absolute-percent","exact")
                }else{
                    playPause()
                }
            }
            mpvStage.addEventFilter(MouseEvent.MOUSE_MOVED) {
                command("osd-msg-bar", "show-progress")
                it.consume()
            }
            setBindings()
        }

    }
    private fun setBindings(){
        ShowStreamer.mainController.currentlyPlayingLabel.textProperty().bind(currentVideo.asString())
        currentVideo.addListener { observable, oldValue, newValue ->
            outroRange.value=newValue?.skipData?.outroEnd?.toDouble()?.let {
                currentVideo.value?.skipData?.outroBegin?.toDouble()?.rangeTo(
                    it
                )
            }
            introRange.value=newValue?.skipData?.introEnd?.toDouble()?.let {
                currentVideo.value?.skipData?.introBegin?.toDouble()?.rangeTo(
                    it
                )
            }
        }
        currentIndex.addListener { _, _, newValue ->
            Platform.runLater {
                try{
                    currentVideo.value=currentPlaylist.value[newValue.toInt()]
                }catch (e:Exception){
                    println("No index $newValue")}
            }
        }
    }
    private fun handleTabSwitch(mouseEvent: MouseEvent): Boolean {
        return when(mouseEvent.button){
            MouseButton.FORWARD->{mouseEvent.consume();ShowStreamer.mainController.nextTab();true}
            MouseButton.BACK ->{mouseEvent.consume();ShowStreamer.mainController.prevTab();true}
            else -> {false}
        }
    }
    private fun checkBounds(mouseEvent:MouseEvent): Boolean {
        return (mouseEvent.screenX>mpvStage.x
                && mouseEvent.screenX<mpvStage.x+mpvStage.width
                && mouseEvent.screenY>mpvStage.y
                && mouseEvent.sceneY<mpvStage.y+mpvStage.height
                && MainController.currentScene==MainController.mpvScene)
    }
    private fun handlePropertyChange(event: MPV.mpv_event): Boolean {
        if(loopShouldStop)return true
        if(event.data != null){
            val event_property = MPV.mpv_event_property(event.data!!)
            val data = event_property.data
//            println(event_property.name)
            when(event_property.name){
                "duration"->{duration.value=data?.getDouble(0);}
                "time-pos"->{timePos.value=data?.getDouble(0);}
                "pause" -> isPausedProperty.value=data?.getInt(0)?.toBoolean()?.not()
                "time-remaining"->{
                    data?.getDouble(0)?.roundToInt()?.let {
                        handleTime(it)
                    }
                }
                "playlist-pos"->{
//                    println("playlist: "+data?.getInt(0));
                    Platform.runLater {
                        currentIndex.value=data?.getInt(0)
                    }
                }
                else->{}
            }
        }
        return false
    }
    private fun Int.toBoolean()= this==1
    private fun handleKeyPress(keyEvent:KeyEvent){
        when(keyEvent.code){
            KeyCode.RIGHT -> {seek(5.0);return}
            KeyCode.LEFT -> {seek(-5.0);return}
            KeyCode.UP -> {addVolume(5);return}
            KeyCode.DOWN -> {addVolume(-5);return}
            KeyCode.Q -> {addVolume(5);return}
            KeyCode.A -> {addVolume(-5);return}
            KeyCode.X -> {if(!keyEvent.isControlDown) seek(5.0) else{changePlaylistPos(1)};return}
            KeyCode.Z -> {if(!keyEvent.isControlDown)seek(-5.0) else {changePlaylistPos(-1)};return}
            KeyCode.F -> {setFullScreen(!isFullScreen.value);return}
            KeyCode.ESCAPE -> {setFullScreen(false);return}
            KeyCode.M -> {setMute(!isMuted);return}
            KeyCode.D -> {if(!keyEvent.isControlDown)addSpeed(0.1f)else seek(90.0);return}
            KeyCode.S -> {if(!keyEvent.isControlDown)addSpeed(-0.1f)else seek(-90.0);return}
            KeyCode.R -> {if(currentSpeed!=1.0f) setSpeed(1.0f)else setSpeed(2.0f);return}
            KeyCode.TAB -> {command("script-message","osc-playlist");return}
            KeyCode.SPACE -> {playPause();return}
            else -> {}
        }
        var key = if(keyEvent.isControlDown){
            "CTRL+"} else if(keyEvent.isAltDown) {
            "ALT+"} else if(keyEvent.isShiftDown){
            "SHIFT+" }else{""}
        key+=when(keyEvent.code){
            KeyCode.DIGIT0 -> "0"
            KeyCode.DIGIT1 -> "1"
            KeyCode.DIGIT2 -> "2"
            KeyCode.DIGIT3 -> "3"
            KeyCode.DIGIT4 -> "4"
            KeyCode.DIGIT5 -> "5"
            KeyCode.DIGIT6 -> "6"
            KeyCode.TAB -> "TAB"
            else -> return
        }
        keyEvent.consume()
        command("keypress",key)
    }
    private fun handleTime(time_remaining:Int): Boolean {
        if (introRange.value != null || outroRange.value != null) {
            if(introRange.value!!.contains(timePos.value)){
                seek(introRange.value!!.endInclusive,true)
                return true
            }
            if(outroRange.value!!.contains(timePos.value)){
                seek(outroRange.value!!.endInclusive,true)
                return true
            }
        }
        skipOP?.let {
            if (timePos.value < it) {
                seek(skipOP!!.toDouble(), true)
                return true
            }
        }
        skipED?.let {
            if(time_remaining < it){
                changePlaylistPos(1)
                return true
            }
        }
        return false
    }
    //endregion
    //region playback control
    private fun setVolume(volume:Int){
        this.volume=volume
        setProperty("volume","$volume")
    }
    private fun addVolume(addedVolume:Int){
        volume+=addedVolume
        command("osd-msg","add","volume","$addedVolume")
    }
    private fun seek(pos:Double, absolute:Boolean=false){
        if(absolute){
            command("seek",pos.toString(),"absolute","exact")
        }else{
            command("seek",pos.toString(),"relative","exact")
        }
    }
    private fun setFullScreen(fullscreen:Boolean){
        if(!fullscreen){
            rebasePlayer()
        }else{
            mpvStage.x = 0.0
            mpvStage.y=0.0
            mpvStage.width = screenSize.width.toDouble()
            mpvStage.height = screenSize.height.toDouble()

        }
        isFullScreen.value=fullscreen
    }
    fun playPause(){
        if(currentVideo.value==null)return
        isPausedProperty.value=!isPausedProperty.value
        command("cycle","pause")
    }
    private fun setPause(pause: Boolean)=setProperty("pause",if(pause) "yes" else "no")
    private fun setMute(mute: Boolean) = setProperty("mute",if(mute)"yes" else "no").also { isMuted=mute }
    private fun validateSpeed(speed:Float) = !(0<speed && speed<100)
    private fun setSpeed(speed:Float){
        if(validateSpeed(speed))return
        currentSpeed=speed
        setProperty("speed",speed.toString())
        showText("Speed: $speed")
    }
    private fun addSpeed(addedSpeed:Float){
        if(validateSpeed(currentSpeed+addedSpeed))return
        currentSpeed+=addedSpeed
        command("osd-msg","add","speed","$addedSpeed")
    }
    private fun showText(text:String)=command("show-text", text)
    fun stop(){
        loopShouldStop=true
        lastLoop?.join()
        println("stopped loop")
        mpv.mpv_terminate_destroy(handle)
    }
    var lastChangedPlaylistPos=0L
    fun changePlaylistPos(pos:Int, absolute:Boolean=false){
        if(unixTime-lastChangedPlaylistPos<1)return
        lastChangedPlaylistPos= unixTime
        if(absolute && validatePlaylistIndex(pos)){
            command("playlist-play-index","$pos")
        }else{
            if(validatePlaylistIndex(currentIndex.value+pos)){
                if(pos<0) command("playlist-prev") else command("playlist-next")
            }
        }
    }
    private fun validatePlaylistIndex(pos:Int) = pos<currentPlaylist.value.size && pos >= 0
    //endregion
    //region load playlist
    fun loadPlaylist(videos:List<Video>, index:Int=0){
        loadedFromLocal.value=false
        currentPlaylist.value.clear()
        currentPlaylist.value.addAll(videos)
        command("playlist-play-index","none")
        command("playlist-clear")
        videos.forEachIndexed { i,video->
            command("loadfile",video.url,"append")
        }

        Platform.runLater {
//            println(index)
//            currentIndex.value=index
//            currentVideo.value=currentPlaylist.value[index]
        }
        mpv.mpv_observe_property(handle, 0, "time-pos", MPV_FORMAT.DOUBLE.id)
        mpv.mpv_observe_property(handle, 0, "duration", MPV_FORMAT.DOUBLE.id)
        mpv.mpv_observe_property(handle, 0, "playtime-remaining", MPV_FORMAT.DOUBLE.id)
        mpv.mpv_observe_property(handle, 0, "time-remaining", MPV_FORMAT.DOUBLE.id)
        mpv.mpv_observe_property(handle, 0, "eof", MPV_FORMAT.FLAG.id)
        mpv.mpv_observe_property(handle, 0, "eof-reached", MPV_FORMAT.FLAG.id)
        mpv.mpv_observe_property(handle, 0, "pause", MPV_FORMAT.FLAG.id)
        mpv.mpv_observe_property(handle,0,"playlist-pos", MPV_FORMAT.INT64.id)

        loopShouldStop=true
        lastLoop?.join()
        isPausedProperty.value=true
        loopShouldStop=false
        lastLoop = Thread{startEventLoop(index)}
        lastLoop!!.start()
    }
    fun loadPlaylistFromFolder(){
        val directoryChooser=FileChooser()
        directoryChooser.initialDirectory = File("D:\\TV")
        val newPath = directoryChooser.showOpenDialog(ShowStreamer.primaryStage) ?: return
        val directory=newPath.parent//"D:\\TV\\temp\\wushangshendi"
        val folder = File(directory).listFiles { _, name -> name.endsWith(".mp4") } ?: return
        folder.sortBy {
            val ep=if(it.name.startsWith("Episode")){
                it.name.substringBetween("Episode ",":").toIntOrNull()
            }else{it.name.toIntOrNull()}
            ep
        }
        val videos= folder.map {
            val name=if(it.name.startsWith("Episode"))it.name.replaceFirst(".",":") else it.name
            Video(
                name.substringBefore(".mp4"),
                it.parent.substringAfterLast("\\"),
                it.absolutePath,
                "",
                true,
                quality = ""
            )
        }.toMutableList()

        mpvHistory=File("$directory\\.mpv.history")
        var index=0
        if(mpvHistory!!.exists()){
            index=videos.indexOfFirst { it.url.endsWith(mpvHistory!!.readText().trim()) }
        }
        loadPlaylist(videos,index)
        ShowStreamer.mainController.mpvBtn.fire()
        loadedFromLocal.value=true
    }
    private fun initMpvHistory(){
        val mpvHistoryListener=ChangeListener<Video?>(){ _, _, newValue ->
            if(newValue!=null){
                Platform.runLater{
                    mpvHistory!!.writeText(newValue.name+".mp4")
                }
            }
        }
        val loadedFromLocalFileListener = ChangeListener<Boolean>(){ _, _, newValue ->
            Platform.runLater {
                if(newValue){
                    currentVideo.addListener(mpvHistoryListener)
                }else{
                    currentVideo.removeListener(mpvHistoryListener)
                }
            }
        }

        mpvHistoryEnabled.addListener { _, _, newValue ->
            Platform.runLater {
                if(newValue){
                    loadedFromLocal.addListener(loadedFromLocalFileListener)
                }else{
                    loadedFromLocal.removeListener(loadedFromLocalFileListener)
                    currentVideo.removeListener(mpvHistoryListener)
                }
            }
        }
        mpvHistoryEnabled.value=true
    }
    //endregion

    var mpvHistory:File? = null
    var mpvHistoryEnabled = SimpleBooleanProperty(false)
    private var loadedFromLocal = SimpleBooleanProperty(false)

}
