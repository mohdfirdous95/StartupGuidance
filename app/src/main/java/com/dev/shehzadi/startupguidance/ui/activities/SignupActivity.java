package com.dev.shehzadi.startupguidance.ui.activities;

import android.Manifest;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.dev.shehzadi.startupguidance.R;
import com.dev.shehzadi.startupguidance.models.UserModel;
import com.dev.shehzadi.startupguidance.ui.fragments.DatePickerFragment;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.dev.shehzadi.startupguidance.utils.Util.isValidEmail;

public class SignupActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private SignupViewHolder holder;

    private UserModel user;
    private String date, newPassword, reNewPassword;

    private Uri filePath;
    private final int PICK_IMAGE_REQUEST = 71;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();
        holder = new SignupViewHolder();
        user = new UserModel();

        DateTimeFormatter dtf = DateTimeFormat.forPattern("dd-MM-yyyy");

        holder.buttonSignup.setOnClickListener(view -> {
            user.setFullName(holder.etFullName.getText().toString().trim());
            user.setGender(holder.rbMale.isChecked() ? "Male" : "Female");
            date = holder.etDOB.getText().toString().trim();
            if(!TextUtils.isEmpty(date)){
                LocalDate localDate = dtf.parseLocalDate(date);
                String d = "" + localDate.getDayOfMonth();
                d = (d.length() == 1) ? "0" + d : d;

                String m = "" + localDate.getMonthOfYear();
                m = (m.length() == 1) ? "0" + m : m;

                String y = "" + localDate.getYear();
                user.setDateOfBirth(d +  m + y);
            }
            user.setEmailId(holder.etEmail.getText().toString().trim());
            user.setPhoneNo(holder.etPhoneNumber.getText().toString().trim());
            newPassword = holder.etNewPassword.getText().toString().trim();
            reNewPassword = holder.etReNewPassword.getText().toString().trim();

            if(validateForm())
                createAccount();
            else
                holder.showError("Fill all required* fields");
        });

        holder.ivAddProfileImage.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null )
        {
            filePath = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                holder.ivAddProfileImage.setImageBitmap(bitmap);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void createAccount(){
        holder.startLoading();

        auth.createUserWithEmailAndPassword(user.getEmailId(), newPassword)
                .addOnCompleteListener(this, task -> {
                    if(!task.isSuccessful()){
                        Log.e("CreateAcc", "Error: " + task.getException());
                    }
                    else {
                        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (firebaseUser != null) {
                            firebaseUser.sendEmailVerification()
                                .addOnCompleteListener(task1 -> {
                                    if(task1.isSuccessful()){
                                        user.setUid(firebaseUser.getUid());
                                        auth.signOut();
                                        uploadDetails();
                                        Intent loginIntent = new Intent(this, LoginActivity.class);
                                        loginIntent.putExtra("accJustCreated", "accJustCreated");
                                        startActivity(loginIntent);
                                        finish();
                                    }
                                    else {
                                        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmailId(), newPassword);

                                        firebaseUser.reauthenticate(credential)
                                            .addOnCompleteListener(task2 -> {
                                                firebaseUser.delete()
                                                    .addOnCompleteListener(task3 -> {
                                                        if(task3.isSuccessful()){
                                                            holder.showError("Some error occurred while creating your account. Please try again.");
                                                        }
                                                    });
                                                });
                                    }
                                });
                        }
                    }
                    holder.stopLoading();
                });
    }

    private void uploadDetails() {
        if(user != null){
            DatabaseReference reference = FirebaseDatabase.getInstance()
                    .getReference("Users/" + user.getUid());

            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    reference.setValue(user);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });


//            if(filePath != null){
//                final ProgressDialog progressDialog = new ProgressDialog(this);
//                progressDialog.setTitle("Uploading your profile image...");
//                progressDialog.show();
//
//                StorageReference ref = FirebaseStorage.getInstance().getReference().child("ProfileImages/"+ userModel.getUid());
//                ref.putFile(filePath)
//                        .addOnSuccessListener(taskSnapshot -> {
//                            progressDialog.dismiss();
//                            Toast.makeText(this, "Profile image uploaded", Toast.LENGTH_SHORT).show();
//                        })
//                        .addOnFailureListener(e -> {
//                            progressDialog.dismiss();
//                            Toast.makeText(this, "Failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
//                        })
//                        .addOnProgressListener(taskSnapshot -> {
//                            double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
//                                    .getTotalByteCount());
//                            progressDialog.setMessage("Uploaded "+(int)progress+"%");
//                        });
//            }
        }
    }

    public boolean validateForm(){
        boolean isValid = true, isPasswordFilled = true;

        if (TextUtils.isEmpty(user.getFullName())){
            holder.etFullName.setError("Full name is required");
            isValid = false;
        }

        if (holder.radioGroupGender.getCheckedRadioButtonId() == -1){
            isValid = false;
        }

        if (TextUtils.isEmpty(date)){
            holder.etDOB.setError("Date of birth is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(user.getEmailId())){
            holder.etEmail.setError("Email ID is required");
            isValid = false;
        }
        else if (isValidEmail(user.getEmailId())){
            holder.etEmail.setError("Email ID is not valid");
            isValid = false;
        }

        if (TextUtils.isEmpty(newPassword)){
            holder.etNewPassword.setError("You need to create a password");
            isValid = false;
            isPasswordFilled = false;
        }

        if (TextUtils.isEmpty(reNewPassword)){
            holder.etReNewPassword.setError("Re-enter your new password");
            isValid = false;
            isPasswordFilled = false;
        }

        if(isPasswordFilled){
            if (newPassword.length() < 8){
                holder.etNewPassword.setError("Password should've at least 8 characters");
                isValid = false;
            }
            else if (!newPassword.equals(reNewPassword)){
                holder.etReNewPassword.setError("Passwords do not match");
                isValid = false;
            }
        }

        return isValid;
    }

    class SignupViewHolder{

        CircleImageView ivAddProfileImage;
        RadioGroup radioGroupGender;
        RadioButton rbMale, rbFemale;
        EditText etFullName, etDOB, etEmail, etPhoneNumber, etNewPassword, etReNewPassword;
        Button buttonSignup;
        FrameLayout frameLayoutProgressBar;
        TextView tvError;

        public SignupViewHolder(){
            ivAddProfileImage = findViewById(R.id.imageView_addProfileImage_signup);
            radioGroupGender = findViewById(R.id.radioGroup_gender_signup);
            rbMale = findViewById(R.id.radioButton_gender_male_signup);
            rbFemale = findViewById(R.id.radioButton_gender_female_signup);
            etFullName = findViewById(R.id.editText_fullName_signup);
            etDOB = findViewById(R.id.editText_dob_signup);
            etEmail = findViewById(R.id.editText_email_signup);
            etPhoneNumber = findViewById(R.id.editText_phoneNumber_signup);
            etNewPassword = findViewById(R.id.editText_newPassword_signup);
            etReNewPassword = findViewById(R.id.editText_reNewPassword_signup);
            buttonSignup = findViewById(R.id.button_signup);
            frameLayoutProgressBar = findViewById(R.id.frameLayout_progressBar_signup);
            tvError = findViewById(R.id.textView_error_signup);

            DialogFragment fragment = new DatePickerFragment();

            etDOB.setOnClickListener(view -> {
                InputMethodManager imm = (InputMethodManager) etDOB.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(etDOB.getWindowToken(), 0);
                fragment.show(getFragmentManager(), "DOB");
            });
        }

        public void showError(String errorMessage){
            tvError.setVisibility(View.VISIBLE);
            tvError.setText(errorMessage);
        }

        public void startLoading(){
            ivAddProfileImage.setEnabled(true);
            rbMale.setEnabled(false);
            rbFemale.setEnabled(false);
            etFullName.setEnabled(false);
            etDOB.setEnabled(false);
            etEmail.setEnabled(false);
            etPhoneNumber.setEnabled(false);
            etNewPassword.setEnabled(false);
            etReNewPassword.setEnabled(false);
            buttonSignup.setEnabled(false);
            frameLayoutProgressBar.setVisibility(View.VISIBLE);
            tvError.setVisibility(View.GONE);
        }

        public void stopLoading(){
            ivAddProfileImage.setEnabled(true);
            rbMale.setEnabled(true);
            rbFemale.setEnabled(true);
            etFullName.setEnabled(true);
            etDOB.setEnabled(true);
            etEmail.setEnabled(true);
            etPhoneNumber.setEnabled(true);
            etNewPassword.setEnabled(true);
            etReNewPassword.setEnabled(true);
            buttonSignup.setEnabled(true);
            frameLayoutProgressBar.setVisibility(View.GONE);
        }
    }
}
