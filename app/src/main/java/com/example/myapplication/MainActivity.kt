package com.example.myapplication

import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.reflect.Field


class MainActivity : AppCompatActivity() {

    private val mSnapHelper = PagerSnapHelper()
    private var mAdapter: DemoAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAdapter = DemoAdapter()
        val layoutManager = GalleryLayoutManager(GalleryLayoutManager.HORIZONTAL)
        layoutManager.attach(rv_demo, 2)
        //setup adapter for your RecycleView
        mSnapHelper.attachToRecyclerView(rv_demo)
        rv_demo.setAdapter(mAdapter)
        rv_demo.setOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var currentPage = -1
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) { //如果滚动结束
                    val snapView: View = mSnapHelper.findSnapView(layoutManager)!!
                    val currentPageIndex: Int = layoutManager.getPosition(snapView)
                    if (currentPage != currentPageIndex) { //防止重复提示
                        currentPage = currentPageIndex
//                        layoutManager.mCurSelectedPosition=currentPageIndex
                        Toast.makeText(
                            this@MainActivity,
                            "当前是第" + currentPageIndex + "页",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })

        v_click.setOnClickListener {
//            rv_demo.smoothScrollToPosition(17)
            rv_demo.scrollToPosition(17)
        }
//        Handler().postDelayed({rv_demo.smoothScrollToPosition(4)},5000)
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