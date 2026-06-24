package com.securenet.auditor.data.repository

import com.securenet.auditor.data.db.PasswordDao
import com.securenet.auditor.data.db.PasswordEntity
import kotlinx.coroutines.flow.Flow

class PasswordRepository(private val passwordDao: PasswordDao) {
    val allPasswords: Flow<List<PasswordEntity>> = passwordDao.getAllPasswords()

    suspend fun insert(password: PasswordEntity) {
        passwordDao.insertPassword(password)
    }

    suspend fun update(password: PasswordEntity) {
        passwordDao.updatePassword(password)
    }

    suspend fun delete(password: PasswordEntity) {
        passwordDao.deletePassword(password)
    }

    suspend fun getPasswordById(id: Int): PasswordEntity? {
        return passwordDao.getPasswordById(id)
    }
}
