package com.showstreamer

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.showstreamer.parsers.watch.TvType
import com.showstreamer.parsers.watch.extractors.Vidstream
import com.showstreamer.parsers.watch.providers.animeproviders.Gogoanime
import com.showstreamer.parsers.watch.providers.animeproviders.NineAnime
import com.showstreamer.parsers.watch.providers.animeproviders.Ximalaya
import com.showstreamer.parsers.watch.providers.mixedproviders.Iyf
import com.showstreamer.parsers.watch.providers.mixedproviders.Mudvod
import com.showstreamer.utils.base64Decoded
import com.showstreamer.utils.network.Requests
import javafx.application.Application
import kotlinx.coroutines.runBlocking
import java.nio.charset.Charset
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.system.measureTimeMillis


const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
var app = Requests().apply { defaultHeaders = mapOf("user-agent" to USER_AGENT) }
val mapper = JsonMapper.builder().addModule(KotlinModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build()!!


class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.setProperty("javafx.sg.warn", "false");
//            Application.launch(ShowStreamer::class.java)
            runBlocking {
                val m = NineAnime()
                println(NineAnime.decodeVrf("ab6/pL62s6RbkiH8gxfPL6dZQl5qkGB2K9nw0+h55Yf+EDoAS8Nhmif1Z1OnnSwc8FdH7y80mQK2prVX6qg1gAwaQrNxxb//qQetdyzN2xp0GL1F7GXHjN3wjt1Nz2cKn8rsK+l4BUrlOA==","hlPeNwkncH0fq9so"))
//                val input="BaJqvOSZxhUVCevfs1fWWHboHESmn1E92U0FCa40TlFP+TuFNdRUtz6bdnAOyCy0cmXwyZonaGBC9Q+SaD46dM5d6On4HSSpHzzzu533AZsgDtTl8aZXooI7EfRy8tQLHu8ZBfezs1rASmOP2RLfXRQ+BRLf/LU8hf0UToYtb4mQ6oGERZMIY49l5YjWA8X2s7WFrJvKA6E145LqfpJ5FICYs2x6Xf6xRKdVxpvi5JY="
//                    val d = m.cryptoHandler(input,m.iv,m.secretKey,false)
//                val e = m.cryptoHandler(d,m.iv,m.secretKey,true)
//                println(d)
//                println(e)
//                var data="{\"data\":\"w89bNzwCrlGHreXFIaZoZsPURVbae+n4MEw8S9r1ik4v8D4Q\\/KzZce4COgOsjArosYGgB9veuIN9kihL9agPnFeoIZi+k23vlo3PtWCEQ0str9JjWG49ek7JXFRAHWIsZZ1RhYRZOhRalr5KLrhhXneZQex1sMEGAjwxQQJYbDQ0r2pcG5yzcktcVHA5iZV\\/I4C59kPypZyvk76sgwIsJSi1PEkfzgwsPPZLaBRha0b7G+CAbnLXS0yshvMtZS2KxUPXqmmYTV1jhxJqO5fRusCcZyfRYe+dNjA+uIDJlUwN2wLv3TUvnax+owSUOVndnDS5vNmhZY80LSUmDFMgdJLQf90wxfa+HcWTczFdGuYed79G1lYPNabxsgL6OjX8Stmur1j0r\\/3Ign0y1qQYSGMOTViEn7Wfh3Ira\\/mbrTe44BJahDuSVO+TbTNtutwa8CP52cIRkc1q4lLL+U+OAmL2TGjy\\/JVSJlmY3i08JZ4okAp\\/CcXFCRolnpBNnSkLv7EgSx0XfzpeargJG7EnEPaIDr9vkau+Cl6O+pmBA0k6laih9W4pYrSlV8Rgag87v4+sd9h94myW5JQGwc7PeL8eWmr6XIc0jzS\\/eouWgfvExqITjvvprGoQhqQbShxRNDSKJoIXKzEy2pZuomZzMNqzv2oZWqMtfqTzEWxUk24pCx42AT3TxlfU5zmTG3N4RueZMlXYSOVnANBLW3KZL9KReKr86IYEYL6n84Cc7vBMylSMLP2fHLU5dDqPynRysDcobnfKBMr+FTDePiPSpjkW30xeBQLGWG9kM34dSwaOknD33pA8arb892LNnmiIUv\\/mMZXI0GLRGth5kdunuaxcFv3yCw12XH7YiDFjUDw4yYLE9JLcvaaTuS6ZlQTGW9wla9e6cdC3ZL+pRW0645T\\/6o3+RBS2G1oBBBxq89hg8lZcRQvwdliCtVp9KIh6Ndv9gPkixGviPYQO0WNXCE1Hz8P0Dp3OisCdTIHEQDctcGRHka2U6a2VNa7gWqROcVjnE+wqel5mk7DE8ljuJdS1FCsR+WLPoMPlUQiwdN4=\"}"
//                data=data.substringAfter("{\"data\":\"").substringBefore("\"}")
//                val s="w89bNzwCrlGHreXFIaZoZsPURVbae+n4MEw8S9r1ik4v8D4Q\\/KzZce4COgOsjArosYGgB9veuIN9kihL9agPnFeoIZi+k23vlo3PtWCEQ0str9JjWG49ek7JXFRAHWIsZZ1RhYRZOhRalr5KLrhhXneZQex1sMEGAjwxQQJYbDQ0r2pcG5yzcktcVHA5iZV\\/I4C59kPypZyvk76sgwIsJSi1PEkfzgwsPPZLaBRha0b7G+CAbnLXS0yshvMtZS2KxUPXqmmYTV1jhxJqO5fRusCcZyfRYe+dNjA+uIDJlUwN2wLv3TUvnax+owSUOVndnDS5vNmhZY80LSUmDFMgdJLQf90wxfa+HcWTczFdGuYed79G1lYPNabxsgL6OjX8Stmur1j0r\\/3Ign0y1qQYSGMOTViEn7Wfh3Ira\\/mbrTe44BJahDuSVO+TbTNtutwa8CP52cIRkc1q4lLL+U+OAmL2TGjy\\/JVSJlmY3i08JZ4okAp\\/CcXFCRolnpBNnSkLv7EgSx0XfzpeargJG7EnEPaIDr9vkau+Cl6O+pmBA0k6laih9W4pYrSlV8Rgag87v4+sd9h94myW5JQGwc7PeL8eWmr6XIc0jzS\\/eouWgfsDbEAxgOgLKOyR07K1E912eyTdv\\/H7EDZEwj5gzkqs\\/R+l\\/Hb8jxcImbbH8PeAonPeAhM3pp7a27hbrdBEpyV5J6MMJpa71VwDxRzoy+mRadABxa43PlN7QMTKzNoOtqqtiGTs8fbJVv7D+4uIEjWxWeWMPHHU5j8dQAHidw3tWHhcyzrVwZRhP9QZ8P\\/Dg6EfZhbRvqf5f+QXIxVm\\/dZ1FBmWm7Cezw3tx2FB10YcIjdfE2glAsatFVpMAvJz8NXiBQ+PvsLtbR\\/F9TF9y2JfiIGy5CLii4ig6uluSfFGrnMp4df7SPUUiGfOPjTNn3C\\/QFZLuaF5vJUNkIbfgnaK+O7CufF94dMmiqCyAZXSorMe8KDwmhSEYW6Mg+2F7FrMtawdfsb+0p8sVylEqLQD6po3Tv3SkYPKttGQfSpVYMsQANdB+Epg14kOfXNvVoo="
//                val dataDecrypted = m.cryptoHandler(s, m.iv, m.secretDecryptKey, false)
//                println(dataDecrypted)
            }


//            runBlocking {
//                val i = Iyf();
//                val l = i.latest(1,TvType.Anime)[0];
//                println(l);
//                val e = i.loadDetails(l).episodes[0];
//                println(e)
//                i.loadSource(e)
//
//
//            }
//            runBlocking {
//                println(Vidstream.extract("G6JOQV5ZRP9O"))
//                println(NineAnime.getKey())
//                private const val baseTable  ="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=_"
//                val s = NineAnime.decrypt("ab6/pL62s6RbkiH8gxfPL6dZQl5qkGB2K9nw0+h55Yf+EDoAS8MKgCeVZSvahzUGlFs=")
//                println(s)
//                s.forEach { print(it.code.toString()+" ") }
//                println()
//
//                val d =NineAnime.cipher(NineAnime.getKey().decipher,s)
//                d.forEach { print(it.code.toString()+ " ") }
//                println(NineAnime.getKey().decipher)
//                println(NineAnime.encodeVrf("1199550",NineAnime.getKey()))
//            }

        }
    }
}


fun decrypt(key: String, encrypted: ByteArray?): String? {
    val raw = key.toByteArray(Charset.forName("UTF-8"))
//    require(raw.size == 16) { "Invalid key size." }
    val skeySpec = SecretKeySpec(raw, "AES")
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(
        Cipher.DECRYPT_MODE, skeySpec,
        IvParameterSpec(ByteArray(16))
    )
    val original = cipher.doFinal(encrypted)
    return String(original, Charset.forName("UTF-8"))
}

