package edu.uga.cs.roommateshopping.adapters;

import android.app.AlertDialog;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import edu.uga.cs.roommateshopping.R;
import edu.uga.cs.roommateshopping.models.Purchase;
import edu.uga.cs.roommateshopping.models.ShoppingItem;

public class PurchaseHistoryAdapter extends RecyclerView.Adapter<PurchaseHistoryAdapter.ViewHolder> {
    private List<Purchase> purchases;
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US);
    private final DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

    public PurchaseHistoryAdapter(List<Purchase> purchases) {
        this.purchases = purchases;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_purchase_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Purchase purchase = purchases.get(position);
        
        // Display items
        holder.itemsTextView.setText(String.join(", ", purchase.getItemNames()));
        
        // Display total amount
        holder.totalAmountTextView.setText(currencyFormatter.format(purchase.getTotalAmount()));
        
        // Display purchaser
        holder.purchasedByTextView.setText("Purchased by: " + purchase.getPurchasedBy());
        
        // Display date
        holder.dateTextView.setText(dateFormatter.format(purchase.getPurchaseDate()));

        // Set up edit price button
        holder.editPriceButton.setOnClickListener(v -> showEditPriceDialog(v, purchase));

        // Set up select items button
        holder.selectItemsButton.setOnClickListener(v -> showSelectItemsDialog(v, purchase, holder));

        // Initially disable return items button
        holder.returnItemsButton.setEnabled(false);
        
        // Set up return items button
        holder.returnItemsButton.setOnClickListener(v -> returnSelectedItems(v, purchase, holder));

    }

    private void showEditPriceDialog(View view, Purchase purchase) {
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setTitle("Edit Total Amount");

        final EditText input = new EditText(view.getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.valueOf(purchase.getTotalAmount()));
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            try {
                double newAmount = Double.parseDouble(input.getText().toString());
                updatePurchaseAmount(purchase, newAmount);
            } catch (NumberFormatException e) {
                Toast.makeText(view.getContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showSelectItemsDialog(View view, Purchase purchase, ViewHolder holder) {
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setTitle("Select Items to Return");

        String[] items = purchase.getItemNames().toArray(new String[0]);
        boolean[] checkedItems = new boolean[items.length];

        builder.setMultiChoiceItems(items, checkedItems, (dialog, which, isChecked) -> {
            if (isChecked) {
                holder.selectedItems.add(purchase.getItemIds().get(which));
            } else {
                holder.selectedItems.remove(purchase.getItemIds().get(which));
            }
            holder.returnItemsButton.setEnabled(!holder.selectedItems.isEmpty());
        });

        builder.setPositiveButton("Done", (dialog, which) -> {});
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            holder.selectedItems.clear();
            holder.returnItemsButton.setEnabled(false);
        });

        builder.show();
    }

    private void returnSelectedItems(View view, Purchase purchase, ViewHolder holder) {
        // Create a new list for remaining items
        List<String> remainingItemIds = new ArrayList<>(purchase.getItemIds());
        List<String> remainingItemNames = new ArrayList<>(purchase.getItemNames());

        // Update shopping items status in Firebase
        for (String itemId : holder.selectedItems) {
            mDatabase.child("shopping_items").child(itemId).child("purchased").setValue(false);
            mDatabase.child("shopping_items").child(itemId).child("purchasedBy").setValue(null);
            mDatabase.child("shopping_items").child(itemId).child("purchaseDate").setValue(null);

            // Remove item from remaining lists
            int index = remainingItemIds.indexOf(itemId);
            if (index != -1) {
                remainingItemIds.remove(index);
                remainingItemNames.remove(index);
            }
        }

        // Update purchase with remaining items but keep the original total amount
        purchase.setItemIds(remainingItemIds);
        purchase.setItemNames(remainingItemNames);
        // Note: We're not updating the total amount anymore to maintain the group purchase cost

        // If no items remain, remove the purchase
        if (remainingItemIds.isEmpty()) {
            mDatabase.child("purchases").child(purchase.getId()).removeValue();
        } else {
            // Update the purchase with remaining items
            mDatabase.child("purchases").child(purchase.getId()).setValue(purchase);
        }

        // Reset selection state
        holder.selectedItems.clear();
        holder.returnItemsButton.setEnabled(false);
    }

    private void updatePurchaseAmount(Purchase purchase, double newAmount) {
        purchase.setTotalAmount(newAmount);
        mDatabase.child("purchases").child(purchase.getId()).setValue(purchase);
    }

    @Override
    public int getItemCount() {
        return purchases.size();
    }

    public void updatePurchases(List<Purchase> newPurchases) {
        this.purchases = newPurchases;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemsTextView;
        TextView totalAmountTextView;
        TextView purchasedByTextView;
        TextView dateTextView;
        ImageButton editPriceButton;
        Button selectItemsButton;
        Button returnItemsButton;
        List<String> selectedItems;

        ViewHolder(View itemView) {
            super(itemView);
            itemsTextView = itemView.findViewById(R.id.itemsTextView);
            totalAmountTextView = itemView.findViewById(R.id.totalAmountTextView);
            purchasedByTextView = itemView.findViewById(R.id.purchasedByTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            editPriceButton = itemView.findViewById(R.id.editPriceButton);
            selectItemsButton = itemView.findViewById(R.id.selectItemsButton);
            returnItemsButton = itemView.findViewById(R.id.returnItemsButton);
            selectedItems = new ArrayList<>();
        }
    }
}
