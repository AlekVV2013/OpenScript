package com.example.freeassistant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.freeassistant.tasks.ResultItem

class ResultAdapter(
    private val onClick: (ResultItem) -> Unit
) : RecyclerView.Adapter<ResultAdapter.ResultViewHolder>() {

    private val items = mutableListOf<ResultItem>()

    inner class ResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val subtitleTextView: TextView = itemView.findViewById(R.id.subtitleTextView)

        fun bind(item: ResultItem) {
            titleTextView.text = item.title
            subtitleTextView.text = item.subtitle
            itemView.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_result, parent, false)
        return ResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<ResultItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
