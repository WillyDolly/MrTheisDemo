package com.popland.pop.sum;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
//remaining problems: scale for faster recognition, picture>preview, trained data
public class MainActivity extends AppCompatActivity implements View.OnClickListener{
CameraPreview cameraPreview;
FrameLayout frameLayout;
FocusBox focusBox;
TextView tv;
ImageView iv;
ImageButton ibShoot;
    Camera camera;
String dataPath ="";
String lang = "eng";
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length>0){
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED)
                createCamera();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        frameLayout = (FrameLayout)findViewById(R.id.frameLayout);
        focusBox = (FocusBox) findViewById(R.id.focusBox);
        iv = (ImageView)findViewById(R.id.iv);
        tv = (TextView) findViewById(R.id.result);
        ibShoot = (ImageButton)findViewById(R.id.ibShoot);
        ibShoot.setOnClickListener(this);

        dataPath = getFilesDir()+"/tessract/";//internal storage as same with openOutFile()
        checkFile(new File(dataPath+"tessdata/"));
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},100);
        else
            createCamera();
    }

    public void createCamera(){
        try {
            camera = Camera.open();
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(0,cameraInfo);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            Toast.makeText(MainActivity.this,rotation +  "", Toast.LENGTH_SHORT).show();
            int degree = 0;
            switch(rotation){
                case Surface.ROTATION_0: degree = 0; break;
                case Surface.ROTATION_90: degree = 90; break;
                case Surface.ROTATION_180: degree = 180; break;
                case Surface.ROTATION_270: degree = 270; break;
            }
            camera.setDisplayOrientation((cameraInfo.orientation - degree +360)%360);
            Camera.Parameters parameters = camera.getParameters();
            //inspect focus mode on different devices
            if(parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){//#solution "tapToFocus": autoFocus(AutoFocusCallback)
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                camera.setParameters(parameters);
            }
            cameraPreview = new CameraPreview(this,camera);
            frameLayout.addView(cameraPreview,0);
        }catch(Exception ex) {
            Toast.makeText(MainActivity.this, "BACK CAMERA MAY BE IN USE", Toast.LENGTH_SHORT).show();
        }
    }

    public void checkFile(File dir){
        if(!dir.exists() && dir.mkdirs())//if file not exist And folder "tessdata" created
            copyFile();
    }

    //https://github.com/tesseract-ocr/tessdata/blob/3.04.00/eng.traineddata
    public void copyFile(){
            try {
                InputStream is = getAssets().open("tessdata/"+lang+".traineddata");
                OutputStream os = new FileOutputStream(dataPath+"tessdata/eng.traineddata");
                byte[] bytes = new byte[1024];
                int len;//amout of bytes read successfully
                while((len=is.read(bytes))!=-1){//end of tream return -1
                    os.write(bytes,0,len);
                }
                os.flush();
                os.close();
                is.close();
                Toast.makeText(MainActivity.this,"COPY OK", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(MainActivity.this,"COPY ERROR", Toast.LENGTH_SHORT).show();
            }
    }

    @Override
    public void onClick(View view) {
       switch(view.getId()){
           case R.id.ibShoot:
               camera.takePicture(null, null, pictureCallback);
               ibShoot.setEnabled(false);
               break;
       }
    }

    Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] bytes, Camera camera) {
            Bitmap image = cropBitmap(bytes,focusBox.getBox());
            //iv.setImageBitmap(image);
            //image.recycle();

            TessBaseAPI tessBaseAPI = new TessBaseAPI();
            tessBaseAPI.setDebug(true);
            tessBaseAPI.init(dataPath,lang);//dataPath ends with / and parent folder of tessdata
            //tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST,"0123456789");
            //tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST,"@#$%^&*_+=[]}{" +
            //        "'\\|~`/<>");
            tessBaseAPI.setImage(image);//should be right after init or IllegalStateException because of tessBaseAPI.end()
            String extractedText = tessBaseAPI.getUTF8Text();
//            if ( lang.equalsIgnoreCase("eng") ) {
//                extractedText = extractedText.replaceAll("[^a-zA-Z0-9]+", " ");
//            }

            extractedText = extractedText.trim();

            tv.setText(extractedText);
            tessBaseAPI.end();
            image.recycle();
            camera.startPreview();
            ibShoot.setEnabled(true);
        }
    };

    public Bitmap cropBitmap(byte[] bytes,Rect box){
        //frame scale up to bitmap. To find corresponding coordinates , size of frame's box on bitmap, account for Wf/Wb , Hf/Hb
        int Swidth = frameLayout.getWidth();//2392X1356
        int Sheight = frameLayout.getHeight();

        int Bl = box.left;
        int Bt = box.top;
        int Bw = box.width();
        int Bh = box.height();

        //ratio box/screen
        float BSl = (float)(Bl* Math.pow(Swidth,-1));// int/int = int ; int/double = double
        float BSw = (float)(Bw* Math.pow(Swidth,-1));
        float BSt = (float)(Bt* Math.pow(Sheight,-1));
        float BSh = (float)(Bh* Math.pow(Sheight,-1));
        //full bitmap
        Bitmap bmp = BitmapFactory.decodeByteArray(bytes,0,bytes.length);//4160X3120
        int Iw = bmp.getWidth();
        int Ih = bmp.getHeight();

        //crop bitmap
        int xPixel = (int)(BSl*Iw);
        int yPixel = (int)(BSt*Ih);
        int pixelCol = (int)(BSw*Iw);
        int pixelRow = (int)(BSh*Ih);

        //if box limited within frameLayout, xPixel+pixelCol < bmp.getWidth
        Bitmap result = Bitmap.createBitmap(bmp,xPixel,yPixel,pixelCol,pixelRow);
        bmp.recycle();
        try {
            result = result.copy(Bitmap.Config.ARGB_8888, true);
            Log.i("RRR",result.getWidth()+"-"+result.getHeight());
            return result;
        }catch(NullPointerException ex){
            return result;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(camera!=null){
            camera.release();
            camera = null;
        }
    }

}
//Samsung S3 and Note2 works well with autofocusCallback and FOCUS_MODE_CONTINUOUS_VIDEO and FOCUS_MODE_AUTO
//Samsung S4 do not work with FOCUS_MODE_AUTO.
//LG G3 do not work with autofocusCallback properly in this phone callback returns only if image had been changed during focus.
//Samsung S5 do not call autofocusCallback if FOCUS_MODE_CONTINUOUS_VIDEO been choosed.
//Samsung S8 autofocus mode parameters should be set differently.


