package net.akehurst.oslc.core.util

@OptIn(ExperimentalWasmJsInterop::class)
actual fun openUrl(url: String) {
    js("window.open(url, '_blank','popup')")
}