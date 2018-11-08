package com.shalzz.attendance

import com.google.common.truth.Truth.assertThat
import com.shalzz.attendance.util.DefaultConfig
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.manifest.AndroidManifest
import org.robolectric.res.Fs
import java.util.HashSet

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [DefaultConfig.EMULATE_SDK])
class PermissionsTest {

    @Test
    fun shouldMatchPermissions() {
        val manifest = AndroidManifest(
            Fs.fileFromPath(MERGED_MANIFEST),
            null, null
        )

        assertThat(HashSet(manifest.usedPermissions))
            .containsExactly(*EXPECTED_PERMISSIONS )
    }

    companion object {

        private val EXPECTED_PERMISSIONS = arrayOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.READ_SYNC_SETTINGS",
            "android.permission.WRITE_SYNC_SETTINGS",
            "android.permission.AUTHENTICATE_ACCOUNTS",
            "android.permission.GET_ACCOUNTS",
            "android.permission.MANAGE_ACCOUNTS",
            "com.android.vending.BILLING",
            "android.permission.WAKE_LOCK",
            "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
            "com.google.android.c2dm.permission.RECEIVE"
        )

        private const val MERGED_MANIFEST = "build/intermediates/merged_manifests/" +
            BuildConfig.FLAVOR +
            "/release/processReleaseManifest/merged/AndroidManifest.xml"
    }
}