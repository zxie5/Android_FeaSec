package com.tutsplus.facedetection;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Paul on 11/4/15.
 */
public class FaceOverlayView extends View {

    private Bitmap mBitmap;
    private SparseArray<Face> mFaces;

    public FaceOverlayView(Context context) {
        this(context, null);
    }

    public FaceOverlayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FaceOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setBitmap( Bitmap bitmap ) {
        mBitmap = bitmap;
        FaceDetector detector = new FaceDetector.Builder( getContext() )
                .setTrackingEnabled(true)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.ACCURATE_MODE)
                .build();

        if (!detector.isOperational()) {
            //Handle contingency
        } else {
            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
            mFaces = detector.detect(frame);
            detector.release();
        }
        logFaceData();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if ((mBitmap != null) && (mFaces != null)) {
            double scale = drawBitmap(canvas);
            drawFaceLandmarks(canvas, scale);
        }
    }

    private double drawBitmap(Canvas canvas) {
        double viewWidth = canvas.getWidth();
        double viewHeight = canvas.getHeight();
        double imageWidth = mBitmap.getWidth();
        double imageHeight = mBitmap.getHeight();
        double scale = Math.min(viewWidth / imageWidth, viewHeight / imageHeight);

        Rect destBounds = new Rect(0, 0, (int)(imageWidth * scale), (int)(imageHeight * scale));
        canvas.drawBitmap(mBitmap, null, destBounds, null);
        return scale;
    }

    private void drawFaceBox(Canvas canvas, double scale) {
        //This should be defined as a member variable rather than
        //being created on each onDraw request, but left here for
        //emphasis.
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        float left = 0;
        float top = 0;
        float right = 0;
        float bottom = 0;

        for( int i = 0; i < mFaces.size(); i++ ) {
            Face face = mFaces.valueAt(i);

            left = (float) ( face.getPosition().x * scale );
            top = (float) ( face.getPosition().y * scale );
            right = (float) scale * ( face.getPosition().x + face.getWidth() );
            bottom = (float) scale * ( face.getPosition().y + face.getHeight() );

            canvas.drawRect( left, top, right, bottom, paint );
        }
    }

    private void drawFaceLandmarks( Canvas canvas, double scale ) {
        Paint paint = new Paint();
        paint.setColor( Color.GREEN );
        paint.setStyle( Paint.Style.STROKE );
        paint.setStrokeWidth( 5 );

        for( int i = 0; i < mFaces.size(); i++ ) {
            Face face = mFaces.valueAt(i);

            for ( Landmark landmark : face.getLandmarks() ) {
                int cx = (int) ( landmark.getPosition().x * scale );
                int cy = (int) ( landmark.getPosition().y * scale );

                canvas.drawCircle( cx, cy, 10, paint );
            }

        }
    }

    /**
     *
     * The function that can get the landmarks of the Faces
     * and return the calculated landmarks in a single ArrayList
     *
     * @return ArrayList<ArrayList<ArrayList<Integer>>> Faces  [x,y][face#][landmark#]
     */
    public ArrayList<ArrayList<ArrayList<Integer>>> calculateLandmarks() {
    ArrayList<ArrayList<ArrayList<Integer>>> Faces = new ArrayList<ArrayList<ArrayList<Integer>>>();
    ArrayList<ArrayList<Integer>> FacesX = new ArrayList<ArrayList<Integer>>();
    ArrayList<ArrayList<Integer>> FacesY = new ArrayList<ArrayList<Integer>>();
    for (int i = 0; i < mFaces.size(); i++) {
        Face face = mFaces.valueAt(i);
        int count = 0;
        ArrayList<Integer> landmarksX = new ArrayList<Integer>();
        ArrayList<Integer> landmarksY = new ArrayList<Integer>();
        for (Landmark landmark : face.getLandmarks()) {
            int cx = (int) (landmark.getPosition().x);
            int cy = (int) (landmark.getPosition().y);

            landmarksX.add(cx);
            landmarksY.add(cy);
        }
        FacesX.add(landmarksX);
        FacesY.add(landmarksY);

    }
    Faces.add(FacesX);
    Faces.add(FacesY);
    //saveData(Faces);
    //double [] validDistRatio = calculateSignature(Faces);

    return Faces;
}


    private void logFaceData() {
        float smilingProbability;
        float leftEyeOpenProbability;
        float rightEyeOpenProbability;
        float eulerY;
        float eulerZ;
        for( int i = 0; i < mFaces.size(); i++ ) {
            Face face = mFaces.valueAt(i);

            smilingProbability = face.getIsSmilingProbability();
            leftEyeOpenProbability = face.getIsLeftEyeOpenProbability();
            rightEyeOpenProbability = face.getIsRightEyeOpenProbability();
            eulerY = face.getEulerY();
            eulerZ = face.getEulerZ();

            Log.e( "Tuts+ Face Detection", "Smiling: " + smilingProbability );
            Log.e( "Tuts+ Face Detection", "Left eye open: " + leftEyeOpenProbability );
            Log.e( "Tuts+ Face Detection", "Right eye open: " + rightEyeOpenProbability );
            Log.e( "Tuts+ Face Detection", "Euler Y: " + eulerY );
            Log.e( "Tuts+ Face Detection", "Euler Z: " + eulerZ );
        }
    }

    public boolean saveData(Context context, double [] dataToSave){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        JSONArray arr = new JSONArray(Arrays.asList(dataToSave));
        return prefs.edit().putString("SavedData",arr.toString()).commit();
    }

    public double[] loadData(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        JSONArray resultJson;

        String savedData = prefs.getString("SavedData","No data stored");
        double [] list;

        try {
            resultJson = new JSONArray(savedData);
            resultJson = (JSONArray) resultJson.get(0);

            if (resultJson != null) {
                int len = resultJson.length();
                list = new double[len];
                for (int i=0;i<len;i++){
                    list[i]=resultJson.getDouble(i);
                }
            } else {
                return null;
            }

        }
        catch (Exception e){
            return null;
        }

        return list;
    }

    public double calcDistance(int a,int b){
        return Math.hypot(a,b);
    }

    public double[] calculateSignature(ArrayList<ArrayList<ArrayList<Integer>>> Faces){

        for (int i = 0; i < Faces.get(0).size(); i++) {
            ArrayList<Integer> landmarksX = Faces.get(0).get(i);
            ArrayList<Integer> landmarksY = Faces.get(1).get(i);
            //int count = 0;

            int numberOfPoints = landmarksX.size();

            int validDistSize = numberOfPoints*(numberOfPoints-1)/2;
            double [] validDist = new double[validDistSize];

            int validDistCount = 0;
            for (int j = 0; j<numberOfPoints;j++) {//First point
                int firstX = landmarksX.get(j);
                int firstY = landmarksY.get(j);
                for(int k = j+1; k<numberOfPoints;k++) {//Second point
                    int secondX = landmarksX.get(k);
                    int secondY = landmarksY.get(k);
                    double currDist = calcDistance(secondX-firstX,secondY-firstY);
                    validDist[validDistCount] = currDist;
                    validDistCount++;
                }
            }


            double [] validDistRatio = new double[validDistSize*(validDistSize-1)/2];
            validDistCount = 0;
            for (int j = 0; j<validDistSize;j++){
                for(int k = j+1; k<validDistSize;k++) {
                    validDistRatio[validDistCount] = validDist[j]/validDist[k];
                    validDistCount++;
                }
            }

            return validDistRatio;
        }
        return null;
    }
}
