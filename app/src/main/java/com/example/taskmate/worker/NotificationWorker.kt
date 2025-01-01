package com.example.taskmate.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.taskmate.R

class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val taskName = inputData.getString("taskName") ?: ""
        val category = inputData.getString("category") ?: ""
        val timeDeadline = inputData.getString("deadlineAndTime") ?: "Waktu deadlinenya"
        val time = inputData.getString("time") ?: "Mendekati harinya"

        // Create Notification
        val channelId = "taskmate_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Task Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.logo_taskmate)
            .setContentTitle("Pengingat tugas: $taskName")
            .setContentText("Nama Kategori: $category. \nAyo selesaikan tugasnya sebelum \n$timeDeadline!")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Nama Kategori: $category. \nAyo selesaikan tugasnya sebelum \n$timeDeadline!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()


        notificationManager.notify(taskName.hashCode(), notification)

        return Result.success()
    }
}
