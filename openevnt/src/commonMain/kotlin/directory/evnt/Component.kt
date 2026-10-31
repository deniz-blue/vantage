package directory.evnt

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val TYPE_FIELD = "\$type"

@Serializable(with = ComponentSerializer::class)
sealed interface Component {
    val typeId: String

    @Serializable
    data class MarkdownText(val content: Translations, val flavor: String? = null) : Component {
        override val typeId get() = TYPE_ID

        companion object {
            const val TYPE_ID = "directory.evnt.text.markdown#0"
        }
    }

    @Serializable
    data class PlainText(val content: Translations) : Component {
        override val typeId get() = TYPE_ID

        companion object {
            const val TYPE_ID = "directory.evnt.text.plain#0"
        }
    }

    @Serializable
    data class Link(val url: String, val name: Translations? = null, val disabled: Boolean? = null) : Component {
        override val typeId get() = TYPE_ID

        companion object {
            const val TYPE_ID = "directory.evnt.component.link#0"
        }
    }

    @Serializable
    data class Source(val url: String) : Component {
        override val typeId get() = TYPE_ID

        companion object {
            const val TYPE_ID = "directory.evnt.component.source#0"
        }
    }

    @Serializable
    data class SplashMedia(val media: Media, val roles: List<String>? = null) : Component {
        override val typeId get() = TYPE_ID

        companion object {
            const val TYPE_ID = "directory.evnt.component.splashMedia#0"
        }
    }

    @Serializable
    data class Languages(val languages: List<Language>) : Component {
        override val typeId get() = TYPE_ID

        @Serializable
        data class Language(val code: String)

        companion object {
            const val TYPE_ID = "directory.evnt.component.languages#0"
        }
    }

    @Serializable
    data class PlaceId(val type: String, val id: String) : Component {
        override val typeId get() = TYPE_ID

        companion object {
            const val TYPE_ID = "directory.evnt.component.placeId#0"
        }
    }

    @Serializable
    data class Relation(val type: String, val target: String) : Component {
        override val typeId get() = TYPE_ID

        companion object {
            const val TYPE_ID = "directory.evnt.component.relation#0"
        }
    }

    @Serializable
    data class Coordinates(val latitude: Double, val longitude: Double, val uncertainty: Double? = null) : Component {
        override val typeId get() = TYPE_ID

        companion object {
            const val TYPE_ID = "directory.evnt.component.coordinates#0"
        }
    }

    data class Unknown(override val typeId: String, val fields: JsonObject) : Component
}

internal val KNOWN_COMPONENT_SERIALIZERS: Map<String, KSerializer<out Component>> = mapOf(
    Component.MarkdownText.TYPE_ID to Component.MarkdownText.serializer(),
    Component.PlainText.TYPE_ID to Component.PlainText.serializer(),
    Component.Link.TYPE_ID to Component.Link.serializer(),
    Component.Source.TYPE_ID to Component.Source.serializer(),
    Component.SplashMedia.TYPE_ID to Component.SplashMedia.serializer(),
    Component.Languages.TYPE_ID to Component.Languages.serializer(),
    Component.PlaceId.TYPE_ID to Component.PlaceId.serializer(),
    Component.Relation.TYPE_ID to Component.Relation.serializer(),
    Component.Coordinates.TYPE_ID to Component.Coordinates.serializer(),
)

internal object ComponentSerializer : KSerializer<Component> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("directory.evnt.Component")

    override fun deserialize(decoder: Decoder): Component {
        val input = decoder as? JsonDecoder ?: throw SerializationException("Components can only be read from JSON")
        val fields = input.decodeJsonElement().jsonObject
        val typeId = fields[TYPE_FIELD]?.jsonPrimitive?.content
            ?: throw SerializationException("A component needs a $TYPE_FIELD")
        val serializer = KNOWN_COMPONENT_SERIALIZERS[typeId] ?: return Component.Unknown(typeId, JsonObject(fields - TYPE_FIELD))
        return input.json.decodeFromJsonElement(serializer, JsonObject(fields - TYPE_FIELD))
    }

    override fun serialize(encoder: Encoder, value: Component) {
        val output = encoder as? JsonEncoder ?: throw SerializationException("Components can only be written as JSON")
        val fields = when (value) {
            is Component.Unknown -> value.fields
            else -> {
                @Suppress("UNCHECKED_CAST")
                val serializer = KNOWN_COMPONENT_SERIALIZERS.getValue(value.typeId) as KSerializer<Component>
                output.json.encodeToJsonElement(serializer, value).jsonObject
            }
        }
        output.encodeJsonElement(JsonObject(mapOf(TYPE_FIELD to JsonPrimitive(value.typeId)) + (fields - TYPE_FIELD)))
    }
}
