package com.answufeng.db.demo

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class DemoPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = DemoSection.entries.size

    override fun createFragment(position: Int): Fragment =
        DemoSectionFragment.newInstance(DemoSection.entries[position])
}
