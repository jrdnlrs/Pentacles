package com.example.forenscan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class Learn_LearnMoreFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Ensure this matches the file you just created: fragment_learn_more
        return inflater.inflate(R.layout.fragment_learn_more, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. App Guide Button -> Go BACK to Home
        val appGuideBtn = view.findViewById<Button>(R.id.btn_app_guide)
        appGuideBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, LearnFragment())
                .addToBackStack(null)
                .commit()
        }

        // 2. Security Tips Button -> Go to Security Tips
        val securityTipsBtn = view.findViewById<Button>(R.id.btn_security_tips)
        securityTipsBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, Learn_SecurityTipsFragment())
                .addToBackStack(null)
                .commit()
        }

        // Note: "Learn More" button does nothing because we are already here.
    }
}