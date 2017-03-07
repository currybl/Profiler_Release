package com.brandoncurry.profiler.ui.landing;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Toast;

import com.brandoncurry.profiler.R;
import com.brandoncurry.profiler.global.Constants;
import com.brandoncurry.profiler.global.ProfilerApplication;
import com.brandoncurry.profiler.models.Profile;
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

public class RegisterActivity extends AppCompatActivity {

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        mDatabase = FirebaseDatabase.getInstance().getReference();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                } else {
                }
            }
        };


        profileImage = (ImageView) findViewById(R.id.ivProfilePhoto);
        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Constants.SDK_VERSION > Build.VERSION_CODES.LOLLIPOP_MR1) {
                    if (!ProfilerApplication.checkIfAlreadyhavePermission(RegisterActivity.this)) {
                        ProfilerApplication.requestPhotoPermissions(RegisterActivity.this);
                    }
                }
                ActivityCompat.requestPermissions(RegisterActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                Intent i = new Intent();
                i.setType("image/*");
                i.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(i, getResources().getString(R.string.select_photo)),SELECT_PICTURE );
            }
        });
        profileImage.setDrawingCacheEnabled(true);
        profileImage.buildDrawingCache();

        etName = (EditText) findViewById(R.id.etUserName);
        etEmail = (EditText) findViewById(R.id.etEmail);
        etPassword = (EditText) findViewById(R.id.etPassword);
        etAge = (EditText) findViewById(R.id.etAge);
        etHobbies = (EditText) findViewById(R.id.etHobbies);
        rbMale = (RadioButton) findViewById(R.id.rbMale);
        rbFemale = (RadioButton) findViewById(R.id.rbFemale);
        btnRegister = (Button) findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(validateForm()){
                    uploadImage();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
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

    public void registerUser(String email, String password){
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                createNewProfile(user.getUid(), imageUrl, etName.getText().toString().trim(), etAge.getText().toString().trim(), getGender(), etHobbies.getText().toString().trim(), getColorForGender());
                                Intent main = new Intent(RegisterActivity.this, MainActivity.class);
                                startActivity(main);
                            }
                        }
                    }
                });

    }

    protected  void onActivityResult(int requestCode, int resultCode, Intent data){
        if(resultCode==RESULT_OK){
            if(requestCode==SELECT_PICTURE){
                Uri selectedImageUri = data.getData();
                if (null != selectedImageUri) {
                    String path = getPathFromURI(selectedImageUri);
                    profileImage.setImageURI(selectedImageUri);

                }
            }
        }
    }

    private String getPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }

    private void createNewProfile(String userId, String imageUrl, String name, String age, String gender, String hobbies, String bgColor) {
        Profile profile = new Profile(UUID.randomUUID().toString(), userId, imageUrl, name, age, gender, hobbies, bgColor);
        mDatabase.child("Profile").child(userId).setValue(profile);
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
                registerUser(etEmail.getText().toString().trim(), etPassword.getText().toString().trim());
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
                && etPassword.getText().toString().length() >=6
                && !etAge.getText().toString().isEmpty()
                && !etHobbies.getText().toString().isEmpty()
                && (rbMale.isChecked() || rbFemale.isChecked())){
            return true;
        } else {
            if(etPassword.getText().toString().length() <6)
                Toast.makeText(RegisterActivity.this, getResources().getString(R.string.invalid_password), Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}
