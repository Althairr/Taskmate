    package com.example.taskmate

    import android.content.Context
    import android.os.Bundle
    import android.os.Handler
    import android.os.Looper
    import android.text.format.DateFormat
    import android.util.Log
    import android.view.GestureDetector
    import android.view.LayoutInflater
    import android.view.MotionEvent
    import android.view.View
    import android.view.ViewGroup
    import android.widget.ImageView
    import android.widget.TextView
    import androidx.core.content.ContextCompat
    import androidx.fragment.app.Fragment
    import androidx.recyclerview.widget.LinearLayoutManager
    import androidx.recyclerview.widget.RecyclerView
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.firestore.FirebaseFirestore
    import com.example.taskmate.databinding.FragmentHomeBinding
    import java.util.Calendar
    import com.example.taskmate.carousel.CarouselItem
    import java.text.SimpleDateFormat
    import java.util.Date
    import java.util.Locale

    class HomeFragment : Fragment() {
        private lateinit var binding: FragmentHomeBinding
        private lateinit var adapter: GroupedTaskAdapter
        private val groupedItems = mutableListOf<ListItem>()
        private lateinit var currentTimeTextView: TextView
        private val handler = Handler(Looper.getMainLooper())
        private lateinit var dateViews: List<TextView>
        private lateinit var dayViews: List<TextView>
        private var currentCarouselPosition = 0

        // Carousel data
        private val carouselItems = listOf(
            CarouselItem(
                "INFORMASI BERANDA",
                "Beranda menampilkan daftar tugas yang belum terselesaikan, diurutkan berdasarkan prioritas dengan tugas yang paling mendesak berada di bagian atas. Pengguna dapat menekan salah satu tugas untuk melihat detail informasi, termasuk deskripsi, tenggat waktu, dan kategori tugas."
            ),
            CarouselItem(
                "INFORMASI KALENDER",
                "Halaman kalender menunjukkan informasi tentang tanggal-tanggal yang memiliki tugas, mempermudah pengguna melacak jadwal. Selain itu, kalender menyoroti tanggal hari ini sehingga pengguna dapat langsung mengetahui tugas apa saja yang harus diselesaikan pada hari tersebut."
            ),
            CarouselItem(
                "INFORMASI TAMBAH TUGAS",
                "Halaman ini memungkinkan pengguna menambahkan tugas baru dengan detail lengkap, termasuk nama kategori tugas, nama tugas, waktu tenggat tugas, serta waktu notifikasi. Notifikasi akan membantu mengingatkan pengguna untuk menyelesaikan tugas sebelum tenggat waktu."
            ),
            CarouselItem(
                "INFORMASI ARSIP",
                "Arsip berisi daftar tugas yang telah selesai maupun yang telah melewati tenggat waktu. Pengguna dapat melihat dan memfilter tugas berdasarkan kategori tertentu untuk mempermudah pencarian tugas yang sudah dikelompokkan sebelumnya."
            ),
            CarouselItem(
                "INFORMASI AKUN",
                "Halaman akun memberikan fitur personalisasi seperti mengganti nama pengguna, foto profil, dan kata sandi. Selain itu, pengguna juga dapat keluar dari akun atau menghapus akun jika diperlukan."
            )
        )

        private val updateTimeRunnable = object : Runnable {
            override fun run() {
                updateTime()
                handler.postDelayed(this, 1000)
            }
        }

        private val carouselRunnable = object : Runnable {
            override fun run() {
                currentCarouselPosition = (currentCarouselPosition + 1) % carouselItems.size
                updateCarousel()
                handler.postDelayed(this, 5000) // Change slide every 5 seconds
            }
        }

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            binding = FragmentHomeBinding.inflate(inflater, container, false)
            currentTimeTextView = binding.currentTime1

            // Initialize date and day views
            initializeDateAndDayViews()

            return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            // Start time updates
            handler.post(updateTimeRunnable)

            // Setup date display
            setupDateDisplay()

            // Setup carousel
            setupCarousel()

            // Set layout manager for RecyclerView
            binding.taskRecyclerView.layoutManager = LinearLayoutManager(requireContext())

            // Initialize the adapter with an empty list
            adapter = GroupedTaskAdapter(groupedItems)
            binding.taskRecyclerView.adapter = adapter

            // Fetch tasks from Firebase
            fetchTasksFromFirebase()
        }

        private fun setupCarousel() {
            // Initial update
            updateCarousel()

            // Start auto-sliding
            handler.postDelayed(carouselRunnable, 5000)

            // Setup touch listeners for manual navigation
            binding.informationBox.setOnTouchListener(object : OnSwipeTouchListener(requireContext()) {
                override fun onSwipeLeft() {
                    currentCarouselPosition = (currentCarouselPosition + 1) % carouselItems.size
                    updateCarousel()
                    resetCarouselTimer()
                }

                override fun onSwipeRight() {
                    currentCarouselPosition = if (currentCarouselPosition > 0) {
                        currentCarouselPosition - 1
                    } else {
                        carouselItems.size - 1
                    }
                    updateCarousel()
                    resetCarouselTimer()
                }
            })

            // Setup indicator clicks
            val indicators = listOf(
                binding.indicator1,
                binding.indicator2,
                binding.indicator3,
                binding.indicator4,
                binding.indicator5
            )

            indicators.forEachIndexed { index, indicator ->
                indicator.setOnClickListener {
                    currentCarouselPosition = index
                    updateCarousel()
                    resetCarouselTimer()
                }
            }
        }

        private fun resetCarouselTimer() {
            handler.removeCallbacks(carouselRunnable)
            handler.postDelayed(carouselRunnable, 5000)
        }

        private fun updateCarousel() {
            val currentItem = carouselItems[currentCarouselPosition]

            // Update text with animation
            binding.informationTitle.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    binding.informationTitle.text = currentItem.title
                    binding.informationTitle.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .start()
                }
                .start()

            binding.informationText.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    binding.informationText.text = currentItem.content
                    binding.informationText.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .start()
                }
                .start()

            // Update indicators
            updateIndicators()
        }

        private fun updateIndicators() {
            val indicators = listOf(
                binding.indicator1,
                binding.indicator2,
                binding.indicator3,
                binding.indicator4,
                binding.indicator5
            )

            indicators.forEachIndexed { index, indicator ->
                indicator.setImageResource(
                    if (index == currentCarouselPosition)
                        R.drawable.filled_circle
                    else
                        R.drawable.empty_circle
                )
            }
        }

        private fun initializeDateAndDayViews() {
            // Initialize date views using binding
            dateViews = listOf(
                binding.date5,
                binding.date6,
                binding.date7,
                binding.date8,
                binding.date9,
                binding.date10,
                binding.date11
            )

            // Initialize day views using binding
            dayViews = listOf(
                binding.day1,
                binding.day2,
                binding.day3,
                binding.day4,
                binding.day5,
                binding.day6,
                binding.day7
            )
        }

        private fun setupDateDisplay() {
            val calendar = Calendar.getInstance()

            // Set up dates and days
            for (i in 0 until 7) {
                val dayCalendar = Calendar.getInstance()
                dayCalendar.add(Calendar.DAY_OF_MONTH, i - 3) // Center current date

                // Set date number
                dateViews[i].text = dayCalendar.get(Calendar.DAY_OF_MONTH).toString()

                // Set day name in Indonesian
                val dayName = when (dayCalendar.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> "Sen"
                    Calendar.TUESDAY -> "Sel"
                    Calendar.WEDNESDAY -> "Rab"
                    Calendar.THURSDAY -> "Kam"
                    Calendar.FRIDAY -> "Jum"
                    Calendar.SATURDAY -> "Sab"
                    Calendar.SUNDAY -> "Min"
                    else -> ""
                }
                dayViews[i].text = dayName

                // Check if this is today
                val isToday = i == 3 // Since we centered the current date
                if (isToday) {
                    dateViews[i].setBackgroundResource(R.drawable.circle_red)
                    dateViews[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.cream))
                    dayViews[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                } else {
                    dateViews[i].setBackgroundResource(R.drawable.circle_pink)
                    dateViews[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.cream))
                    dayViews[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.pink))
                }
            }
        }

        private fun updateTime() {
            val calendar = Calendar.getInstance()
            val currentTime = DateFormat.format("HH:mm", calendar).toString()
            binding.currentTime1.text = currentTime
        }

        private fun fetchTasksFromFirebase() {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            Log.d("HomeFragment", "User ID: $userId")

            if (userId == null) {
                Log.e("HomeFragment", "User not authenticated")
                return
            }

            val db = FirebaseFirestore.getInstance().collection("tasks")
                .whereEqualTo("userId", userId)

            db.addSnapshotListener { querySnapshot, e ->
                if (e != null) {
                    Log.e("HomeFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (querySnapshot != null) {
                    Log.d("HomeFragment", "Documents count: ${querySnapshot.size()}")

                    groupedItems.clear()
                    val taskMap = mutableMapOf<String, MutableMap<String, MutableList<TaskItem>>>()

                    for (taskDocument in querySnapshot.documents) {
                        val taskId = taskDocument.id // Task ID
                        val deadlineAndTime = taskDocument.getString("deadlineAndTime")
                        val taskName = taskDocument.getString("taskName")
                        val category = taskDocument.getString("category")
                        val isCompleted = taskDocument.getBoolean("status") ?: false

                        if (deadlineAndTime != null && taskName != null && category != null) {
                            val (time, date) = splitDate(deadlineAndTime)

                            // Check if the task is already past the deadline
                            if (isTaskPastDeadline(date)) {
                                // Skip tasks that are past the deadline
                                continue
                            }

                            // Create TaskItem object with taskId, taskName, time, and isCompleted
                            val taskItem = TaskItem(taskId, taskName, time, isCompleted)

                            // Group by date first, then by category
                            if (taskMap[date] == null) {
                                taskMap[date] = mutableMapOf()
                            }

                            if (taskMap[date]?.get(category) == null) {
                                taskMap[date]?.put(category, mutableListOf())
                            }

                            taskMap[date]?.get(category)?.add(taskItem)
                        }
                    }

                    // Now display: Group by date first, then category within that date
                    for ((date, categoryMap) in taskMap) {
                        // Add the date header
                        groupedItems.add(DateHeader(date))

                        // For each category within this date, add the category header
                        for ((category, tasks) in categoryMap) {
                            // Add the category header
                            groupedItems.add(CategoryHeader(category))

                            // Add the tasks for this category
                            groupedItems.addAll(tasks)
                        }
                    }

                    adapter.notifyDataSetChanged()

                    if (groupedItems.isEmpty()) {
                        binding.emptyTaskMessage.visibility = View.VISIBLE
                        binding.taskRecyclerView.visibility = View.GONE
                        Log.e("HomeFragment", "No tasks available")
                    } else {
                        binding.emptyTaskMessage.visibility = View.GONE
                        binding.taskRecyclerView.visibility = View.VISIBLE
                    }
                }
            }
        }

        private fun isTaskPastDeadline(deadline: String): Boolean {
            val dateFormat = SimpleDateFormat("HH:mm, d MMMM yyyy", Locale.getDefault())
            return try {
                val taskDeadline = dateFormat.parse(deadline) // Parse tanggal dari string
                val currentDate = Date() // Tanggal saat ini
                taskDeadline != null && taskDeadline.before(currentDate) // Bandingkan
            } catch (e: Exception) {
                e.printStackTrace()
                false // Jika parsing gagal, anggap tidak melewati batas waktu
            }
        }


        private fun splitDate(deadlineAndTime: String): Pair<String, String> {
            val parts = deadlineAndTime.split(", ")
            return if (parts.size == 2) {
                Pair(parts[0], parts[1])
            } else {
                Pair("", "")
            }
        }

        override fun onDestroyView() {
            super.onDestroyView()
            handler.removeCallbacks(updateTimeRunnable)
            handler.removeCallbacks(carouselRunnable)
        }
    }

    // OnSwipeTouchListener.kt
    open class OnSwipeTouchListener(context: Context) : View.OnTouchListener {
        private val gestureDetector = GestureDetector(context, GestureListener())

        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            return event?.let { gestureDetector.onTouchEvent(it) } ?: false
        }

        private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val diffX = e2.x - (e1?.x ?: 0f)
                val diffY = e2.y - (e1?.y ?: 0f)

                if (kotlin.math.abs(diffX) > kotlin.math.abs(diffY)) {
                    if (kotlin.math.abs(diffX) > SWIPE_THRESHOLD &&
                        kotlin.math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight()
                        } else {
                            onSwipeLeft()
                        }
                        return true
                    }
                }
                return false
            }
        }

        open fun onSwipeRight() {}
        open fun onSwipeLeft() {}
    }