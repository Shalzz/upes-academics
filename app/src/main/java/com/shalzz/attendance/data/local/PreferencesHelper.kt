/*
 * Copyright (c) 2013-2018 Shaleen Jain <shaleen.jain95@gmail.com>
 *
 * This file is part of College Academics.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shalzz.attendance.data.local

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.shalzz.attendance.data.model.SemVersion
import com.shalzz.attendance.injection.ApplicationContext
import com.shalzz.attendance.sync.MyAccountManager
import com.shalzz.attendance.ui.main.MainActivity
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@SuppressLint("CommitPrefEdits")
@Singleton
class PreferencesHelper @Inject
constructor(@ApplicationContext context: Context) {

    private val mPref: SharedPreferences

    val loginStatus: Boolean
        get() = mPref.getBoolean("LOGGEDIN", false)

    val userId: String?
        get() = mPref.getString("USERNAME", null)

    val token: String?
        get() = mPref.getString("TOKEN", null)

    val clg: String?
        get() = mPref.getString("COLLEGE", null)

    init {
        mPref = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
    }

    fun clear() {
        mPref.edit().clear().apply()
    }

    fun setLoggedIn() {
        mPref.edit().putBoolean(
            "LOGGEDIN", true
        ).commit()
    }

    fun saveToken(token: String) {
        mPref.edit().putString(
                "TOKEN", token
        ).commit()
    }

    fun saveCollege(clg: String) {
        mPref.edit().putString(
                "COLLEGE", clg
        ).commit()
    }

    /**
     * Saves the user details in shared preferences and sets login status to true.
     * @param username Username
     */
    fun saveUser(username: String, password: String, clg: String) {
        val editor = mPref.edit()
        editor.putString("USERNAME", username)
        editor.putString("PASSWORD", password)
        editor.putString("COLLEGE", clg)
        editor.commit()
    }

    /**
     * Removes the user details from the shared preferences and sets login status to false.
     */
    fun removeUser() {
        val editor = mPref.edit()
        editor.putBoolean("LOGGEDIN", false)
        editor.remove("USERNAME")
        editor.remove("TOKEN")
        editor.commit()
    }

    fun upgradePrefsIfNecessary(context: Context) {
        val version = SemVersion(mPref.getString(PREF_VERSION_KEY, "v0.0.0")!!)
        Timber.d("Old preference version: %s", version)

        when  {
            upgrade(version, "v3.2.2") -> {
                mPref.edit().putBoolean("LOGGEDIN", false).commit() // Required !!
                MyAccountManager.removeSyncAccount(context)
            }
            else -> Timber.d("Preference upgrade not required.")
        }
    }

    private fun upgrade (oldVersion : SemVersion, newVersion: String): Boolean {
        val version = SemVersion(newVersion)
        return if (oldVersion < version) {
            mPref.edit().putString(PREF_VERSION_KEY, version.version).apply()
            Timber.d("Upgrading preferences to: %s", version)
            true
        }
        else false
    }

    companion object {

        private val PREF_FILE_NAME = "attendance_pref_file"

        private val PREF_VERSION_KEY = "prefs-version"
    }
}
