package com.showstreamer.mpv
import com.sun.jna.Callback
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.Structure.FieldOrder
import com.sun.jna.ptr.PointerByReference
import com.sun.jna.win32.StdCallLibrary
import java.util.*


interface MPV : StdCallLibrary {
    fun mpv_client_api_version(): Long
    fun mpv_create(): Long
    fun mpv_initialize(handle: Long): Int
    fun mpv_command(handle: Long, args: Array<String?>?): Int
    fun mpv_command_string(handle: Long, args: String?): Int
    fun mpv_get_property_string(handle: Long, name: String?): Pointer?
    fun mpv_set_property_string(handle: Long, name: String?, data: String?): Int
    fun mpv_set_option_string(handle: Long, name: String?, data: String?): Int
    fun mpv_free(data: Pointer?)
    fun mpv_set_option(handle: Long, name: String?, format: Int, data: Pointer?): Int
    fun mpv_wait_event(handle: Long, timeOut: Double): mpv_event?
    fun mpv_request_event(handle: Long, event_id: Int, enable: Int): Int
    fun mpv_observe_property(handle: Long, reply_userdata:Int, name:String,format:Int)
    fun mpv_event_name(event:Int):Pointer?
    fun mpv_terminate_destroy(handle: Long)
    fun mpv_render_context_create(render_context: PointerByReference?, handle: Long, params: mpv_render_param?): Int

    fun mpv_render_context_free(render_context: Pointer?)

    fun mpv_set_wakeup_callback(handle: Long, callback: on_wakeup?, d: Pointer?)

    fun mpv_render_context_set_update_callback(render_context: Pointer?, callback: on_render_update?, d: Pointer?)

    fun mpv_render_context_update(render_context: Pointer?): Int

    fun mpv_render_context_render(render_context: Pointer?, params: mpv_render_param?): Int

    fun mpv_render_context_report_swap(render_context: Pointer?)

    companion object {
        val INSTANCE = Native.load("C:\\Users\\Jeffx\\Downloads\\mpv-dev-x86_64-20220904-git-d433c5d\\mpv-2.dll", MPV::class.java)
        }
    @FieldOrder("event_id", "error", "reply_userdata", "data")
    class mpv_event : Structure() {
        @JvmField
        var event_id:Int = 0
        @JvmField
        var error = 0
        @JvmField
        var reply_userdata: Long = 0
        @JvmField
        var data: Pointer? = null
        override fun getFieldOrder(): MutableList<String> {
            return mutableListOf("event_id", "error", "reply_userdata", "data")
        }
    }
    @FieldOrder("name", "format", "data")
    class mpv_event_property(p: Pointer) : Structure(p) {
        @JvmField
        var name:String =""
        @JvmField
        var format:Int = 0
        @JvmField
        var data: Pointer? = null
        init {
            read()
        }
    }
}
@FieldOrder("fbo", "w", "h", "internal_format")
class mpv_opengl_fbo : Structure() {
    @JvmField
    var fbo = 0
    @JvmField
    var w = 0
    @JvmField
    var h = 0
    @JvmField
    var internal_format = 0
    override fun getFieldOrder(): List<String> {
        return Arrays.asList("fbo", "w", "h", "internal_format")
    }
}

interface on_wakeup : Callback {
    fun callback(d: Pointer?)
}

interface on_render_update : Callback {
    fun callback(d: Pointer?)
}
interface mpv_render_update_flag {
    companion object {
        const val MPV_RENDER_UPDATE_FRAME = 1
    }
}
