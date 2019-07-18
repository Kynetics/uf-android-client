package com.kynetics.uf.clientexample.fragment

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.kynetics.uf.android.api.v1.UFServiceMessageV1
import com.kynetics.uf.clientexample.R
import com.kynetics.uf.clientexample.databinding.StateDetailBinding
import com.kynetics.uf.clientexample.dummy.MessageHistory
import com.kynetics.uf.clientexample.dummy.format
import kotlinx.android.synthetic.main.state_detail.view.*
import kotlin.math.max
import kotlin.math.pow

/**
 * A fragment representing a single State detail screen.
 * This fragment is either contained in a [StateListActivity]
 * in two-pane mode (on tablets) or a [StateDetailActivity]
 * on handsets.
 */
class StateDetailFragment : Fragment(), UFServiceInteractionFragment {

    /**
     * The dummy content this fragment is presenting.
     */
    private var item: MessageHistory.StateEntry? = null
    private var adapter:ArrayAdapter<MessageHistory.EventEntry>? = null
    private var binding: StateDetailBinding? = null
    private var stateDetail: StateDetail? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            if (it.containsKey(ARG_ITEM_ID)) {
                // Load the dummy content specified by the fragment
                // arguments. In a real-world scenario, use a Loader
                // to load content from a content provider.
                item = MessageHistory.ITEM_MAP[it.getLong(ARG_ITEM_ID)]
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate<StateDetailBinding>(
                inflater, R.layout.state_detail, container, false)
        val view = binding!!.root
        //here data must be an instance of the class MarsDataProvider
        if(item!= null){
            binding!!.data = item!!
            adapter = ArrayAdapter(activity,android.R.layout.simple_list_item_1, item!!.events)
            view.events_list.adapter = adapter
        }
        binding!!.root.details_list?.adapter
        customizeCardView()
        return view

    }

    class StateDetail(downloading: UFServiceMessageV1.State.Downloading){
        val details:List<FileDownloading>
        private val detailMap:Map<String,FileDownloading>

        init {
            detailMap = downloading.artifacts.map {
                it.name to FileDownloading(it.name, it.size, 0.0)
            }.toMap()
            details = detailMap.values.toList()
        }

        fun updateDetail(key:String, value:Double){
            this.detailMap[key]?.percent = value
        }

        fun containsKey(key:String):Boolean{
            return detailMap.containsKey(key)
        }

        data class FileDownloading(val fileName:String, val size:Long, var percent:Double = 0.0){
            override fun toString(): String {
                return "$fileName (${(size / 2.0.pow(20.0)).format(2)} MB) - Downloaded ${percent.format(2)}%"
            }
        }
    }

    private fun customizeCardView(){
        when(item!!.state){
            is UFServiceMessageV1.State.Downloading -> {
                stateDetail = StateDetail(item!!.state as UFServiceMessageV1.State.Downloading)
                binding?.root?.details_title?.text = "Files to donwload:"
                binding?.root?.details_title?.visibility = View.VISIBLE
                binding?.root?.details_list?.visibility = View.VISIBLE
                binding?.root?.details_list?.adapter = ArrayAdapter(activity, android.R.layout.simple_list_item_1, stateDetail!!.details)

                item!!.events.forEach{
                    onMessageReceived(it.event)
                }
            }

            else -> {
                binding?.root?.details_list?.adapter = ArrayAdapter(activity, android.R.layout.simple_list_item_1, listOf<String>())
            }
        }

    }

    private fun updateDetails(key:String, percent:Double){
        if(stateDetail?.containsKey(key) == true){
            stateDetail?.updateDetail(key, percent)
            println(stateDetail?.details)
            (binding?.root?.details_list?.adapter as ArrayAdapter<*>).notifyDataSetChanged()

        }
    }


    companion object {
        /**
         * The fragment argument representing the item ID that this fragment
         * represents.
         */
        const val ARG_ITEM_ID = "item_id"
    }

    //todo replace with object observer
    override fun onMessageReceived(message: UFServiceMessageV1) {
        if(message is UFServiceMessageV1.Event){
            item?.unread = 0
            adapter?.notifyDataSetChanged()
            val size = item?.events?.size ?: 0
            binding?.root?.events_list?.setSelection(max(0, size - 1))

            when{
                message is UFServiceMessageV1.Event.StartDownloadFile  && item?.state is UFServiceMessageV1.State.Downloading -> updateDetails(message.fileName, 0.0)
                message is UFServiceMessageV1.Event.DownloadProgress && item?.state is UFServiceMessageV1.State.Downloading   -> updateDetails(message.fileName, message.percentage)
                message is UFServiceMessageV1.Event.FileDownloaded && item?.state is UFServiceMessageV1.State.Downloading     -> updateDetails(message.fileDownloaded, 100.0)
                else -> {}
            }
        }

    }
}