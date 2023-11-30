package com.personyze.androidsdk;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class PersonyzeHttp
{	private final String GATEWAY_URL;
	private final String USER_AGENT;
	private RequestQueue requestQueue;
	private String httpAuth;
	private String apiKeyInUse;
	public String apiKey;

	PersonyzeHttp(String GATEWAY_URL, String USER_AGENT)
	{	this.GATEWAY_URL = GATEWAY_URL;
		this.USER_AGENT = USER_AGENT;
	}

	public void setContext(Context context)
	{	requestQueue = Volley.newRequestQueue(context);
	}

	public void fetch(String path, final String postData, final PersonyzeTracker.AsyncResult<String> asyncResult)
	{	if (apiKey != null)
		{	if (!apiKey.equals(apiKeyInUse))
			{	if (apiKey.length() != 40)
				{	asyncResult.error(new PersonyzeError("API Key must be 40 characters", PersonyzeError.Type.MALFORMED_API_KEY));
					return;
				}
				apiKeyInUse = apiKey;
				String creds = "api:" + apiKeyInUse;
				httpAuth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.DEFAULT);
			}
			requestQueue.add
			(	new StringRequest
				(	postData==null ? Request.Method.GET : Request.Method.POST,
					GATEWAY_URL+path,
					new Response.Listener<String>()
					{	@Override public void onResponse(String response)
						{	if (response!=null && response.length()>0)
							{	asyncResult.success(response);
							}
							else
							{	asyncResult.error(null);
							}
						}
					},
					new Response.ErrorListener()
					{	@Override public void onErrorResponse(VolleyError error)
						{	String message = null;
							PersonyzeError.Type type = PersonyzeError.Type.OTHER;
							if (error.networkResponse != null)
							{	if (error.networkResponse.statusCode == 500)
								{	type = PersonyzeError.Type.HTTP_500;
									try
									{	message = new String(error.networkResponse.data, "utf-8");
									}
									catch (UnsupportedEncodingException e)
									{	// not interesting
									}
								}
								else if (error.networkResponse.statusCode == 503)
								{	type = PersonyzeError.Type.HTTP_503;
									message = "Service temporarily unavailable";
								}
								else if (error.networkResponse.statusCode == 401)
								{	type = PersonyzeError.Type.HTTP_401;
									message = "Invalid API key";
								}
							}
							if (message == null)
							{	message = error.getLocalizedMessage();
								if (message == null)
								{	message = "HTTP request failed";
								}
							}
							asyncResult.error(new PersonyzeError(message, type));
						}
					}
				)
				{	@Override public String getBodyContentType()
					{	return "application/json; charset=utf-8";
					}

					@Override public byte[] getBody() throws AuthFailureError
					{	if (postData != null)
						{	try
							{	return postData.getBytes("utf-8");
							}
							catch (UnsupportedEncodingException e)
							{	throw new AuthFailureError("Encoding problem");
							}
						}
						return null;
					}

					@Override public Map<String, String> getHeaders()
					{	HashMap<String, String> params = new HashMap<>();
						params.put("Authorization", httpAuth);
						params.put("User-Agent", USER_AGENT);
						return params;
					}
				}
			);
		}
		else
		{	asyncResult.error(new PersonyzeError("PersonyzeTracker not initialized"));
		}
	}
}
