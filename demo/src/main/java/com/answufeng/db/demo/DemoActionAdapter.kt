package com.answufeng.db.demo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class DemoActionAdapter(
    private val onRun: (DemoAction) -> Unit
) : ListAdapter<DemoActionItem, DemoActionAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_demo_action, parent, false)
        return VH(view, onRun)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(itemView: View, private val onRun: (DemoAction) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvActionTitle)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvActionSubtitle)
        private val tvApi: TextView = itemView.findViewById(R.id.tvApiTag)
        private val btnRun: MaterialButton = itemView.findViewById(R.id.btnRun)

        fun bind(item: DemoActionItem) {
            val ctx = itemView.context
            tvTitle.setText(item.titleRes)
            tvSubtitle.setText(item.subtitleRes)
            tvApi.setText(item.apiTagRes)
            val run = { onRun(item.action) }
            btnRun.setOnClickListener { run() }
            itemView.setOnClickListener { run() }
        }
    }

    private object Diff : DiffUtil.ItemCallback<DemoActionItem>() {
        override fun areItemsTheSame(old: DemoActionItem, new: DemoActionItem) =
            old.action == new.action

        override fun areContentsTheSame(old: DemoActionItem, new: DemoActionItem) =
            old == new
    }
}
