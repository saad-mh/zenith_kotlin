package com.saadm.zenith.domain.backup

import java.io.InputStream
import java.io.OutputStream

/**
 * Backup to one file the user picks, encrypted with a passphrase. Restore replaces everything on
 * the device. Delete-all clears the data and the settings. None of this touches the network.
 */
interface LocalBackup {

    /** Writes everything in the database, encrypted with [passphrase] (at least 8 characters). */
    suspend fun writeBackup(output: OutputStream, passphrase: CharArray)

    /**
     * Replaces everything on the device with the backup. The backup is checked in full before
     * anything is removed, and the replacement is one transaction. [replaceEverything] must be true:
     * the caller has told the user what will be lost.
     */
    suspend fun restoreBackup(input: InputStream, passphrase: CharArray, replaceEverything: Boolean)

    /** Clears the database and the settings. [confirmation] must be exactly "DELETE". */
    suspend fun deleteAllData(confirmation: String)

    /** Empties the app's cache. Returns the bytes freed. */
    suspend fun clearCache(): Long
}
