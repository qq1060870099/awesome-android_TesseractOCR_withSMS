package com.suzhiwei.gsm.ocr;

//import net.viralpatel.android.speechtotextdemo.R;
//import java.util.List;
//import android.widget.ImageButton;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Switch;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.mikewong.tool.tesseract.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

public class MainActivity extends Activity {

	// 异步激发
	static PendingIntent paIntent;
	static SmsManager smsManager; // sms管理器

	protected static final int RESULT_SPEECH = 0x10; // 语音识别返回
	private static final int PHOTO_CAPTURE = 0x11;// 拍照
	private static final int PHOTO_RESULT = 0x12;// 结果
	private static final int SHOWRESULT = 0x101;// 显示结果返回代码
	private static final int SHOWTREATEDIMG = 0x102;// 读取图像返回代码
	// private static final int SHOWVOICE = 0x103;

	private static String LANGUAGE = "eng"; // 图像识别语言初始化为英文
	private static String IMG_PATH = getSDPath() + java.io.File.separator
			+ "ocrtest"; // 获取图像的地址
	private static String str_result; // 识别文本结果

	
	// private static TextView txtView;
	private static TextView tv_result; // 显示结果

	private static EditText edt_result;// 识别结果编辑文本
	private static EditText phonenum; // 电话号码编辑文本

	private static Button btnSpeak; // button of speak
	private static Button btnSent; // 发送短信按钮
	private static Button btnClear; // 清除按钮
	private static Button btnCamera; // 摄像头获取图像按钮
	private static Button btnSelect; // 选择图像按钮

	private static Switch autosent; // 自动发送开关
	private static CheckBox chPreTreat; // 图像二值化按钮
	private static RadioGroup radioGroup; // 单选按钮组

	private static ImageView ivSelected; // 选择的图像
	private static ImageView ivTreated; // 处理完的图像

	private static Bitmap bitmapSelected;// 选择的位图
	private static Bitmap bitmapTreated;// 处理完的位图

	// 该handler用于处理修改结果的任务
	public static Handler myHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SHOWRESULT:
				if (str_result.equals("")) {
					edt_result.setText("识别失败");
					tv_result.setText("识别失败");
				} else {
					edt_result.setText(str_result);
					tv_result.setText(str_result);

					if (autosent.isChecked()) {
						String pn = phonenum.getText().toString().trim();
						String sc = edt_result.getText().toString().trim();
						smsManager.sendTextMessage(pn, // 目标电话号码
								null, // 短信中心号码，测试可以不填
								sc, // 短信内容
								paIntent, // sentIntent:这个意图包装了短信发送状态的信息
								null);
					}//end of if autosent
				}// end of if textResult
				break;
			case SHOWTREATEDIMG:
				edt_result.setText("识别中...");
				tv_result.setText("识别中...");
				showPicture(ivTreated, bitmapTreated);
				break;

			}
			super.handleMessage(msg);
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// 若文件夹不存在 首先创建文件夹
		File path = new File(IMG_PATH);
		if (!path.exists()) {
			path.mkdirs();
		}

		btnSpeak = (Button) findViewById(R.id.btnSpeak); // 语音识别的启动按钮
		btnCamera = (Button) findViewById(R.id.btn_camera);
		btnSelect = (Button) findViewById(R.id.btn_select);
		btnSent = (Button) findViewById(R.id.btnSent);
		btnClear = (Button) findViewById(R.id.btnClear);
		phonenum = (EditText) findViewById(R.id.phone_num);
		edt_result = (EditText) findViewById(R.id.tv_result);
		autosent = (Switch) findViewById(R.id.sw_autosent);
		tv_result = (TextView) findViewById(R.id.tvshow);
		ivSelected = (ImageView) findViewById(R.id.iv_selected);
		ivTreated = (ImageView) findViewById(R.id.iv_treated);
		chPreTreat = (CheckBox) findViewById(R.id.ch_pretreat);
		radioGroup = (RadioGroup) findViewById(R.id.radiogroup);
		
		
		btnCamera.setOnClickListener(new cameraButtonListener());
		btnSelect.setOnClickListener(new selectButtonListener());
		btnSpeak.setOnClickListener(new speakButtonListener());
		
		paIntent = PendingIntent.getBroadcast(this, 0, new Intent(), 0);
		smsManager = SmsManager.getDefault();

		btnSent.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String pn = phonenum.getText().toString().trim();
				String sc = edt_result.getText().toString().trim();
				smsManager.sendTextMessage(pn, // 目标电话号码
						null, // 短信中心号码，测试可以不填
						sc, // 短信内容
						paIntent, // sentIntent:这个意图包装了短信发送状态的信息
						null);
			}// end of void onClick()

		});

		btnClear.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				tv_result.setText("");
				edt_result.setText("");
			}
		});

		// 用于设置解析语言
		radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch (checkedId) {
				case R.id.rb_en:
					LANGUAGE = "eng";
					break;
				case R.id.rb_ch:
					LANGUAGE = "chi_sim";
					break;
				}// end of switch
			} // end of void onCheckedChanged()
		});// end of OnCheckedChangeListener()

	}// end of void onCreate()

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}// end of boolean onCreateOptionsMenu(Menu menu)

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case RESULT_SPEECH: {
			if (resultCode == RESULT_OK && null != data) {
				
				edt_result.setText("识别中...");
				tv_result.setText("识别中...");
				ArrayList<String> text = data
						.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				str_result = text.get(0);
				Message msg = new Message();
				msg.what = SHOWRESULT;
				myHandler.sendMessage(msg);


			}// end if (resultCode == RESULT_OK && null != data)
			break;
		}// end of case RESULT_SPEECH
		case PHOTO_CAPTURE: {
			if (resultCode == RESULT_OK) {
				// 调用系统图片编辑进行裁剪
				startPhotoCrop(Uri.fromFile(new File(IMG_PATH, "temp.jpg")));
			}
			break;
		}// end case PHOTO_CAPTURE

		case PHOTO_RESULT: {

			if (resultCode == RESULT_OK) {

				bitmapSelected = decodeUriAsBitmap(Uri.fromFile(new File(
						IMG_PATH, "temp_cropped.jpg")));

				// 显示选择的图片
				showPicture(ivSelected, bitmapSelected);

				// 新线程来处理识别
				new Thread(new Runnable() {
					@Override
					public void run() {

						bitmapTreated = ImgPretreatment
								.converyToGrayImg(bitmapSelected);
						if (chPreTreat.isChecked()) {
							bitmapTreated = ImgPretreatment
									.doPretreatment(bitmapTreated);
						} //end if chPreTreat.isChecked()
						
						Message msg = new Message();
						msg.what = SHOWTREATEDIMG;
						myHandler.sendMessage(msg);
						
						str_result = doOcr(bitmapTreated, LANGUAGE);
						Message msg2 = new Message();
						msg2.what = SHOWRESULT;
						myHandler.sendMessage(msg2);

	
					}// end of run()

				}// end of Runnable()

				).start(); // start Thread(Runnable())
			}// end if (resultCode == RESULT_OK)
			break;
		}// end of case PHOTO_RESULT

		}// end of switch (requestCode)

	}// end of onActivityResult

	// 语音识别
	class speakButtonListener implements OnClickListener {
		@Override
		// 当点击事件发生时，该方法激活
		public void onClick(View v) {

			// 建立一个Intent,用来启动 语音识别器
			Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
			// 一些启动参数
			intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");

			try {
				startActivityForResult(intent, RESULT_SPEECH);
				edt_result.setText("");
				tv_result.setText("");
			} catch (ActivityNotFoundException a) {
				// 定义一个toast 提示没有检测到 语音识别应用
				Toast t = Toast.makeText(getApplicationContext(),
						"Ops! Your device doesn't support Speech to Text",
						Toast.LENGTH_SHORT);
				t.show();// 显示toast t
			} // end of try-catch

		} // end of onClick

	};

	// 拍照识别
	class cameraButtonListener implements OnClickListener {

		@Override
		public void onClick(View arg0) {
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			intent.putExtra(MediaStore.EXTRA_OUTPUT,
					Uri.fromFile(new File(IMG_PATH, "temp.jpg")));
			startActivityForResult(intent, PHOTO_CAPTURE);
		}
	};

	// 从相册选取照片并裁剪
	class selectButtonListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			intent.setType("image/*");
			intent.putExtra("crop", "true");
			intent.putExtra("scale", true);
			intent.putExtra("return-data", false);
			intent.putExtra(MediaStore.EXTRA_OUTPUT,
					Uri.fromFile(new File(IMG_PATH, "temp_cropped.jpg")));
			intent.putExtra("outputFormat",
					Bitmap.CompressFormat.JPEG.toString());
			intent.putExtra("noFaceDetection", true); // no face detection
			startActivityForResult(intent, PHOTO_RESULT);
		}

	}

	// 将图片显示在view中
	public static void showPicture(ImageView iv, Bitmap bmp) {
		iv.setImageBitmap(bmp);
	}

	/**
	 * 进行图片识别
	 * 
	 * @param bitmap
	 *            待识别图片
	 * @param language
	 *            识别语言
	 * @return 识别结果字符串
	 */
	public String doOcr(Bitmap bitmap, String language) {
		TessBaseAPI baseApi = new TessBaseAPI();

		baseApi.init(getSDPath(), language);

		// 必须加此行，tess-two要求BMP必须为此配置
		bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

		baseApi.setImage(bitmap);

		String text = baseApi.getUTF8Text();

		baseApi.clear();
		baseApi.end();

		return text;
	}

	/**
	 * 获取sd卡的路径
	 * 
	 * @return 路径的字符串
	 */
	public static String getSDPath() {
		File sdDir = null;
		boolean sdCardExist = Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
		if (sdCardExist) {
			sdDir = Environment.getExternalStorageDirectory();// 获取外存目录
		}
		return sdDir.toString();
	}

	/**
	 * 调用系统图片编辑进行裁剪
	 */
	public void startPhotoCrop(Uri uri) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(uri, "image/*");
		intent.putExtra("crop", "true");
		intent.putExtra("scale", true);
		intent.putExtra(MediaStore.EXTRA_OUTPUT,
				Uri.fromFile(new File(IMG_PATH, "temp_cropped.jpg")));
		intent.putExtra("return-data", false);
		intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
		intent.putExtra("noFaceDetection", true); // no face detection
		startActivityForResult(intent, PHOTO_RESULT);
	}

	/**
	 * 根据URI获取位图
	 * 
	 * @param uri
	 * @return 对应的位图
	 */
	private Bitmap decodeUriAsBitmap(Uri uri) {
		Bitmap bitmap = null;
		try {
			bitmap = BitmapFactory.decodeStream(getContentResolver()
					.openInputStream(uri));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		return bitmap;
	}
}// end of MainAcitivity()
