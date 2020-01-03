package com.with.app.ui.chatlist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

import com.with.app.R
import com.with.app.data.local.ChatListVO
import com.with.app.data.local.ChatUserVO
import com.with.app.data.remote.ResponseChatListArrayData
import com.with.app.data.remote.RoomIdData
import com.with.app.manage.RequestManager
import com.with.app.ui.chatlist.recylcerview.ChatListAdapter
import com.with.app.extension.addListener
import com.with.app.extension.gone
import com.with.app.extension.safeEnqueue
import com.with.app.extension.visible
import com.with.app.ui.evaluation.EvaluateActivity
import kotlinx.android.synthetic.main.fragment_chat_list.*
import org.koin.android.ext.android.inject

class ChatListFragment : Fragment() {

    private val requestManager : RequestManager by inject()

    private var myIdx = requestManager.authManager.idx

    private var value : ChatUserVO =
        ChatUserVO()

    private var responseData : List<ResponseChatListArrayData> = listOf()

    private lateinit var data : MutableList<ChatListVO>
    private lateinit var adapter : ChatListAdapter
    private lateinit var lm : LinearLayoutManager
    private lateinit var reference : DatabaseReference
    private lateinit var usersReference : DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat_list, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        loading.playAnimation()
        loading.loop(true)

        reference = FirebaseDatabase.getInstance().reference
        usersReference = reference.child("users")

        btn_evaluation.setOnClickListener {
            startActivity(Intent(context, EvaluateActivity::class.java))
            evaluation.gone()
        }

        btn_close.setOnClickListener {
            evaluation.gone()
            for (item in responseData) {
                if (item.evalFlag == 2) {
                    requestManager.requestNoEvaluation(RoomIdData(item.roomId)).safeEnqueue()
                }
            }
        }

        data = mutableListOf()
        requestManager.requestChatList()
            .safeEnqueue(
                onSuccess = {
                    if (it.success) {
                        responseData = it.data
                        Log.e("data", responseData.toString())
                        for (item in responseData) {
                            if (item.evalFlag == 2) {
                                tv_name.text = "${requestManager.authManager.name}님"
                                evaluation.visible()
                                break
                            }
                        }
                        fireBaseChatListener()
                    }
                },
                onError = {
                    loading.gone()
                },
                onFailure = {
                    loading.gone()
                }
            )


        adapter = ChatListAdapter(data)
        lm = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        rv_chat_list.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        rv_chat_list.layoutManager = lm
        rv_chat_list.adapter = adapter
    }

    private fun fireBaseChatListener() {
        usersReference.child("$myIdx").addListener(
            onChildAdded = {
                    snap, _ ->
                value = snap.getValue(ChatUserVO::class.java)!!
                for (item in responseData) {
                    if (item.roomId == snap.key)
                        data.add(ChatListVO(item, value))
                }
                adapter.notifyDataSetChanged()
            },
            onChildChanged = {
                snap, _ ->
                value = snap.getValue(ChatUserVO::class.java)!!
                adapter.notifyDataSetChanged()
                data.filter {
                    it.ourServer.roomId == snap.key
                }.forEach{
                    it.fireBase = value
                }
            }
        )
        if(data.isEmpty()) {
            loading.gone()
        }
    }
}
