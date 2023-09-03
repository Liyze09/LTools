package net.liyze.basin.core.remote

import net.liyze.basin.core.CommandParser
import net.liyze.basin.core.Server
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.smartboot.socket.MessageProcessor
import org.smartboot.socket.extension.protocol.ByteArrayProtocol
import org.smartboot.socket.transport.AioQuickServer
import org.smartboot.socket.transport.AioSession
import java.io.IOException
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class RemoteServer(token: String, port: Int, remoteParser: CommandParser) : Server {
    private val token: String
    private val port: Int
    private val parser: CommandParser
    var server: AioQuickServer? = null

    init {
        servers.add(this)
        this.token = token
        this.port = port
        parser = remoteParser
    }

    /**
     * Stop remote Server
     */
    override fun stop() {
        server!!.shutdown()
    }

    /**
     * Start remote server
     */
    override fun start(): RemoteServer {
        val processor = MessageProcessor { s: AioSession, b: ByteArray? ->
            val cipher: Cipher
            var msg = ""
            val outputStream = s.writeBuffer()
            try {
                cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
                val keySpec: SecretKey = SecretKeySpec(token.toByteArray(StandardCharsets.UTF_8), "AES")
                cipher.init(Cipher.DECRYPT_MODE, keySpec)
                msg = String(cipher.doFinal(b), StandardCharsets.UTF_8)
            } catch (e: Exception) {
                try {
                    outputStream.write("Illegal BRC Request.".toByteArray(StandardCharsets.UTF_8))
                } catch (ex: IOException) {
                    LOGGER.error(ex.toString())
                }
                LOGGER.warn(e.toString())
            }
            if (!msg.startsWith("brc:")) {
                try {
                    val bytes = "Illegal BRC Request.".toByteArray(StandardCharsets.UTF_8)
                    LOGGER.warn("Illegal BRC Request: {} from {}", msg, s.remoteAddress.toString())
                    outputStream.writeInt(bytes.size)
                    outputStream.write(bytes)
                } catch (e: IOException) {
                    LOGGER.warn("Illegal BRC Request: {}", msg)
                }
                return@MessageProcessor
            }
            if (!parser.sync().parseString(msg.substring(4))) {
                try {
                    val bytes = "Failed to run the command.".toByteArray(StandardCharsets.UTF_8)
                    outputStream.writeInt(bytes.size)
                    outputStream.write(bytes)
                } catch (ex: IOException) {
                    LOGGER.error(ex.toString())
                }
            } else {
                try {
                    val bytes = "Okay".toByteArray(StandardCharsets.UTF_8)
                    outputStream.writeInt(bytes.size)
                    outputStream.write(bytes)
                    LOGGER.info("BRC Request: {} from {}", msg, s.remoteAddress.toString())
                } catch (e: IOException) {
                    LOGGER.warn(e.toString())
                }
            }
        }
        server = AioQuickServer(port, ByteArrayProtocol(), processor)
        server!!.setLowMemory(true)
        try {
            server!!.start()
        } catch (e: IOException) {
            LOGGER.error("Failed to start server {}", e.toString())
        }
        return this
    }

    companion object {
        @JvmField
        val servers: MutableList<Server> = ArrayList()
        val LOGGER: Logger = LoggerFactory.getLogger(RemoteServer::class.java)
    }
}
