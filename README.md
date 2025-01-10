# address-keeper
Address Keeper is an Android application built in Java that allows users to create, edit, and delete entities.

By pressing on an entity, the Map screen opens, where users can mark the location by tapping anywhere on the Google Maps map.
When the location is changed, the address text and weather temperature are updated dynamically to reflect the new selection.
Users can also overwrite the address text with their own custom text.

All the changes are saved using SharedPreferences and File.

## Setup

To use the application, you'll need:
1. A [Google Maps](https://developers.google.com/maps/documentation/javascript/get-api-key) API key.
2. An [OpenWeather](https://openweathermap.org/api) API key.

**Note:** Both APIs have free plans.

Replace the following with your API keys for:
- Google Maps (in `AndroidManifest.xml)`:
```
android:value="YOUR_GOOGLE_MAPS_API_KEY" />
```

- OpenWeather (in `gradle.properties)`:
```
WEATHER_API_KEY=YOUR_OPEN_WEATHER_API_KEY
```

Sync the project afterwards and run the app.

## Preview
https://github.com/user-attachments/assets/15493618-6684-41c7-a257-08dc8f95e78c

<div style="display: flex; justify-content: space-between;">
    <img src="https://github.com/user-attachments/assets/d7d424f1-6b80-41a8-82c0-27a7958692ba" width="32.9%" />
    <img src="https://github.com/user-attachments/assets/eb7d4f5e-ebdc-4c01-b6e3-966e5e572d80" width="32.9%" />
    <img src="https://github.com/user-attachments/assets/c8e39705-1b08-4ccc-a19a-98bbaa8f4209" width="32.9%" />
    <img src="https://github.com/user-attachments/assets/132b94ac-e140-47bb-b663-cf6fa93cd4e7" width="32.9%" />
    <img src="https://github.com/user-attachments/assets/e802d009-8319-4f9b-bae7-2d0acc60875e" width="32.9%" />
    <img src="https://github.com/user-attachments/assets/ccd5c1db-9580-4712-a3e0-1c41bff28a32" width="32.9%" />
    <img src="https://github.com/user-attachments/assets/91e13199-a679-46b7-be4e-2284ff527a1c" width="32.9%" />
    <img src="https://github.com/user-attachments/assets/21d2f5a0-8832-48f1-aec7-8d6ed8a8b417" width="32.9%" />
    <img src="https://github.com/user-attachments/assets/ab22f4ad-936c-4d4f-8249-6a1a3194bce3" width="32.9%" />
    <img src="https://github.com/user-attachments/assets/74c1897a-fd27-457f-b36b-f43ead9d9561" width="32.9%" />
    <img src="https://github.com/user-attachments/assets/5629d69f-6889-4a16-bf16-a6b9eb49f8b8" width="32.9%" />
    <img src="https://github.com/user-attachments/assets/5c693e1a-aa35-49dd-90b5-ed2e773939e7" width="32.9%" />
</div>
