package edu.uga.cs.roomateshoppingapp.adapters;

import android.app.AlertDialog;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

import edu.uga.cs.roomateshoppingapp.R;
import edu.uga.cs.roomateshoppingapp.models.ShoppingItem;

public class ShoppingListAdapter extends RecyclerView.Adapter<ShoppingListAdapter.ViewHolder> {
    private List<ShoppingItem> shoppingItems;
    private OnItemSelectionListener listener;

    public interface OnItemSelectionListener {
        void onItemSelected(int position, boolean isSelected);
    }

    public ShoppingListAdapter(List<ShoppingItem> items, OnItemSelectionListener listener) {
        this.shoppingItems = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shopping_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position >= shoppingItems.size()) return;

        ShoppingItem item = shoppingItems.get(position);
        if (item == null) return;

        holder.itemNameTextView.setText(item.getName());
        
        // Set checkbox state without triggering listener
        holder.itemCheckBox.setOnCheckedChangeListener(null);
        holder.itemCheckBox.setChecked(false);
        
        holder.itemCheckBox.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && pos < shoppingItems.size() && listener != null) {
                listener.onItemSelected(pos, true);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && pos < shoppingItems.size()) {
                holder.itemCheckBox.setChecked(true);
            }
        });

        holder.editButton.setOnClickListener(v -> showEditDialog(v, item));
        holder.deleteButton.setOnClickListener(v -> showDeleteDialog(v, item));
    }

    private void showEditDialog(View view, ShoppingItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setTitle("Edit Item");

        final EditText input = new EditText(view.getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(item.getName());
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                FirebaseDatabase.getInstance().getReference()
                        .child("shopping_items")
                        .child(item.getId())
                        .child("name")
                        .setValue(newName);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showDeleteDialog(View view, ShoppingItem item) {
        new AlertDialog.Builder(view.getContext())
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    FirebaseDatabase.getInstance().getReference()
                            .child("shopping_items")
                            .child(item.getId())
                            .removeValue();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return shoppingItems != null ? shoppingItems.size() : 0;
    }

    public void updateItems(List<ShoppingItem> items) {
        this.shoppingItems = items;
        notifyDataSetChanged();
    }

    public List<ShoppingItem> getSelectedItems() {
        return shoppingItems.stream()
                .filter(ShoppingItem::isSelected)
                .collect(java.util.stream.Collectors.toList());
    }

    public ArrayList<Integer> getSelectedPositions() {
        ArrayList<Integer> selectedPositions = new ArrayList<>();
        for (int i = 0; i < shoppingItems.size(); i++) {
            if (shoppingItems.get(i).isSelected()) {
                selectedPositions.add(i);
            }
        }
        return selectedPositions;
    }

    public void restoreSelectedItems(ArrayList<Integer> selectedPositions) {
        for (Integer position : selectedPositions) {
            if (position < shoppingItems.size()) {
                shoppingItems.get(position).setSelected(true);
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public CheckBox itemCheckBox;
        public TextView itemNameTextView;
        public ImageButton editButton;
        public ImageButton deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemCheckBox = itemView.findViewById(R.id.itemCheckBox);
            itemNameTextView = itemView.findViewById(R.id.itemNameTextView);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
