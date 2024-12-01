package edu.uga.cs.roomateshoppingapp.adapters;

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
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.uga.cs.roomateshoppingapp.R;
import edu.uga.cs.roomateshoppingapp.models.Purchase;
import edu.uga.cs.roomateshoppingapp.models.ShoppingItem;

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
        
        // Format items as a comma-separated list
        String items = String.join(", ", purchase.getItemNames());
        
        holder.itemsTextView.setText(items);
        holder.totalAmountTextView.setText(currencyFormatter.format(purchase.getTotalAmount()));
        holder.purchasedByTextView.setText("Purchased by: " + purchase.getPurchasedBy());
        holder.dateTextView.setText(dateFormatter.format(new Date(purchase.getPurchaseDate())));

        // Handle single item purchases differently
        boolean isSingleItem = purchase.getItemIds().size() == 1;
        holder.selectItemsButton.setEnabled(!isSingleItem);
        holder.selectItemsButton.setVisibility(isSingleItem ? View.GONE : View.VISIBLE);
        
        if (isSingleItem) {
            // For single items, auto-select the item
            holder.selectedItems = new ArrayList<>();
            holder.selectedItems.add(purchase.getItemIds().get(0));
            holder.returnItemsButton.setEnabled(true);
        } else {
            // For multiple items, initialize or restore selected items list
            if (holder.selectedItems == null) {
                holder.selectedItems = new ArrayList<>();
            }
            holder.returnItemsButton.setEnabled(!holder.selectedItems.isEmpty());
        }

        holder.editPriceButton.setOnClickListener(v -> showEditPriceDialog(v, purchase));
        holder.selectItemsButton.setOnClickListener(v -> showSelectItemsDialog(v, purchase, holder));
        holder.returnItemsButton.setOnClickListener(v -> returnSelectedItems(v, purchase, holder));
    }

    private void showEditPriceDialog(View view, Purchase purchase) {
        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setTitle("Edit Total Price");

        final EditText input = new EditText(view.getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.format(Locale.US, "%.2f", purchase.getTotalAmount()));
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                double newAmount = Double.parseDouble(input.getText().toString());
                if (newAmount > 0) {
                    updatePurchaseAmount(purchase, newAmount);
                } else {
                    Toast.makeText(view.getContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(view.getContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showSelectItemsDialog(View view, Purchase purchase, ViewHolder holder) {
        String[] items = purchase.getItemNames().toArray(new String[0]);
        boolean[] checkedItems = new boolean[items.length];
        
        // Set checked state based on currently selected items
        for (int i = 0; i < items.length; i++) {
            checkedItems[i] = holder.selectedItems.contains(purchase.getItemIds().get(i));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setTitle("Select Items to Return");
        builder.setMultiChoiceItems(items, checkedItems, (dialog, which, isChecked) -> {
            if (isChecked) {
                holder.selectedItems.add(purchase.getItemIds().get(which));
            } else {
                holder.selectedItems.remove(purchase.getItemIds().get(which));
            }
            holder.returnItemsButton.setEnabled(!holder.selectedItems.isEmpty());
        });

        builder.setPositiveButton("Done", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            holder.selectedItems.clear();
            holder.returnItemsButton.setEnabled(false);
        });

        builder.show();
    }

    private void returnSelectedItems(View view, Purchase purchase, ViewHolder holder) {
        if (holder.selectedItems.isEmpty()) {
            return;
        }

        boolean isSingleItem = purchase.getItemIds().size() == 1;
        String message;
        if (isSingleItem) {
            message = "Are you sure you want to return this item?";
        } else {
            message = String.format(Locale.US, 
                "Are you sure you want to return %d selected items?", 
                holder.selectedItems.size());
        }

        new AlertDialog.Builder(view.getContext())
                .setTitle("Confirm Return")
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) -> {
                    processItemReturn(purchase, holder);
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void processItemReturn(Purchase purchase, ViewHolder holder) {
        boolean isSingleItem = purchase.getItemIds().size() == 1;

        // Update items in Firebase
        for (String itemId : holder.selectedItems) {
            int index = purchase.getItemIds().indexOf(itemId);
            if (index >= 0) {
                String itemName = purchase.getItemNames().get(index);
                ShoppingItem item = new ShoppingItem(itemName);
                item.setId(itemId);
                item.setPurchased(false);
                item.setPurchasedBy(null);
                item.setPurchasedDate(0);

                mDatabase.child("shopping_items").child(itemId).setValue(item);
            }
        }

        // Update or remove the purchase based on remaining items
        List<String> remainingItemIds = new ArrayList<>(purchase.getItemIds());
        List<String> remainingItemNames = new ArrayList<>(purchase.getItemNames());
        
        for (String itemId : holder.selectedItems) {
            int index = remainingItemIds.indexOf(itemId);
            if (index >= 0) {
                remainingItemIds.remove(index);
                remainingItemNames.remove(index);
            }
        }

        if (remainingItemIds.isEmpty()) {
            // Remove the entire purchase
            mDatabase.child("purchases").child(purchase.getId()).removeValue();
        } else if (!isSingleItem) {
            // Update the purchase with remaining items but keep the same total amount
            purchase.setItemIds(remainingItemIds);
            purchase.setItemNames(remainingItemNames);
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
