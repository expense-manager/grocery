package com.quarkworks.apartmentgroceries.profile;

import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.quarkworks.apartmentgroceries.MyApplication;
import com.quarkworks.apartmentgroceries.R;
import com.quarkworks.apartmentgroceries.service.DataStore;
import com.quarkworks.apartmentgroceries.service.PopupDialog;
import com.quarkworks.apartmentgroceries.service.SyncUser;
import com.quarkworks.apartmentgroceries.service.Utilities;
import com.quarkworks.apartmentgroceries.service.models.RUser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import bolts.Continuation;
import bolts.Task;

public class ProfileDetailActivity extends AppCompatActivity {
    private static final String TAG = ProfileDetailActivity.class.getSimpleName();

    private static final int SELECT_PICTURE_REQUEST_CODE = 1;
    private static final String USER_ID = "userId";
    private String userId;
    private Uri outputFileUri;
    private RUser rUser;

    /*
     * References
     */
    private TextView emailTextView;
    private TextView phoneTextView;
    private ImageView profileImageView;

    public static void newIntent(Context context, String userId) {
        Intent intent = new Intent(context, ProfileDetailActivity.class);
        intent.putExtra(USER_ID, userId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_detail_activity);


        userId = getIntent().getStringExtra(USER_ID);
        rUser = DataStore.getInstance().getRealm().where(RUser.class)
                .equalTo(USER_ID, userId).findFirst();

        /*
         * Get view references
         */
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar_id);
        TextView titleTextView = (TextView) toolbar.findViewById(R.id.toolbar_title_id);
        emailTextView = (TextView) findViewById(R.id.user_detail_email_text_view_id);
        phoneTextView = (TextView) findViewById(R.id.profile_detail_phone_text_view_id);
        TextView usernameTextView = (TextView) findViewById(R.id.profile_detail_username_text_view_id);
        profileImageView = (ImageView) findViewById(R.id.profile_detail_profile_image_view_id);

        /*
         * Set view data
         */
        titleTextView.setText(getString(R.string.title_activity_profile_detail));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        emailTextView.setText(rUser.getEmail());
        phoneTextView.setText(rUser.getPhone());
        usernameTextView.setText(rUser.getUsername());

        Glide.with(this)
                .load(rUser.getUrl())
                .placeholder(R.drawable.ic_launcher)
                .centerCrop()
                .crossFade()
                .into(profileImageView);

        /*
            Set view OnClickListener
         */

        if (isAuthorizedUser(userId)) {
            profileImageView.setOnClickListener(profileImageViewOnClick);
            emailTextView.setOnClickListener(emailOnClick);
            phoneTextView.setOnClickListener(phoneOnClick);
        }
    }

    private View.OnClickListener profileImageViewOnClick =  new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            openImageIntent();
        }
    };

    private View.OnClickListener emailOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            FragmentManager manager = getFragmentManager();
            PopupDialog editPhoneDialog = PopupDialog.newInstance(getString(R.string.email),
                    RUser.JsonKeys.EMAIL, emailTextView.getText().toString());
            editPhoneDialog.show(manager, getString(R.string.email));
        }
    };

    private View.OnClickListener phoneOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            FragmentManager manager = getFragmentManager();
            PopupDialog editPhoneDialog = PopupDialog.newInstance(getString(R.string.phone),
                    RUser.JsonKeys.PHONE, phoneTextView.getText().toString());
            editPhoneDialog.show(manager, getString(R.string.phone));
            editPhoneDialog.setNoticeDialogListener(noticeDialogListener);
        }
    };

    private PopupDialog.NoticeDialogListener noticeDialogListener = new PopupDialog.NoticeDialogListener() {
        @Override
        public void onDialogPositiveClick(final PopupDialog dialog) {
            Continuation<RUser, Void> onUpdateProfileFinished = new Continuation<RUser, Void>() {
                @Override
                public Void then(Task<RUser> task){
                    if (task.isFaulted()) {
                        Exception exception = task.getError();
                        Log.e(TAG, "Error in updateProfile", exception);
                        Toast.makeText(getApplication(), getString(R.string.update_failure), Toast.LENGTH_SHORT).show();
                    }

                    emailTextView.setText(task.getResult().getEmail());
                    phoneTextView.setText(task.getResult().getPhone());
                    Toast.makeText(getApplication().getApplicationContext(), getString(R.string.update_success), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();

                    return null;
                }
            };

            if (dialog.task != null) {
                dialog.task.continueWith(onUpdateProfileFinished, Task.UI_THREAD_EXECUTOR);
            }
        }

        @Override
        public void onDialogNegativeClick(PopupDialog dialog) {

        }
    };

    private void openImageIntent() {
        String directoryName = Utilities.dateToString(new Date(), getString(R.string.photo_date_format_string));
        Log.d(TAG, directoryName);
        final File root = new File(Environment.getExternalStorageDirectory() + File.separator + directoryName + File.separator);
        root.mkdirs();
        final File sdImageMainDirectory = new File(root, directoryName);
        outputFileUri = Uri.fromFile(sdImageMainDirectory);

        final List<Intent> cameraIntents = new ArrayList<>();
        final Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = getPackageManager();
        final List<ResolveInfo> listCamera = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCamera) {
            final String packageName = res.activityInfo.packageName;
            final Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(packageName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            cameraIntents.add(intent);
        }

        final Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

        final Intent chooserIntent = Intent.createChooser(galleryIntent, getString(R.string.photo_select_source));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[cameraIntents.size()]));
        startActivityForResult(chooserIntent, SELECT_PICTURE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE_REQUEST_CODE) {
                final boolean isCamera;
                if (data == null) {
                    isCamera = true;
                } else {
                    final String action = data.getAction();
                    isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
                }

                Uri selectedImageUri;
                if (isCamera) {
                    selectedImageUri = outputFileUri;
                } else {
                    selectedImageUri = data.getData();
                }

                try {
                    InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                    try {
                        String photoName = Utilities.dateToString(new Date(), getString(R.string.photo_date_format_string));
                        byte[] inputData = Utilities.getBytesFromInputStream(inputStream);
                        Bitmap bitmap = Utilities.decodeSampledBitmapFromByteArray(inputData, 0, 400, 400);
                        int dimension = Utilities.getCenterCropDimensionForBitmap(bitmap);
                        bitmap = ThumbnailUtils.extractThumbnail(bitmap, dimension, dimension);
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                        byte[] sampledInputData = stream.toByteArray();

                        Continuation<RUser, Void> checkUpdatingPhoto = new Continuation<RUser, Void>() {
                            @Override
                            public Void then(Task<RUser> task) {
                                if (task.isFaulted()) {
                                    Log.e(TAG, "Failed updating photo", task.getError());
                                    Toast.makeText(getApplicationContext(),
                                            getString(R.string.update_failure), Toast.LENGTH_SHORT).show();
                                    return null;
                                }

                                Glide.with(getApplication())
                                        .load(task.getResult().getUrl())
                                        .placeholder(R.drawable.ic_launcher)
                                        .centerCrop()
                                        .crossFade()
                                        .into(profileImageView);

                                Toast.makeText(getApplicationContext(),
                                        getString(R.string.update_success), Toast.LENGTH_SHORT).show();

                                return null;
                            }
                        };

                        SyncUser.updateProfilePhoto(photoName + ".jpg", sampledInputData)
                                .continueWith(checkUpdatingPhoto, Task.UI_THREAD_EXECUTOR);
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading image byte data from uri");
                    }
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "Error file with uri " + selectedImageUri + " not found", e);
                }
            }
        }
    }

    private boolean isAuthorizedUser(String userId) {
        SharedPreferences sharedPreferences =
                MyApplication.getContext().getSharedPreferences(
                        MyApplication.getContext()
                                .getString(R.string.login_or_sign_up_session), 0);
        String loginUserId = sharedPreferences.getString(RUser.JsonKeys.USER_ID, null);

        return userId.equals(loginUserId);
    }
}