package com.arjanvlek.oxygenupdater.activities

import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.extensions.enableEdgeToEdgeUiSupport

/**
 * Sets support action bar and enables home up button on the toolbar.
 * Additionally, it sets up a full screen activity if its theme is [R.style.Theme_Oxygen_FullScreen]
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
abstract class SupportActionBarActivity : AppCompatActivity() {

    override fun setContentView(
        @LayoutRes layoutResId: Int
    ) = super.setContentView(layoutResId).also {
        setupToolbar()
    }

    override fun setContentView(
        view: View
    ) = super.setContentView(view).also {
        setupToolbar()
    }

    override fun setContentView(
        view: View,
        params: ViewGroup.LayoutParams
    ) = super.setContentView(view, params).also {
        setupToolbar()
    }

    private fun setupToolbar() {
        // Postpone the transition until the window's decor view has finished its layout.
        postponeEnterTransition()

        val decor = window.decorView
        decor.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                decor.viewTreeObserver.removeOnPreDrawListener(this)
                startPostponedEnterTransition()
                return true
            }
        })

        // We must use `findViewById` because
        // neither ViewBinding nor Kotlin View Extensions will correctly resolve an individual activity's toolbar
        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // allow activity to draw itself full screen
        enableEdgeToEdgeUiSupport()
    }
}
