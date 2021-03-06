package terry.com.greyleveltoconcentrationcamera;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    String[] PERMISSIONS = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.CAMERA",
    };

    static {
        System.loadLibrary("native-lib");
    }

    private static int REQUEST_ORIGINAL = 0;// 请求图片信号标识
    private static int CROP_PHOTO = 1;

    Uri uri;
    Button button;
    ImageView imageView;
    TextView greyLevelTextView;
    TextView concentrationTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        for (String per : PERMISSIONS) {
            if (checkSelfPermission(per) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(PERMISSIONS, 1);
            }
        }
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();

        imageView = findViewById(R.id.image);
        greyLevelTextView = findViewById(R.id.greylevel);
        concentrationTextView = findViewById(R.id.concentration);
        button = findViewById(R.id.capturbtn);

        uri = Uri.fromFile(StoreFileUtil.tempFile());

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v == button) {
                    //调用系统相机的Intent
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    //将照片文件的位置传入intent
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                    //发出intent启动系统相机
                    startActivityForResult(intent, CROP_PHOTO);
                }
            }
        });
    }

    //系统相机返回时的回调方法
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == CROP_PHOTO) {
                //从临时照片文件的位置加载照片
                Bitmap bitmap = BitmapFactory.decodeFile(StoreFileUtil.tempFile().getAbsolutePath());
                //计算当前bitmap高度和宽度
                int w = bitmap.getWidth(), h = bitmap.getHeight();
                /**此处是完成拍照裁剪代码，结果值不合理**/
                /*int rectSize=300;
                Bitmap cropPic=Bitmap.createBitmap(bitmap,w/2-rectSize,h/2-rectSize,2*rectSize,2*rectSize,new Matrix(),true);*/
                //创建像素数组
                int[] pix = new int[w * h];
                //得到像素值
                bitmap.getPixels(pix, 0, w, 0, 0, w, h);
                //得到圆形图像
                int[] grayResultPixes = getCirclePicture(pix, w, h);
                //创建bitmap临时变量
                Bitmap  circlePic= Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
                //将结果像素存入
                circlePic.setPixels(grayResultPixes, 0, w, 0, 0, w, h);
                //将图片设置给ImageView显示
                imageView.setImageBitmap(circlePic);
                //计算灰度值
                double greyLevl = getBitmapGray(bitmap);
                double concentration = -145.7491 + 1.27 * greyLevl;
                if (concentration < 0) {
                    concentration = 0;
                }
                if (concentration > 100) {
                    concentration = 100;
                }
                greyLevelTextView.setText(String.format("%.2f", greyLevl));
                concentrationTextView.setText(String.format("%.2f", concentration));
            }
        }
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */

    public native double getBitmapGray(Bitmap bitmap);//计算灰度值的方法
    public native int[] getCirclePicture(int [] buf,int w,int h);//尝试方法
}

