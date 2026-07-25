package com.hlebushek.openscript

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Typeface
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hlebushek.openscript.tasks.ResultItem
import com.google.android.material.color.MaterialColors

data class ChatMessage(
    val id: Long,
    val text: String,
    val isFromUser: Boolean,
    val time: String,
    val items: List<ResultItem> = emptyList(),
    val isTyping: Boolean = false
)

class ChatAdapter(
    private val onItemClicked: (ResultItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<ChatMessage>()

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_ASSISTANT = 1
        private const val TYPE_TYPING = 2
    }

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun updateMessageText(id: Long, text: String) {
        val index = messages.indexOfFirst { it.id == id }
        if (index >= 0) {
            messages[index] = messages[index].copy(text = text)
            notifyItemChanged(index)
        }
    }

    fun removeMessage(id: Long) {
        val index = messages.indexOfFirst { it.id == id }
        if (index >= 0) {
            messages.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when {
            message.isTyping -> TYPE_TYPING
            message.isFromUser -> TYPE_USER
            else -> TYPE_ASSISTANT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> UserViewHolder(
                inflater.inflate(R.layout.item_message_user, parent, false)
            )
            TYPE_TYPING -> TypingViewHolder(
                inflater.inflate(R.layout.item_message_typing, parent, false)
            )
            else -> AssistantViewHolder(
                inflater.inflate(R.layout.item_message_assistant, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is UserViewHolder -> holder.bind(message)
            is AssistantViewHolder -> holder.bind(message, onItemClicked)
            is TypingViewHolder -> holder.startDots()
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is TypingViewHolder) {
            holder.stopDots()
        }
        super.onViewRecycled(holder)
    }

    override fun getItemCount(): Int = messages.size

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val text: TextView = view.findViewById(R.id.userMessageText)
        private val time: TextView = view.findViewById(R.id.userMessageTime)
        fun bind(message: ChatMessage) {
            text.text = message.text
            time.text = message.time
        }
    }

    class AssistantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val text: TextView = view.findViewById(R.id.assistantMessageText)
        private val time: TextView = view.findViewById(R.id.assistantMessageTime)
        private val itemsContainer: LinearLayout =
            view.findViewById(R.id.assistantItemsContainer)

        fun bind(message: ChatMessage, onItemClicked: (ResultItem) -> Unit) {
            text.text = message.text
            time.text = message.time
            itemsContainer.removeAllViews()
            message.items.forEachIndexed { index, item ->
                if (index > 0) {
                    itemsContainer.addView(buildDivider())
                }
                itemsContainer.addView(buildRow(item, onItemClicked))
            }
            itemsContainer.visibility =
                if (message.items.isEmpty()) View.GONE else View.VISIBLE
        }

        private fun buildDivider(): View {
            val context = itemView.context
            val divider = View(context)
            divider.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            )
            val color = MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorOutlineVariant,
                0x33888888
            )
            divider.setBackgroundColor(color)
            return divider
        }

        private fun buildRow(
            item: ResultItem,
            onItemClicked: (ResultItem) -> Unit
        ): View {
            val context = itemView.context
            val density = context.resources.displayMetrics.density
            val padding = (10 * density).toInt()
            val row = LinearLayout(context)
            row.orientation = LinearLayout.VERTICAL
            row.setPadding(padding, padding, padding, padding)
            row.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val ripple = TypedValue()
            context.theme.resolveAttribute(
                android.R.attr.selectableItemBackground,
                ripple,
                true
            )
            row.setBackgroundResource(ripple.resourceId)
            val title = TextView(context)
            title.text = item.title
            title.textSize = 14f
            title.setTypeface(title.typeface, Typeface.BOLD)
            title.setTextColor(
                MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorOnSurface,
                    0xFF000000.toInt()
                )
            )
            row.addView(title)
            if (item.subtitle.isNotBlank()) {
                val subtitle = TextView(context)
                subtitle.text = item.subtitle
                subtitle.textSize = 12f
                subtitle.setTextColor(
                    MaterialColors.getColor(
                        context,
                        com.google.android.material.R.attr.colorOnSurfaceVariant,
                        0xFF666666.toInt()
                    )
                )
                row.addView(subtitle)
            }
            row.setOnClickListener { onItemClicked(item) }
            return row
        }
    }

    class TypingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val dots: List<View> = listOf(
            view.findViewById(R.id.dot1),
            view.findViewById(R.id.dot2),
            view.findViewById(R.id.dot3)
        )
        private val animators = mutableListOf<ObjectAnimator>()

        fun startDots() {
            stopDots()
            dots.forEachIndexed { index, dot ->
                val animator = ObjectAnimator.ofFloat(dot, "alpha", 0.25f, 1f).apply {
                    duration = 500
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                    startDelay = index * 150L
                }
                animator.start()
                animators.add(animator)
            }
        }

        fun stopDots() {
            animators.forEach { it.cancel() }
            animators.clear()
        }
    }
}
