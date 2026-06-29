package com.securenet.auditor.network

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Properties

class SshClient {

    data class SshResult(
        val output: String,
        val exitStatus: Int,
        val error: String? = null
    )

    private var session: Session? = null
    private var jsch = JSch()

    suspend fun connect(
        host: String,
        user: String,
        password: String? = null,
        privateKey: String? = null,
        port: Int = 22
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            disconnect()
            
            if (privateKey != null) {
                jsch.addIdentity("vault-key", privateKey.toByteArray(), null, null)
            }

            session = jsch.getSession(user, host, port)
            
            if (password != null && privateKey == null) {
                session?.setPassword(password)
            }

            val config = Properties()
            config["StrictHostKeyChecking"] = "no"
            session?.setConfig(config)
            session?.timeout = 10000
            
            session?.connect()
            session?.isConnected == true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun executeCommand(command: String): SshResult = withContext(Dispatchers.IO) {
        val currentSession = session
        if (currentSession == null || !currentSession.isConnected) {
            return@withContext SshResult("", -1, "Not connected")
        }

        try {
            val channel = currentSession.openChannel("exec") as ChannelExec
            channel.setCommand(command)
            
            val outputStream = ByteArrayOutputStream()
            val errorStream = ByteArrayOutputStream()
            channel.setOutputStream(outputStream)
            channel.setErrStream(errorStream)
            
            channel.connect()
            
            while (!channel.isClosed) {
                kotlinx.coroutines.delay(100)
            }
            
            val exitStatus = channel.exitStatus
            val output = outputStream.toString()
            val error = errorStream.toString()
            
            channel.disconnect()
            
            SshResult(
                output = output.ifEmpty { error },
                exitStatus = exitStatus,
                error = if (exitStatus != 0 && error.isNotEmpty()) error else null
            )
        } catch (e: Exception) {
            SshResult("", -1, e.message)
        }
    }

    fun disconnect() {
        session?.disconnect()
        session = null
        jsch = JSch() // Reset JSch to clear identities
    }

    fun isConnected(): Boolean {
        return session?.isConnected == true
    }
}
