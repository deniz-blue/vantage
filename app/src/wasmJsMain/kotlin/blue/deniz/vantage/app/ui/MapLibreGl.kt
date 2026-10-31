@file:JsModule("maplibre-gl")

package blue.deniz.vantage.app.ui

@JsName("Map")
internal external class MapLibreMap(options: JsAny) : JsAny {
    fun on(type: String, listener: (JsAny) -> Unit): MapLibreMap
    fun on(type: String, layerId: String, listener: (JsAny) -> Unit): MapLibreMap
    fun remove()
}

internal external fun setWorkerUrl(url: String)
