package com.showstreamer.controllers

import com.showstreamer.parsers.watch.ShowResponse
import javafx.fxml.Initializable
import javafx.geometry.Pos
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import java.net.URL
import java.util.*

class HomeController:Initializable {

    lateinit var bannerListView: ListView<ShowResponse>
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        bannerListView.setCellFactory { lscell() }
    }
    fun setHomePage(pageLists:List<Array<Any>>){
        pageLists.forEach {
            val listView = ListView<Any>().apply {
                this.items.addAll(*it)
            }
        }
    }
    class lscell:ListCell<ShowResponse>(){
        val imageView= ImageView()
        init {
            alignment= Pos.CENTER
        }
        override fun updateItem(item: ShowResponse?, empty: Boolean) {
            super.updateItem(item, empty)
            if (empty || item == null) {
                text = null;
                graphic = null;
            } else {
//                onMouseClicked=item.handleMouseEvent
                imageView.image= Image(item.posterUrl,true)
                imageView.isPreserveRatio=true
                imageView.fitHeight=this.listView.height
                graphic = imageView
            }
        }
    }

}