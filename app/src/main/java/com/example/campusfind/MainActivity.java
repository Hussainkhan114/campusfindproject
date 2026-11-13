package com.example.campusfind;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButtonToggleGroup;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    DBHelper db;
    SharedPreferences sp;
    String userEmail, userName;

    TextView tvToolbarTitle;
    LinearLayout layoutHome, layoutReport, layoutNearby, layoutProfile;
    BottomNavigationView bottomNav;

    // Home Views
    EditText etSearch;
    MaterialButtonToggleGroup toggleHomeFilter;
    ListView lvHome;
    String currentFilter = "All";
    ArrayList<String> homeIds = new ArrayList<>();

    // Report Views
    MaterialButtonToggleGroup toggleReportType;
    EditText etRepTitle, etRepPlace, etRepContact;
    Spinner spRepCategory;
    Button btnUseLocation, btnSaveItem;
    TextView tvRepCoords;
    String selectedType = "Lost";
    double currentLat = 31.38930, currentLng = 74.24040;
    boolean locationFetched = false;

    // Nearby Views
    TextView tvYourLocation;
    ListView lvNearby;
    ArrayList<String> nearbyIds = new ArrayList<>();

    // Profile Views
    TextView tvProfName, tvProfEmail;
    Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DBHelper(this);
        sp = getSharedPreferences("CampusSession", MODE_PRIVATE);
        userEmail = sp.getString("email", "");
        userName = sp.getString("name", "Student");

        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        layoutHome = findViewById(R.id.layoutHome);
        layoutReport = findViewById(R.id.layoutReport);
        layoutNearby = findViewById(R.id.layoutNearby);
        layoutProfile = findViewById(R.id.layoutProfile);
        bottomNav = findViewById(R.id.bottomNav);

        setupHome();
        setupReport();
        setupNearby();
        setupProfile();

        bottomNav.setOnItemSelectedListener(item -> {
            layoutHome.setVisibility(View.GONE);
            layoutReport.setVisibility(View.GONE);
            layoutNearby.setVisibility(View.GONE);
            layoutProfile.setVisibility(View.GONE);

            int id = item.getItemId();
            if (id == R.id.nav_home) {
                tvToolbarTitle.setText("CampusFind");
                layoutHome.setVisibility(View.VISIBLE);
                loadHomeList();
            } else if (id == R.id.nav_nearby) {
                tvToolbarTitle.setText("Nearby Items");
                layoutNearby.setVisibility(View.VISIBLE);
                fetchLocationAndLoadNearby();
            } else if (id == R.id.nav_report) {
                tvToolbarTitle.setText("Report an Item");
                layoutReport.setVisibility(View.VISIBLE);
            } else if (id == R.id.nav_profile) {
                tvToolbarTitle.setText("Profile");
                layoutProfile.setVisibility(View.VISIBLE);
            }
            return true;
        });

        loadHomeList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (layoutHome.getVisibility() == View.VISIBLE) loadHomeList();
        if (layoutNearby.getVisibility() == View.VISIBLE) fetchLocationAndLoadNearby();
    }

    // --- TAB 1: HOME ---
    private void setupHome() {
        etSearch = findViewById(R.id.etSearch);
        toggleHomeFilter = findViewById(R.id.toggleHomeFilter);
        lvHome = findViewById(R.id.lvHome);

        toggleHomeFilter.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnFilterAll) currentFilter = "All";
                else if (checkedId == R.id.btnFilterLost) currentFilter = "Lost";
                else if (checkedId == R.id.btnFilterFound) currentFilter = "Found";
                loadHomeList();
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { loadHomeList(); }
            public void afterTextChanged(Editable s) {}
        });

        lvHome.setOnItemClickListener((parent, view, position, id) -> {
            Intent i = new Intent(this, ItemDetailActivity.class);
            i.putExtra("ITEM_ID", homeIds.get(position));
            startActivity(i);
        });
    }

    private void loadHomeList() {
        homeIds.clear();
        ArrayList<HashMap<String, String>> list = new ArrayList<>();
        Cursor c = db.getAllItems(currentFilter, etSearch.getText().toString());
        while (c.moveToNext()) {
            String id = c.getString(0);
            String type = c.getString(1).toUpperCase();
            String title = c.getString(2);
            String cat = c.getString(3);
            String place = c.getString(4);
            String date = c.getString(6);
            String status = c.getString(9);

            homeIds.add(id);
            HashMap<String, String> map = new HashMap<>();
            map.put("title", title);
            map.put("sub", cat + " • " + place + "\n" + date);
            map.put("badge", status.equals("Returned") ? "RETURNED" : type);
            list.add(map);
        }
        c.close();

        SimpleAdapter adapter = new SimpleAdapter(this, list, R.layout.row_item,
                new String[]{"title", "sub", "badge"},
                new int[]{R.id.tvRowTitle, R.id.tvRowSub, R.id.tvRowRight});
        lvHome.setAdapter(adapter);
    }

    // --- TAB 2: REPORT AN ITEM (R3, R4, R5, F6, F7, F8) ---
    private void setupReport() {
        toggleReportType = findViewById(R.id.toggleReportType);
        etRepTitle = findViewById(R.id.etRepTitle);
        spRepCategory = findViewById(R.id.spRepCategory);
        etRepPlace = findViewById(R.id.etRepPlace);
        etRepContact = findViewById(R.id.etRepContact);
        btnUseLocation = findViewById(R.id.btnUseLocation);
        tvRepCoords = findViewById(R.id.tvRepCoords);
        btnSaveItem = findViewById(R.id.btnSaveItem);

        String[] categories = {"Electronics", "Wallets & Cards", "Keys", "Books & Stationery", "Other"};
        spRepCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories));

        toggleReportType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                selectedType = (checkedId == R.id.btnTypeLost) ? "Lost" : "Found";
            }
        });

        btnUseLocation.setOnClickListener(v -> requestAndFetchGPS(true));

        btnSaveItem.setOnClickListener(v -> {
            String title = etRepTitle.getText().toString().trim();
            String place = etRepPlace.getText().toString().trim();
            String contact = etRepContact.getText().toString().trim();
            String category = spRepCategory.getSelectedItem().toString();

            // R3 Field-level errors
            boolean valid = true;
            if (title.length() < 3) {
                etRepTitle.setError("Title must be at least 3 characters");
                valid = false;
            }
            if (place.isEmpty()) {
                etRepPlace.setError("Place is required");
                valid = false;
            }
            if (!contact.matches("03\\d{9}")) {
                etRepContact.setError("11 digits starting with 03 (e.g. 03001234567)");
                valid = false;
            }
            if (!locationFetched) {
                Toast.makeText(this, "Please tap 'Use my current location' first", Toast.LENGTH_SHORT).show();
                valid = false;
            }
            if (!valid) return;

            String nextId = db.getNextItemId();
            String today = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH).format(new Date());

            if (db.insertItem(nextId, selectedType, title, category, place, contact, today, currentLat, currentLng, userEmail, userName)) {
                Toast.makeText(this, "Item Saved (" + nextId + ")", Toast.LENGTH_SHORT).show();
                etRepTitle.setText("");
                etRepPlace.setText("");
                etRepContact.setText("");
                locationFetched = false;
                tvRepCoords.setText("Lat --, Lng --");
                bottomNav.setSelectedItemId(R.id.nav_home);
            }
        });
    }

    // --- TAB 3: NEARBY ITEMS (R7, F11) ---
    private void setupNearby() {
        tvYourLocation = findViewById(R.id.tvYourLocation);
        lvNearby = findViewById(R.id.lvNearby);

        lvNearby.setOnItemClickListener((parent, view, position, id) -> {
            Intent i = new Intent(this, ItemDetailActivity.class);
            i.putExtra("ITEM_ID", nearbyIds.get(position));
            startActivity(i);
        });
    }

    private void fetchLocationAndLoadNearby() {
        requestAndFetchGPS(false);
    }

    private void requestAndFetchGPS(boolean fromReportForm) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (loc != null) {
            currentLat = loc.getLatitude();
            currentLng = loc.getLongitude();
        } else {
            // Default UOL Campus coordinates if emulator GPS is not set
            currentLat = 31.38930;
            currentLng = 74.24040;
        }
        locationFetched = true;

        if (fromReportForm) {
            // R4: 5 decimal places
            tvRepCoords.setText(String.format(Locale.US, "Lat %.5f, Lng %.5f", currentLat, currentLng));
        } else {
            tvYourLocation.setText(String.format(Locale.US, "Your location:\n%.5f, %.5f", currentLat, currentLng));
            loadNearbySortedList();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestAndFetchGPS(layoutReport.getVisibility() == View.VISIBLE);
            } else {
                // R4 exact message on denial without crashing
                Toast.makeText(this, "Location permission is required to tag this item", Toast.LENGTH_LONG).show();
                if (layoutNearby.getVisibility() == View.VISIBLE) {
                    tvYourLocation.setText(String.format(Locale.US, "Your location:\n%.5f, %.5f", currentLat, currentLng));
                    loadNearbySortedList();
                }
            }
        }
    }

    // R7: Distance calculation & nearest-first sorting
    private static class ItemDist {
        String id, title, sub, distText;
        float meters;
    }

    private void loadNearbySortedList() {
        ArrayList<ItemDist> temp = new ArrayList<>();
        Cursor c = db.getAllItems("All", "");
        while (c.moveToNext()) {
            ItemDist item = new ItemDist();
            item.id = c.getString(0);
            String type = c.getString(1).toUpperCase();
            item.title = c.getString(2);
            String place = c.getString(4);
            double lat = c.getDouble(7);
            double lng = c.getDouble(8);

            float[] results = new float[1];
            Location.distanceBetween(currentLat, currentLng, lat, lng, results);
            item.meters = results[0];

            // R7: whole metres below 1,000 m, otherwise km with 1 decimal (e.g. 1.2 km)
            if (item.meters < 1000) {
                item.distText = Math.round(item.meters) + " m";
            } else {
                item.distText = String.format(Locale.US, "%.1f km", item.meters / 1000.0f);
            }
            item.sub = place + " • " + type;
            temp.add(item);
        }
        c.close();

        Collections.sort(temp, (a, b) -> Float.compare(a.meters, b.meters));

        nearbyIds.clear();
        ArrayList<HashMap<String, String>> list = new ArrayList<>();
        for (ItemDist d : temp) {
            nearbyIds.add(d.id);
            HashMap<String, String> map = new HashMap<>();
            map.put("title", d.title);
            map.put("sub", d.sub);
            map.put("dist", d.distText);
            list.add(map);
        }

        SimpleAdapter adapter = new SimpleAdapter(this, list, R.layout.row_item,
                new String[]{"title", "sub", "dist"},
                new int[]{R.id.tvRowTitle, R.id.tvRowSub, R.id.tvRowRight});
        lvNearby.setAdapter(adapter);
    }

    // --- TAB 4: PROFILE & LOGOUT (F3, F9, R2) ---
    private void setupProfile() {
        tvProfName = findViewById(R.id.tvProfName);
        tvProfEmail = findViewById(R.id.tvProfEmail);
        btnLogout = findViewById(R.id.btnLogout);

        tvProfName.setText(userName);
        tvProfEmail.setText(userEmail);

        btnLogout.setOnClickListener(v -> {
            sp.edit().clear().apply();
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }
}