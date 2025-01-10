package uk.ac.aston.cs3mdd.addresskeeper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import uk.ac.aston.cs3mdd.addresskeeper.databinding.FragmentFirstBinding;
import uk.ac.aston.cs3mdd.addresskeeper.model.User;
import uk.ac.aston.cs3mdd.addresskeeper.model.UserListAdapter;
import uk.ac.aston.cs3mdd.addresskeeper.model.UsersViewModel;

public class FirstFragment extends Fragment {
    private FragmentFirstBinding binding;
    private UsersViewModel viewModel;
    private RecyclerView mRecyclerView;
    private UserListAdapter mAdapter;
    private static final int PICK_IMAGE_REQUEST = 1;
    private String selectedImagePath;
    private ImageView userImageView;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        viewModel = new ViewModelProvider(requireActivity(), new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new UsersViewModel(getContext());
            }
        }).get(UsersViewModel.class);

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRecyclerView = view.findViewById(R.id.recyclerview);
        TextView emptyListMessage = view.findViewById(R.id.empty_list_message);

        mAdapter = new UserListAdapter(getContext(), viewModel.getAllUsers().getValue(), mRecyclerView);

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // attaches the ItemTouchHelper to the RecyclerView
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();

                // notifies adapter to swap items
                mAdapter.onItemMove(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                // persist the changes to the viewmodel
                viewModel.updateUserOrder(mAdapter.getCurrentList());
            }
        });
        itemTouchHelper.attachToRecyclerView(mRecyclerView);

        viewModel.getAllUsers().observe(getViewLifecycleOwner(), userList -> {
            if (userList != null && !userList.isEmpty()) {
                mRecyclerView.setVisibility(View.VISIBLE);
                emptyListMessage.setVisibility(View.GONE);
                mAdapter.updateData(userList);
            } else {
                mRecyclerView.setVisibility(View.GONE);
                emptyListMessage.setVisibility(View.VISIBLE);
            }
        });

        mRecyclerView = view.findViewById(R.id.recyclerview);
        ImageButton deleteAllUsersButton = view.findViewById(R.id.delete_all_entities_button);

        mAdapter = new UserListAdapter(getContext(), viewModel.getAllUsers().getValue(), mRecyclerView);

        mAdapter.setDeleteCallback(user -> {
            // deletes the user and show a Snackbar with undo option
            viewModel.deleteUser(user);
            Snackbar.make(view, "Entity deleted", Snackbar.LENGTH_LONG)
                    .setAction("UNDO", v -> {
                        viewModel.redoLastDeletion();
                        Toast.makeText(getContext(), "Entity restored", Toast.LENGTH_SHORT).show();
                    })
                    .show();
        });

        mAdapter.setEditCallback(user -> showEditUsernameDialog(user));

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // adds extra padding on the last user to avoid the floating action button
        // to be in the way of the last user icons
        int paddingInDp = 80;
        float scale = getResources().getDisplayMetrics().density;
        int paddingInPx = (int) (paddingInDp * scale + 0.5f);
        mRecyclerView.setPadding(0, 0, 0, paddingInPx);
        mRecyclerView.setClipToPadding(false);

        EditText searchField = view.findViewById(R.id.search_field);
        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                mAdapter.getFilter().filter(charSequence);
                if (charSequence.length() > 0) {
                    itemTouchHelper.attachToRecyclerView(null);
                } else {
                    itemTouchHelper.attachToRecyclerView(mRecyclerView);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        deleteAllUsersButton.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Delete All Entities")
                    .setMessage("Are you sure you want to delete all entities? This action cannot be undone.")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // first confirmation
                        new AlertDialog.Builder(getContext())
                                .setTitle("Confirm Deletion")
                                .setMessage("This is your last chance to confirm. Do you really want to delete ALL entities?")
                                .setPositiveButton("Yes, Delete All", (confirmDialog, confirmWhich) -> {
                                    // second confirmation
                                    viewModel.deleteAllUsers();
                                    Snackbar.make(view, "All entities deleted", Snackbar.LENGTH_LONG)
                                            .setAction("UNDO", undoView -> {
                                                viewModel.redoDeleteAll();
                                                Toast.makeText(getContext(), "All entities restored", Toast.LENGTH_SHORT).show();
                                            })
                                            .show();
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        final Observer<List<User>> userListObserver = userList -> {
            if (userList != null && !userList.isEmpty()) {
                mRecyclerView.setVisibility(View.VISIBLE);
                emptyListMessage.setVisibility(View.GONE);
                mAdapter.updateData(userList);
            } else {
                mRecyclerView.setVisibility(View.GONE);
                emptyListMessage.setVisibility(View.VISIBLE);
            }
        };

        viewModel.getAllUsers().observe(getViewLifecycleOwner(), userListObserver);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void showEditUsernameDialog(User user) {
        EditText firstNameEditText = new EditText(getContext());
        firstNameEditText.setText(user.getName().getFirst());
        firstNameEditText.setHint("Enter First Name");

        EditText lastNameEditText = new EditText(getContext());
        lastNameEditText.setText(user.getName().getLast());
        lastNameEditText.setHint("Enter Last Name");

        SharedPreferences sharedPreferences = getContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String savedLocation = sharedPreferences.getString(user.toString() + "_location", null);
        float savedLatitude = sharedPreferences.getFloat(user.toString() + "latitude", 0);
        float savedLongitude = sharedPreferences.getFloat(user.toString() + "longitude", 0);

        FrameLayout imageContainer = new FrameLayout(getContext());
        imageContainer.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        ));

        userImageView = new ImageView(getContext());
        userImageView.setPadding(10, 10, 10, 10);
        userImageView.setAdjustViewBounds(true);
        userImageView.setMaxHeight(500);
        userImageView.setMaxWidth(500);

        if (user.getPicture() != null && user.getPicture().getLarge() != null) {
            File imageFile = new File(user.getPicture().getLarge());
            if (imageFile.exists()) {
                Picasso.get().load(imageFile).into(userImageView);
            } else {
                userImageView.setImageResource(R.drawable.ic_nopicture);
            }
        } else {
            userImageView.setImageResource(R.drawable.ic_nopicture);
        }

        ImageView removeImageIcon = new ImageView(getContext());
        removeImageIcon.setImageResource(R.drawable.ic_cross);
        removeImageIcon.setLayoutParams(new FrameLayout.LayoutParams(
                60, 60,
                Gravity.TOP | Gravity.END
        ));
        removeImageIcon.setPadding(10, 10, 10, 10);
        removeImageIcon.setColorFilter(getResources().getColor(R.color.error), PorterDuff.Mode.SRC_IN);

        removeImageIcon.setOnClickListener(v -> {
            user.getPicture().setLarge(null);
            userImageView.setImageResource(R.drawable.ic_nopicture);
            selectedImagePath = null;
        });

        imageContainer.addView(userImageView);
        imageContainer.addView(removeImageIcon);

        Button selectImageButton = new Button(getContext());
        selectImageButton.setText("Change Picture");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);
        layout.addView(firstNameEditText);
        layout.addView(lastNameEditText);
        layout.addView(imageContainer);
        layout.addView(selectImageButton);

        selectImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        new AlertDialog.Builder(getContext())
                .setTitle("Edit Entity")
                .setView(layout)
                .setPositiveButton("Save", (dialogInterface, i) -> {
                    String updatedFirstName = firstNameEditText.getText().toString().trim();
                    String updatedLastName = lastNameEditText.getText().toString().trim();

                    if (updatedFirstName.isEmpty() || updatedLastName.isEmpty()) {
                        Toast.makeText(getContext(), "Both first and last names must be provided.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    boolean isNameTaken = viewModel.getAllUsers().getValue().stream()
                            .anyMatch(existingUser ->
                                    existingUser != null &&
                                            !existingUser.getId().equals(user.getId()) &&
                                            existingUser.getName().getFirst().equalsIgnoreCase(updatedFirstName) &&
                                            existingUser.getName().getLast().equalsIgnoreCase(updatedLastName));

                    if (isNameTaken) {
                        Toast.makeText(getContext(), "A user with this name already exists!", Toast.LENGTH_SHORT).show();
                    } else {
                        user.getName().setFirst(updatedFirstName);
                        user.getName().setLast(updatedLastName);

                        if (selectedImagePath != null) {
                            user.getPicture().setLarge(selectedImagePath);
                        }

                        viewModel.updateUser(user);

                        // resaves the location information after editing
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        if (savedLocation != null) {
                            editor.putString(user.toString() + "_location", savedLocation);
                        }
                        editor.putFloat(user.toString() + "latitude", savedLatitude);
                        editor.putFloat(user.toString() + "longitude", savedLongitude);
                        editor.apply();

                        Toast.makeText(getContext(), "User updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .create()
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == requireActivity().RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();

            // saves the selected image to internal storage
            String savedImagePath = saveImageToInternalStorage(imageUri);

            if (savedImagePath != null) {
                selectedImagePath = savedImagePath;
                if (userImageView != null) {
                    Picasso.get().load(new File(savedImagePath)).into(userImageView);
                }
            } else {
                Toast.makeText(getContext(), "Failed to save image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String saveImageToInternalStorage(Uri imageUri) {
        try {
            // gets the bitmap from the URI
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), imageUri);

            // create a unique file name for the image
            File file = new File(requireContext().getFilesDir(), "user_image_" + System.currentTimeMillis() + ".png");

            // writes the bitmap to the file
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            outputStream.close();

            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
