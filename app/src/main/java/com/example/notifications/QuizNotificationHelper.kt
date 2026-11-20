package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.QuizAttempt
import com.example.model.WeeklyReviewScheduler

object QuizNotificationHelper {
    const val CHANNEL_ID = "quizzy_quiz_validated"
    const val SUNDAY_REVIEW_CHANNEL_ID = "quizzy_sunday_review"
    private const val CHANNEL_NAME = "Quiz validés par les enfants"
    private const val CHANNEL_DESCRIPTION = "Alerte le parent à chaque fois qu'un enfant termine et valide un quiz."
    private const val SUNDAY_CHANNEL_NAME = "Révision du dimanche (14h)"
    private const val SUNDAY_CHANNEL_DESCRIPTION = "Rappelle chaque dimanche à 14h le quiz de 10 questions sur les erreurs de la semaine."
    private const val PREFS_NAME = "quizzy_parent_notifications"
    private const val SUNDAY_ALARM_REQUEST_CODE = 14007

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    enableVibration(true)
                    setShowBadge(true)
                }
                manager.createNotificationChannel(channel)
            }
            if (manager.getNotificationChannel(SUNDAY_REVIEW_CHANNEL_ID) == null) {
                val sundayChannel = NotificationChannel(
                    SUNDAY_REVIEW_CHANNEL_ID,
                    SUNDAY_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = SUNDAY_CHANNEL_DESCRIPTION
                    enableVibration(true)
                    setShowBadge(true)
                }
                manager.createNotificationChannel(sundayChannel)
            }
        }
    }

    fun buildQuizValidatedNotificationContent(attempt: QuizAttempt): Pair<String, String> {
        val safeChildName = attempt.childName.trim().ifBlank { "Ton enfant" }
        val title = "Quiz validé par $safeChildName !"
        val xpPart = if (attempt.xpEarned > 0L) " (+${attempt.xpEarned} XP)" else ""
        val body = buildString {
            append("$safeChildName vient de valider un quiz")
            if (attempt.subject.isNotBlank()) {
                append(" de ${attempt.subject}")
            }
            if (attempt.lessonTitle.isNotBlank()) {
                append(" (« ${attempt.lessonTitle} »)")
            }
            append(" : ${attempt.score}/${attempt.totalQuestions} (${attempt.percentage} %)$xpPart.")
        }
        return title to body
    }

    fun showQuizValidatedNotification(context: Context, attempt: QuizAttempt): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannel(context)

        val (title, body) = buildQuizValidatedNotificationContent(attempt)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(
            context,
            attempt.id.hashCode(),
            openIntent,
            pendingFlags
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            val notificationId = if (attempt.id.isNotBlank()) {
                attempt.id.hashCode()
            } else {
                System.currentTimeMillis().toInt()
            }
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun showSundayReviewReminderNotification(
        context: Context,
        childName: String,
        missedCount: Int
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannel(context)

        val safeName = childName.trim().ifBlank { "Champion" }
        val title = "Quiz du Dimanche (14h) prêt pour $safeName !"
        val body = if (missedCount > 0) {
            "Il est 14h ! Relève le défi de 10 nouvelles questions sur tes $missedCount thématiques à renforcer de la semaine."
        } else {
            "Il est 14h ! Ouvre Quizzy pour ton quiz de révision hebdomadaire de 10 questions."
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            SUNDAY_ALARM_REQUEST_CODE,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, SUNDAY_REVIEW_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(SUNDAY_ALARM_REQUEST_CODE, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    /**
     * Schedules an inexact reminder for next Sunday at 14:00 (Play-Policy compliant,
     * requires zero restricted exact-alarm permissions).
     */
    fun scheduleNextSundayReviewAlarm(
        context: Context,
        childName: String,
        missedCount: Int
    ) {
        if (missedCount <= 0) return
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAtMillis = WeeklyReviewScheduler.nextSunday14hMillis()
        val intent = Intent(appContext, SundayReviewAlarmReceiver::class.java).apply {
            putExtra(SundayReviewAlarmReceiver.EXTRA_CHILD_NAME, childName)
            putExtra(SundayReviewAlarmReceiver.EXTRA_MISSED_COUNT, missedCount)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            SUNDAY_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        runCatching {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun isFamilySeeded(context: Context, familyId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("seeded_$familyId", false)
    }

    fun getNotifiedAttemptIds(context: Context, familyId: String): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet("notified_$familyId", emptySet())?.toSet() ?: emptySet()
    }

    fun markAttemptsNotified(context: Context, familyId: String, ids: Collection<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val merged = (getNotifiedAttemptIds(context, familyId) + ids).toList().takeLast(200).toSet()
        prefs.edit()
            .putBoolean("seeded_$familyId", true)
            .putStringSet("notified_$familyId", merged)
            .apply()
    }

    fun getReadAttemptIds(context: Context, familyId: String): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet("read_$familyId", emptySet())?.toSet() ?: emptySet()
    }

    fun markAttemptsRead(context: Context, familyId: String, ids: Collection<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val merged = (getReadAttemptIds(context, familyId) + ids).toList().takeLast(200).toSet()
        prefs.edit()
            .putStringSet("read_$familyId", merged)
            .apply()
    }
}
