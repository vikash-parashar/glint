package app.glint.widgets.catalog

/**
 * Every widget type that appears in the system widget picker. Each has its own
 * receiver (Android needs one per picker entry) but they all share one renderer.
 */
enum class WidgetKind(val title: String, val receiver: String) {
    ClockDigital("Clock", "ClockDigitalReceiver"),
    ClockAnalog("Analog clock", "ClockAnalogReceiver"),
    WorldClock("World clock", "WorldClockReceiver"),
    Date("Date", "DateReceiver"),
    Agenda("Calendar", "AgendaReceiver"),
    Weather("Weather", "WeatherReceiver"),
    Battery("Battery", "BatteryReceiver"),
    Steps("Steps", "StepsReceiver"),
    Music("Music", "MusicReceiver"),
    Note("Note", "NoteReceiver"),
    Checklist("Checklist", "ChecklistReceiver"),
    Quote("Quote", "QuoteReceiver"),
    Countdown("Countdown", "CountdownReceiver"),
    Apps("Quick apps", "AppsReceiver"),
    Search("Search", "SearchReceiver"),
    Photo("Photo", "PhotoReceiver"),
    Focus("Focus", "FocusReceiver"),
    Alarm("Next alarm", "AlarmReceiver"),
    Sky("Moon & device", "SkyReceiver");

    companion object {
        fun fromReceiverClass(className: String?): WidgetKind? =
            entries.firstOrNull { className?.endsWith(".${it.receiver}") == true }
    }
}

/** Which extra setup a design needs when it's added. */
enum class Needs { Nothing, City, Calendar, Steps, MediaAccess, Text, Apps, Zones, PhotoPick, CountdownDate }

data class WidgetDesign(
    val id: String,
    val kind: WidgetKind,
    val name: String,
    val blurb: String,
    val pro: Boolean = false,
    val needs: Needs = Needs.Nothing,
)

object WidgetCatalog {
    private fun d(kind: WidgetKind, id: String, name: String, blurb: String, pro: Boolean = false, needs: Needs = Needs.Nothing) =
        WidgetDesign(id, kind, name, blurb, pro, needs)

    val all: List<WidgetDesign> = listOf(
        // Clock (7)
        d(WidgetKind.ClockDigital, "clock_big", "Big clock", "Large time with the date"),
        d(WidgetKind.ClockDigital, "clock_stacked", "Stacked", "Hours over minutes, poster style"),
        d(WidgetKind.ClockDigital, "clock_mono", "Mono", "24-hour monospace time"),
        d(WidgetKind.ClockDigital, "clock_serif", "Serif", "Book-style numerals", pro = true),
        d(WidgetKind.ClockDigital, "clock_minimal", "Minimal", "Just the time"),
        d(WidgetKind.ClockDigital, "clock_greeting", "Greeting", "Good morning, with the time", pro = true),
        d(WidgetKind.ClockDigital, "clock_pill", "Pill", "Time and date in one line"),
        // Analog (4)
        d(WidgetKind.ClockAnalog, "analog_classic", "Classic", "Hour ticks"),
        d(WidgetKind.ClockAnalog, "analog_numerals", "Numerals", "12, 3, 6, 9", pro = true),
        d(WidgetKind.ClockAnalog, "analog_dots", "Dots", "Twelve dots"),
        d(WidgetKind.ClockAnalog, "analog_minimal", "Minimal", "Hands only", pro = true),
        // World (2)
        d(WidgetKind.WorldClock, "world_dual", "Two cities", "Your time and one more", needs = Needs.Zones),
        d(WidgetKind.WorldClock, "world_trio", "Three cities", "Your time and two more", pro = true, needs = Needs.Zones),
        // Date (4)
        d(WidgetKind.Date, "date_big", "Big date", "Day number, weekday and month"),
        d(WidgetKind.Date, "date_month", "Month", "This month with today marked"),
        d(WidgetKind.Date, "date_week", "Week strip", "Seven days, today highlighted"),
        d(WidgetKind.Date, "date_page", "Tear-off page", "Desk calendar page", pro = true),
        // Agenda (2)
        d(WidgetKind.Agenda, "agenda_next", "Next event", "Your next meeting", needs = Needs.Calendar),
        d(WidgetKind.Agenda, "agenda_list", "Agenda", "Next four events", pro = true, needs = Needs.Calendar),
        // Weather (5)
        d(WidgetKind.Weather, "weather_now", "Now", "Temperature and condition", needs = Needs.City),
        d(WidgetKind.Weather, "weather_big", "Big temperature", "One huge number", needs = Needs.City),
        d(WidgetKind.Weather, "weather_range", "High and low", "Today's range and feels-like", needs = Needs.City),
        d(WidgetKind.Weather, "weather_pill", "Pill", "Compact one-line weather", needs = Needs.City),
        d(WidgetKind.Weather, "weather_days", "Three days", "Forecast for the next days", pro = true, needs = Needs.City),
        // Battery (4)
        d(WidgetKind.Battery, "battery_percent", "Percent", "Big number"),
        d(WidgetKind.Battery, "battery_ring", "Ring", "Charge as a ring"),
        d(WidgetKind.Battery, "battery_bar", "Bar", "Charge bar and status"),
        d(WidgetKind.Battery, "battery_pill", "Pill", "Compact charge indicator"),
        // Steps (3)
        d(WidgetKind.Steps, "steps_count", "Steps", "Steps today", needs = Needs.Steps),
        d(WidgetKind.Steps, "steps_ring", "Goal ring", "Progress to your daily goal", pro = true, needs = Needs.Steps),
        d(WidgetKind.Steps, "steps_bar", "Goal bar", "Steps and distance", needs = Needs.Steps),
        // Music (3)
        d(WidgetKind.Music, "music_player", "Player", "Artwork, title and controls", needs = Needs.MediaAccess),
        d(WidgetKind.Music, "music_compact", "Compact", "Title and play button", needs = Needs.MediaAccess),
        d(WidgetKind.Music, "music_controls", "Controls", "Previous, play, next", pro = true, needs = Needs.MediaAccess),
        // Notes (2)
        d(WidgetKind.Note, "note_sticky", "Sticky note", "Tap to write", needs = Needs.Text),
        d(WidgetKind.Checklist, "checklist", "Checklist", "Tick items off on the home screen", needs = Needs.Text),
        // Quote (2)
        d(WidgetKind.Quote, "quote_serif", "Quote", "A new quote every day"),
        d(WidgetKind.Quote, "quote_bold", "Bold quote", "Big type, daily", pro = true),
        // Countdown (3)
        d(WidgetKind.Countdown, "countdown_days", "Days until", "Count down to a date", needs = Needs.CountdownDate),
        d(WidgetKind.Countdown, "progress_year", "Year progress", "How much of the year is gone"),
        d(WidgetKind.Countdown, "progress_day", "Day progress", "How much of today is gone", pro = true),
        // Apps (2)
        d(WidgetKind.Apps, "apps_row", "Four apps", "Your four favourite apps", needs = Needs.Apps),
        d(WidgetKind.Apps, "apps_grid", "Eight apps", "Two rows of apps", pro = true, needs = Needs.Apps),
        // Search (1)
        d(WidgetKind.Search, "search_pill", "Search", "Search the web"),
        // Photo (2)
        d(WidgetKind.Photo, "photo_frame", "Photo", "One photo you pick", needs = Needs.PhotoPick),
        d(WidgetKind.Photo, "photo_date", "Photo + date", "Photo with today's date", pro = true, needs = Needs.PhotoPick),
        // Focus (2)
        d(WidgetKind.Focus, "focus_start", "Focus", "Start a session in one tap"),
        d(WidgetKind.Focus, "focus_streak", "Focus streak", "Days in a row you focused"),
        // Alarm (1)
        d(WidgetKind.Alarm, "alarm_next", "Next alarm", "When your next alarm rings"),
        // Sky & device (2)
        d(WidgetKind.Sky, "moon_phase", "Moon phase", "Tonight's moon", pro = true),
        d(WidgetKind.Sky, "storage", "Storage", "Free space on your phone"),
    )

    fun byId(id: String?) = all.firstOrNull { it.id == id }
    fun forKind(kind: WidgetKind) = all.filter { it.kind == kind }
    fun default(kind: WidgetKind) = forKind(kind).first()
}
