package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import javax.xml.transform.Transformer


class MainActivity : AppCompatActivity() {

    private var mAdapter: DemoAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAdapter = DemoAdapter()
        val layoutManager = GalleryLayoutManager(GalleryLayoutManager.HORIZONTAL)
        //layoutManager.attach(mPagerRecycleView);  default selected position is 0
        //layoutManager.attach(mPagerRecycleView);  default selected position is 0
        layoutManager.attach(rv_demo, 30)
        //设置滑动缩放效果

        //设置滑动缩放效果
//        layoutManager.setItemTransformer(Transformer())

        //...
        //setup adapter for your RecycleView

        //...
        //setup adapter for your RecycleView
        rv_demo.setAdapter(mAdapter)
    }
}