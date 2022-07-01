package com.matrix_maeny.dogs;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private RequestQueue requestQueue;
    private ProgressDialog progressDialog;

    private String imageUrl;
    private String status;
    private ImageView dogIv;
    private final int STORAGE_PERMISSION_CODE = 1;

    private ScaleGestureDetector gestureDetector;
    private float scaleFactor = 1.0f;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        initialize();

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermission();
        }
    }

    private void requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Permission needed")
                    .setMessage("Storage permission needed to save images")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
                        }
                    }).setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).create().show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission denied..! Please enable manually", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initialize() {
        dogIv = findViewById(R.id.dogIv);
        ConstraintLayout constraintLayout = findViewById(R.id.constraintLayout);

        int height = constraintLayout.getMeasuredHeight();
        int width = constraintLayout.getMeasuredWidth();

        int dim = Math.min(height, width);
        dim -= 100;
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(dim, dim);


        dogIv.setLayoutParams(layoutParams);


        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Fetching...");

        requestQueue = Volley.newRequestQueue(MainActivity.this);
        gestureDetector = new ScaleGestureDetector(MainActivity.this, new ScaleListener());
        getDog();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh_data:
                // refresh data;
                getDog();
                break;
            case R.id.save_data:
                //save image;
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestStoragePermission();
                } else {
                    saveImage();

                }
                break;
            case R.id.about_app:
                // go to about activity
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void getDog() {


        progressDialog.show();
        requestQueue.getCache().clear();

        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        String url = "https://dog.ceo/api/breeds/image/random";

        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.GET, url, null, response -> {

            try {
                imageUrl = response.getString("message");
                status = response.getString("status");
                postImage();
            } catch (Exception e) {
                e.printStackTrace();
            }
            progressDialog.dismiss();
            dogIv.setScaleX(1f);
            dogIv.setScaleY(1f);
            scaleFactor = 1f;

        }, error -> {

            progressDialog.dismiss();
            String msg = error.toString();
            if (msg.contains("UnknownHost")) {
                msg = "Error: No Internt";
            }

            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        queue.add(objectRequest);
    }

    private void postImage() {
        if (status.equals("success")) {
            Picasso.get().load(imageUrl).placeholder(R.drawable.placeholder).into(dogIv);
        }
    }

    private void saveImage() {


        int pos = imageUrl.lastIndexOf('/');
        String name = imageUrl.substring(pos + 1);

        try {
            dogIv.setDrawingCacheEnabled(true);
            Bitmap bitmap = dogIv.getDrawingCache();

            File root = Environment.getExternalStorageDirectory();
            File newFile = new File(root.getAbsolutePath() + "/DCIM/Camera/" + name);
            try {
                if (newFile.createNewFile()) {
                    FileOutputStream outputStream = new FileOutputStream(newFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    outputStream.close();
                    Toast.makeText(this, "Image saved", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(this, "Error saving file", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (newFile.mkdir()) {
                    FileOutputStream outputStream = new FileOutputStream(newFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    outputStream.close();
                    Toast.makeText(this, "Image2 saved", Toast.LENGTH_SHORT).show();

                } else
                    Toast.makeText(this, "Error saving file", Toast.LENGTH_SHORT).show();

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        dogIv.destroyDrawingCache();


    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            scaleFactor *= gestureDetector.getScaleFactor();
            scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 10.0f));

            dogIv.setScaleX(scaleFactor);
            dogIv.setScaleY(scaleFactor);
            return true;
        }


    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }
}