package uk.ac.aston.cs3mdd.addresskeeper.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.Pair;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class UsersViewModel extends ViewModel {
    private final MutableLiveData<List<User>> allUsers;
    private final SharedPreferences sharedPreferences;
    private final Stack<Pair<User, Pair<String, Integer>>> deletedUsersStack; // for single deleted users, their custom locations, and positions
    private final Stack<Pair<List<User>, Map<String, String>>> deletedAllUsersStack;
    private static final String USERS_KEY = "users_key";
    private static final String PREFS_NAME = "UserPrefs";
    private static final String LAT_KEY = "latitude";
    private static final String LNG_KEY = "longitude";

    public UsersViewModel(Context context) {
        super();
        allUsers = new MutableLiveData<>(new ArrayList<>());
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        deletedUsersStack = new Stack<>();
        deletedAllUsersStack = new Stack<>();
        loadUsersFromPreferences();
    }

    private void loadUsersFromPreferences() {
        String usersJson = sharedPreferences.getString(USERS_KEY, "");
        Log.i("AJB", "Loaded users JSON: " + usersJson);
        if (!usersJson.isEmpty()) {
            Gson gson = new Gson();
            User[] users = gson.fromJson(usersJson, User[].class);
            List<User> userList = new ArrayList<>(Arrays.asList(users));
            allUsers.setValue(userList);
        }
    }

    private void saveUsersToPreferences() {
        Gson gson = new Gson();
        String usersJson = gson.toJson(allUsers.getValue());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(USERS_KEY, usersJson);
        editor.apply();
    }

    public LiveData<List<User>> getAllUsers() {
        return allUsers;
    }

    public boolean addUser(User user) {
        List<User> userList = getAllUsers().getValue();
        if (userList != null) {
            // checks for duplicate name
            for (User existingUser : userList) {
                if (existingUser.getName().getFirst().equalsIgnoreCase(user.getName().getFirst()) &&
                        existingUser.getName().getLast().equalsIgnoreCase(user.getName().getLast())) {
                    return false;
                }
            }
            userList.add(user);
            allUsers.setValue(userList);
            saveUsersToPreferences();
            return true;
        }
        return false;
    }

    public void deleteUser(User user) {
        if (user == null) return;

        // saves user, custom location, coordinates, and position to the deleted stack
        String customLocation = sharedPreferences.getString(user.toString() + "_location", null);
        float latitude = sharedPreferences.contains(user.toString() + "latitude")
                ? sharedPreferences.getFloat(user.toString() + "latitude", 0)
                : 0;
        float longitude = sharedPreferences.contains(user.toString() + "longitude")
                ? sharedPreferences.getFloat(user.toString() + "longitude", 0)
                : 0;

        List<User> currentUsers = allUsers.getValue();
        if (currentUsers != null) {
            int position = currentUsers.indexOf(user);
            deletedUsersStack.push(new Pair<>(user, new Pair<>(customLocation, position)));

            // saves coordinates in the user object if missing
            if (user.getLocation() == null) {
                user.setLocation(new Location());
            }
            if (user.getLocation().getCoordinates() == null) {
                user.getLocation().setCoordinates(new Coordinates(latitude, longitude));
            }

            // removes location data from SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(user.toString() + "_location");
            editor.remove(user.toString() + "latitude");
            editor.remove(user.toString() + "longitude");
            editor.apply();

            currentUsers.remove(user);
            allUsers.setValue(currentUsers);
            saveUsersToPreferences();
        }
    }

    public void updateUser(User updatedUser) {
        List<User> currentUsers = allUsers.getValue();
        if (currentUsers != null) {
            for (int i = 0; i < currentUsers.size(); i++) {
                User currentUser = currentUsers.get(i);
                if (currentUser.getId() != null && updatedUser.getId() != null &&
                        currentUser.getId().equals(updatedUser.getId())) {

                    if (updatedUser.getLocation() == null) {
                        updatedUser.setLocation(currentUser.getLocation());
                    } else if (updatedUser.getLocation().getCoordinates() == null &&
                            currentUser.getLocation() != null) {
                        updatedUser.getLocation().setCoordinates(currentUser.getLocation().getCoordinates());
                    }

                    currentUsers.set(i, updatedUser);
                    break;
                }
            }
            allUsers.setValue(new ArrayList<>(currentUsers));
            saveUsersToPreferences();
        }
    }

    public void deleteAllUsers() {
        List<User> currentUsers = allUsers.getValue();
        if (currentUsers != null) {
            Map<String, String> customLocations = new HashMap<>();
            Map<String, Pair<Float, Float>> coordinates = new HashMap<>();

            for (User user : currentUsers) {
                String customLocation = sharedPreferences.getString(user.toString() + "_location", null);
                float latitude = sharedPreferences.getFloat(user.toString() + "latitude", 0);
                float longitude = sharedPreferences.getFloat(user.toString() + "longitude", 0);

                if (customLocation != null) {
                    customLocations.put(user.toString(), customLocation);
                }
                coordinates.put(user.toString(), new Pair<>(latitude, longitude));
            }

            deletedAllUsersStack.push(new Pair<>(new ArrayList<>(currentUsers), customLocations));

            for (User user : currentUsers) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove(user.toString() + "_location");
                editor.remove(user.toString() + "latitude");
                editor.remove(user.toString() + "longitude");
                editor.apply();
            }

            currentUsers.clear();
            allUsers.setValue(currentUsers);
            saveUsersToPreferences();
        }
    }

    public void redoLastDeletion() {
        if (!deletedUsersStack.isEmpty()) {
            Pair<User, Pair<String, Integer>> lastDeleted = deletedUsersStack.pop();
            User user = lastDeleted.first;
            String customLocation = lastDeleted.second.first;
            int position = lastDeleted.second.second;

            List<User> currentUsers = allUsers.getValue();
            if (currentUsers != null) {
                // restores user to its original position
                currentUsers.add(position, user);
                allUsers.setValue(currentUsers);
                saveUsersToPreferences();
            }

            // restores custom location and coordinates
            if (user.getLocation() != null && user.getLocation().getCoordinates() != null) {
                float latitude = (float) user.getLocation().getCoordinates().getLatitude();
                float longitude = (float) user.getLocation().getCoordinates().getLongitude();

                SharedPreferences.Editor editor = sharedPreferences.edit();
                if (customLocation != null) {
                    editor.putString(user.toString() + "_location", customLocation);
                }
                editor.putFloat(user.toString() + "latitude", latitude);
                editor.putFloat(user.toString() + "longitude", longitude);
                editor.apply();
            }
        }
    }

    public void redoDeleteAll() {
        if (!deletedAllUsersStack.isEmpty()) {
            Pair<List<User>, Map<String, String>> lastDeletedAll = deletedAllUsersStack.pop();
            List<User> users = lastDeletedAll.first;
            Map<String, String> customLocations = lastDeletedAll.second;

            for (User user : users) {
                addUser(user);

                // restores custom location and coordinates
                String customLocation = customLocations.get(user.toString());
                if (user.getLocation() != null && user.getLocation().getCoordinates() != null) {
                    float latitude = (float) user.getLocation().getCoordinates().getLatitude();
                    float longitude = (float) user.getLocation().getCoordinates().getLongitude();

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    if (customLocation != null) {
                        editor.putString(user.toString() + "_location", customLocation);
                    }
                    editor.putFloat(user.toString() + "latitude", latitude);
                    editor.putFloat(user.toString() + "longitude", longitude);
                    editor.apply();
                }
            }
        }
    }

    public void updateUserOrder(List<User> updatedList) {
        allUsers.setValue(new ArrayList<>(updatedList));
        saveUsersToPreferences();
    }
}
