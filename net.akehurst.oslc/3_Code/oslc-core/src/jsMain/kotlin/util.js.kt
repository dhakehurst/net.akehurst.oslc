package net.akehurst.oslc.core.util

import kotlinx.browser.window

actual fun openUrl(url: String) {
    window.open(url, "_blank","popup")
}

