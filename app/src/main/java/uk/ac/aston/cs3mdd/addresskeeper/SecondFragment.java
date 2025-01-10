package uk.ac.aston.cs3mdd.addresskeeper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import uk.ac.aston.cs3mdd.addresskeeper.databinding.FragmentSecondBinding;
import uk.ac.aston.cs3mdd.addresskeeper.model.Coordinates;
import uk.ac.aston.cs3mdd.addresskeeper.model.Location;
import uk.ac.aston.cs3mdd.addresskeeper.model.User;

public class SecondFragment extends Fragment implements OnMapReadyCallback {
    private FragmentSecondBinding binding;
    private User user;
    private GoogleMap mMap;
    private Handler handler = new Handler(Looper.getMainLooper());
    private static final String PREFS_NAME = "UserPrefs";
    private static final String LAT_KEY = "latitude";
    private static final String LNG_KEY = "longitude";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        user = SecondFragmentArgs.fromBundle(getArguments()).getUser();
        binding = FragmentSecondBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.textviewName.setText(user.toString());

        if (user.getPicture() != null && user.getPicture().getLarge() != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(user.getPicture().getLarge());
            binding.imageviewUser.setImageBitmap(bitmap);
        }

        SharedPreferences sharedPreferences = getContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String savedLocation = sharedPreferences.getString(user.toString() + "_location", null);

        if (savedLocation != null) {
            // loads the saved location (custom or geocoded)
            binding.textviewLocation.setText(savedLocation);
        } else if (user.getLocation() != null && user.getLocation().getCoordinates() != null) {
            // if no location is saved, fallback to geocoded location from coordinates
            LatLng latLng = new LatLng(
                    user.getLocation().getCoordinates().getLatitude(),
                    user.getLocation().getCoordinates().getLongitude()
            );
            updateLocationText(latLng);
        } else {
            binding.textviewLocation.setText("Location: Not Registered");
        }

        View.OnClickListener editLocationClickListener = v -> {
            binding.textviewLocation.setVisibility(View.GONE);
            binding.edittextLocation.setVisibility(View.VISIBLE);
            binding.edittextLocation.setText(binding.textviewLocation.getText());
            binding.buttonLayout.setVisibility(View.VISIBLE);
            binding.buttonConfirm.setVisibility(View.VISIBLE);
            binding.buttonCancel.setVisibility(View.VISIBLE);
            binding.edittextLocation.requestFocus();
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(binding.edittextLocation, InputMethodManager.SHOW_IMPLICIT);
        };

        binding.textviewLocation.setOnClickListener(editLocationClickListener);
        binding.imageviewEditLocation.setOnClickListener(editLocationClickListener);

        binding.buttonConfirm.setOnClickListener(v -> {
            String updatedLocation = binding.edittextLocation.getText().toString();
            if (!updatedLocation.isEmpty()) {
                binding.textviewLocation.setText(updatedLocation);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(user.toString() + "_location", updatedLocation);
                editor.apply();
            }

            // reverse geocode and update weather
            Geocoder geocoder = new Geocoder(getContext());
            try {
                List<Address> addresses = geocoder.getFromLocationName(updatedLocation, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    LatLng latLng = new LatLng(addresses.get(0).getLatitude(), addresses.get(0).getLongitude());
                    getWeatherData(latLng.latitude, latLng.longitude);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            binding.textviewLocation.setVisibility(View.VISIBLE);
            binding.edittextLocation.setVisibility(View.GONE);
            binding.buttonLayout.setVisibility(View.GONE);
        });

        binding.buttonCancel.setOnClickListener(v -> {
            binding.edittextLocation.setText(binding.textviewLocation.getText());
            binding.textviewLocation.setVisibility(View.VISIBLE);
            binding.edittextLocation.setVisibility(View.GONE);
            binding.buttonLayout.setVisibility(View.GONE);
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        binding.buttonSecond.setOnClickListener(view1 ->
                NavHostFragment.findNavController(SecondFragment.this)
                        .navigate(R.id.action_SecondFragment_to_FirstFragment)
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        binding = null;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        Coordinates savedCoordinates = loadCoordinates(user.toString());
        LatLng initialLocation = savedCoordinates != null
                ? new LatLng(savedCoordinates.getLatitude(), savedCoordinates.getLongitude())
                : new LatLng(51.5074, 0.1278); // default location

        mMap.addMarker(new MarkerOptions()
                .position(initialLocation)
                .title(savedCoordinates != null ? "Saved Location" : "Default Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, 10));

        mMap.getUiSettings().setZoomControlsEnabled(true);

        // updates location text and fetch weather data
        updateLocationText(initialLocation);
        getWeatherData(initialLocation.latitude, initialLocation.longitude);

        enableUserLocation();

        mMap.setOnMapClickListener(latLng -> {
            Log.d("MapClick", "New Location Clicked: " + latLng.toString());

            if (user.getLocation() == null) {
                user.setLocation(new Location());
            }

            Coordinates coordinates = new Coordinates();
            coordinates.setLatitude(latLng.latitude);
            coordinates.setLongitude(latLng.longitude);

            user.getLocation().setCoordinates(coordinates);

            saveCoordinates(latLng, user);
            clearEditedLocation();

            mMap.clear();
            mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Updated Location"));

            float currentZoom = mMap.getCameraPosition().zoom;
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, currentZoom));

            updateLocationText(latLng);
            getWeatherData(latLng.latitude, latLng.longitude);
        });
    }

    private void enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
            return;
        }

        mMap.setMyLocationEnabled(true);
    }

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(requireActivity(),
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableUserLocation();
            } else {
                Log.e("Permission", "Location permission not granted");
            }
        }
    }

    private void clearEditedLocation() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(user.toString() + "_location");
        editor.apply();
    }

    private void updateLocationText(LatLng latLng) {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String savedCustomLocation = sharedPreferences.getString(user.toString() + "_location", null);

        if (savedCustomLocation != null) {
            // uses the custom location if it exists
            binding.textviewLocation.setText(savedCustomLocation);
        } else {
            // fallback to geocoding from coordinates
            Geocoder geocoder = new Geocoder(getContext());
            try {
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    StringBuilder fullAddress = new StringBuilder();

                    if (address.getThoroughfare() != null)
                        fullAddress.append(address.getThoroughfare());
                    if (address.getLocality() != null) {
                        if (fullAddress.length() > 0) fullAddress.append(", ");
                        fullAddress.append(address.getLocality());
                    }
                    if (address.getCountryName() != null) {
                        if (fullAddress.length() > 0) fullAddress.append(", ");
                        fullAddress.append(address.getCountryName());
                    }

                    String geocodedLocation = fullAddress.length() > 0 ? fullAddress.toString() : "Location: Not Registered";

                    // saves the geocoded location to SharedPreferences for fallback
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(user.toString() + "_location", geocodedLocation);
                    editor.apply();

                    binding.textviewLocation.setText(geocodedLocation);
                } else {
                    binding.textviewLocation.setText("Location: Not Registered");
                }
            } catch (IOException e) {
                e.printStackTrace();
                binding.textviewLocation.setText("Location: Not Registered");
            }
        }
    }

    private void saveCoordinates(LatLng latLng, User user) {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putFloat(user.toString() + LAT_KEY, (float) latLng.latitude);
        editor.putFloat(user.toString() + LNG_KEY, (float) latLng.longitude);
        editor.apply();
    }


    private Coordinates loadCoordinates(String userKey) {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (sharedPreferences.contains(userKey + LAT_KEY) && sharedPreferences.contains(userKey + LNG_KEY)) {
            return new Coordinates(
                    sharedPreferences.getFloat(userKey + LAT_KEY, 0),
                    sharedPreferences.getFloat(userKey + LNG_KEY, 0)
            );
        }
        return null;
    }

    private void getWeatherData(double latitude, double longitude) {
        String apiKey = getString(R.string.api_weather_key); // api key is in gradle.properties
        String urlString = "https://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude + "&units=metric&appid=" + apiKey;

        Log.d("WeatherRequest", "Requesting weather data for: " + latitude + ", " + longitude);

        new Thread(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                String response = stringBuilder.toString();

                Log.d("WeatherAPIResponse", response);

                JSONObject jsonObject = new JSONObject(response);

                // checks if 'main' and 'temp' exist in the JSON response
                if (jsonObject.has("main") && jsonObject.getJSONObject("main").has("temp")) {
                    double temperature = jsonObject.getJSONObject("main").getDouble("temp");
                    getActivity().runOnUiThread(() -> {
                        if (binding != null) {
                            binding.textviewTemperature.setText(String.format("%.1fºC", temperature));
                        }
                    });
                } else {
                    // if temperature is missing
                    getActivity().runOnUiThread(() -> {
                        if (binding != null) {
                            binding.textviewTemperature.setText("N/A");
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                getActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        binding.textviewTemperature.setText("Error");
                    }
                });
            }
        }).start();
    }
}
