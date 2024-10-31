package com.example.taskmate.kategori

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmate.R

class KategoriAdapter(
    private val categories: List<String>,
    private val onMoreClick: (View, Int) -> Unit
) : RecyclerView.Adapter<KategoriAdapter.KategoriViewHolder>() {

    class KategoriViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val categoryName: TextView = view.findViewById(R.id.kategori_name)
        val moreIcon: ImageView = view.findViewById(R.id.more_vert)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KategoriViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.kategori_item, parent, false)
        return KategoriViewHolder(view)
    }

    override fun onBindViewHolder(holder: KategoriViewHolder, position: Int) {
        val category = categories[position]
        holder.categoryName.text = category

        // Set click listener for the triple dot
        holder.moreIcon.setOnClickListener {
            onMoreClick(it, position)
        }
    }

    override fun getItemCount(): Int {
        return categories.size
    }
}
