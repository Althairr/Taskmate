package com.example.taskmate

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import java.util.Calendar
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import com.example.taskmate.kategori.KategoriActivity


class ArsipFragment : Fragment() {
    private lateinit var buttonSemua: Button
    private lateinit var buttonMachineLearning: Button
    private lateinit var buttonRTI: Button
    private lateinit var tripleDot: ImageView

    private lateinit var currentTimeTextView: TextView
    private val handler = Handler(Looper.getMainLooper())

    // Runnable to update the time
    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            updateTime()
            handler.postDelayed(this, 1000) // Update every second
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_arsip, container, false)

        currentTimeTextView = view.findViewById(R.id.current_time)

        // Initialize the buttons
        buttonSemua = view.findViewById(R.id.buttonSemua)
        buttonMachineLearning = view.findViewById(R.id.buttonMachineLearning)
        buttonRTI = view.findViewById(R.id.buttonRti)
        tripleDot = view.findViewById(R.id.more_vert)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        handler.post(updateTimeRunnable) // Start the time update

        // Make "Semua" button selected by default
        buttonSemua.isSelected = true
        replaceFragment(SemuaFragment()) // Default to "SemuaFragment"

        // Set click listeners to change the selection and fragment
        buttonSemua.setOnClickListener {
            replaceFragment(SemuaFragment())
            buttonSemua.isSelected = true
            buttonMachineLearning.isSelected = false
            buttonRTI.isSelected = false
        }

        buttonMachineLearning.setOnClickListener {
            replaceFragment(MachineLearningFragment())
            buttonSemua.isSelected = false
            buttonMachineLearning.isSelected = true
            buttonRTI.isSelected = false
        }

        buttonRTI.setOnClickListener {
            replaceFragment(RtiFragment())
            buttonSemua.isSelected = false
            buttonMachineLearning.isSelected = false
            buttonRTI.isSelected = true
        }

        // Set click listener for the triple dot (more_vert) button to start KategoriActivity
        tripleDot.setOnClickListener {
            val intent = Intent(requireContext(), KategoriActivity::class.java)
            startActivity(intent)
        }
    }

    // Method to update time
    private fun updateTime() {
        val calendar = Calendar.getInstance()
        val currentTime = DateFormat.format("HH:mm", calendar).toString()
        currentTimeTextView.text = currentTime
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateTimeRunnable) // Stop updating time when fragment is destroyed
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = childFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragmentContainer, fragment)
        fragmentTransaction.addToBackStack(null)  // Optional: Add to backstack to allow navigation back
        fragmentTransaction.commit()
    }
}
