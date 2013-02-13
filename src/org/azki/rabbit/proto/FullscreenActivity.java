package org.azki.rabbit.proto;

import org.azki.rabbit.proto.util.SystemUiHider;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;


public class FullscreenActivity extends Activity implements OnTouchListener {

	public static String TAG = "RabbitPlay";
	SystemUiHider mSystemUiHider;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		RabbitPlayView view = new RabbitPlayView(this);
		view.setOnTouchListener(this);
		setContentView(view); // SurfaceView�� ����ȭ������ ���

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	// �̺�Ʈ ���� ����: OnTouchListener.onTouch -> View.onTouchEvent
	public boolean onTouch(View view, MotionEvent event) {
		// Log.d(FullscreenActivity.TAG, "Activity.onTouch(): " + event);
		return false; // true ������ View.onTouchEvent()�� �̺�Ʈ ���� �ȵ�
	}

	// Activity�� default handler
	// setContentView()�� View�� ������� ���� ��� �۵�
	public boolean onTouchEvent(MotionEvent event) {
		// Log.d(FullscreenActivity.TAG, "Activity.onTouchEvent(): " + event);
		return true;
	}

}

class RabbitPlayView extends SurfaceView implements SurfaceHolder.Callback {

	private RabbitPlay rPlay;

	public RabbitPlayView(Context context) {
		super(context);
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		rPlay = new RabbitPlay(holder, context);
		setFocusable(true);
	}

	// ����ڰ� ȭ���� �հ������� ������, �� �޼��忡�� �̺�Ʈ�� �޾� ó���Ѵ�.
	// View�� default handler
	public boolean onTouchEvent(MotionEvent event) {
		rPlay.doTouchEvent(event);
		return true;
	}

	// ȭ��(Surface) ����� ����Ǹ� �ڵ����� ȣ��Ǵ� �ݹ� �޼���
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		rPlay.setSurfaceSize(width, height);
	}

	// Surface�� �����ɶ� �ڵ����� ȣ��Ǵ� �ݹ� �޼���
	public void surfaceCreated(SurfaceHolder arg0) {
		rPlay.start();
	}

	// Surface�� �Ҹ�ɶ� �ڵ����� ȣ��Ǵ� �ݹ� �޼���
	public void surfaceDestroyed(SurfaceHolder arg0) {
		rPlay.stop();
	}
}
