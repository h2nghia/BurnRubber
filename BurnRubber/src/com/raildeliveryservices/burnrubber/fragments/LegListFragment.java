package com.raildeliveryservices.burnrubber.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.TextView;
import android.widget.TimePicker;

import com.raildeliveryservices.burnrubber.Constants;
import com.raildeliveryservices.burnrubber.R;
import com.raildeliveryservices.burnrubber.WebServiceConstants;
import com.raildeliveryservices.burnrubber.adapters.LegListCursorAdapter;
import com.raildeliveryservices.burnrubber.data.Leg;
import com.raildeliveryservices.burnrubber.data.Order;
import com.raildeliveryservices.burnrubber.utils.Utils;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class LegListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = LegListFragment.class.getSimpleName();

	private static final int LOADER_ORDER = -1;
	private static final int LOADER_LEGS = -2;
	
	private Activity _activity;
	private long _orderId;
	private boolean _readOnly;
	private ExpandableListView _listView;
	private Button _messageButton;
	private Button _directionsButton;
	private Button _startFileButton;
	private Button _orderImageButton;
	private Button _returnButton;
	private TextView _fileNoText;
	private TextView _hazmatText;
	private TextView _apptTimeText;
	private TextView _voyageNoText;
	private TextView _moveTypeText;
	private EditText _containerNoEditText;
	private EditText _chassisNoEditText;
	private LegListCursorAdapter _listAdapter;
	private Callbacks _callbacks;
	
	public interface Callbacks {
		public void onMessageButtonClick();
		public void onDirectionsButtonClick();
		public void onReturnButtonClick();
		public void onOutboundFormClick(long legId);
		public void onOrderImageButtonClick();
	}
	
	public LegListFragment() {
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		_activity = getActivity();
		
		_messageButton = (Button) _activity.findViewById(R.id.messageButton);
		_directionsButton = (Button) _activity.findViewById(R.id.directionsButton);
		_startFileButton = (Button) _activity.findViewById(R.id.startFileButton);
		_orderImageButton = (Button) _activity.findViewById(R.id.orderImageButton);
		_returnButton = (Button) _activity.findViewById(R.id.returnButton);
		
		_fileNoText = (TextView) _activity.findViewById(R.id.fileNoText);
		_hazmatText = (TextView) _activity.findViewById(R.id.hazmatText);
		_apptTimeText = (TextView) _activity.findViewById(R.id.apptTimeText);
		_voyageNoText = (TextView) _activity.findViewById(R.id.voyageNoText);
		_moveTypeText = (TextView) _activity.findViewById(R.id.moveTypeText);
		_containerNoEditText = (EditText) _activity.findViewById(R.id.containerNoEditText);
		_chassisNoEditText = (EditText) _activity.findViewById(R.id.chassisNoEditText);
		
		_containerNoEditText.setOnFocusChangeListener(_focusChangeListener);
		_chassisNoEditText.setOnFocusChangeListener(_focusChangeListener);
		
		_messageButton.setOnClickListener(_buttonListener);
		_directionsButton.setOnClickListener(_buttonListener);
		_startFileButton.setOnClickListener(_buttonListener);
		_orderImageButton.setOnClickListener(_buttonListener);
		_returnButton.setOnClickListener(_buttonListener);
		
		Bundle bundle = getArguments();
		_orderId = bundle.getLong(Constants.BUNDLE_PARAM_ORDER_ID);
		_readOnly = bundle.getBoolean(Constants.BUNDLE_PARAM_READ_ONLY);
		
		if (_readOnly) {
			_containerNoEditText.setEnabled(false);
			_chassisNoEditText.setEnabled(false);
		}
		
		_listView = (ExpandableListView) _activity.findViewById(R.id.legList);
		_listView.setChoiceMode(ExpandableListView.CHOICE_MODE_SINGLE);
		_listAdapter = new LegListCursorAdapter(_activity, _readOnly);
		_listAdapter.setButtonListener(_listAdapterButtonListener);
		_listView.setAdapter(_listAdapter);
		_listView.setOnGroupExpandListener(new OnGroupExpandListener() {
			@Override
			public void onGroupExpand(int groupPosition) {
				for (int i = 0; i < _listAdapter.getGroupCount(); i++) {
					if (groupPosition != i) {
						_listView.collapseGroup(i);
					}
				}
			}
		});
		
		Loader<Cursor> loader = getLoaderManager().getLoader(LOADER_ORDER);
		if (loader != null && !loader.isReset()) {
			getLoaderManager().restartLoader(LOADER_ORDER, null, this);
		} else {
			getLoaderManager().initLoader(LOADER_ORDER, null, this);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.leg_list_fragment, container, false);
	}
		
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		try {
			_callbacks = (Callbacks) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement Callbacks");
		}
	}
	
	private void startFile() {
		Uri uri = Uri.withAppendedPath(Order.CONTENT_URI, String.valueOf(_orderId));
		ContentValues values = new ContentValues();
		values.put(Order.Columns.STARTED_FLAG, 1);
		_activity.getContentResolver().update(uri, values, null, null);

        JSONObject requestJson = new JSONObject();
        try {
            requestJson.accumulate(WebServiceConstants.FIELD_DRIVER_NO, Utils.getDriverNo(_activity));
            requestJson.accumulate(WebServiceConstants.FIELD_IN_OUT_FLAG, "I");
            requestJson.accumulate(WebServiceConstants.FIELD_FILE_NO, _fileNoText.getText());
            requestJson.accumulate(WebServiceConstants.FIELD_LEG_NO, "1");
            requestJson.accumulate(WebServiceConstants.FIELD_LABEL, "START FILE");
            requestJson.accumulate(WebServiceConstants.FIELD_FORM_NAME, "CANNED");
            requestJson.accumulate(WebServiceConstants.FIELD_MESSAGE_TEXT, _fileNoText.getText());
            requestJson.accumulate(WebServiceConstants.FIELD_CLIENT_DATETIME, Utils.getCurrentDateTime(Constants.ServerDateFormat));

            Utils.sendMessageToServer(_activity, WebServiceConstants.URL_CREATE_MESSAGE, requestJson);
        } catch (Exception e) {
            Log.e(LOG_TAG, "startFile(): "+e.getMessage());
            Log.e(LOG_TAG, "URL: " + WebServiceConstants.URL_CREATE_MESSAGE);
            Log.e(LOG_TAG, "JSON: " + requestJson.toString());

        }

		Loader<Cursor> loader = getLoaderManager().getLoader(LOADER_ORDER);
		if (loader != null && !loader.isReset()) {
			getLoaderManager().restartLoader(LOADER_ORDER, null, this);
		} else {
			getLoaderManager().initLoader(LOADER_ORDER, null, this);
		}

        Log.i(LOG_TAG, "Uri in StartFile(): "+uri.toString());
	}

    /*
    Driver cannot Arrive/Depart if Container or Chassis information are missing.t
     */
	private boolean canArriveDepart() {
		if (TextUtils.isEmpty(_containerNoEditText.getText()) ||
				TextUtils.isEmpty(_chassisNoEditText.getText())) {
			return false;
		} else {
			return true;
		}
	}
	
	private LegListCursorAdapter.AdapterCallbacks _listAdapterButtonListener = new LegListCursorAdapter.AdapterCallbacks() {
		
		@Override
		public void onArriveDepartButtonClick(View v, long legId) {
			
			if (!canArriveDepart()) {
				AlertDialog.Builder alertBuilder = new AlertDialog.Builder(_activity);
				alertBuilder.setTitle(_activity.getString(R.string.container_chassis_error_title));
				alertBuilder.setMessage(_activity.getString(R.string.container_chassis_error_message));
				alertBuilder.setCancelable(false);
				alertBuilder.setPositiveButton(_activity.getString(R.string.ok), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				alertBuilder.create().show();
				
				return;
			}
			
			SimpleDateFormat dateTime = Constants.ServerDateFormat;
			Uri uri = Uri.withAppendedPath(Leg.CONTENT_URI, String.valueOf(legId));
			ContentValues values= new ContentValues();

			switch (v.getId()) {
				case R.id.arriveFromButton:
					values.put(Leg.Columns.ARRIVE_FROM_DATE_TIME, dateTime.format(new Date()));
                    sendArriveToServer(legId);
					break;
				case R.id.departFromButton:
					values.put(Leg.Columns.DEPART_FROM_DATE_TIME, dateTime.format(new Date()));
                    sendDepartToServer(legId);
					break;
				case R.id.arriveToButton:
					values.put(Leg.Columns.ARRIVE_TO_DATE_TIME, dateTime.format(new Date()));
                    sendArriveToServer(legId);
					break;
				case R.id.departToButton:
					values.put(Leg.Columns.DEPART_TO_DATE_TIME, dateTime.format(new Date()));
                    sendEndFileToServer();
					values.put(Leg.Columns.COMPLETED_FLAG, true);
					break;
			}

            Log.i(LOG_TAG, "Uri in onArriveDepartButtonClick: " + uri.toString());
            Log.i(LOG_TAG, "values in onArriveDepartButtonClick: " + values.toString());
			_activity.getContentResolver().update(uri, values, null, null);
		}

        private void sendArriveToServer(long legId){
            Log.d(LOG_TAG, "In sendArriveToServer...");
            Uri uri = Uri.withAppendedPath(Order.CONTENT_URI, String.valueOf(_orderId));
            JSONObject requestJson = new JSONObject();
            try {
                requestJson.accumulate(WebServiceConstants.FIELD_DRIVER_NO, Utils.getDriverNo(_activity));
                requestJson.accumulate(WebServiceConstants.FIELD_IN_OUT_FLAG, "I");
                requestJson.accumulate(WebServiceConstants.FIELD_FILE_NO, _fileNoText.getText());
                requestJson.accumulate(WebServiceConstants.FIELD_LEG_NO, (int) legId);
                requestJson.accumulate(WebServiceConstants.FIELD_LABEL, "ARRIVE");
                requestJson.accumulate(WebServiceConstants.FIELD_FORM_NAME, "CANNED");
                requestJson.accumulate(WebServiceConstants.FIELD_MESSAGE_TEXT, _fileNoText.getText());
                requestJson.accumulate(WebServiceConstants.FIELD_CLIENT_DATETIME, Utils.getCurrentDateTime(Constants.ServerDateFormat));

    //            Utils.sendMessageToServer(_activity, WebServiceConstants.URL_CREATE_MESSAGE, requestJson);
            } catch (Exception e) {
                Log.e(LOG_TAG, "startFile(): "+e.getMessage());
                Log.e(LOG_TAG, "URL: " + WebServiceConstants.URL_CREATE_MESSAGE);
                Log.e(LOG_TAG, "JSON: " + requestJson.toString());

            }
            Log.i(LOG_TAG, "Uri in StartFile(): "+uri.toString());
            Log.i(LOG_TAG, "JSON: " + requestJson.toString());
        }

        private void sendDepartToServer(long legId){

        }

        private void sendEndFileToServer(){

        }

		@Override
		public void onOutboundFormClick(long legId) {
			_callbacks.onOutboundFormClick(legId);
		}

		@Override
		public void onEditArriveDepartButtonClick(View v, long legId) {
			
			final Dialog dialog = new Dialog(_activity);
			final View buttonView = v;
			final long id = legId;
			
			dialog.setContentView(R.layout.edit_date_dialog);
			
			switch (v.getId()) {
			case R.id.arriveFromButton: case R.id.arriveToButton:
				dialog.setTitle(_activity.getString(R.string.edit_arrive_date_time_title));
				break;
			case R.id.departFromButton: case R.id.departToButton:
				dialog.setTitle(_activity.getString(R.string.edit_depart_date_time_title));
				break;
			}
			
			Button cancelButton = (Button) dialog.findViewById(R.id.cancelButton);
			Button saveButton = (Button) dialog.findViewById(R.id.saveButton);
			
			cancelButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.cancel();
				}
			});
			
			saveButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					DatePicker datePicker = (DatePicker) dialog.findViewById(R.id.datePicker);
					TimePicker timePicker = (TimePicker) dialog.findViewById(R.id.timePicker);
					
					int year = datePicker.getYear();
					int month = datePicker.getMonth();
					int day = datePicker.getDayOfMonth();
					int hour = timePicker.getCurrentHour();
					int min = timePicker.getCurrentMinute();
					
					updateArriveDepartDateTime(buttonView, id, year, month, day, hour, min);
					
					dialog.dismiss();
				}
			});
			
			dialog.show();
		}
	};
	
	private void updateArriveDepartDateTime(View v, long legId, int year, int month, int day, int hour, int min) {
		
		SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS", Locale.US);
		Calendar cal = Calendar.getInstance();
		cal.set(year, month, day, hour, min);
		
		String dateTime = dateTimeFormat.format(cal.getTime());
		
		Uri uri = Uri.withAppendedPath(Leg.CONTENT_URI, String.valueOf(legId));
		ContentValues values = new ContentValues();
		
		switch (v.getId()) {
		case R.id.arriveFromButton:
			values.put(Leg.Columns.ARRIVE_FROM_DATE_TIME, dateTime);
			break;
		case R.id.arriveToButton:
			values.put(Leg.Columns.ARRIVE_TO_DATE_TIME, dateTime);
			break;
		case R.id.departFromButton:
			values.put(Leg.Columns.DEPART_FROM_DATE_TIME, dateTime);
			break;
		case R.id.departToButton:
			values.put(Leg.Columns.DEPART_TO_DATE_TIME, dateTime);
			break;
		}
		
		_activity.getContentResolver().update(uri, values, null, null);
	}
	
	private OnClickListener _buttonListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
				case R.id.messageButton:
					_callbacks.onMessageButtonClick();
					break;
				case R.id.directionsButton:
					_callbacks.onDirectionsButtonClick();
					break;
				case R.id.startFileButton:
					startFile();
					break;
				case R.id.orderImageButton:
					_callbacks.onOrderImageButtonClick();
					break;
				case R.id.returnButton:
					_callbacks.onReturnButtonClick();
					break;
			}
		}
	};
	
	private OnFocusChangeListener _focusChangeListener = new OnFocusChangeListener() {
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			
			if (hasFocus) {
				return;
			}
			
			Uri uri = Uri.withAppendedPath(Order.CONTENT_URI, String.valueOf(_orderId));
			ContentValues values = new ContentValues();
			
			switch (v.getId()) {
				case R.id.containerNoEditText:
					values.put(Order.Columns.CONTAINER_NO, _containerNoEditText.getText().toString());
					break;
				case R.id.chassisNoEditText:
					values.put(Order.Columns.CHASSIS_NO, _chassisNoEditText.getText().toString());
					break;
			}
			
			_activity.getContentResolver().update(uri, values, null, null);
		}
	};
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		
		Uri uri = null;
		String selection = null;
		String[] projection = null;
		String sortOrder = null;
		
		switch (id) {
			case LOADER_ORDER:
				uri = Order.CONTENT_URI;
				selection = Order.Columns._ID + " = " + _orderId;
				projection = new String[] { Order.Columns.FILE_NO,
											Order.Columns.PARENT_FILE_NO,
											Order.Columns.HAZMAT_FLAG,
											Order.Columns.APPT_DATE_TIME,
											Order.Columns.VOYAGE_NO,
											Order.Columns.MOVE_TYPE,
											Order.Columns.CONTAINER_NO,
											Order.Columns.CHASSIS_NO,
											Order.Columns.STARTED_FLAG };
				break;
			case LOADER_LEGS:
				uri = Leg.CONTENT_URI;
				selection = Leg.Columns.ORDER_ID + " = " + _orderId;
				projection = new String[] { Leg.Columns._ID,
							   				Leg.Columns.LEG_NO,
							   				Leg.Columns.COMPLETED_FLAG };
				sortOrder = Leg.Columns.LEG_NO;
				break;
		}
		
		
		return new CursorLoader(_activity, uri, projection, selection, null, sortOrder);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		
		if (loader.getId() == LOADER_ORDER) {
			cursor.moveToFirst();
			_fileNoText.setText(cursor.getString(cursor.getColumnIndex(Order.Columns.FILE_NO)));
			
			if (cursor.getInt(cursor.getColumnIndex(Order.Columns.HAZMAT_FLAG)) == 1) {
				_hazmatText.setVisibility(View.VISIBLE);
			} else {
				_hazmatText.setVisibility(View.GONE);
			}
			
			_apptTimeText.setText(cursor.getString(cursor.getColumnIndex(Order.Columns.APPT_DATE_TIME)));
			_voyageNoText.setText(cursor.getString(cursor.getColumnIndex(Order.Columns.VOYAGE_NO)));
			_moveTypeText.setText(cursor.getString(cursor.getColumnIndex(Order.Columns.MOVE_TYPE)));
			_containerNoEditText.setText(cursor.getString(cursor.getColumnIndex(Order.Columns.CONTAINER_NO)));
			_chassisNoEditText.setText(cursor.getString(cursor.getColumnIndex(Order.Columns.CHASSIS_NO)));
			
			boolean startedFlag = cursor.getInt(cursor.getColumnIndex(Order.Columns.STARTED_FLAG)) == 1 ? true : false;
			
			if (!_readOnly) {
				if (startedFlag) {
					_startFileButton.setEnabled(false);
					_startFileButton.setTextColor(_activity.getResources().getColor(R.color.green));
					_containerNoEditText.setEnabled(true);
					_chassisNoEditText.setEnabled(true);
					_listAdapter.setStartedFlag(true);
				} else {
					_startFileButton.setEnabled(true);
					_startFileButton.setTextColor(_activity.getResources().getColor(R.color.red));
					_containerNoEditText.setEnabled(false);
					_chassisNoEditText.setEnabled(false);
					_listAdapter.setStartedFlag(false);
				}
			} else {
				_startFileButton.setEnabled(false);
			}
			
			Loader<Cursor> l = getLoaderManager().getLoader(LOADER_LEGS);
			if (l != null && !l.isReset()) {
				getLoaderManager().restartLoader(LOADER_LEGS, null, this);
			} else {
				getLoaderManager().initLoader(LOADER_LEGS, null, this);
			}
		} else if (loader.getId() == LOADER_LEGS) {
			_listAdapter.setGroupCursor(cursor);
			cursor.moveToFirst();
			int i = 0;
            try {
                do {
                    if (cursor.getInt(cursor.getColumnIndex(Leg.Columns.COMPLETED_FLAG)) != 1) {
                        _listView.expandGroup(i);
                        break;
                    }
                    i += 1;
                } while (cursor.moveToNext());
            } catch (CursorIndexOutOfBoundsException e) {
                Log.e(LOG_TAG, "Cursor index out of bounds:" + e.getMessage());
            }
		}
        try {
			InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE); 
			inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
		} catch (Exception e) {

		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		
		int id = loader.getId();
		
		if (id != LOADER_ORDER || id != LOADER_LEGS) {
			try {
				_listAdapter.setChildrenCursor(id, null);
			} catch (NullPointerException e) {
				
			}
		}

		_listAdapter.setGroupCursor(null);
	}
}
