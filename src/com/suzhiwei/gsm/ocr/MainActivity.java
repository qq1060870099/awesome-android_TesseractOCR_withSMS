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

	// �첽����
	static PendingIntent paIntent;
	static SmsManager smsManager; // sms������

	protected static final int RESULT_SPEECH = 0x10; // ����ʶ�𷵻�
	private static final int PHOTO_CAPTURE = 0x11;// ����
	private static final int PHOTO_RESULT = 0x12;// ���
	private static final int SHOWRESULT = 0x101;// ��ʾ������ش���
	private static final int SHOWTREATEDIMG = 0x102;// ��ȡͼ�񷵻ش���
	// private static final int SHOWVOICE = 0x103;

	private static String LANGUAGE = "eng"; // ͼ��ʶ�����Գ�ʼ��ΪӢ��
	private static String IMG_PATH = getSDPath() + java.io.File.separator
			+ "ocrtest"; // ��ȡͼ��ĵ�ַ
	private static String str_result; // ʶ���ı����

	
	// private static TextView txtView;
	private static TextView tv_result; // ��ʾ���

	private static EditText edt_result;// ʶ�����༭�ı�
	private static EditText phonenum; // �绰����༭�ı�

	private static Button btnSpeak; // button of speak
	private static Button btnSent; // ���Ͷ��Ű�ť
	private static Button btnClear; // �����ť
	private static Button btnCamera; // ����ͷ��ȡͼ��ť
	private static Button btnSelect; // ѡ��ͼ��ť

	private static Switch autosent; // �Զ����Ϳ���
	private static CheckBox chPreTreat; // ͼ���ֵ����ť
	private static RadioGroup radioGroup; // ��ѡ��ť��

	private static ImageView ivSelected; // ѡ���ͼ��
	private static ImageView ivTreated; // �������ͼ��

	private static Bitmap bitmapSelected;// ѡ���λͼ
	private static Bitmap bitmapTreated;// �������λͼ

	// ��handler���ڴ����޸Ľ��������
	public static Handler myHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SHOWRESULT:
				if (str_result.equals("")) {
					edt_result.setText("ʶ��ʧ��");
					tv_result.setText("ʶ��ʧ��");
				} else {
					edt_result.setText(str_result);
					tv_result.setText(str_result);

					if (autosent.isChecked()) {
						String pn = phonenum.getText().toString().trim();
						String sc = edt_result.getText().toString().trim();
						smsManager.sendTextMessage(pn, // Ŀ��绰����
								null, // �������ĺ��룬���Կ��Բ���
								sc, // ��������
								paIntent, // sentIntent:�����ͼ��װ�˶��ŷ���״̬����Ϣ
								null);
					}//end of if autosent
				}// end of if textResult
				break;
			case SHOWTREATEDIMG:
				edt_result.setText("ʶ����...");
				tv_result.setText("ʶ����...");
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

		// ���ļ��в����� ���ȴ����ļ���
		File path = new File(IMG_PATH);
		if (!path.exists()) {
			path.mkdirs();
		}

		btnSpeak = (Button) findViewById(R.id.btnSpeak); // ����ʶ���������ť
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
				smsManager.sendTextMessage(pn, // Ŀ��绰����
						null, // �������ĺ��룬���Կ��Բ���
						sc, // ��������
						paIntent, // sentIntent:�����ͼ��װ�˶��ŷ���״̬����Ϣ
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

		// �������ý�������
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
				
				edt_result.setText("ʶ����...");
				tv_result.setText("ʶ����...");
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
				// ����ϵͳͼƬ�༭���вü�
				startPhotoCrop(Uri.fromFile(new File(IMG_PATH, "temp.jpg")));
			}
			break;
		}// end case PHOTO_CAPTURE

		case PHOTO_RESULT: {

			if (resultCode == RESULT_OK) {

				bitmapSelected = decodeUriAsBitmap(Uri.fromFile(new File(
						IMG_PATH, "temp_cropped.jpg")));

				// ��ʾѡ���ͼƬ
				showPicture(ivSelected, bitmapSelected);

				// ���߳�������ʶ��
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

	// ����ʶ��
	class speakButtonListener implements OnClickListener {
		@Override
		// ������¼�����ʱ���÷�������
		public void onClick(View v) {

			// ����һ��Intent,�������� ����ʶ����
			Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
			// һЩ��������
			intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");

			try {
				startActivityForResult(intent, RESULT_SPEECH);
				edt_result.setText("");
				tv_result.setText("");
			} catch (ActivityNotFoundException a) {
				// ����һ��toast ��ʾû�м�⵽ ����ʶ��Ӧ��
				Toast t = Toast.makeText(getApplicationContext(),
						"Ops! Your device doesn't support Speech to Text",
						Toast.LENGTH_SHORT);
				t.show();// ��ʾtoast t
			} // end of try-catch

		} // end of onClick

	};

	// ����ʶ��
	class cameraButtonListener implements OnClickListener {

		@Override
		public void onClick(View arg0) {
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			intent.putExtra(MediaStore.EXTRA_OUTPUT,
					Uri.fromFile(new File(IMG_PATH, "temp.jpg")));
			startActivityForResult(intent, PHOTO_CAPTURE);
		}
	};

	// �����ѡȡ��Ƭ���ü�
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

	// ��ͼƬ��ʾ��view��
	public static void showPicture(ImageView iv, Bitmap bmp) {
		iv.setImageBitmap(bmp);
	}

	/**
	 * ����ͼƬʶ��
	 * 
	 * @param bitmap
	 *            ��ʶ��ͼƬ
	 * @param language
	 *            ʶ������
	 * @return ʶ�����ַ���
	 */
	public String doOcr(Bitmap bitmap, String language) {
		TessBaseAPI baseApi = new TessBaseAPI();

		baseApi.init(getSDPath(), language);

		// ����Ӵ��У�tess-twoҪ��BMP����Ϊ������
		bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

		baseApi.setImage(bitmap);

		String text = baseApi.getUTF8Text();

		baseApi.clear();
		baseApi.end();

		return text;
	}

	/**
	 * ��ȡsd����·��
	 * 
	 * @return ·�����ַ���
	 */
	public static String getSDPath() {
		File sdDir = null;
		boolean sdCardExist = Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED); // �ж�sd���Ƿ����
		if (sdCardExist) {
			sdDir = Environment.getExternalStorageDirectory();// ��ȡ���Ŀ¼
		}
		return sdDir.toString();
	}

	/**
	 * ����ϵͳͼƬ�༭���вü�
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
	 * ����URI��ȡλͼ
	 * 
	 * @param uri
	 * @return ��Ӧ��λͼ
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
