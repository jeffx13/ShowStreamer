package com.showstreamer.controllers.show;

import javafx.fxml.Initializable
import javafx.scene.control.RadioButton
import javafx.scene.control.TextField
import javafx.scene.control.ToggleGroup
import java.net.URL
import java.util.*

class SettingsController : Initializable {
    lateinit var autoskipED: TextField
    lateinit var autoskipOPEnd: TextField
    lateinit var autoskipOPStart: TextField
    lateinit var autoSkipAlways: RadioButton
    lateinit var autoSkipIfPossible: RadioButton
    lateinit var autoSkipNever: RadioButton
    val autoSkipToggleGroup = ToggleGroup()

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        autoSkipToggleGroup.toggles.addAll(autoSkipAlways,autoSkipNever,autoSkipIfPossible)

    }

}
