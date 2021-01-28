package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.reflect.Field


class MainActivity : AppCompatActivity() {

    private var mAdapter: DemoAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAdapter = DemoAdapter()
        val layoutManager = GalleryLayoutManager(GalleryLayoutManager.HORIZONTAL)
        layoutManager.attach(rv_demo, 2)
        rv_demo.adapter = mAdapter

        v_click.setOnClickListener {
            rv_demo.smoothScrollToPosition(17)
//            rv_demo.scrollToPosition(17)
        }
        layoutManager.setOnItemSelectedListener(object :
            GalleryLayoutManager.OnItemSelectedListener {
            override fun onItemSelected(position: Int) {
                Log.d("MainActivity", "onItemSelected position=$position")
            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })
        setMaxFlingVelocity(rv_demo)
    }

    private fun setMaxFlingVelocity(recyclerView: RecyclerView) {
        try {
            val field: Field = recyclerView.javaClass.getDeclaredField("mMaxFlingVelocity")
            field.isAccessible = true
            field[recyclerView] = 2100
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}