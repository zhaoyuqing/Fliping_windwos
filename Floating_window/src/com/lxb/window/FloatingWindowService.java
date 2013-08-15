package com.lxb.window;

import java.util.ArrayList;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageButton;

public class FloatingWindowService extends Service {

	public static final String OPERATION = "operation";
	public static final int OPERATION_SHOW = 100;
	public static final int OPERATION_HIDE = 101;

	private static final int HANDLE_CHECK_ACTIVITY = 200;

	private boolean isAdded = false; // 是否已增加悬浮窗
	private static WindowManager wm;
	private static WindowManager.LayoutParams params;
	private ImageButton btn_floatView;

	private List<String> homeList; // 桌面应用程序包名列表
	private ActivityManager mActivityManager;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		homeList = getHomes();
		createFloatView();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onStart(Intent intent, int startId) {

		int operation = intent.getIntExtra(OPERATION, OPERATION_SHOW);
		switch (operation) {
		case OPERATION_SHOW:
			mHandler.removeMessages(HANDLE_CHECK_ACTIVITY);
			mHandler.sendEmptyMessage(HANDLE_CHECK_ACTIVITY);
			break;
		case OPERATION_HIDE:
			mHandler.removeMessages(HANDLE_CHECK_ACTIVITY);
			break;
		}
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HANDLE_CHECK_ACTIVITY:
				if (isHome()) {
					if (!isAdded) {
						wm.addView(btn_floatView, params);
						isAdded = true;
					}
				} else {
					if (isAdded) {
						wm.removeView(btn_floatView);
						isAdded = false;
					}
				}
				mHandler.sendEmptyMessageDelayed(HANDLE_CHECK_ACTIVITY, 500);
				break;
			}
		}
	};

	/**
	 * 创建悬浮窗
	 */
	private void createFloatView() {
		btn_floatView = new ImageButton(getApplicationContext());
		btn_floatView.setBackgroundResource(R.drawable.butterfly);
		

		wm = (WindowManager) getApplicationContext().getSystemService(
				Context.WINDOW_SERVICE);
		
		params = new WindowManager.LayoutParams();

		// 设置window type
		params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
		
		
		
		// 调整悬浮窗口至左上角，便于调整坐标
//		params.gravity = Gravity.RIGHT|Gravity.CENTER_VERTICAL;
		
		/*
		 * 如果设置为params.type = WindowManager.LayoutParams.TYPE_PHONE; 那么优先级会降低一些,
		 * 即拉下通知栏不可见
		 */
		params.format = PixelFormat.RGBA_8888; // 设置图片格式，效果为背景透明

		// 设置Window flag
		/*
		 * LayoutParams.TYPE_SYSTEM_ERROR：保证该悬浮窗所有View的最上层
		 * LayoutParams.FLAG_NOT_FOCUSABLE:该浮动窗不会获得焦点，但可以获得拖动
		 * PixelFormat.TRANSPARENT：悬浮窗透明
		 */
		params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
				| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
		/*
		 * 下面的flags属性的效果形同“锁定”。 悬浮窗不可触摸，不接受任何事件,同时不影响后面的事件响应。
		 * wmParams.flags=LayoutParams.FLAG_NOT_TOUCH_MODAL |
		 * LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_NOT_TOUCHABLE;
		 */

		// 设置悬浮窗的长得宽
		params.x = 0;
		params.y = 0;
		params.width = LayoutParams.WRAP_CONTENT;
		params.height = LayoutParams.WRAP_CONTENT;

		// 设置悬浮窗的Touch监听
		btn_floatView.setOnTouchListener(new OnTouchListener() {
			int lastX, lastY;
			int paramX, paramY;

			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					lastX = (int) event.getRawX();
					lastY = (int) event.getRawY();
					paramX = params.x;
					paramY = params.y;
					break;
				case MotionEvent.ACTION_MOVE:
					int dx = (int) event.getRawX() - lastX;
					int dy = (int) event.getRawY() - lastY;
					params.x = paramX + dx;
					params.y = paramY + dy;
					// 更新悬浮窗位置
					wm.updateViewLayout(btn_floatView, params);
					break;

				case MotionEvent.ACTION_UP: // 捕获手指触摸离开动作

					// onTouch方法会屏蔽掉onClick方法，
					// 所以在此用up方法来代替onClick方法，当移动的距离小于N时，认为是点击事件
					dx = (int) event.getRawX() - lastX;
					dy = (int) event.getRawY() - lastY;
					if (dx < 3 && dy < 3) {
						floatingButtonOnclick();
					}
					break;

				}

				return true;
			}
		});


		wm.addView(btn_floatView, params);
		isAdded = true;
	}

	/**
	 * 获得属于桌面的应用的应用包名称
	 * 
	 * @return 返回包含所有包名的字符串列表
	 */
	private List<String> getHomes() {
		List<String> names = new ArrayList<String>();
		PackageManager packageManager = this.getPackageManager();
		// 属性
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(
				intent, PackageManager.MATCH_DEFAULT_ONLY);
		for (ResolveInfo ri : resolveInfo) {
			names.add(ri.activityInfo.packageName);
		}
		return names;
	}

	/**
	 * 判断当前界面是否是桌面
	 */
	public boolean isHome() {
		if (mActivityManager == null) {
			mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		}
		List<RunningTaskInfo> rti = mActivityManager.getRunningTasks(1);
		return homeList.contains(rti.get(0).topActivity.getPackageName());
	}

	private void floatingButtonOnclick() {
		Intent in = new Intent(getApplicationContext(),
				Floating_windowActivity.class);
		in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(in);
	}

}
