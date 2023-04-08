package com.showstreamer.parsers


import com.showstreamer.parsers.watch.SEpisode
import com.showstreamer.parsers.watch.ShowProvider
import com.showstreamer.parsers.watch.SkipData
import com.showstreamer.parsers.watch.TvType
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.Text

enum class SearchType{
    LATEST,POPULAR,SEARCH,WATCHLIST
}
enum class Status{ Ongoing,Completed,Unknown,Upcoming }
enum class ReadType{Manga,Novel}


data class SearchHistory(var type: SearchType?, var tvType: TvType?, var page:Int?, var text:String?=null){
    var readType: ReadType? = null;
}
enum class ListType(val type:String){
    CURRENT("Current"),
    COMPLETED("Completed"),
    ONHOLD("On Hold"),
    DROPPED("Dropped"),
    NONE("Not Watched")
}

data class Video(
    var name: String = "",
    var showName: String = "",
    var url: String,
    var referer: String? = null,
    var isM3u8: Boolean = true,
    var hasAutoSkip: Boolean = false,
    var skipData: SkipData?=null,
    var quality: String? = null,
    var downloadProgress: SimpleDoubleProperty = SimpleDoubleProperty(0.0),
    var shouldStopDownloading: SimpleBooleanProperty = SimpleBooleanProperty(false)
)

class InfoListCell:ListCell<String>(){
    val labelStyle="-fx-background-color: transparent;-fx-text-fill:white;-fx-font-size:15;"
    val infoType=Label().apply { minWidth=80.0;style=labelStyle }
    val info=Label().apply { isWrapText=true;style=labelStyle }
    val hBox = HBox().apply { style=labelStyle }
    init {
        isMouseTransparent=true
        isFocusTraversable=false
        hBox.children.addAll(infoType,info)
        style=labelStyle
    }
    override fun updateItem(item: String?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            text = null
            graphic = null
        } else {
            hBox.prefWidthProperty().bind(this.listView.widthProperty().multiply(0.8))
            this.prefWidthProperty().bind(this.listView.widthProperty())
            infoType.text = item.substringBefore("||").trim(' ')
            info.text = item.substringAfterLast("||").trim(' ')
            graphic=hBox
        }
    }
}
class ProviderListCell:ListCell<ShowProvider>() {
    override fun updateItem(item: ShowProvider?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            text = null
            graphic = null
        } else {
            text = item.name
        }
    }
}
class EpisodeListCell:ListCell<SEpisode>() {
    private val DEFAULT_COLOR="-fx-background-color: black; -fx-text-fill: white;"
    private val SELECTED_COLOR="-fx-background-color: blueviolet; -fx-text-fill: white"
    private var lastItem: SEpisode?=null
    private val selectedProperty = SimpleBooleanProperty(false)
    private val episodeNameText = Text()
    init {
        selectedProperty.addListener{ _, _, newValue ->
            style = if(newValue) SELECTED_COLOR else DEFAULT_COLOR
        }
        setOnMouseClicked {
            if(item!=null){
                item.isSelected.value = !item.isSelected.value
            }
        }

        episodeNameText.font=Font.font("Times New Roman",15.0)
        episodeNameText.fill=Color.WHITE
    }


    override fun updateItem(item: SEpisode?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            text = null
            graphic = null
            style = DEFAULT_COLOR
            if (lastItem != null){
                selectedProperty.unbind()
            }
        } else {
            selectedProperty.bind(item.isSelected)
            episodeNameText.wrappingWidthProperty().bind(listView.widthProperty().subtract(15))


            if("Episode \\d+".toRegex().find(item.episodeName)!=null){
                episodeNameText.text = item.episodeName.replaceFirst(": ",":\n")
            }else{episodeNameText.text = item.episodeName}
            graphic=episodeNameText

            lastItem = item

        }
    }
}
