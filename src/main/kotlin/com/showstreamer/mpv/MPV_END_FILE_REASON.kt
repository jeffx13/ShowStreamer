package com.showstreamer.mpv

enum class MPV_END_FILE_REASON(val id:Int) {
    /**
     * The end of file was reached. Sometimes this may also happen on
     * incomplete or corrupted files, or if the network connection was
     * interrupted when playing a remote file. It also happens if the
     * playback range was restricted with --end or --frames or similar.
     */
    EOF(0),
    /**
     * Playback was stopped by an external action (e.g. playlist controls).
     */
    STOP(2),
    /**
     * Playback was stopped by the quit command or player shutdown.
     */
    QUIT(3),
    /**
     * Some kind of error happened that lead to playback abort. Does not
     * necessarily happen on incomplete or broken files (in these cases, both
     * MPV_END_FILE_REASON_ERROR or MPV_END_FILE_REASON_EOF are possible).
     *
     * mpv_event_end_file.error will be set.
     */
    ERROR(4),
    /**
     * The file was a playlist or similar. When the playlist is read, its
     * entries will be appended to the playlist after the entry of the current
     * file, the entry of the current file is removed, and a MPV_EVENT_END_FILE
     * event is sent with reason set to MPV_END_FILE_REASON_REDIRECT. Then
     * playback continues with the playlist contents.
     * Since API version 1.18.
     */
    REDIRECT(5);
    companion object{
        fun getName(id:Int): String? {
            return values().find { it.id== id }?.name
        }
    }
}