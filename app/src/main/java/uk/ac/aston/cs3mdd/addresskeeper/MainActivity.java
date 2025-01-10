package uk.ac.aston.cs3mdd.addresskeeper;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import uk.ac.aston.cs3mdd.addresskeeper.databinding.ActivityMainBinding;
import uk.ac.aston.cs3mdd.addresskeeper.model.Name;
import uk.ac.aston.cs3mdd.addresskeeper.model.Picture;
import uk.ac.aston.cs3mdd.addresskeeper.model.User;
import uk.ac.aston.cs3mdd.addresskeeper.model.UsersViewModel;

public class MainActivity extends AppCompatActivity {
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri selectedImageUri = null;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        if (binding.toolbar.getOverflowIcon() != null) {
            binding.toolbar.getOverflowIcon().setTint(getResources().getColor(R.color.white));
        }

        // sets up the NavController and AppBarConfiguration
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        // makes back arrow white
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (binding.toolbar.getNavigationIcon() != null) {
                binding.toolbar.getNavigationIcon().setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
            }
        });

        // fab
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setImageResource(R.drawable.ic_plus);
        fab.getDrawable().setTintList(null);
        binding.fab.setOnClickListener(view -> showCreateUserDialog());

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.SecondFragment) {
                // hides the FAB in SecondFragment
                binding.fab.setVisibility(View.GONE);
            } else {
                binding.fab.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showCreateUserDialog() {
        EditText firstNameEditText = new EditText(this);
        firstNameEditText.setHint("First Name");

        EditText lastNameEditText = new EditText(this);
        lastNameEditText.setHint("Last Name");

        Button selectImageButton = new Button(this);
        selectImageButton.setText("Select Picture");

        imageView = new ImageView(this);
        imageView.setVisibility(View.GONE);

        selectImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);
        layout.addView(firstNameEditText);
        layout.addView(lastNameEditText);
        layout.addView(selectImageButton);
        layout.addView(imageView);

        new AlertDialog.Builder(this)
                .setTitle("Create Entity")
                .setView(layout)
                .setPositiveButton("Create", (dialog, which) -> {
                    String firstName = firstNameEditText.getText().toString().trim();
                    String lastName = lastNameEditText.getText().toString().trim();

                    if (!firstName.isEmpty() && !lastName.isEmpty()) {
                        User newUser = new User();
                        Name name = new Name();
                        name.setFirst(firstName);
                        name.setLast(lastName);
                        newUser.setName(name);

                        Picture picture = new Picture();
                        if (selectedImageUri != null) {
                            picture.setLarge(selectedImageUri.getPath());
                            selectedImageUri = null;
                        }
                        newUser.setPicture(picture);

                        UsersViewModel viewModel = new ViewModelProvider(this).get(UsersViewModel.class);
                        boolean isCreated = viewModel.addUser(newUser);

                        if (isCreated) {
                            Toast.makeText(this, "Entity Created", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "An entity with this name already exists!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "You must fill both fields, please try again!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();

            // saves the image to internal storage and get the saved path
            String savedImagePath = saveImageToInternalStorage(imageUri);

            if (savedImagePath != null) {
                selectedImageUri = Uri.fromFile(new File(savedImagePath));
                Bitmap bitmap = BitmapFactory.decodeFile(savedImagePath);
                imageView.setImageBitmap(bitmap);
                imageView.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String saveImageToInternalStorage(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

            File file = new File(getFilesDir(), "user_image_" + System.currentTimeMillis() + ".png");

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }
}
