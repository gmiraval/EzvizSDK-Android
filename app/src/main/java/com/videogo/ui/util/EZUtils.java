package com.videogo.ui.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.videogo.EzvizApplication;
import com.videogo.exception.InnerException;
import com.videogo.openapi.bean.EZAlarmInfo;
import com.videogo.openapi.bean.EZCameraInfo;
import com.videogo.openapi.bean.EZDeviceInfo;
import com.videogo.util.LogUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import ezviz.ezopensdk.R;

public class EZUtils {

    private final static String TAG = EZUtils.class.getSimpleName();

    public static void saveCapturePictrue(String filePath, Bitmap bitmap) throws InnerException {
        if (TextUtils.isEmpty(filePath)){
            LogUtil.d("EZUtils","saveCapturePictrue file is null");
            return;
        }
        File filepath = new File(filePath);
        File parent = filepath.getParentFile();
        if (parent == null || !parent.exists() || parent.isFile()) {
            parent.mkdirs();
        }
        FileOutputStream out = null;
        try {
            // 保存原图

            if (!TextUtils.isEmpty(filePath)) {
                out = new FileOutputStream(filepath);
                bitmap.compress(CompressFormat.JPEG, 100, out);
                //out.write(tempBuf, 0, size);
                out.flush();
                out.close();
                out = null;
            }


        } catch (FileNotFoundException e) {
//            throw new InnerException(e.getLocalizedMessage());
            e.printStackTrace();
        } catch (IOException e) {
//            throw new InnerException(e.getLocalizedMessage());
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }


    public static Object getPrivateMethodInvoke(Object instance, /*Class destClass,*/ String methodName,
                                                Class<?>[] parameterClass, Object... args) throws Exception {
        Class<?>[] parameterTypes = null;
        if(args != null) {
            parameterTypes = parameterClass;
        }
        Method method = instance.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(instance, args);
    }

    public static EZCameraInfo getCameraInfoFromDevice(EZDeviceInfo deviceInfo,int camera_index) {
        if (deviceInfo == null) {
            return null;
        }
        if (deviceInfo.getCameraNum() > 0 && deviceInfo.getCameraInfoList() != null && deviceInfo.getCameraInfoList().size() > camera_index) {
            return deviceInfo.getCameraInfoList().get(camera_index);
        }
        return null;
    }

    public static EZDeviceInfo CopyEzDeviceInfoNoCameraAndDetector(EZDeviceInfo deviceInfo) {
        if (deviceInfo == null) {
            return null;
        }
        EZDeviceInfo ezDeviceInfo = new EZDeviceInfo();
        ezDeviceInfo.setDeviceName(deviceInfo.getDeviceName());
        ezDeviceInfo.setIsEncrypt(deviceInfo.getIsEncrypt());
        ezDeviceInfo.setCameraNum(deviceInfo.getCameraNum());
        ezDeviceInfo.setDefence(deviceInfo.getDefence());
        return deviceInfo;
    }

    private static boolean isEncrypt(String url){
        int ret = 0;
        try {
            String keyOfTargetValue = "isEncrypted";
            String strIsEncrypted = Uri.parse(url).getQueryParameter(keyOfTargetValue);
            if (strIsEncrypted != null){
                ret = Integer.parseInt(strIsEncrypted);
            }else{
                LogUtil.e(TAG, "not find key: " +keyOfTargetValue);
            }
        }catch (Exception e){
          e.printStackTrace();
        }
        return ret == 1;
    }

    public static void loadImage(final Context context, final ImageView imageView, final EZAlarmInfo alarmInfo, final VerifyCodeInput.VerifyCodeErrorListener verifyCodeErrorListener) {
        if (alarmInfo == null){
            return;
        }
        if (!isEncrypt(alarmInfo.getAlarmPicUrl())){
            Glide.with(context).load(alarmInfo.getAlarmPicUrl())
                    .placeholder(R.drawable.notify_bg)
                    .listener(new RequestListener<String, GlideDrawable>() {
                        @Override
                        public boolean onException(Exception e, String s, Target<GlideDrawable> target, boolean b) {
                            if (e != null) {
                                e.printStackTrace();
                            }
                            return false;
                        }
                        @Override
                        public boolean onResourceReady(GlideDrawable glideDrawable, String s, Target<GlideDrawable> target, boolean b, boolean b1) {
                            return false;
                        }
                    })
                    .error(R.drawable.event_list_fail_pic)
                    .into(imageView);
        }else{
            int crypt = 0;
            String pwd = "";
            if (alarmInfo.getCrypt() != 2){ //兼容不支持的服务，继续走旧的默认的设备加密
                crypt = 1;
                pwd = DataManager.getInstance().getDeviceSerialVerifyCode(alarmInfo.getDeviceSerial());
            }else {
                crypt = 2;
                pwd = alarmInfo.getChecksum();
            }

            if (TextUtils.isEmpty(pwd)){
                imageView.setImageResource(R.drawable.alarm_encrypt_image_mid);
                if (verifyCodeErrorListener != null) {
                    verifyCodeErrorListener.verifyCodeError();
                }
                return;
            }
            final int finalCrypt = crypt;
            final String finalPwd = pwd;
            Glide.with(context)
                    .load(alarmInfo.getAlarmPicUrl())
                    .asBitmap()
                    /**************图片加载监听，打印错误信息*************************/
                    .listener(new RequestListener<String, Bitmap>() {
                        @Override
                        public boolean onException(Exception e, String model, Target<Bitmap> target, boolean isFirstResource) {
                            if (e != null) {
                                e.printStackTrace();
                            }
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap resource, String model, Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .placeholder(R.drawable.notify_bg)
                    /**************加密图片本地文件缓存,开发者自己决定缓存机制*******************/
                    .diskCacheStrategy(DiskCacheStrategy.RESULT)
                    .error(R.drawable.event_list_fail_pic)
                    .imageDecoder(new ResourceDecoder<InputStream, Bitmap>() {
                        @Override
                        public Resource<Bitmap> decode(InputStream source, int width, int height) throws IOException {
                            ByteArrayOutputStream output = new ByteArrayOutputStream();
                            byte[] buffer = new byte[4096];
                            Bitmap desBitmap = null;
                            int n = 0;
                            while (-1 != (n = source.read(buffer))) {
                                output.write(buffer, 0, n);
                            }
                            output.flush();
                            output.close();
                            byte[] src = output.toByteArray();
                            if (src == null || src.length <= 0){
                                LogUtil.d("EZUTils","图片加载错误！");
                                return null;
                            }
                            if (!isEncrypt(alarmInfo.getAlarmPicUrl())){
                                desBitmap = BitmapFactory.decodeByteArray(src, 0, src.length);
                            }else{
                            /*************** 开发者需要调用此接口解密 ****************/
                                byte[] data1 = EzvizApplication.getOpenSDK().decryptData(output.toByteArray(), finalPwd, finalCrypt);
                                if (data1 == null || data1.length <= 0) {
                                    LogUtil.d("EZUTils", "verifyCodeError！");
                                    /*************** 验证码错误 ,此处回调是在子线程中，处理UI需调回到主线程****************/
                                    if (verifyCodeErrorListener != null) {
                                        verifyCodeErrorListener.verifyCodeError();
                                    }
                                } else {
                                    desBitmap = BitmapFactory.decodeByteArray(data1, 0, data1.length);
                                }
                            }

                            if (desBitmap != null){
                                return new BitmapResource(desBitmap,DataManager.getInstance().getBitmapPool(context));
                            }
                            return null;
                        }
                        @Override
                        public String getId() {
                            return alarmInfo.getAlarmPicUrl();
                        }
                    })
                    .into(imageView);
        }
    }
}
