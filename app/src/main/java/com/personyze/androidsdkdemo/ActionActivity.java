package com.personyze.androidsdkdemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.personyze.androidsdk.PersonyzeAction;
import com.personyze.androidsdk.PersonyzeTracker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ActionActivity extends AppCompatActivity
{	/// This method is called by Android, when he starts my activity.
	@Override protected void onCreate(Bundle savedInstanceState)
	{	super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_action);

		// Take what parent activity handed me
		Intent intent = getIntent();
		String apiKey = getIntent().getStringExtra(MainActivity.EXTRA_API_KEY);
		PersonyzeAction action = (PersonyzeAction)intent.getSerializableExtra(DemoActivity.EXTRA_ACTION); // this is the selected action that i will present

		// Set activity title to action name
		setTitle(action.getName());

		// Install loading progress handlers
		WebView webView = findViewById(R.id.html);
		webView.setWebViewClient
		(	new WebViewClient()
			{	@Override public void onPageStarted(WebView view, String url, Bitmap favicon)
				{	findViewById(R.id.htmlLoading).setVisibility(View.VISIBLE);
				}

				@Override public void onPageFinished(WebView view, String url)
				{	findViewById(R.id.htmlLoading).setVisibility(View.GONE);
				}
			}
		);

		// Initialize the Personyze tracker. We need to do this before calling other PersonyzeTracker.inst.* methods.
		// It doesn't hurt calling the initializer several times.
		PersonyzeTracker.inst.initialize(this, apiKey);

		// Save html to file to find it in debugger. You can see what exactly Personyze is presenting in webView
		try
		{	String html = action.getContentHtmlDoc();
			if (html != null) // if Content-Type: text/html
			{	File file = new File(getCacheDir(), "content.html");
				FileOutputStream stream = new FileOutputStream(file);
				stream.write(html.getBytes());
				Log.w("Personyze", "HTML saved to "+file.getAbsolutePath()+" - so you can find it in debugger");
			}
		}
		catch (IOException error)
		{	// Couldn't save. Nevermind.
		}

		// Render the html
		if (action.getContentType().equals("text/html"))
		{	// This method renders the action html on provided WebView object.
			// Also it installs event listeners to sensitive regions inside the html, such as [x] (close) button, hyperlinks, and product cells.
			action.renderOnWebView
			(	webView,
				new PersonyzeTracker.Async<PersonyzeAction.Clicked>()
				{	@Override public void callback(final PersonyzeAction.Clicked clicked)
					{	// Got event. This means that you clicked some sensitive region
						// I report this event to Personyze. So there will be CTR and close-rate statistics, and widget contribution rate (products bought from this action)
						PersonyzeTracker.inst.reportActionClicked(clicked);
						// Also i show the event on screen
						((TextView)findViewById(R.id.textStatus)).setText(String.format("clicked=%s; arg=%s; href=%s", clicked.status, clicked.arg, clicked.href));
					}
				}
			);
		}
		else
		{	// If not html, show raw action text
			String html =
			(	"<html>"+
				"<head>"+
					"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">"+
					"<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"+
				"</head>"+
				"<body>"+
				"<plaintext>"+action.getContent()
			);
			webView.loadDataWithBaseURL("https://example.com/", html, "text/html; charset=utf-8", "utf-8", null);
		}
	}
}
