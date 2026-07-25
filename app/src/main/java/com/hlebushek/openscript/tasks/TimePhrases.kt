package com.hlebushek.openscript.tasks

object TimePhrases {
    private const val CURRENT_TIME_COMMAND = "what time is it"
    private const val ALARM_COMMAND = "set alarm for 07:30"
    private const val TIMER_COMMAND = "set timer for 10 minutes"

    private val enCurrentTime = setOf(
        "what time is it",
        "what is the time",
        "what's the time",
        "whats the time",
        "current time",
        "current time now",
        "time now",
        "tell me the time",
        "show me the time",
        "show current time",
        "the time please",
        "do you have the time"
    )

    private val ruCurrentTime = setOf(
        "который час",
        "сколько времени",
        "текущее время",
        "какое сейчас время",
        "время сейчас",
        "покажи время",
        "покажи текущее время",
        "скажи время",
        "скажи текущее время",
        "что за время сейчас",
        "сколько времени сейчас",
        "не подскажешь который час"
    )

    private val enAlarm = setOf(
        "set alarm for 07:30",
        "set an alarm for 07:30",
        "create alarm for 07:30",
        "add alarm for 07:30",
        "wake me up at 07:30",
        "alarm at 07:30",
        "set alarm to 07:30",
        "set my alarm for 07:30",
        "schedule alarm for 07:30",
        "make an alarm at 07:30",
        "set alarm clock for 07:30",
        "set a wake up alarm for 07:30"
    )

    private val ruAlarm = setOf(
        "поставь будильник на 07:30",
        "поставь будильник на 07:30 утра",
        "создай будильник на 07:30",
        "добавь будильник на 07:30",
        "разбуди меня в 07:30",
        "будильник на 07:30",
        "установи будильник на 07:30",
        "поставь мой будильник на 07:30",
        "запланируй будильник на 07:30",
        "сделай будильник в 07:30",
        "поставь будильник на 7:30",
        "разбуди меня в 7:30 утра"
    )

    private val enTimer = setOf(
        "set timer for 10 minutes",
        "start timer for 10 minutes",
        "create timer for 10 minutes",
        "timer for 10 minutes",
        "countdown for 10 minutes",
        "set a 10 minute timer",
        "start a 10 minute timer",
        "set timer to 10 minutes",
        "set my timer for 10 minutes",
        "make a timer for 10 minutes",
        "start a timer for 10 min",
        "set countdown for 10 minutes"
    )

    private val ruTimer = setOf(
        "поставь таймер на 10 минут",
        "запусти таймер на 10 минут",
        "создай таймер на 10 минут",
        "таймер на 10 минут",
        "обратный отсчет на 10 минут",
        "установи таймер на 10 минут",
        "запусти 10 минутный таймер",
        "поставь таймер на 10 мин",
        "установи мой таймер на 10 минут",
        "сделай таймер на 10 минут",
        "запусти таймер на десять минут",
        "поставь таймер на 10 минут и запусти"
    )

    fun findCommand(input: String, language: String): String? {
        val normalized = TimeParser.normalize(input)

        if (normalized in enCurrentTime || normalized in ruCurrentTime) {
            return CURRENT_TIME_COMMAND
        }

        if (normalized in enAlarm || normalized in ruAlarm) {
            return ALARM_COMMAND
        }

        if (normalized in enTimer || normalized in ruTimer) {
            return TIMER_COMMAND
        }

        return null
    }
}
