package com.tutsplus.facedetection;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Paul on 11/4/15.
 */
public class MainActivity extends AppCompatActivity {

    private FaceOverlayView mFaceOverlayView;
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    final Context mContext = this;
    final int CAMERA_PIC_REQUEST = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        setContentView(R.layout.welcome);
        final Button takePicButton = (Button) findViewById(R.id.takePic);
        takePicButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                String path = Environment.getExternalStorageDirectory().getAbsolutePath();
                path += "/myPicture.jpg";
                File file = new File( path );
                Uri imageFileUri= Uri.fromFile(file);
                cameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageFileUri);
                // request code
                startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
            }
        });
    }


    protected void onActivityResult(int requestCode, final int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_PIC_REQUEST) {
            if (resultCode == RESULT_OK) {
                // Image captured and saved to fileUri specified in the Intent


                String photoPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                photoPath += "/myPicture.jpg";
                BitmapFactory.Options options = new BitmapFactory.Options();

                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bitmap = BitmapFactory.decodeFile(photoPath, options);
                ExifInterface exif = null;
                try {
                    exif = new ExifInterface(photoPath);
                }
                catch (Exception e){

                }
                String orientString = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
                int orientation = orientString != null ? Integer.parseInt(orientString) :  ExifInterface.ORIENTATION_NORMAL;

                int rotationAngle = 0;
                if (orientation == ExifInterface.ORIENTATION_ROTATE_90) rotationAngle = 90;
                if (orientation == ExifInterface.ORIENTATION_ROTATE_180) rotationAngle = 180;
                if (orientation == ExifInterface.ORIENTATION_ROTATE_270) rotationAngle = 270;

                Matrix matrix = new Matrix();
                matrix.setRotate(rotationAngle, (float) bitmap.getWidth() / 2, (float) bitmap.getHeight() / 2);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, options.outWidth, options.outHeight, matrix, true);
                final Bitmap bitmapToSave = bitmap;



                //Start calculation
                setContentView(R.layout.activity_main);
                mFaceOverlayView = (FaceOverlayView) findViewById( R.id.face_overlay );

                //InputStream stream = getResources().openRawResource( R.raw.face );
                //Bitmap bitmap = BitmapFactory.decodeStream(stream);

                mFaceOverlayView.setBitmap(bitmap);
                mFaceOverlayView.calculateLandmarks();
                final ArrayList<ArrayList<ArrayList<Integer>>> faces = mFaceOverlayView.calculateLandmarks();
                final ArrayList<ArrayList<Integer>> landX = faces.get(0);
                final ArrayList<ArrayList<Integer>> landY = faces.get(1);
                final double [] validDistRatio = mFaceOverlayView.calculateSignature(faces);



                final Button showButton = (Button) findViewById(R.id.show_result);
                showButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        // Perform action on click
                        setContentView(R.layout.result);
                        final Button exitButton = (Button) findViewById(R.id.exit);
                        exitButton.setOnClickListener(new View.OnClickListener(){
                            public void onClick(View v) {
                                finish();
                                System.exit(0);
                            }
                        });

                        final Button saveButton = (Button) findViewById(R.id.saveButton);
                        saveButton.setOnClickListener(new View.OnClickListener(){
                            public void onClick(View v) {
                                mFaceOverlayView.saveData(mContext, validDistRatio);
                                TextView resultView = (TextView) findViewById(R.id.result_figure);
                                resultView.setText("Saving complete");

                                //save the image taken
                                FileOutputStream out = null;
                                try {
                                    String photoPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                                    photoPath += "/saved_face.jpg";
                                    out = new FileOutputStream(photoPath);
                                    bitmapToSave.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                                    // PNG is a lossless format, the compression factor (100) is ignored
                                } catch (Exception e) {
                                    e.printStackTrace();
                                } finally {
                                    try {
                                        if (out != null) {
                                            out.close();
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }




                            }
                        });

                        final Button validateButton = (Button) findViewById(R.id.validate);
                        validateButton.setOnClickListener(new View.OnClickListener(){
                            public void onClick(View v) {
                                double [] storedData = mFaceOverlayView.loadData(mContext);
                                TextView resultView = (TextView) findViewById(R.id.result_figure);
                                if (storedData == null){
                                    resultView.setText("no data");
                                } else {

                                    double dist = 0;
                                    //ArrayList<Double> distCheck = new ArrayList<Double>();
                                    for (int i = 0; i < storedData.length; i++) {
                                        dist = dist + (storedData[i] - validDistRatio[i]) * (storedData[i] - validDistRatio[i]);
                                    }
                                    resultView.setText("Dist" + dist);

                                    setContentView(R.layout.validate);
                                    ImageView first_pic = (ImageView) findViewById(R.id.first_pic);
                                    ImageView second_pic = (ImageView) findViewById(R.id.second_pic);
                                    TextView validate_result = (TextView) findViewById(R.id.result);
                                    TextView validate_result_text = (TextView) findViewById(R.id.result_text);
                                    String comparisonResult = "";
                                    if (dist <= 5){
                                        comparisonResult = "Must be the same person";
                                        validate_result_text.setTextColor(Color.GREEN);
                                    } else if (dist<=10){
                                        comparisonResult = "Look similar \n maybe the same person";
                                        validate_result_text.setTextColor(Color.YELLOW);
                                    } else {
                                        comparisonResult = "Probably Not the same person";
                                        validate_result_text.setTextColor(Color.RED);
                                    }
                                    validate_result.setText("Feature difference calculated:" + dist);
                                    validate_result_text.setText(comparisonResult);
                                    first_pic.setImageBitmap(bitmapToSave);

                                    String photoPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                                    photoPath += "/saved_face.jpg";
                                    BitmapFactory.Options options = new BitmapFactory.Options();

                                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                                    Bitmap bitmap = BitmapFactory.decodeFile(photoPath, options);
                                    second_pic.setImageBitmap(bitmap);
                                    final Button retakeButton = (Button) findViewById(R.id.retake_pic);
                                    retakeButton.setOnClickListener(new View.OnClickListener() {
                                        public void onClick(View v) {
                                            setContentView(R.layout.welcome);
                                            final Button takePicButton = (Button) findViewById(R.id.takePic);
                                            takePicButton.setOnClickListener(new View.OnClickListener(){
                                                public void onClick(View v) {
                                                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                                                    String path = Environment.getExternalStorageDirectory().getAbsolutePath();
                                                    path += "/myPicture.jpg";
                                                    File file = new File( path );
                                                    Uri imageFileUri= Uri.fromFile(file);
                                                    cameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageFileUri);
                                                    // request code
                                                    startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
                                                }
                                            });

                                        }
                                    });
                                }
                            }
                        });


/**
                        TextView resultView = (TextView) findViewById(R.id.result_figure);

                        for (int i = 0; i < landX.size();i++) {
                            resultView.append("Face"+i+":\n");
                            for (int j = 0; j < landX.get(i).size(); j++) {
                                resultView.append("     "+landX.get(i).get(j) + "," + landY.get(i).get(j)+"\n");
                            }
                        }
                        resultView.append("Ratio:\n");
                        for (int i = 0; i < validDistRatio.length;i++) {
                            resultView.append(validDistRatio[i]+"\n");

                        }
**/


                    }
                });

            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the image capture
            } else {
                // Image capture failed, advise user
            }
        }
    }


}
