package com.brandoncurry.profiler.ui.profile;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.brandoncurry.profiler.R;
import com.brandoncurry.profiler.global.Constants;
import com.brandoncurry.profiler.models.Profile;
import com.brandoncurry.profiler.ui.landing.LandingActivity;
import com.brandoncurry.profiler.utils.ProfileManager;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends AppCompatActivity {

    public static final String PROFILE_ID = "profileid";
    public static final String PROFILE_TYPE_SELF = "profiletype";
    private String profileId;
    private DatabaseReference ref;
    private FirebaseUser thisUser;
    private Profile currentProfile;
    private DatabaseReference mDatabase;
    RelativeLayout rlProfileBackground;
    private TextView tvHobbies, tvAgeGender;
    private ImageView ivProfileImage;
    ProfileManager profileManager;
    private Button btnLogout;
    private boolean thisIsMe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        getSupportActionBar().setDisplayHomeAsUpEnabled (true);
        thisUser = FirebaseAuth.getInstance().getCurrentUser();
        rlProfileBackground = (RelativeLayout) findViewById(R.id.rlProfileBackground);
        ivProfileImage = (ImageView) findViewById(R.id.ivProfilePhoto);
        tvHobbies = (TextView) findViewById(R.id.tvHobbies);
        tvAgeGender = (TextView) findViewById(R.id.tvAgeGender);
        btnLogout = (Button) findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent logout = new Intent(ProfileActivity.this, LandingActivity.class);
                startActivity(logout);
            }
        });

        mDatabase = FirebaseDatabase.getInstance().getReference();
        ref = mDatabase.child("Profile");

        Intent getExtras = getIntent();
        profileId = getExtras.getStringExtra(PROFILE_ID);
        thisIsMe = getExtras.getBooleanExtra(PROFILE_TYPE_SELF, false);

        if(thisIsMe) {
            getProfile(Constants.GET_PROFILE_BY_USER_ID, profileId);
        } else {
            getProfile(Constants.GET_PROFILE_BY_PROFILE_ID, profileId);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if(thisIsMe)
        inflater.inflate(R.menu.profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            case R.id.action_edit:
                showEditOptions();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void getProfile(String idType, String id){
         Query query =  ref.orderByChild(idType).equalTo(id);
                query.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        for (DataSnapshot snapshot: dataSnapshot.getChildren()) {
                            currentProfile = snapshot.getValue(Profile.class);
                        }

                        try{
                            thisIsMe =  currentProfile.userId.equals(thisUser.getUid());
                        } catch (NullPointerException npe){
                            thisIsMe = false;
                        }

                        drawProfile(currentProfile);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }


    public void drawProfile(Profile profile){

        try{
            if(thisIsMe){
                setTitle(profile.name + getResources().getString(R.string.me));
            } else {
                setTitle(profile.name);
                btnLogout.setVisibility(View.GONE);
            }

            rlProfileBackground.setBackgroundColor(ProfileManager.getBackgroundColor(this, currentProfile.bgColor));

            Glide.with(getApplicationContext())
                    .load(profile.imageUrl)
                    .centerCrop()
                    .crossFade()
                    .into(ivProfileImage);

            tvHobbies.setText(profile.hobbies);
            tvAgeGender.setText(profile.age + " " + profile.gender);
        } catch (NullPointerException npe){
            if(thisIsMe){
                showNoProfileDialog();
            } else {
                showNoProfileDialog();
                Toast.makeText(ProfileActivity.this, getResources().getString(R.string.profile_error), Toast.LENGTH_LONG).show();
            }
        }
    }

    public void showNoProfileDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getResources().getString(R.string.profile_error_message))
                .setTitle(getResources().getString(R.string.profile_not_found))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.gotit), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void showEditOptions(){
        final CharSequence[] items = {
                getString(R.string.change_background_color),
                getString(R.string.edit_hobbies),
                getString(R.string.remove_profile)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.profile_options));
        builder.setItems(items, new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int item) {

                switch (item){
                    case 0:
                        showBackgroundColorOptions();
                        break;
                    case 1:
                        editHobbies();
                        break;
                    case 2:
                        showProfileRemovalPrompt();
                        break;
                }
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void showBackgroundColorOptions(){
        final CharSequence[] items = {
                "Red",
                "Orange",
                "Green",
                "Yellow",
                "Blue",
                "Purple",
                "Black"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.choose_color));
        builder.setItems(items, new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int item) {

                String color = items[item].toString();
                profileManager.setCustomBackgroundColor(color, thisUser.getUid());
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void showProfileRemovalPrompt(){
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        profileManager.removeProfile(currentProfile.userId);
                        finish();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
        builder.setMessage(getString(R.string.confirm_remove_profile)).setPositiveButton(getString(R.string.yes), dialogClickListener)
                .setNegativeButton(getString(R.string.no), dialogClickListener).show();
    }

    public void editHobbies(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ProfileActivity.this);
        alertDialog.setTitle(getString(R.string.edit_hobbies));
        alertDialog.setMessage(getString(R.string.tell_the_world));

        final EditText input = new EditText(ProfileActivity.this);
        input.setHint(currentProfile.hobbies);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        alertDialog.setView(input);
        alertDialog.setIcon(R.drawable.ic_action_edit_profile);

        alertDialog.setPositiveButton(getString(R.string.save),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String newHobbies = input.getText().toString();
                        profileManager.updateHobbies(newHobbies, thisUser.getUid());
                    }
                });

        alertDialog.setNegativeButton(getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        alertDialog.show();

}


}
