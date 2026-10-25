package com.lucasmarujo.gitwallpaper.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.XmlResourceParser
import android.util.Xml
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lucasmarujo.gitwallpaper.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

@RunWith(AndroidJUnit4::class)
class ContributionWidgetProviderTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val component = ComponentName(context, ContributionWidgetProvider::class.java)

    @Test
    fun `the widget provider is registered privately with its metadata`() {
        val receiver = context.packageManager.getReceiverInfo(component, PackageManager.GET_META_DATA)

        assertFalse(receiver.exported)
        assertEquals(R.xml.contribution_widget_info, receiver.metaData.getInt(AppWidgetManager.META_DATA_APPWIDGET_PROVIDER))
    }

    @Test
    fun `the widget can be resized freely in both directions from a single cell`() {
        val parser = providerInfoParser()

        assertEquals(AppWidgetProviderInfo.RESIZE_BOTH, parser.getAttributeIntValue(ANDROID_NAMESPACE, "resizeMode", 0))
        assertEquals(
            AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
            parser.getAttributeIntValue(ANDROID_NAMESPACE, "widgetCategory", 0),
        )
        assertEquals(0, parser.getAttributeIntValue(ANDROID_NAMESPACE, "updatePeriodMillis", -1))
        val density = context.resources.displayMetrics.density
        val attributes = context.resources.obtainAttributes(
            Xml.asAttributeSet(parser),
            intArrayOf(android.R.attr.minResizeWidth, android.R.attr.minResizeHeight),
        )
        try {
            assertTrue(attributes.getDimension(0, Float.MAX_VALUE) / density <= SINGLE_CELL_DP)
            assertTrue(attributes.getDimension(1, Float.MAX_VALUE) / density <= SINGLE_CELL_DP)
        } finally {
            attributes.recycle()
        }
    }

    private fun providerInfoParser(): XmlResourceParser {
        val parser = context.resources.getXml(R.xml.contribution_widget_info)
        while (parser.next() != XmlPullParser.START_TAG) Unit
        assertEquals("appwidget-provider", parser.name)
        return parser
    }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        const val SINGLE_CELL_DP = 40f
    }
}
