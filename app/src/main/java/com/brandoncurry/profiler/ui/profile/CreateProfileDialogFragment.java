package com.brandoncurry.profiler.ui.profile;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;

import com.brandoncurry.profiler.R;
import com.brandoncurry.profiler.global.Constants;
import com.brandoncurry.profiler.models.Profile;
import com.brandoncurry.profiler.ui.landing.RegisterActivity;
import com.brandoncurry.profiler.ui.main.MainActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

/**
 * Created by BrandonCurry on 3/7/17.
 */

public class CreateProfileDialogFragment extends DialogFragment {

    private ImageView profileImage;
    private EditText etName, etEmail, etPassword, etAge, etHobbies;
    private Button btnRegister;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mDatabase;
    private FirebaseStorage storage;
    private String imageUrl;
    private RadioButton rbMale, rbFemale;

    private static final int SELECT_PICTURE = 100;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_register, container, false);

        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        mDatabase = FirebaseDatabase.getInstance().getReference();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(FirebaseAuth firebaseAuth) {
            }
        };


        profileImage = (ImageView) rootView.findViewById(R.id.ivProfilePhoto);
        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setType("image/*");
                i.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(i, getResources().getString(R.string.select_photo)),SELECT_PICTURE );
            }
        });
        profileImage.setDrawingCacheEnabled(true);
        profileImage.buildDrawingCache();

        etName = (EditText) rootView.findViewById(R.id.etUserName);
        etEmail = (EditText) rootView.findViewById(R.id.etEmail);
        etPassword = (EditText) rootView.findViewById(R.id.etPassword);
        etAge = (EditText) rootView.findViewById(R.id.etAge);
        etHobbies = (EditText) rootView.findViewById(R.id.etHobbies);
        rbMale = (RadioButton) rootView.findViewById(R.id.rbMale);
        rbFemale = (RadioButton) rootView.findViewById(R.id.rbFemale);
        btnRegister = (Button) rootView.findViewById(R.id.btnRegister);
        btnRegister.setText(getResources().getString(R.string.create_profile));

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(validateForm()){
                    uploadImage();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(getResources().getString(R.string.missing_profile_info))
                            .setCancelable(false)
                            .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
            }
        });

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if(resultCode== getActivity().RESULT_OK){
            if(requestCode==SELECT_PICTURE){
                Uri selectedImageUri = data.getData();
                if (null != selectedImageUri) {
                    profileImage.setImageURI(selectedImageUri);
                }
            }
        }
    }

    private void createNewProfile(String userId, String imageUrl, String name, String age, String gender, String hobbies, String bgColor) {
        //User is already registered, just create a new profile
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Profile profile = new Profile(UUID.randomUUID().toString(), userId, imageUrl, name, age, gender, hobbies, bgColor);
            mDatabase.child("Profile").child(userId).setValue(profile);
            Intent main = new Intent(getActivity(), MainActivity.class);
            getActivity().startActivity(main);
        } else {

        }

    }

    public void uploadImage(){
        StorageReference storageRef = storage.getReference();

        StorageReference profilePhotosRef = storageRef.child("profilephoto.jpg");
        StorageReference mountainImagesRef = storageRef.child("images/profilephoto.jpg");

        profilePhotosRef.getName().equals(mountainImagesRef.getName());
        profilePhotosRef.getPath().equals(mountainImagesRef.getPath());

        Bitmap bitmap = profileImage.getDrawingCache();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = profilePhotosRef.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                imageUrl = downloadUrl.toString();
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user!=null){
                    createNewProfile(user.getUid(), imageUrl, etName.getText().toString().trim(), etAge.getText().toString().trim(), getGender(), etHobbies.getText().toString().trim(), getColorForGender());
                } else {

                }

            }
        });
    }

    public String getGender(){
        if(rbMale.isChecked()){
            return Constants.PROFILE_MALE;
        } else if (rbFemale.isChecked()){
            return Constants.PROFILE_FEMALE;
        } else return "";
    }

    public String getColorForGender(){
        switch (getGender()){
            case Constants.PROFILE_MALE:
                return Constants.DEFAULT_BACKGROUND_COLOR_BLUE;
            case Constants.PROFILE_FEMALE:
                return Constants.DEFAULT_BACKGROUND_COLOR_GREEN;
            default:
                return Constants.DEFAULT_BACKGROUND_COLOR_BLUE;
        }
    }

    public boolean validateForm(){
        if(!etName.getText().toString().isEmpty()
                && !etAge.getText().toString().isEmpty()
                && !etHobbies.getText().toString().isEmpty()
                && (rbMale.isChecked() || rbFemale.isChecked())){
            return true;
        } else return false;
    }

}