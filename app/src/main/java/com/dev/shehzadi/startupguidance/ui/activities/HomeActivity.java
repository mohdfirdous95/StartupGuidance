package com.dev.shehzadi.startupguidance.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.dev.shehzadi.startupguidance.R;
import com.dev.shehzadi.startupguidance.models.UserModel;
import com.dev.shehzadi.startupguidance.ui.fragments.ChatUsersFragment;
import com.dev.shehzadi.startupguidance.ui.fragments.EventFragment;
import com.dev.shehzadi.startupguidance.ui.fragments.StartupStoryFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private FloatingActionButton fab;
    private View header;
    private ImageView ivHeaderUserImage;
    private TextView tvHeaderUsername, tvHeaderEmail;

    private FirebaseAuth auth;
    private DatabaseReference reference;

    private FragmentManager manager;
    private UserModel user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        auth = FirebaseAuth.getInstance();
        reference = FirebaseDatabase.getInstance().getReference("Users/" + auth.getUid());

        manager = getSupportFragmentManager();
        manager.beginTransaction().replace(R.id.content_main, new EventFragment()).commit();

        fab = findViewById(R.id.fab);
        fab.setVisibility(View.GONE);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        header = navigationView.getHeaderView(0);
        ivHeaderUserImage = header.findViewById(R.id.imageView_nav_header);
        tvHeaderUsername = header.findViewById(R.id.textView_username_nav_header);
        tvHeaderEmail = header.findViewById(R.id.textView_userEmail_nav_header);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                user = dataSnapshot.getValue(UserModel.class);

                if (user != null) {
                    tvHeaderUsername.setText(user.getFullName());
                    tvHeaderEmail.setText(user.getEmailId());

                    String photo = user.getPhotoLocation();

                    if(photo != null && !photo.trim().equals("")){
                        Glide.with(HomeActivity.this)
                                .load(photo)
                                .into(ivHeaderUserImage);
                    }
                    else {
                        ivHeaderUserImage.setOnClickListener(view -> {

                        });
                    }

                    if(user.isSuperUser()){
                        fab.setVisibility(View.VISIBLE);
                        Log.e("FAB", "FAB.VISIBLE");
                    }
                    else {
                        fab.setVisibility(View.GONE);
                        Log.e("FAB", "FAB.GONE");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        if (user != null)
            Log.e("User", user.toString());

    }

    public FloatingActionButton getFAB() {
        return fab;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id){
            case R.id.action_settings:
                break;
            case R.id.action_logout:
                new AlertDialog.Builder(this)
                        .setTitle("Logout")
                        .setMessage("Are you sure you want to logout?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            auth.signOut();
                            Intent logout = getBaseContext().getPackageManager()
                                    .getLaunchIntentForPackage( getBaseContext().getPackageName() );
                            logout.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(logout);
                        })
                        .setNegativeButton("No", null)
                        .show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id){
            case R.id.nav_events:
                manager.beginTransaction().replace(R.id.content_main, new EventFragment()).commit();
                break;
            case R.id.nav_startup_stories:
                manager.beginTransaction().replace(R.id.content_main, new StartupStoryFragment()).commit();
                break;
            case R.id.nav_chat:
                manager.beginTransaction().replace(R.id.content_main, new ChatUsersFragment()).commit();
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
