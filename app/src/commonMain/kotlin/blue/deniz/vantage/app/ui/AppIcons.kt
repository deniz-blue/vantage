package blue.deniz.vantage.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddLink
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Dehaze
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.automirrored.filled.List as FilledList
import androidx.compose.material.icons.filled.CalendarMonth as FilledCalendarMonth
import androidx.compose.material.icons.filled.CheckCircle as FilledCheckCircle
import androidx.compose.material.icons.filled.Home as FilledHome
import androidx.compose.material.icons.filled.Map as FilledMap
import androidx.compose.material.icons.filled.Settings as FilledSettings

object AppIcons {
    val Home = Icons.Outlined.Home
    val HomeSelected = Icons.Filled.FilledHome
    val List = Icons.AutoMirrored.Outlined.List
    val ListSelected = Icons.AutoMirrored.Filled.FilledList
    val Calendar = Icons.Outlined.CalendarMonth
    val CalendarSelected = Icons.Filled.FilledCalendarMonth
    val DateRange = Icons.Outlined.DateRange
    val Map = Icons.Outlined.Map
    val MapSelected = Icons.Filled.FilledMap
    val Settings = Icons.Outlined.Settings
    val Bell = Icons.Outlined.Notifications
    val SettingsSelected = Icons.Filled.FilledSettings
    val Plus = Icons.Outlined.Add
    val Search = Icons.Outlined.Search
    val Filter = Icons.Outlined.FilterList
    val ArrowLeft = Icons.AutoMirrored.Outlined.ArrowBack
    val Link = Icons.Outlined.Link
    val Refresh = Icons.Outlined.Refresh
    val AlertTriangle = Icons.Outlined.WarningAmber
    val MapPin = Icons.Outlined.Place
    val Clock = Icons.Outlined.Schedule
    val World = Icons.Outlined.Public
    val Flag = Icons.Outlined.Flag
    val ExternalLink = Icons.AutoMirrored.Outlined.OpenInNew
    val Trash = Icons.Outlined.Delete
    val Share = Icons.Outlined.Share
    val DotsVertical = Icons.Outlined.MoreVert
    val Database = Icons.Outlined.Storage
    val Broadcast = Icons.Outlined.Sensors
    val ChevronLeft = Icons.Outlined.ChevronLeft
    val ChevronRight = Icons.Outlined.ChevronRight
    val CheckBox = Icons.Outlined.CheckBox
    val CheckBoxBlank = Icons.Outlined.CheckBoxOutlineBlank
    val Check = Icons.Outlined.Check
    val Minus = Icons.Outlined.Remove
    val Circle = Icons.Outlined.RadioButtonUnchecked
    val CheckCircle = Icons.Filled.FilledCheckCircle
    val SelectAll = Icons.Outlined.SelectAll
    val Copy = Icons.Outlined.ContentCopy
    val Braces = Icons.Outlined.DataObject
    val X = Icons.Outlined.Close
    val CalendarOff = Icons.Outlined.EventBusy
    val CalendarCheck = Icons.Outlined.EventAvailable
    val ClockUpdate = Icons.Outlined.Update
    val QuestionCircle = Icons.AutoMirrored.Outlined.HelpOutline
    val Pencil = Icons.Outlined.Edit
    val WorldDownload = Icons.Outlined.CloudDownload
    val WorldUpload = Icons.Outlined.CloudUpload
    val FileDownload = Icons.Outlined.FileDownload
    val FileUpload = Icons.Outlined.FileUpload
    val Smartphone = Icons.Outlined.Smartphone
    val UserEdit = Icons.Outlined.ManageAccounts
    val Language = Icons.Outlined.Translate
    val ArrowUp = Icons.Outlined.ArrowUpward
    val Code = Icons.Outlined.Code
    val Rss = Icons.Outlined.RssFeed
    val ChevronDown = Icons.Outlined.ExpandMore
    val ChevronUp = Icons.Outlined.ExpandLess
    val Directions = Icons.Outlined.Directions
    val AddLink = Icons.Outlined.AddLink
    val AddPlace = Icons.Outlined.AddLocationAlt
    val AddImage = Icons.Outlined.AddPhotoAlternate
    val Image = Icons.Outlined.Image
    val History = Icons.Outlined.History
    val Notes = Icons.AutoMirrored.Outlined.Notes
    val Paste = Icons.Outlined.ContentPaste
    val DragHandle = Icons.Outlined.DragHandle
    val Sunny = Icons.Outlined.WbSunny
    val ClearNight = Icons.Outlined.NightsStay
    val PartlyCloudy = materialSymbol("PartlyCloudyDay", "M440-760v-160h80v160h-80Zm266 110-56-56 113-114 56 57-113 113Zm54 210v-80h160v80H760Zm3 299L650-254l56-56 114 112-57 57ZM254-650 141-763l57-57 112 114-56 56Zm-14 450h180q25 0 42.5-17.5T480-260q0-25-17-42.5T421-320h-51l-20-48q-14-33-44-52.5T240-440q-50 0-85 35t-35 85q0 50 35 85t85 35Zm0 80q-83 0-141.5-58.5T40-320q0-83 58.5-141.5T240-520q60 0 109.5 32.5T423-400q58 0 97.5 43T560-254q-2 57-42.5 95.5T420-120H240Zm320-134q-5-20-10-39t-10-39q45-19 72.5-59t27.5-89q0-66-47-113t-113-47q-60 0-105 39t-53 99q-20-5-41-9t-41-9q14-88 82.5-144T480-720q100 0 170 70t70 170q0 77-44 138.5T560-254Zm-79-226Z")
    val PartlyCloudyNight = materialSymbol("PartlyCloudyNight", "M504-425Zm20 385H420l20-12.5q20-12.5 43.5-28t43.5-28l20-12.5q81-6 149.5-49T805-285q-86-8-163-43.5T504-425q-61-61-97-138t-43-163q-77 43-120.5 118.5T200-444v12l-12 5.5q-12 5.5-26.5 11.5T135-403.5l-12 5.5q-2-11-2.5-23t-.5-23q0-146 93-257.5T450-840q-18 99 11 193.5T561-481q71 71 165.5 100T920-370q-26 144-138 237T524-40Zm-284-80h180q25 0 42.5-17.5T480-180q0-25-17-42.5T422-240h-52l-20-48q-14-33-44-52.5T240-360q-50 0-85 34.5T120-240q0 50 35 85t85 35Zm0 80q-83 0-141.5-58.5T40-240q0-83 58.5-141.5T240-440q60 0 109.5 32.5T423-320q57 2 97 42.5t40 97.5q0 58-41 99t-99 41H240Z")
    val Cloudy = Icons.Outlined.Cloud
    val Fog = Icons.Outlined.Dehaze
    val Drizzle = Icons.Outlined.Grain
    val Rain = Icons.Outlined.Umbrella
    val Snow = Icons.Outlined.AcUnit
    val Thunderstorm = Icons.Outlined.Thunderstorm
    val Thermometer = Icons.Outlined.Thermostat
    val Tag = Icons.Outlined.Sell
    val Label = Icons.AutoMirrored.Outlined.Label
    val Hidden = Icons.Outlined.VisibilityOff
    val Info = Icons.Outlined.Info
}

private fun materialSymbol(name: String, pathData: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, viewportWidth = 960f, viewportHeight = 960f)
        .addGroup(translationY = 960f)
        .addPath(addPathNodes(pathData), fill = SolidColor(Color.Black))
        .build()
