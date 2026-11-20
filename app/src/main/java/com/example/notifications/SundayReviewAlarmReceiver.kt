package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SundayReviewAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val childName = intent.getStringExtra(EXTRA_CHILD_NAME).orEmpty()
        val missedCount = intent.getIntExtra(EXTRA_MISSED_COUNT, 0)
        QuizNotificationHelper.showSundayReviewReminderNotification(
            context = context,
            childName = childName,
            missedCount = missedCount
        )
    }

    companion object {
        const val EXTRA_CHILD_NAME = "extra_child_name"
        const val EXTRA_MISSED_COUNT = "extra_missed_count"
    }
}
