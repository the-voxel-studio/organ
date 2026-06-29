package fr.studio.voxel.organ

import androidx.test.platform.app.InstrumentationRegistry

object TestAssetHelper {
    fun readAsset(fileName: String): String {
        val context = InstrumentationRegistry.getInstrumentation().context
        return context.assets.open(fileName).bufferedReader().use { it.readText() }
    }
}
