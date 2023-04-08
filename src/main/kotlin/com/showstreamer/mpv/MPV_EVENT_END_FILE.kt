package com.showstreamer.mpv

import com.sun.jna.Pointer
import com.sun.jna.Structure

@Structure.FieldOrder("reason", "error", "playlist_entry_id","playlist_insert_id","playlist_insert_num_entries")
class MPV_EVENT_END_FILE(p: Pointer) : Structure(p) {
    @JvmField
    var reason: Int? = null
    @JvmField
    var error = 0
    @JvmField
    var playlist_entry_id: Int? = null
    @JvmField
    var playlist_insert_id: Int? = null
    @JvmField
    var playlist_insert_num_entries = 0

    init {
        read()
    }
}