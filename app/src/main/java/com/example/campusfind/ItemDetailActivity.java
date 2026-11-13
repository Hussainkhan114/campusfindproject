package com.example.campusfind;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class ItemDetailActivity extends AppCompatActivity {

    DBHelper db;
    String itemId, title, reportedByEmail;
    double lat, lng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        db = new DBHelper(this);
        itemId = getIntent().getStringExtra("ITEM_ID");

        SharedPreferences sp = getSharedPreferences("CampusSession", MODE_PRIVATE);
        String loggedEmail = sp.getString("email", "");

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvTitle = findViewById(R.id.tvDetTitle);
        TextView tvBadges = findViewById(R.id.tvDetBadges);
        TextView tvId = findViewById(R.id.tvDetId);
        TextView tvCat = findViewById(R.id.tvDetCategory);
        TextView tvPlace = findViewById(R.id.tvDetPlace);
        TextView tvDate = findViewById(R.id.tvDetDate);
        TextView tvReportedBy = findViewById(R.id.tvDetReportedBy);
        TextView tvLocation = findViewById(R.id.tvDetLocation);
        Button btnOpenMaps = findViewById(R.id.btnOpenMaps);
        Button btnMarkReturned = findViewById(R.id.btnMarkReturned);

        btnBack.setOnClickListener(v -> finish());

        Cursor c = db.getItemById(itemId);
        if (c.moveToFirst()) {
            String type = c.getString(1).toUpperCase();
            title = c.getString(2);
            String cat = c.getString(3);
            String place = c.getString(4);
            String contact = c.getString(5);
            String date = c.getString(6);
            lat = c.getDouble(7);
            lng = c.getDouble(8);
            String status = c.getString(9).toUpperCase();
            reportedByEmail = c.getString(10);
            String reportedByName = c.getString(11);

            tvTitle.setText(title);
            tvBadges.setText(type + "   •   " + status);
            tvId.setText(itemId);
            tvCat.setText(cat);
            tvPlace.setText(place + " (" + contact + ")");
            tvDate.setText(date);
            tvReportedBy.setText(reportedByName);
            tvLocation.setText(String.format(Locale.US, "%.5f, %.5f", lat, lng));

            // R8: Only the user who reported an item sees "Mark as Returned"
            if (loggedEmail.equalsIgnoreCase(reportedByEmail) && !status.equals("RETURNED")) {
                btnMarkReturned.setVisibility(View.VISIBLE);
            } else {
                btnMarkReturned.setVisibility(View.GONE);
            }
        }
        c.close();

        // F10: Open in Maps with a pin on the place
        btnOpenMaps.setOnClickListener(v -> {
            String uri = String.format(Locale.US, "geo:%.5f,%.5f?q=%.5f,%.5f(%s)", lat, lng, lat, lng, Uri.encode(title));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            startActivity(mapIntent);
        });

        // R8 & F12: Mark as Returned
        btnMarkReturned.setOnClickListener(v -> {
            db.markItemReturned(itemId);
            tvBadges.setText(tvBadges.getText().toString().split("•")[0].trim() + "   •   RETURNED");
            btnMarkReturned.setVisibility(View.GONE);
            Toast.makeText(this, "Marked as Returned", Toast.LENGTH_SHORT).show();
        });
    }
}
