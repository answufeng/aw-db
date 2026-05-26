package com.answufeng.db.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DemoSectionFragment : Fragment() {

    private val section: DemoSection by lazy {
        DemoSection.entries[requireArguments().getInt(ARG_SECTION_ORDINAL)]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_demo_section, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<android.widget.TextView>(R.id.tvSectionTitle)
            .setText(section.titleRes)
        view.findViewById<android.widget.TextView>(R.id.tvSectionDesc)
            .setText(section.descRes)

        val runner = activity as? DemoRunner ?: return
        val list = view.findViewById<RecyclerView>(R.id.rvActions)
        val adapter = DemoActionAdapter { runner.run(it) }
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        adapter.submitList(DemoCatalog.itemsFor(section))
    }

    companion object {
        private const val ARG_SECTION_ORDINAL = "section"

        fun newInstance(section: DemoSection): DemoSectionFragment =
            DemoSectionFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_ORDINAL, section.ordinal)
                }
            }
    }
}
