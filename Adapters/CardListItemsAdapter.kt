package com.manager.mymanager.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.manager.mymanager.R
import com.manager.mymanager.activities.TaskListActivity
import com.manager.mymanager.models.Card
import com.manager.mymanager.models.SelectedMembers
import kotlinx.android.synthetic.main.activity_card_details.*
import kotlinx.android.synthetic.main.item_card.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

open class CardListItemsAdapter(
    private val context: Context,
    private var list: ArrayList<Card>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var onClickListener: OnClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return MyViewHolder(
            LayoutInflater.from(context).inflate(R.layout.item_card, parent, false))
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = list[position]

        if (holder is MyViewHolder) {

            holder.itemView.tv_card_name.text = model.name

            if (model.labelColor.isNotEmpty()) {
                holder.itemView.view_label_color.visibility = View.VISIBLE
                holder.itemView.view_label_color.setBackgroundColor(Color.parseColor(model.labelColor))
            } else {
                holder.itemView.view_label_color.visibility = View.GONE
            }

            if ((context as TaskListActivity).mAssignedMemberDetailList.size > 0) {
                // A instance of selected members list.
                val selectedMembersList: ArrayList<SelectedMembers> = ArrayList()

                // Here we got the detail list of members and add it to the selected members list as required.
                for (i in context.mAssignedMemberDetailList.indices) {
                    for (j in model.assignedTo) {
                        if (context.mAssignedMemberDetailList[i].id == j) {
                            val selectedMember = SelectedMembers(
                                context.mAssignedMemberDetailList[i].id,
                                context.mAssignedMemberDetailList[i].image
                            )
                            selectedMembersList.add(selectedMember)
                        }
                    }
                }

                if (selectedMembersList.size > 0) {

                    if (selectedMembersList.size == 1 && selectedMembersList[0].id == model.createdBy) {
                        holder.itemView.rv_card_selected_members_list.visibility = View.GONE
                    } else {
                        holder.itemView.rv_card_selected_members_list.visibility = View.VISIBLE

                        holder.itemView.rv_card_selected_members_list.layoutManager =
                            GridLayoutManager(context, 4)
                        val adapter = CardMemberListItemsAdapter(context, selectedMembersList, false)
                        holder.itemView.rv_card_selected_members_list.adapter = adapter
                        adapter.setOnClickListener(object :
                            CardMemberListItemsAdapter.OnClickListener {
                            override fun onClick() {
                                if (onClickListener != null) {
                                    onClickListener!!.onClick(position)
                                }
                            }
                        })
                    }
                } else {
                    holder.itemView.rv_card_selected_members_list.visibility = View.GONE
                }
            }
            if(model.done=="true"){
                holder.itemView.task_done.visibility=View.VISIBLE
            }else{
                holder.itemView.task_done.visibility=View.GONE
            }
            var mSelectedDueDateMilliSeconds:Long=0
            mSelectedDueDateMilliSeconds=model.dueDate
            if(mSelectedDueDateMilliSeconds>0){
                holder.itemView.tv_selected_card_due_date.visibility=View.VISIBLE
                val simpleDateFormat= SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                val selectedDate= simpleDateFormat.format(Date(mSelectedDueDateMilliSeconds))
                if(model.dueTime.isNotEmpty()){
                    holder.itemView.tv_selected_card_due_date.text="Due $selectedDate ${model.dueTime}"
                }else{
                    holder.itemView.tv_selected_card_due_date.text="Due Date $selectedDate"
                }

            }else{
                holder.itemView.tv_selected_card_due_date.visibility=View.GONE
            }

            holder.itemView.setOnClickListener {
                if (onClickListener != null) {
                    onClickListener!!.onClick(position)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }


    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    interface OnClickListener {
        fun onClick(position: Int)
    }

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}