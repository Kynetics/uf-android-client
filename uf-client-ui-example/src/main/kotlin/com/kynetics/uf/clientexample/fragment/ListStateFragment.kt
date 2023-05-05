/*
 * Copyright © 2017-2023  Kynetics  LLC
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package com.kynetics.uf.clientexample.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.kynetics.uf.android.api.v1.UFServiceMessageV1
import com.kynetics.uf.clientexample.R
import com.kynetics.uf.clientexample.activity.MainActivity
import com.kynetics.uf.clientexample.data.MessageHistory
import com.kynetics.uf.clientexample.data.MessageHistory.CAPACITY
import com.kynetics.uf.clientexample.data.toDate
import kotlinx.android.synthetic.main.state_list_content.view.*
import kotlinx.android.synthetic.main.state_list_fragment.view.*
import kotlin.math.min

class ListStateFragment : androidx.fragment.app.Fragment(), UFServiceInteractionFragment {

    var twoPane = false
    var selectedItem = -1
    var currentSize = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.containsKey(ARG_TWO_PANE)) {
                // Load the dummy content specified by the fragment
                // arguments. In a real-world scenario, use a Loader
                // to load content from a content provider.
                twoPane = it.getBoolean(ARG_TWO_PANE)
            }
        }
        childFragmentManager.addFragmentOnAttachListener { _,_ ->
            adapter?.notifyDataSetChanged()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val rootView = inflater.inflate(R.layout.state_list_fragment, container, false)

        setupRecyclerView(rootView.state_list_recycler_view)
        return rootView
    }

    var adapter: SimpleItemRecyclerViewAdapter? = null

    private fun setupRecyclerView(recyclerView: androidx.recyclerview.widget.RecyclerView) {
        adapter = SimpleItemRecyclerViewAdapter(requireActivity(), MessageHistory.ITEMS, twoPane)
        recyclerView.adapter = adapter
    }

    inner class SimpleItemRecyclerViewAdapter(
            private val parentActivity: androidx.fragment.app.FragmentActivity,
            private val values: List<MessageHistory.StateEntry>,
            private val twoPane: Boolean
    ) :
            androidx.recyclerview.widget.RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        private val onClickListener: View.OnClickListener

        init {
            onClickListener = View.OnClickListener { v ->
                val item = v.tag as MessageHistory.StateEntry
                val itemIndex = MessageHistory.ITEMS.indexOf(item)

                val fragment = StateDetailFragment().apply {
                    arguments = Bundle().apply {
                        putLong(StateDetailFragment.ARG_ITEM_ID, item.id)
                    }
                }

                item.unread = 0

                adapter!!.notifyDataSetChanged()

                if (twoPane) {

                    selectedItem = if (itemIndex == selectedItem) -1 else itemIndex

                    parentActivity.supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.state_detail_container, fragment)
                            .commit()
                } else {
                    (parentActivity as MainActivity).changePage(fragment)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.state_list_content, parent, false)

            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.idView.text = item.id.toDate()
            holder.contentView.text = item.state.name.toString()
            holder.badge.visibility = if (item.unread == 0) View.GONE else View.VISIBLE
            holder.badge.text = item.unread.toString()
            with(holder.itemView) {
                tag = item
                setOnClickListener(onClickListener)
            }

            if (selectedItem == position) {
                (holder.contentView.parent.parent as ViewGroup).setBackgroundColor(Color.LTGRAY)
            } else {
                (holder.contentView.parent.parent as ViewGroup).setBackgroundColor(Color.WHITE)
            }
        }

        override fun getItemCount() = values.size

        inner class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            val idView: TextView = view.date_text
            val contentView: TextView = view.state_name
            val badge: TextView = view.unread_event
        }
    }

    override fun onMessageReceived(message: UFServiceMessageV1) {
        if (currentSize != MessageHistory.ITEMS.size) {
            currentSize = MessageHistory.ITEMS.size
            if (selectedItem >= 0 && selectedItem < MessageHistory.ITEMS.size) {
                MessageHistory.ITEMS[selectedItem].unread = 0
                selectedItem = min(CAPACITY, selectedItem + 1)
            }
        }
        adapter?.notifyDataSetChanged()
    }

    companion object {
        /**
         * The fragment argument representing the item ID that this fragment
         * represents.
         */
        const val ARG_TWO_PANE = "two_pane"
    }
}
