package me.xopyip.webremote

import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.*

class MainFragment : androidx.leanback.app.DetailsSupportFragment() {

    private var mBackgroundTimer: Timer? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onActivityCreated(savedInstanceState)

        setupUIElements()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: " + mBackgroundTimer?.toString())
        mBackgroundTimer?.cancel()
    }

    private fun setupUIElements() {
        title = getString(R.string.browse_title)
        searchAffordanceColor = ContextCompat.getColor(requireContext(), R.color.search_opaque)
    }

    companion object {
        private val TAG = "MainFragment"
    }
}