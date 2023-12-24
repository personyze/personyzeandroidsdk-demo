package com.personyze.androidsdkdemo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/// This is first screen that prompts to enter API key
public class MainActivity extends AppCompatActivity
{	/// "extra" name for intent (see below)
	public static final String EXTRA_API_KEY = "com.personyze.androidsdkdemo.API_KEY";
	private static final int ASK_POST_NOTIFICATIONS = 1;

	/// I will store API that you enter in text field to storage and reload it on next run.
	protected SharedPreferences storage;

	/// This method is called by Android, when he starts my application.
	@Override protected void onCreate(Bundle savedInstanceState)
	{	super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Request notifications permission
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
		{	int perm = ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS);
			if (perm != PackageManager.PERMISSION_GRANTED)
			{	ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.POST_NOTIFICATIONS}, ASK_POST_NOTIFICATIONS);
			}
		}

		// You entered API key. We will move on to the next screen
		((Button)findViewById(R.id.buttonKeyOk)).setOnClickListener
		(	(View view) ->
			{	String apiKey = ((EditText)findViewById(R.id.editKey)).getText().toString();
				// Store key
				SharedPreferences.Editor editor = storage.edit();
				editor.putString("API Key", apiKey);
				editor.apply();
				// Go to next activity
				Intent intent = new Intent(this, DemoActivity.class);
				intent.putExtra(EXTRA_API_KEY, apiKey);
				startActivity(intent);
			}
		);

		// Reload API key from storage
		storage = getSharedPreferences("Personyze Demo", Context.MODE_PRIVATE);
		String apiKey = storage.getString("API Key", "");
		((EditText)findViewById(R.id.editKey)).setText(apiKey);
	}
}
