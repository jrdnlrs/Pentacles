package com.example.forenscan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class LearnFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_learn, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- NAVIGATION TABS LOGIC ---
        // 1. Security Tips Button -> Go to Security Tips
        val securityTipsBtn = view.findViewById<Button>(R.id.btn_security_tips)
        securityTipsBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, Learn_SecurityTipsFragment())
                .addToBackStack(null)
                .commit()
        }

        // 2. Learn More Button -> Go to Learn More
        val learnMoreBtn = view.findViewById<Button>(R.id.btn_learn_more)
        learnMoreBtn.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, Learn_LearnMoreFragment())
                .addToBackStack(null)
                .commit()
        }

        // Note: "App Guide" does nothing here because we are already on that page.


        // --- EXPANDABLE CARDS LOGIC ---
        // We call our helper function for each card we want to work
        setupExpandableCard(view, R.id.header_item_1, R.id.body_item_1)
        setupExpandableCard(view, R.id.header_item_2, R.id.body_item_2)
        setupExpandableCard(view, R.id.header_item_3, R.id.body_item_3)
        setupExpandableCard(view, R.id.header_item_4, R.id.body_item_4)
        setupExpandableCard(view, R.id.header_item_5, R.id.body_item_5)
    }

    // --- HELPER FUNCTION ---
    // This function takes the IDs of a header and a body, finds them,
    // and sets up the click listener to toggle visibility.
    private fun setupExpandableCard(parentView: View, headerId: Int, bodyId: Int) {
        val header = parentView.findViewById<View>(headerId)
        val body = parentView.findViewById<View>(bodyId)

        // Safety check: make sure the views actually exist to prevent crashes
        if (header != null && body != null) {
            header.setOnClickListener {
                if (body.visibility == View.VISIBLE) {
                    body.visibility = View.GONE
                } else {
                    body.visibility = View.VISIBLE
                }
            }
        }
    }
}