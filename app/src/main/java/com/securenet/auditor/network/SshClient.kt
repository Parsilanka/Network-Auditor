package com.securenet.auditor.network

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.UnknownHostException
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
    ): ConnectionResult = withContext(Dispatchers.IO) {
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
            
            session?.connect(10000)
            
            if (session?.isConnected == true) {
                ConnectionResult.Success
            } else {
                ConnectionResult.Failure("Connection failed without error message")
            }
        } catch (e: JSchException) {
            val msg = e.message ?: ""
            when {
                msg.contains("Auth fail", ignoreCase = true) -> 
                    ConnectionResult.Failure("Authentication failed: Invalid username, password, or key")
                msg.contains("timeout", ignoreCase = true) -> 
                    ConnectionResult.Failure("Connection timed out: Server unreachable or firewall blocking port $port")
                msg.contains("Connection refused", ignoreCase = true) ->
                    ConnectionResult.Failure("Connection refused: SSH service not running on port $port")
                else -> ConnectionResult.Failure("SSH Error: ${e.message}")
            }
        } catch (e: UnknownHostException) {
            ConnectionResult.Failure("Network error: Unknown host '$host'")
        } catch (e: Exception) {
            ConnectionResult.Failure("Error: ${e.message ?: "Unknown connection error"}")
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
            
            channel.connect(5000)
            
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

    sealed class ConnectionResult {
        object Success : ConnectionResult()
        data class Failure(val message: String) : ConnectionResult()
    }
}
