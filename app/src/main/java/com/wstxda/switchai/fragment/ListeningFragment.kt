package com.wstxda.switchai.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.wstxda.switchai.R

class ListeningFragment : Fragment(R.layout.fragment_listening) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.cancelListening).setOnClickListener {
            findNavController().navigateUp()
        }
    }
}
