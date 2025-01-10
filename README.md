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

https://github.com/user-attachments/assets/57b80566-a5ad-464e-8b73-60beed01ede5

<div style="display: flex; justify-content: space-between;">
    <img src="https://github.com/user-attachments/assets/1b8ef46d-4d69-4389-88d6-ddf3537b9e15" width="32.9%" />
    <img src="https://github.com/user-attachments/assets/740d7e6d-fa92-4759-86f9-1fad974fbbc2" width="32.9%" />
    <img src="https://github.com/user-attachments/assets/a0ff3bbf-af79-41e2-8c48-71c35ffefe3e" width="32.9%" />
    <img src="https://github.com/user-attachments/assets/2281aed5-6702-4625-8d3f-6de8976b8bbe" width="32.9%" />
    <img src="https://github.com/user-attachments/assets/ab8a2908-648f-4743-9086-b54ff18cf4b6" width="32.9%" />
    <img src="https://github.com/user-attachments/assets/4d5859bd-df7b-4dce-be84-d43def23518f" width="32.9%" />
    <img src="https://github.com/user-attachments/assets/1fb8c2ca-c8ad-4899-b223-3da9e8a98a63" width="32.9%" />
    <img src="https://github.com/user-attachments/assets/10baea59-36b7-42a1-916d-78bec35eec87" width="32.9%" />
    <img src="https://github.com/user-attachments/assets/33cddfa9-dc64-4d08-a00c-083f46d861b5" width="32.9%" />
    <img src="https://github.com/user-attachments/assets/aabcf877-8810-49ae-a4e0-90848418aaad" width="32.9%" />
    <img src="https://github.com/user-attachments/assets/73c923c7-e0ae-45fc-b89e-5ec853a8b402" width="32.9%" />
    <img src="https://github.com/user-attachments/assets/4cc7e698-fc6a-4d79-a573-31d19f1594f3" width="32.9%" />
</div>
