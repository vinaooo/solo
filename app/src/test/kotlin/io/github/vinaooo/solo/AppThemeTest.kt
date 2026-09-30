package io.github.vinaooo.solo

import android.content.Context
import android.util.TypedValue
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class AppThemeTest {

    @Test
    @Config(qualifiers = "night")
    fun `at night the window theme is dark so the system does not invert the in-app light theme`() {
        isLightWindowTheme() shouldBe false
    }

    @Test
    @Config(qualifiers = "notnight")
    fun `by day the window theme is light`() {
        isLightWindowTheme() shouldBe true
    }

    private fun isLightWindowTheme(): Boolean {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val theme = context.resources.newTheme().apply { applyStyle(R.style.Theme_Solo, true) }
        val value = TypedValue()
        theme.resolveAttribute(android.R.attr.isLightTheme, value, true) shouldBe true
        return value.data != 0
    }
}
