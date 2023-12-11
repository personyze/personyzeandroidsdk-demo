package com.personyze.androidsdkdemo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;

import com.personyze.androidsdk.PersonyzeAction;
import com.personyze.androidsdk.PersonyzeCondition;
import com.personyze.androidsdk.PersonyzeResult;
import com.personyze.androidsdk.PersonyzeTracker;

import java.util.ArrayList;

/// This is "Demo" screen where you can test various API calls
public class DemoActivity extends AppCompatActivity
{	/// Event record class. I will accumulate events in array of this records before sending.
	private static class Event
	{	static final int USER_DATA = 0;
		static final int PRODUCT_VIEWED = 1;
		static final int PRODUCT_ADDED_TO_CART = 2;
		static final int PRODUCT_LIKED = 3;
		static final int PRODUCT_PURCHASED = 4;
		static final int PRODUCT_UNLIKED = 5;
		static final int PRODUCT_REMOVED_FROM_CART = 6;
		static final int PRODUCTS_PURCHASED = 7;
		static final int PRODUCTS_UNLIKED = 8;
		static final int PRODUCTS_REMOVED_FROM_CART = 9;
		static final int ARTICLE_VIEWED = 10;
		static final int ARTICLE_LIKED = 11;
		static final int ARTICLE_COMMENTED = 12;
		static final int ARTICLE_UNLIKED = 13;
		static final int ARTICLE_GOAL = 14;

		int what;
		String arg1;
		String arg2;

		Event(int what, String arg1, String arg2)
		{	this.what = what;
			this.arg1 = arg1;
			this.arg2 = arg2;
		}
	}

	/// Class for table row. When i receive conditions and actions from Personyze, i display them in a table.
	private static class ResultRow
	{	String text;
		boolean hasDetail;

		ResultRow(String text, boolean hasDetail)
		{	this.text = text;
			this.hasDetail = hasDetail;
		}
	}

	/// "extra" name for intent, to pass selected action to ActionActivity
	public static final String EXTRA_ACTION = "com.personyze.androidsdkdemo.ACTION";

	private View.OnClickListener clickedShowAction;
	private ArrayList<Event> events = new ArrayList<>();
	private PersonyzeResult personyzeResult;

	/// This method is called by Android, when he starts my activity.
	@Override protected void onCreate(Bundle savedInstanceState)
	{	super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_demo);

		// savedInstanceState
		if (savedInstanceState != null)
		{	((EditText)findViewById(R.id.editNav)).setText(savedInstanceState.getString("editNav"));
			((Spinner)findViewById(R.id.spinnerUserField)).setSelection(savedInstanceState.getInt("spinnerUserField"));
			((EditText)findViewById(R.id.editUserField)).setText(savedInstanceState.getString("editUserField"));
			((EditText)findViewById(R.id.editUserValue)).setText(savedInstanceState.getString("editUserValue"));
			((Spinner)findViewById(R.id.spinnerProductOrArticle)).setSelection(savedInstanceState.getInt("spinnerProductOrArticle"));
			((EditText)findViewById(R.id.editProductId)).setText(savedInstanceState.getString("editProductId"));
			updateView(); // set spinnerProductStatus options, and textEvents
			((Spinner)findViewById(R.id.spinnerProductStatus)).setSelection(savedInstanceState.getInt("spinnerProductStatus"));
		}
		else
		{	updateView(); // set textEvents
		}

		// Get API key from parent activity.
		final String apiKey = getIntent().getStringExtra(MainActivity.EXTRA_API_KEY);

		// Initialize the Personyze tracker. We need to do this before calling other PersonyzeTracker.inst.* methods.
		PersonyzeTracker.inst.initialize(apiKey);

		// spinnerUserField
		final Spinner spinnerUserField = findViewById(R.id.spinnerUserField);
		spinnerUserField.setOnItemSelectedListener
		(	new AdapterView.OnItemSelectedListener()
			{	@Override public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
				{	findViewById(R.id.editUserField).setVisibility(position==4 ? View.VISIBLE : View.GONE);
				}

				@Override public void onNothingSelected(AdapterView<?> parentView)
				{
				}
			}
		);

		// spinnerProductOrArticle
		final Context context = this;
		final Spinner spinnerProductOrArticle = findViewById(R.id.spinnerProductOrArticle);
		spinnerProductOrArticle.setOnItemSelectedListener
		(	new AdapterView.OnItemSelectedListener()
			{	@Override public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id)
				{	updateView();
				}

				@Override public void onNothingSelected(AdapterView<?> parentView)
				{
				}
			}
		);

		// clickedShowAction
		clickedShowAction = new View.OnClickListener()
		{	@Override public void onClick(View v)
			{	TableRow tr = (TableRow)v.getParent();
				int nRow = (Integer)tr.getTag();
				int n = nRow - (2 + personyzeResult.conditions.size());
				if (n >= 0 && n < personyzeResult.actions.size())
				{	// intent
					Intent intent = new Intent(context, ActionActivity.class);
					intent.putExtra(MainActivity.EXTRA_API_KEY, apiKey);
					intent.putExtra(EXTRA_ACTION, personyzeResult.actions.get(n));
					startActivity(intent);
				}
			}
		};

		// Laod current state. This shows currently matching conditions and actions in a table.
		trackerSend(false);
	}

	/// This method is called by Android, when he (temporarily?) shuts down this activity.
	/// @param outState - I will restore the current state from this object in onCreate().
	@Override protected void onSaveInstanceState(Bundle outState)
	{	super.onSaveInstanceState(outState);
		outState.putString("editNav", ((EditText)findViewById(R.id.editNav)).getText().toString());
		outState.putInt("spinnerUserField", ((Spinner)findViewById(R.id.spinnerUserField)).getSelectedItemPosition());
		outState.putString("editUserField", ((EditText)findViewById(R.id.editUserField)).getText().toString());
		outState.putString("editUserValue", ((EditText)findViewById(R.id.editUserValue)).getText().toString());
		outState.putInt("spinnerProductOrArticle", ((Spinner)findViewById(R.id.spinnerProductOrArticle)).getSelectedItemPosition());
		outState.putString("editProductId", ((EditText)findViewById(R.id.editProductId)).getText().toString());
		outState.putInt("spinnerProductStatus", ((Spinner)findViewById(R.id.spinnerProductStatus)).getSelectedItemPosition());
	}

	/// You clicked "Start new session".
	public void clickedNewSession(View view)
	{	// I call tracker API
		PersonyzeTracker.inst.startNewSession(this);
		setResultRows(new ArrayList<ResultRow>());
	}

	/// You clicked "Clear cache".
	public void clickedClearCache(View view)
	{	// I call tracker API
		PersonyzeTracker.inst.clearCache(this).addOnFailureListener
		(	new OnFailureListener()
			{	@Override public void onFailure(@NonNull Exception e)
				{	Toast.makeText(DemoActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
				}
			}
		);
	}

	/// You clicked "Navigate to document".
	public void clickedNav(View view)
	{	// I only open additional settings, if document name is not empty.
		String doc = ((EditText)findViewById(R.id.editNav)).getText().toString();
		if (!doc.isEmpty())
		{	findViewById(R.id.panEvents).setVisibility(View.VISIBLE);
		}
	}

	/// You clicked "Add user profile data".
	public void clickedUser(View view)
	{	int nField = ((Spinner)findViewById(R.id.spinnerUserField)).getSelectedItemPosition();
		String field = nField==0 ? "email" : nField==1 ? "first_name" : nField==2 ? "last_name" : nField==3 ? "phone" : null;
		if (field == null)
		{	// Custom field
			field = ((EditText)findViewById(R.id.editUserField)).getText().toString();
		}
		if (!field.isEmpty())
		{	String value = ((EditText)findViewById(R.id.editUserValue)).getText().toString();
			// Currently i only accumulate this event in "events". I will send it when you click "Proceed navigation", or "Events only".
			events.add(new Event(Event.USER_DATA, field, value));
			// Update text "1 event queued"
			updateView();
		}
	}

	/// You clicked "Add product interation".
	public void clickedProduct(View view)
	{	boolean isProduct = ((Spinner)findViewById(R.id.spinnerProductOrArticle)).getSelectedItemPosition() == 0;
		String productId = ((EditText)findViewById(R.id.editProductId)).getText().toString();
		int nStatus = ((Spinner)findViewById(R.id.spinnerProductStatus)).getSelectedItemPosition();
		if (!productId.isEmpty())
		{	switch (nStatus)
			{	case 0: nStatus = isProduct ? Event.PRODUCT_VIEWED : Event.ARTICLE_VIEWED; break;
				case 1: nStatus = isProduct ? Event.PRODUCT_ADDED_TO_CART : Event.ARTICLE_COMMENTED; break;
				case 2: nStatus = isProduct ? Event.PRODUCT_LIKED : Event.ARTICLE_LIKED; break;
				case 3: nStatus = isProduct ? Event.PRODUCT_PURCHASED : Event.ARTICLE_GOAL; break;
				case 4: nStatus = Event.PRODUCT_REMOVED_FROM_CART; break;
				case 5: nStatus = Event.PRODUCT_UNLIKED; break;
			}
			// Currently i only accumulate this event in "events". I will send it when you click "Proceed navigation", or "Events only".
			events.add(new Event(nStatus, productId, null));
			// Update text "1 event queued"
			updateView();
		}
	}

	/// You clicked "All products (in cart) bought".
	public void clickedBought(View view)
	{	// Currently i only accumulate this event in "events". I will send it when you click "Proceed navigation", or "Events only".
		events.add(new Event(Event.PRODUCTS_PURCHASED, null, null));
		// Update text "1 event queued"
		updateView();
	}

	/// You clicked "Empty cart".
	public void clickedEmptyCart(View view)
	{	// Currently i only accumulate this event in "events". I will send it when you click "Proceed navigation", or "Events only".
		events.add(new Event(Event.PRODUCTS_REMOVED_FROM_CART, null, null));
		// Update text "1 event queued"
		updateView();
	}

	/// You clicked "Unlike all".
	public void clickedUnlike(View view)
	{	// Currently i only accumulate this event in "events". I will send it when you click "Proceed navigation", or "Events only".
		events.add(new Event(Event.PRODUCTS_UNLIKED, null, null));
		// Update text "1 event queued"
		updateView();
	}

	/// Update visual elements.
	void updateView()
	{	// textEvents: Update text "1 event queued"
		((TextView)findViewById(R.id.textEvents)).setText(events.size()+" events");

		// spinnerProductStatus: If you selected "Article", load relevant options to spinnerProductStatus.
		final Spinner spinnerProductStatus = findViewById(R.id.spinnerProductStatus);
		boolean isProduct = ((Spinner)findViewById(R.id.spinnerProductOrArticle)).getSelectedItemPosition() == 0;
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, isProduct ? R.array.product_status : R.array.article_status, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerProductStatus.setAdapter(adapter);
	}

	/// You clicked "Cancel navigation".
	public void clickedCancel(View view)
	{	// Hide the additional settings.
		findViewById(R.id.panEvents).setVisibility(View.GONE);
		// Clear queued events.
		events.clear();
		// Update text "0 events queued"
		updateView();
	}

	/// You clicked "Log only events (don't log navigation to a new document, that is, events are related to current document)".
	public void clickedLogEvents(View view)
	{	trackerSend(false);
	}

	/// You clicked "Proceed navigation".
	public void clickedGo(View view)
	{	trackerSend(true);
	}

	/// Send pending events and navigation (if any). And get the currently matching conditions and actions. Show them in a table.
	void trackerSend(boolean withNavigation)
	{	// Hide additional settings
		findViewById(R.id.panEvents).setVisibility(View.GONE);
		// Call the "navigate" API
		if (withNavigation)
		{	PersonyzeTracker.inst.navigate(((EditText)findViewById(R.id.editNav)).getText().toString());
		}
		// Translate pending events to API calls
		for (Event e : events)
		{	switch (e.what)
			{	case Event.USER_DATA: PersonyzeTracker.inst.logUserData(e.arg1, e.arg2); break;
				case Event.PRODUCT_VIEWED: PersonyzeTracker.inst.productViewed(e.arg1); break;
				case Event.PRODUCT_ADDED_TO_CART: PersonyzeTracker.inst.productAddedToCart(e.arg1); break;
				case Event.PRODUCT_LIKED: PersonyzeTracker.inst.productLiked(e.arg1); break;
				case Event.PRODUCT_PURCHASED: PersonyzeTracker.inst.productPurchased(e.arg1); break;
				case Event.PRODUCT_UNLIKED: PersonyzeTracker.inst.productUnliked(e.arg1); break;
				case Event.PRODUCT_REMOVED_FROM_CART: PersonyzeTracker.inst.productRemovedFromCart(e.arg1); break;
				case Event.PRODUCTS_PURCHASED: PersonyzeTracker.inst.productsPurchased(); break;
				case Event.PRODUCTS_UNLIKED: PersonyzeTracker.inst.productsUnliked(); break;
				case Event.PRODUCTS_REMOVED_FROM_CART: PersonyzeTracker.inst.productsRemovedFromCart(); break;
				case Event.ARTICLE_VIEWED: PersonyzeTracker.inst.articleViewed(e.arg1); break;
				case Event.ARTICLE_LIKED: PersonyzeTracker.inst.articleLiked(e.arg1); break;
				case Event.ARTICLE_COMMENTED: PersonyzeTracker.inst.articleCommented(e.arg1); break;
				case Event.ARTICLE_UNLIKED: PersonyzeTracker.inst.articleUnliked(e.arg1); break;
				case Event.ARTICLE_GOAL: PersonyzeTracker.inst.articleGoal(e.arg1); break;
			}
		}
		// Clear pending events
		events.clear();
		// Update text "0 events queued"
		updateView();
		// Clear the current error message and current result (conditions and actions table)
		findViewById(R.id.textError).setVisibility(View.GONE);
		setResultRows(new ArrayList<ResultRow>());
		// Start loading
		findViewById(R.id.loading).setVisibility(View.VISIBLE);
		findViewById(R.id.buttonNav).setVisibility(View.GONE);
		PersonyzeTracker.inst.getResult(this).addOnCompleteListener
		(	new OnCompleteListener<PersonyzeResult>()
			{	@Override public void onComplete(@NonNull Task<PersonyzeResult> task)
				{	if (task.isSuccessful())
					{	// Loaded successfully
						findViewById(R.id.loading).setVisibility(View.GONE);
						findViewById(R.id.buttonNav).setVisibility(View.VISIBLE);
						// Save the result to personyzeResult to use it later
						personyzeResult = task.getResult();
						// Present the conditions/actions table
						ArrayList<ResultRow> rows = new ArrayList<>();
						rows.add(new ResultRow("**"+personyzeResult.conditions.size()+" conditions**", false));
						for (PersonyzeCondition item : personyzeResult.conditions)
						{   rows.add(new ResultRow(item.getName(), false));
						}
						rows.add(new ResultRow("**"+personyzeResult.actions.size()+" actions**", false));
						for (PersonyzeAction item : personyzeResult.actions)
						{   rows.add(new ResultRow((item.getName()) + " ("+item.getContentType()+")", true));
						}
						setResultRows(rows);
					}
					else
					{	// Error with the requested things
						findViewById(R.id.loading).setVisibility(View.GONE);
						findViewById(R.id.buttonNav).setVisibility(View.VISIBLE);
						// Present red error text
						((TextView)findViewById(R.id.textError)).setText(task.getException().getLocalizedMessage());
						findViewById(R.id.textError).setVisibility(View.VISIBLE);
					}
				}
			}
		);
	}

	/// Populate result table with rows
	void setResultRows(ArrayList<ResultRow> rows)
	{	TableLayout table = findViewById(R.id.tableRes);
		table.removeAllViews();
		int i = 0;
		for (ResultRow row : rows)
		{	// tr
			TableRow tr = new TableRow(this);
			tr.setTag(i++);
			tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT));
			table.addView(tr);
			// text
			TextView text = new TextView(this);
			text.setText(row.text);
			text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18.0f);
			tr.addView(text);
			// buttonShow
			if (row.hasDetail)
			{	ImageButton buttonShow = new ImageButton(this);
				buttonShow.setImageResource(R.drawable.ic_visibility_black_24dp);
				buttonShow.setBackgroundColor(Color.TRANSPARENT);
				buttonShow.setOnClickListener(clickedShowAction);
				tr.addView(buttonShow);
			}
		}
	}
}
