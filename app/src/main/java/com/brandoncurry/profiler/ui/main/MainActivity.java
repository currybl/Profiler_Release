package com.brandoncurry.profiler.ui.main;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.brandoncurry.profiler.R;
import com.brandoncurry.profiler.adapters.ProfilesAdapter;
import com.brandoncurry.profiler.global.Constants;
import com.brandoncurry.profiler.global.ProfilerApplication;
import com.brandoncurry.profiler.models.Profile;
import com.brandoncurry.profiler.ui.profile.CreateProfileDialogFragment;
import com.brandoncurry.profiler.ui.profile.ProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    Toolbar toolbar;
    ArrayList<Profile> profileList;
    private RecyclerView profilesRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private DatabaseReference ref;
    private FirebaseDatabase database;
    private FirebaseUser thisUser;
    private Button btnAddProfile;
    private Query query;
    private Constants.FilterType currentFilterType = Constants.FilterType.NONE;
    private Constants.SortType currentSortType = Constants.SortType.ASCENDING;


    AlertDialog filterDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        thisUser = FirebaseAuth.getInstance().getCurrentUser();

        toolbar.setNavigationIcon(R.drawable.ic_action_profile);
        setSupportActionBar(toolbar);
        setTitle("");

        btnAddProfile = (Button) findViewById(R.id.btnAddProfile);
        btnAddProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(thisUser!=null){
                    showAddProfileDialog();

                } else {
                    ProfilerApplication.showLoginDialog(MainActivity.this, getString(R.string.action_create_profile));
                }
            }
        });

        profilesRecyclerView = (RecyclerView) findViewById(R.id.rvProfiles);
        profileList = new ArrayList<>();

        database = FirebaseDatabase.getInstance();
        ref = database.getReference().child("Profile");

        getProfiles(Constants.FilterType.NONE, Constants.SortType.ASCENDING);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_filter:
                showFilteringOptions();
                return true;
            case R.id.action_order:
                sortProfiles(currentSortType);
                return true;
            case android.R.id.home:
                    Intent profileIntent = new Intent(this, ProfileActivity.class);
                    profileIntent.putExtra(ProfileActivity.PROFILE_ID, thisUser.getUid());
                    profileIntent.putExtra(ProfileActivity.PROFILE_TYPE_SELF, true);
                    startActivity(profileIntent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void showAddProfileDialog(){
        android.app.FragmentManager fm = getFragmentManager();
        CreateProfileDialogFragment dialogFragment = new CreateProfileDialogFragment ();
        dialogFragment.show(fm, getResources().getString(R.string.title_add_profile));
    }

    public void showFilteringOptions(){


        final CharSequence[] filterOptions = {
                getResources().getString(R.string.all),
                getResources().getString(R.string.male),
                getResources().getString(R.string.female),
                getResources().getString(R.string.name),
                getResources().getString(R.string.age)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.filter_profiles));

        builder.setItems(filterOptions, new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        switch (i){
                            case 0:
                                getProfiles(Constants.FilterType.NONE, currentSortType);
                                currentFilterType = Constants.FilterType.NONE;
                                break;
                            case 1:
                                getProfiles(Constants.FilterType.MALE, currentSortType);
                                currentFilterType = Constants.FilterType.MALE;
                                break;
                            case 2:
                                getProfiles(Constants.FilterType.FEMALE, currentSortType);
                                currentFilterType = Constants.FilterType.FEMALE;
                                break;
                            case 3:
                                getProfiles(Constants.FilterType.NAME, currentSortType);
                                currentFilterType = Constants.FilterType.NAME;
                                break;
                            case 4:
                                getProfiles(Constants.FilterType.AGE, currentSortType);
                                currentFilterType = Constants.FilterType.AGE;
                                break;
                        }

                        filterDialog.dismiss();
                    }
                });

        builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        filterDialog = builder.create();
        filterDialog.show();
}


    public void getProfiles(Constants.FilterType filterType, Constants.SortType sortType){
        switch (filterType){
            case NONE:
                query =  ref.orderByChild("id");
                break;
            case MALE:
                query =  ref.orderByChild("gender").equalTo("Male");
                break;
            case FEMALE:
                query =  ref.orderByChild("gender").equalTo("Female");
                break;
            case AGE:
                query =  ref.orderByChild("age");
                break;
            default:
                query =  ref.orderByChild("id");
        }
        if(sortType.equals(sortType.ASCENDING)){
                query.limitToFirst(100);
        } else {
                query.limitToLast(100);
        }

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                profileList.clear();

                for (DataSnapshot snapshot: dataSnapshot.getChildren()) {
                    Profile profile = snapshot.getValue(Profile.class);
                    profileList.add(profile);
                }

                mLayoutManager = new LinearLayoutManager(MainActivity.this);
                profilesRecyclerView.setLayoutManager(mLayoutManager);

                mAdapter = new ProfilesAdapter(MainActivity.this, profileList);
                profilesRecyclerView.setAdapter(mAdapter);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    public void sortProfiles(Constants.SortType sortType){
        if(currentSortType.equals(Constants.SortType.ASCENDING)){
            currentSortType = Constants.SortType.DESCENDING;
        } else {
            currentSortType = Constants.SortType.ASCENDING;
        }
        getProfiles(currentFilterType, sortType);
    }

}
