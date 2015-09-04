package pl.waw.kurpiel.crio;

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import android.util.Log;
import android.widget.Toast;
import java.util.List;



public class MainActivity extends Activity implements OnClickListener {

    Button btnTackPic;
    TextView tvHasCamera, tvHasCameraApp;
    ImageView ivThumbnailPhoto;
    Bitmap bitMap;
    static int TAKE_PICTURE = 1;
    String upLoadServerUri = "http://crio.kurpiel.waw.pl/ul/ul.php";
    ProgressDialog dialog = null;
    TextView messageText;
    Uri photoPath = null;
    String path = null;
    int serverResponseCode = 0;
    EditText editTextEmail = null;
    String osv = System.getProperty("os.version"); // OS version
    String sdk = android.os.Build.VERSION.SDK;      // API Level
    String dvc = android.os.Build.DEVICE;           // Device
    String mdl = android.os.Build.MODEL;            // Model
    String prd = android.os.Build.PRODUCT;          // Product
    String brn = android.os.Build.BRAND;
    String ver = Build.VERSION.RELEASE;
    ProgressDialog progress = null;

    @Override
    public void onPause() {
        super.onPause();

        if ((progress != null) && progress.isShowing())
            progress.dismiss();
        progress = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Get reference to views
        //tvHasCamera = (TextView) findViewById(R.id.tvHasCamera);
        //tvHasCameraApp = (TextView) findViewById(R.id.tvHasCameraApp);
        messageText = (TextView) findViewById(R.id.messageText);
        btnTackPic = (Button) findViewById(R.id.btnTakePic);
        ivThumbnailPhoto = (ImageView) findViewById(R.id.ivThumbnailPhoto);
        editTextEmail = (EditText) findViewById(R.id.EditTextEmail);


        try{
            String name = UserEmailFetcher.getEmail(this).toString();
            editTextEmail.setText(name, TextView.BufferType.EDITABLE);
        }
        catch (VerifyError e)
        { // Happens if the AccountManager is not available (e.g. 1.x)
            //
        }

/*
        // Does your device have a camera?
        if(hasCamera()){
            tvHasCamera.setBackgroundColor(0xFF00CC00);
            tvHasCamera.setText("+");
        }

        // Do you have Camera Apps?
        if(hasDefualtCameraApp(MediaStore.ACTION_IMAGE_CAPTURE)){
            tvHasCameraApp.setBackgroundColor(0xFF00CC00);
            tvHasCameraApp.setText("+");
        }
*/
        // add onclick listener to the button
        btnTackPic.setOnClickListener(this);

    }

    // on button "btnTackPic" is clicked
    @Override
    public void onClick(View view) {

        // create intent with ACTION_IMAGE_CAPTURE action
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // this part to save captured image on provided path
        final File file = new File(Environment.getExternalStorageDirectory(), "skin.jpg");
        photoPath = Uri.fromFile(file);
        //intent.putExtra(MediaStore.EXTRA_OUTPUT, photoPath);
        path = MediaStore.EXTRA_OUTPUT + photoPath;


        // start camera activity
        startActivityForResult(intent, TAKE_PICTURE);



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if (requestCode == TAKE_PICTURE && resultCode== RESULT_OK && intent != null && editTextEmail.getText().toString().trim().length() > 0){
            // get bundle
            Bundle extras = intent.getExtras();

            // get bitmap
            bitMap = (Bitmap) extras.get("data");
            ivThumbnailPhoto.setMaxHeight(500);
            ivThumbnailPhoto.setMaxHeight(300);
            ivThumbnailPhoto.setImageBitmap(bitMap);


            progress = ProgressDialog.show(MainActivity.this, "", "Uploading file...", true);

            new Thread(new Runnable() {
                public void run() {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (editTextEmail.getText().toString().trim().length() > 0) {
                                //Toast.makeText(MainActivity.this, "Uploading started.....", Toast.LENGTH_SHORT).show();
                                //messageText.setText("Uploading started.....");

                            }
                        }
                    });

                    uploadFile(path);

                }
            }).start();


        }
    }

    // method to check if you have a Camera
    private boolean hasCamera(){
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    // method to check you have Camera Apps
    private boolean hasDefualtCameraApp(String action){
        final PackageManager packageManager = getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        return list.size() > 0;

    }

    public int uploadFile(String sourcefile) {

        String[] projection = new String[]{
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Images.ImageColumns.DATE_TAKEN,
                MediaStore.Images.ImageColumns.MIME_TYPE
        };
        final Cursor cursor = getContentResolver()
                .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                        null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");

        cursor.moveToFirst();

            String imageLocation = cursor.getString(1);

            File sourceFile = new File(imageLocation);


        String fileName =  editTextEmail.getText() + ".jpg";


        //fileName="DSC_0016.JPG";

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        //File sourceFile = new File(sourcefile);

        if (!sourceFile.isFile()) {

            if ((progress != null) && progress.isShowing()) {
                progress.dismiss();
            }


            Log.e("uploadFile", "Source File not exist :"
                    +path);

            runOnUiThread(new Runnable() {
                public void run() {
                    messageText.setText(path);
                }
            });

            return 0;

        }
        else
        {
            try {

                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(upLoadServerUri);

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", fileName);
                conn.setRequestProperty("osv", osv);
                conn.setRequestProperty("sdk", sdk);
                conn.setRequestProperty("dvc", dvc);
                conn.setRequestProperty("mdl", mdl);
                conn.setRequestProperty("prd", prd);
                conn.setRequestProperty("brn", brn);
                conn.setRequestProperty("ver", ver);


                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\"; filename=\"" + fileName + "\"" + lineEnd);

                        dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {

                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i("uploadFile", "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);

                if(serverResponseCode == 200){

                    runOnUiThread(new Runnable() {
                        public void run() {
                            String msg = null;
                            if (editTextEmail.getText().toString().trim().length() > 0) {
                                Toast.makeText(MainActivity.this, "File Upload Completed.", Toast.LENGTH_SHORT).show();
                                //msg = "File Upload Completed.\n\nPlease wait for feedback.";
                                msg = "Please wait for feedback.";
                            } else {
                                Toast.makeText(MainActivity.this, "Email please.", Toast.LENGTH_SHORT).show();
                                //msg = "Email please.";

                            }
                            messageText.setText(msg);
                            if (editTextEmail.getText().toString().trim().length() > 0) {
                                //Toast.makeText(MainActivity.this, "File Upload Complete.", Toast.LENGTH_SHORT).show();
                            }

                        }
                    });

                }

                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {

                if ((progress != null) && progress.isShowing()) {
                    progress.dismiss();
                }

                ex.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        messageText.setText("MalformedURLException Exception : check script url.");
                        Toast.makeText(MainActivity.this, "MalformedURLException",
                                Toast.LENGTH_SHORT).show();
                    }
                });

                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {

                if ((progress != null) && progress.isShowing()) {
                    progress.dismiss();
                }

                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        messageText.setText("Got Exception : see logcat ");
                        Toast.makeText(MainActivity.this, "Got Exception : see logcat ",
                                Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e("Upload file to server", "Exception : "
                        + e.getMessage(), e);
            }
            if ((progress != null) && progress.isShowing()) {
                progress.dismiss();
            }

            return serverResponseCode;

        } // End else block
    }

}