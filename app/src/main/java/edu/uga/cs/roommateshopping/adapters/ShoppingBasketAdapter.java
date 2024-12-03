package edu.uga.cs.roommateshopping.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import edu.uga.cs.roommateshopping.R;
import edu.uga.cs.roommateshopping.models.ShoppingItem;
import edu.uga.cs.roommateshopping.services.ShoppingBasketServices;

public class ShoppingBasketAdapter extends RecyclerView.Adapter<ShoppingBasketAdapter.ViewHolder> {
    private final List<ShoppingItem> basketItems;
    private final ShoppingBasketServices basketService;

    public ShoppingBasketAdapter(List<ShoppingItem> basketItems) {
        this.basketItems = basketItems;
        this.basketService = new ShoppingBasketServices();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shopping_basket, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShoppingItem item = basketItems.get(position);

        holder.itemNameTextView.setText(item.getName());
        holder.itemQuantityTextView.setText(String.valueOf(item.getQuantity()));

        // Move item to the shopping list
        holder.removeButton.setOnClickListener(v -> {
            ShoppingBasketServices basketService = new ShoppingBasketServices();
            basketService.moveItemToShoppingList(item.getId(), item, new ShoppingBasketServices.DatabaseCallback() {
                @Override
                public void onSuccess(String message) {
                    // Remove the item from the local basket list
                    basketItems.remove(position);

                    // Notify the adapter about the removed item
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, basketItems.size());

                    // Inform the user of the successful operation
                    Toast.makeText(v.getContext(), message, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(String errorMessage) {
                    // Inform the user of the error
                    Toast.makeText(v.getContext(), "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });

    }

    @Override
    public int getItemCount() {
        return basketItems.size();
    }

    public void updateItems(List<ShoppingItem> newBasketItems) {
        this.basketItems.clear();
        this.basketItems.addAll(newBasketItems);
        notifyDataSetChanged(); // Notify the adapter about data changes
    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemNameTextView;
        TextView itemQuantityTextView;
        ImageButton removeButton;

        ViewHolder(View itemView) {
            super(itemView);
            itemNameTextView = itemView.findViewById(R.id.itemNameTextView);
            itemQuantityTextView = itemView.findViewById(R.id.itemQuantityTextView);
            removeButton = itemView.findViewById(R.id.removeButton);
        }
    }
}
