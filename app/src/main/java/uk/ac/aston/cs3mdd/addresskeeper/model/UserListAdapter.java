package uk.ac.aston.cs3mdd.addresskeeper.model;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import uk.ac.aston.cs3mdd.addresskeeper.FirstFragmentDirections;
import uk.ac.aston.cs3mdd.addresskeeper.R;

public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.UserViewHolder> implements Filterable {
    private final RecyclerView mRecyclerView;
    private List<User> mUserList;
    private List<User> mUserListFull;
    private final LayoutInflater mInflater;
    private Consumer<User> deleteCallback;
    private Consumer<User> editCallback;

    public UserListAdapter(Context context, List<User> userList, RecyclerView recyclerView) {
        mInflater = LayoutInflater.from(context);
        this.mUserList = userList;
        this.mUserListFull = new ArrayList<>(userList);
        this.mRecyclerView = recyclerView;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View mItemView = mInflater.inflate(R.layout.user_item, parent, false);
        return new UserViewHolder(mItemView, this, mRecyclerView);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = mUserList.get(position);
        holder.user = user;
        Name name = user.getName();
        String displayName = name.getFirst() + " " + name.getLast();
        holder.usernameView.setText(displayName);

        Log.d("UserListAdapter", "Binding user: " + displayName + ", Image Path: " + user.getPicture().getLarge());

        String imagePath = user.getPicture() != null ? user.getPicture().getLarge() : null;
        if (imagePath != null && !imagePath.isEmpty()) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Picasso.get()
                        .load(imageFile)
                        .placeholder(R.drawable.ic_nopicture)
                        .error(R.drawable.ic_nopicture)
                        .into(holder.userImage, new com.squareup.picasso.Callback() {
                            @Override
                            public void onSuccess() {
                                Log.d("UserListAdapter", "Image loaded successfully for user: " + displayName);
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e("UserListAdapter", "Error loading image for user: " + displayName, e);
                                holder.userImage.setImageResource(R.drawable.ic_nopicture);
                            }
                        });
            } else {
                Log.w("UserListAdapter", "Image file does not exist for user: " + displayName + " at path: " + imagePath);
                holder.userImage.setImageResource(R.drawable.ic_nopicture);// placeholder image if the image doesn't exist
            }
        } else {
            Log.w("UserListAdapter", "No valid image path for user: " + displayName);
            holder.userImage.setImageResource(R.drawable.ic_nopicture); // placeholder image if the image doesn't exist
        }

        // adds touch listener to the ScrollView
        holder.scrollView.setOnTouchListener((view, motionEvent) -> {
            // checks if the ScrollView can scroll
            if (holder.scrollView.getChildAt(0).getHeight() > holder.scrollView.getHeight()) {
                // ScrollView is scrollable, so disable parent RecyclerView scrolling
                mRecyclerView.requestDisallowInterceptTouchEvent(true);
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    mRecyclerView.requestDisallowInterceptTouchEvent(false);
                }
            } else {
                // the ScrollView is not scrollable, so let parent RecyclerView handle touch events
                mRecyclerView.requestDisallowInterceptTouchEvent(false);
            }
            return false;
        });

        holder.itemView.setOnClickListener(v -> {
            FirstFragmentDirections.ActionFirstFragmentToSecondFragment action =
                    FirstFragmentDirections.actionFirstFragmentToSecondFragment(user);
            Navigation.findNavController(v).navigate(action);
        });
    }

    @Override
    public int getItemCount() {
        return this.mUserList.size();
    }

    @Override
    public Filter getFilter() {
        return userFilter;
    }

    private final Filter userFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            List<User> filteredList = new ArrayList<>();
            if (charSequence == null || charSequence.length() == 0) {
                filteredList.addAll(mUserListFull);
            } else {
                String filterPattern = charSequence.toString().toLowerCase().trim();
                for (User user : mUserListFull) {
                    String fullName = user.getName().getFirst().toLowerCase() + " " + user.getName().getLast().toLowerCase();
                    if (fullName.contains(filterPattern)) {
                        filteredList.add(user);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            results.count = filteredList.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            mUserList.clear();
            mUserList.addAll((List<User>) filterResults.values);
            notifyDataSetChanged();
        }
    };

    public void updateData(List<User> list) {
        if (list != null) {
            this.mUserList = new ArrayList<>(list);
            this.mUserListFull = new ArrayList<>(list);
            notifyDataSetChanged();
        }
    }

    public void setDeleteCallback(Consumer<User> callback) {
        this.deleteCallback = callback;
    }

    public void setEditCallback(Consumer<User> callback) {
        this.editCallback = callback;
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        public final TextView usernameView;
        public final ImageView userImage;
        public final ScrollView scrollView;
        final UserListAdapter mAdapter;
        public User user;
        public final ImageView deleteIcon;

        public UserViewHolder(@NonNull View itemView, UserListAdapter adapter, RecyclerView recyclerView) {
            super(itemView);
            usernameView = itemView.findViewById(R.id.username);
            userImage = itemView.findViewById(R.id.userimage);
            scrollView = itemView.findViewById(R.id.scrollview_username);
            deleteIcon = itemView.findViewById(R.id.deleteIcon);
            mAdapter = adapter;

            itemView.findViewById(R.id.deleteIcon).setOnClickListener(v -> deleteCallback.accept(user));
            itemView.findViewById(R.id.editIcon).setOnClickListener(v -> editCallback.accept(user));

            deleteIcon.setOnClickListener(view -> showDeleteConfirmationDialog(view.getContext(), user));
        }

        private void showDeleteConfirmationDialog(Context context, User user) {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Entity")
                    .setMessage("Are you sure you want to delete " + user.getName().getFirst() + " " + user.getName().getLast() + "?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        if (mAdapter.deleteCallback != null) {
                            mAdapter.deleteCallback.accept(user);
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
    }

    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mUserList, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mUserList, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    public List<User> getCurrentList() {
        return mUserList;
    }
}
