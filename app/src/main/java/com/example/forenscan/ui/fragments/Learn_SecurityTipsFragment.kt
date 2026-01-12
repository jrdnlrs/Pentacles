package com.example.forenscan.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.forenscan.R

class Learn_SecurityTipsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // If this is still red, go to "Build" menu -> "Clean Project"
        return inflater.inflate(R.layout.fragment_learn_security_tips, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. App Guide Button
        val appGuideBtn = view.findViewById<Button>(R.id.btn_app_guide)
        appGuideBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, LearnFragment())
                .addToBackStack(null)
                .commit()
        }

        // 2. Learn More Button
        val learnMoreBtn = view.findViewById<Button>(R.id.btn_learn_more)
        learnMoreBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, Learn_LearnMoreFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}