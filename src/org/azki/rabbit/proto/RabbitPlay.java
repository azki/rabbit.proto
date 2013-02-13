package org.azki.rabbit.proto;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class RabbitPlay implements Runnable {
	static final int MAX_ITEM = 8;
	
	boolean onGame, onPause;

	private Paint backPaint;
	private RectF backRect;
	private Paint textPaint;

	private ObjectItem[] objectArr;
	private SurfaceHolder mSurfaceHolder;

	private long lastPostTime = 0;
	private long lastSecondsTime = 0;
	private long curFrameCnt = 0;
	private long lastFPS = 0;

	Thread thread;
	
	Rabbit rabbit;
	Hurdle hurdle;

	public RabbitPlay(SurfaceHolder surfaceHolder, Context context) {
		mSurfaceHolder = surfaceHolder;
		Resources mRes = context.getResources();

		backPaint = new Paint();
		backPaint.setAntiAlias(true);
		backPaint.setARGB(255, 255, 255, 255);
		backRect = new RectF(0, 0, 0, 0);

		textPaint = new Paint();
		textPaint.setAntiAlias(true);
		textPaint.setARGB(255, 0, 0, 0);

		initObjects(mRes);
	}

	public void initObjects(Resources mRes) {
		int i = 0;
		objectArr = new ObjectItem[MAX_ITEM];
		rabbit = new Rabbit(mRes);
		objectArr[MAX_ITEM - 1] = rabbit;
		hurdle = new Hurdle(mRes);
		objectArr[i++] = hurdle;
	}

	// 화면 사이즈가 변경되면 배경화면 크기도 조절해 준다.
	public void setSurfaceSize(int width, int height) {
		synchronized (mSurfaceHolder) {
			backRect.set(0, 0, width, height);
			//backImg = Bitmap.createScaledBitmap(backImg, width, height, true);
			for (ObjectItem item : objectArr) {
				if (item != null) {
					item.initSize(width, height);
				}
			}
		}
	}

	// 에니메이션 시작
	public void start() {
		Log.d(FullscreenActivity.TAG, "start()");
		onGame = true;
		thread = new Thread(this);
		thread.start();
	}

	// 에니메이션 종료
	public void stop() {
		Log.d(FullscreenActivity.TAG, "stop()");
		onGame = false;
		resume();
		// thread.interrupt();
	}

	// 에니메이션 일시중지
	public void pause() {
		Log.d(FullscreenActivity.TAG, "pause()");
		onPause = true;
	}

	// 에니메이션 일시중지 해제
	public synchronized void resume() {
		onPause = false;
		notifyAll();
	}

	public void run() {
		Log.d(FullscreenActivity.TAG, "run()");
		while (onGame) {
			Canvas c = null;
			try {
				long currentTime = System.currentTimeMillis();
				if (1000 < currentTime - lastSecondsTime) {
					lastFPS = curFrameCnt;
					curFrameCnt = 0;
					lastSecondsTime = currentTime;
				}
				long deltaTime = 0 < lastPostTime ? currentTime - lastPostTime : 0;
				calObjectFrame(deltaTime);
				lastPostTime = currentTime;
				curFrameCnt += 1;
				c = mSurfaceHolder.lockCanvas(null);
				doDraw(c);
				Thread.sleep(1);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (c != null)
					// Paint작업이 끝났으면 Canvas를 풀어주고, 화면을 갱신한다.
					// 주의할 점은 Exception이 발생해도 Canvas lock이 풀어지도록
					// finally안에서 이 메서드를 호출해야 안전하다.
					mSurfaceHolder.unlockCanvasAndPost(c);
				// 에니메이션 종료
				if (!onGame)
					break;
				// 에니메이션 일시중지
				// wait pool로 들어가서, notifyAll()에 의해 부활될때까지 손가락 빨고 있기
				if (onPause) {
					try {
						synchronized (this) {
							wait();
						}
					} catch (Exception e2) {
					}
				}
				// wait pool에서 겨우 나왔는데, 사용자가 변심해서 아예 에니메이션을
				// 종료시켰는지 재확인
				if (!onGame)
					break;
			}
		}
	}

	boolean doKeyDown(int keyCode, KeyEvent msg) {
		Log.d(FullscreenActivity.TAG, "doKeyDown()");
		synchronized (mSurfaceHolder) {
			return false;
		}
	}

	boolean doKeyUp(int keyCode, KeyEvent msg) {
		Log.d(FullscreenActivity.TAG, "doKeyUp()");
		synchronized (mSurfaceHolder) {
			return false;
		}
	}

	// 터치 이벤트가 눌린 좌표에 물방울 객체를 찍어준다.
	boolean doTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_MOVE) {
			rabbit.moveTo(event.getX());
			return true;
		}
		if (event.getAction() == MotionEvent.ACTION_UP) {
			rabbit.stopMove();
			rabbit.jump();
			return true;
		}
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			return true;
		}
		return false;
	}

	private void calObjectFrame(long deltaTime) {
		for (ObjectItem obj : objectArr) {
			if (obj != null && obj.isAlive()) {
				obj.calFrame(deltaTime);
			}
		}
	}

	public void doDraw(Canvas canvas) {
		// 배경화면 칠하기
		if (canvas != null) {
			canvas.drawRect(backRect, backPaint);
			for (ObjectItem obj : objectArr) {
				if (obj != null && obj.isAlive()) {
					obj.draw(canvas);
				}
			}
			canvas.drawText("FPS: " + lastFPS, 0, 10, textPaint);
		}
	}

	// //////////////////////////////////////////////////////////////////////////////////////////
	// //////////////////////////////////////////////////////////////////////////////////////////
	// //////////////////////////////////////////////////////////////////////////////////////////
	// //////////////////////////////////////////////////////////////////////////////////////////
	// //////////////////////////////////////////////////////////////////////////////////////////
	
	public interface ObjectItem {
		public void initSize(int width, int height);
		public boolean isAlive();
		public void calFrame(long delta);
		public void draw(Canvas canvas);
	}
	
	public class Rabbit implements ObjectItem {
		static final double WIDTH_V = 0.32;
		static final double HEIGHT_V = 0.18;
		static final double INIT_BOTTOM_MARGIN_V = 0.18;
		static final double MOVE_V = 1;
		static final double JUMP_V = -0.2;
		static final double JUMP_SCALE_V = 0.001;
		static final long JUMP_DELAY = 1000;
		static final long RUN_MOTION_DELAY = 400;
		final int [] CHAR_IMAGES = {
			R.drawable.ra1,
			R.drawable.ra2,
			R.drawable.ra3,
			R.drawable.ra4
		};
		
		private Resources mRes;
		private Bitmap bitmap;
		private Bitmap [] bitmapArr;
		private Paint paint;
		private double bitmapScale;
		private int mWidth, mHeight;
		private float mX, mY, mInitY;
		private float moveTo;
		private long runMotionDelay;
		private int runMotionMode;
		private long jumpDelay;
		
		Rabbit(Resources res) {
			mRes = res;
			paint = new Paint();
			bitmapScale = 1;
		}
		public void initSize(int width, int height) {
			mWidth = (int)(width * WIDTH_V);
			mHeight = (int)(height * HEIGHT_V);
			bitmapArr = new Bitmap[CHAR_IMAGES.length];
			for (int i = 0; i < CHAR_IMAGES.length; i += 1) {
				Bitmap bitmapTemp = BitmapFactory.decodeResource(mRes, CHAR_IMAGES[i]);
				bitmapArr[i] = Bitmap.createScaledBitmap(bitmapTemp, mWidth, mHeight, true);
				bitmapTemp.recycle();
			}
			mY = (int)(height - mHeight / 2 - (int)(height * INIT_BOTTOM_MARGIN_V));
			mInitY = mY;
			mX = (int)((width - mWidth) / 2);
			moveTo = mX;
		}
		public void moveTo(float toX) {
			moveTo = toX;
		}
		public void stopMove(){
			moveTo = mX;
		}
		public Bitmap copyBitmapWithRotate(Bitmap b, float degrees) {
            Matrix m = new Matrix();
            m.postRotate(degrees);
            try {
                return Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
            } catch (OutOfMemoryError ex) {
            	Log.d("", Log.getStackTraceString(ex));
                return b;
            }
	    }
		public void jump(){
			if (jumpDelay <= 0) {
				jumpDelay = JUMP_DELAY;
			}
		}
		public void setSpeed(int speed) {
			
		}
		public void crash(){
			
		}
		public void calMoveFrame(long delta){
			int moveXUnit = (int)(delta * MOVE_V);
			if (moveTo < mX - moveXUnit) {
				mX = mX - moveXUnit;
			} else if (mX + moveXUnit < moveTo) {
				mX = mX + moveXUnit;
			}
		}
		public void calRunFrame(long delta){
			if (0 < runMotionDelay) {
				runMotionDelay -= delta;
				if ((double)runMotionDelay / RUN_MOTION_DELAY < (double)(bitmapArr.length - runMotionMode - 1) / bitmapArr.length) {
					runMotionMode += 1;
					if (bitmapArr.length <= runMotionMode) {
						runMotionMode = 0;
					}
				}
				bitmap = bitmapArr[runMotionMode];
			} else {
				runMotionDelay = RUN_MOTION_DELAY;
				bitmap = bitmapArr[0];
			}
		}
		public void calJumpFrame(long delta){
			if (0 < jumpDelay) {
				jumpDelay -= delta;
				int moveYUnit = (int)(delta * JUMP_V);
				if (jumpDelay < JUMP_DELAY / 2) {
					mY = mY - moveYUnit;
				} else {
					mY = mY + moveYUnit;
				}
				bitmapScale = 1 + (JUMP_DELAY - Math.abs(JUMP_DELAY / 2 - jumpDelay)) * JUMP_SCALE_V;
				bitmap = bitmapArr[0];
			} else {
				mY = mInitY;
				bitmapScale = 1;
			}
		}
		@Override
		public void calFrame(long delta) {
			calMoveFrame(delta);
			calRunFrame(delta);
			calJumpFrame(delta);
		}
		@Override
		public boolean isAlive(){
			return true;
		}
		@Override
		public void draw(Canvas canvas) {
			if (bitmap != null) {
				int drawWidth = (int)(bitmap.getWidth() * bitmapScale);
				int drawHeight = (int)(bitmap.getHeight() * bitmapScale);
				int x = (int)(mX - drawWidth / 2);
				int y = (int)(mY - drawHeight / 2);
				Rect src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
				Rect dst = new Rect(x, y, x + drawWidth, y + drawHeight);
				canvas.drawBitmap(bitmap, src, dst, paint);
			}
		}
	}
	
	public class Hurdle implements ObjectItem {
		static final double WIDTH_V = 0.032;
		static final double HEIGHT_V = 0.0045;
		static final double INIT_Y_V = 0.25;
		static final double MOVE_V = 0.4;
		static final long DELAY = 3000;
		
		private Resources mRes;
		private Bitmap bitmap;
		private Bitmap [] bitmapArr;
		private Paint paint;
		private double bitmapScale;
		
		private int mWidth, mHeight;
		private float mX, mY, mInitY;
		private float moveTo;
		private long mScale;
		private long myDelay;
		
		Hurdle(Resources res) {
			mRes = res;
			paint = new Paint();
			bitmapScale = 1;
			myDelay = DELAY;
		}
		@Override
		public void initSize(int width, int height) {
			mWidth = (int)(width * WIDTH_V);
			mHeight = (int)(height * HEIGHT_V);
			mY = (int)(height * INIT_Y_V);
			mInitY = mY;
			mX = (int)((width - mWidth) / 2);
			moveTo = mX;
		}
		@Override
		public boolean isAlive() {
			return true;
		}
		public void calMoveFrame(long delta){
			mY += MOVE_V * delta;
		}
		public void calScaleFrame(long delta){
			mScale = mScale + delta;
		}
		@Override
		public void calFrame(long delta) {
			calMoveFrame(delta);
			calScaleFrame(delta);
			myDelay -= delta;
			if (myDelay < 0) {
				mY = mInitY;
				mScale = 0;
				myDelay = DELAY;
			}
		}
		@Override
		public void draw(Canvas canvas) {
			canvas.save();
			canvas.scale(1.0f + (float)mScale / 75.0f, 1.0f + (float)mScale / 250.0f, mX + (mWidth / 2), mY + (mHeight / 2));
			canvas.drawRect(mX, mY, mX + mWidth, mY + mHeight, paint);
			canvas.restore();
		}
	}
}
