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
		setContentView(view); // SurfaceView를 메인화면으로 등록

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	// 이벤트 전달 순서: OnTouchListener.onTouch -> View.onTouchEvent
	public boolean onTouch(View view, MotionEvent event) {
		// Log.d(FullscreenActivity.TAG, "Activity.onTouch(): " + event);
		return false; // true 설정시 View.onTouchEvent()로 이벤트 전달 안됨
	}

	// Activity의 default handler
	// setContentView()로 View를 등록하지 않을 경우 작동
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

	// 사용자가 화면을 손가락으로 찍으면, 이 메서드에서 이벤트를 받아 처리한다.
	// View의 default handler
	public boolean onTouchEvent(MotionEvent event) {
		rPlay.doTouchEvent(event);
		return true;
	}

	// 화면(Surface) 사이즈가 변경되면 자동으로 호출되는 콜백 메서드
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		rPlay.setSurfaceSize(width, height);
	}

	// Surface가 생성될때 자동으로 호출되눈 콜백 메서드
	public void surfaceCreated(SurfaceHolder arg0) {
		rPlay.start();
	}

	// Surface가 소멸될때 자동으로 호출되눈 콜백 메서드
	public void surfaceDestroyed(SurfaceHolder arg0) {
		rPlay.stop();
	}
}
