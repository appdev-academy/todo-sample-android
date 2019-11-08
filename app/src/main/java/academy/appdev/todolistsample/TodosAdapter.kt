package academy.appdev.todolistsample

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.todo_item.view.*
import org.jetbrains.anko.layoutInflater
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.sdk25.coroutines.onLongClick

class TodosAdapter(
    val owner: MainActivity,
    var data: List<Pair<String, MainActivity.ToDoItem>>? = null
) :
    RecyclerView.Adapter<TodosAdapter.VH>() {

    fun swapData(newData: List<Pair<String, MainActivity.ToDoItem>>?) {
        data = newData
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        parent.context.layoutInflater.inflate(R.layout.todo_item, parent, false)
    )

    override fun getItemCount() = data?.size ?: 0

    override fun onBindViewHolder(holder: VH, position: Int) {
        data?.getOrNull(position)?.let { holder.bind(it) }

    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: Pair<String, MainActivity.ToDoItem>) {
            itemView.apply {
                title.text = item.second.title
                item.second.dueDate?.let { dateText.text = format.format(it) }
                onClick {
                    owner.onItemClicked(item.first,item.second)
                }
                onLongClick {
                    owner.onItemClicked(item.first,item.second)
                }
            }
        }
    }
}