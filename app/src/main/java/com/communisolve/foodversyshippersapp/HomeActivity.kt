package com.communisolve.foodversyshippersapp

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.communisolve.foodversyshippersapp.common.Common
import com.communisolve.foodversyshippersapp.databinding.ActivityHomeBinding
import com.communisolve.foodversyshippersapp.ui.ShippingActivity
import com.google.firebase.auth.FirebaseAuth
import io.paperdb.Paper

class HomeActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarHome.toolbar)

        checkStartTrip()
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_home)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_signOut
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val headerView = navView.getHeaderView(0)
        var txt_user = headerView.findViewById<TextView>(R.id.txt_user)
        txt_user.setText("Welcome, ${Common.currentShipperUser!!.name}")

        navView.menu.findItem(R.id.nav_signOut).setOnMenuItemClickListener {
            FirebaseAuth.getInstance().signOut()
            Common.currentShipperUser = null
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return@setOnMenuItemClickListener true
        }

    }

    private fun checkStartTrip() {
        Paper.init(this)
        val data = Paper.book().read<String>(Common.TRIP_DATA)

        if (!TextUtils.isEmpty(data))
            startActivity(Intent(this,ShippingActivity::class.java))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_home)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onResume() {
        super.onResume()
        checkStartTrip()
    }
}