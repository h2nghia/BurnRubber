package com.raildeliveryservices.burnrubber;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.raildeliveryservices.burnrubber.fragments.OrderListFragment;
import com.raildeliveryservices.burnrubber.utils.Services;
import com.raildeliveryservices.burnrubber.utils.Utils;

public class OrderActivity extends BaseFragmentActivity implements OrderListFragment.Callbacks {
	
	private FragmentManager _fm;
	private FragmentTransaction _ft;
    private static Intent msgIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		
		Services.startGpsService(this);
		
		_fm = getSupportFragmentManager();

        msgIntent = new Intent(this, MessageActivity.class);
		loadOrders();
	}
	
	private void loadOrders() {
		
		Fragment f = new OrderListFragment();
		Bundle bundle = new Bundle();
		bundle.putBoolean(Constants.BUNDLE_PARAM_TRIP_HISTORY, false);
		f.setArguments(bundle);
		
		_ft = _fm.beginTransaction();
		_ft.replace(R.id.contentFrameLayout, f);
		_ft.commit();
	}

	@Override
	public void onOrderListItemClick(long orderId, boolean readOnly) {
		
		Bundle bundle = new Bundle();
		bundle.putLong(Constants.BUNDLE_PARAM_ORDER_ID, orderId);
		
		if (!Utils.isUserOnline(this) || readOnly) {
			bundle.putBoolean(Constants.BUNDLE_PARAM_READ_ONLY, true);
		} else {
			bundle.putBoolean(Constants.BUNDLE_PARAM_READ_ONLY, false);
		}
		
		Intent intent = new Intent(this, LegActivity.class);
		intent.putExtras(bundle);
		startActivity(intent);
	}

	@Override
	public void onMessageButtonClick() {
       startActivity( new Intent(this, MessageActivity.class));
	}

    public static Intent getOriginalMsgIntent(){
        return msgIntent;
    }

	@Override
	public void onTripHistoryButtonClick() {
		
		Fragment f = new OrderListFragment();
		Bundle bundle = new Bundle();
		bundle.putBoolean(Constants.BUNDLE_PARAM_TRIP_HISTORY, true);
		f.setArguments(bundle);
		
		_ft = _fm.beginTransaction();
		_ft.replace(R.id.contentFrameLayout, f);
		_ft.commit();
	}

	@Override
	public void onReturnButtonClick() {
		loadOrders();
	}

	@Override
	public void onFormListItemClick(String formName) {
		
		Bundle bundle = new Bundle();
		bundle.putString(Constants.BUNDLE_PARAM_FORM_NAME, formName);
		Intent intent = new Intent(this, FormActivity.class);
		intent.putExtras(bundle);
		startActivity(intent);
	}
	
	@Override
	public void onLogoffButtonClick() {
		
		Utils.setUserLoggedIn(this, false);
		
		Services.stopGpsService(this);
		Services.stopLocationService(this);
		Services.stopMessagesDownloadService(this);
		Services.stopOrdersDownloadService(this);
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {;
		}
		
		finish();
		System.exit(0);
	}
	
}